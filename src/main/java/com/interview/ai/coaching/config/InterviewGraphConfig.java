package com.interview.ai.coaching.config;

import com.interview.ai.coaching.graph.Category;
import com.interview.ai.coaching.graph.InterviewState;
import com.interview.ai.coaching.nodes.*;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.Command;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Configuration class for the Interview Coaching Graph.
 * 
 * This class is responsible for configuring the langgraph4j graph
 * infrastructure used in the interview coaching application. It creates
 * and wires together all graph components with conditional routing.
 * 
 * <h3>Graph Flow</h3>
 * <pre>
 * START → ClassifierAgent → (conditional routing)
 * 
 *   ├─ Category.JAVA      → JavaInterviewAgent → END
 *   ├─ Category.SPRING    → SpringInterviewAgent → END
 *   ├─ Category.AWS       → AwsInterviewAgent → END
 *   └─ Category.UNKNOWN   → InterviewAgent → END
 * </pre>
 * 
 * <p>The classifier determines the question category, then routes
 * to the appropriate specialized agent. Each expert agent generates
 * an answer and terminates at END.</p>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see StateGraph
 * @see CompiledGraph
 * @see ClassifierAgentNode
 */
@Slf4j
@Configuration
public class InterviewGraphConfig {

    // ==================== Node Beans ====================

    @Bean
    public ClassifierAgentNode classifierAgentNode(ChatClient chatClient) {
        return new ClassifierAgentNode(chatClient);
    }

    @Bean
    public JavaInterviewAgentNode javaInterviewAgentNode(ChatClient chatClient) {
        return new JavaInterviewAgentNode(chatClient);
    }

    @Bean
    public SpringInterviewAgentNode springInterviewAgentNode(ChatClient chatClient) {
        return new SpringInterviewAgentNode(chatClient);
    }

    @Bean
    public AwsInterviewAgentNode awsInterviewAgentNode(ChatClient chatClient) {
        return new AwsInterviewAgentNode(chatClient);
    }

    @Bean
    public InterviewAgentNode interviewAgentNode(ChatClient chatClient) {
        return new InterviewAgentNode(chatClient);
    }

    // ==================== Graph Compilation ====================

    /**
     * Creates and compiles the interview coaching graph with conditional routing.
     * 
     * <p>Graph structure:</p>
     * <pre>
     * START → classifier → { java_agent | spring_agent | aws_agent | general_agent }
     *                                                    ↓
     *                                                    END
     * </pre>
     * 
     * @param classifierNode the classifier agent node
     * @param javaNode the Java expert node
     * @param springNode the Spring expert node
     * @param awsNode the AWS expert node
     * @param fallbackNode the general fallback node
     * @return the compiled graph ready for execution
     * @throws org.bsc.langgraph4j.GraphStateException if graph is invalid
     */
    @Bean
    public CompiledGraph<InterviewState> compiledGraph(
            ClassifierAgentNode classifierNode,
            JavaInterviewAgentNode javaNode,
            SpringInterviewAgentNode springNode,
            AwsInterviewAgentNode awsNode,
            InterviewAgentNode fallbackNode) throws org.bsc.langgraph4j.GraphStateException {

        // Wrap each node in AsyncNodeAction for addNode()
        AsyncNodeAction<InterviewState> classifierAction = state -> {
            var result = classifierNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        AsyncNodeAction<InterviewState> javaAction = state -> {
            var result = javaNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        AsyncNodeAction<InterviewState> springAction = state -> {
            var result = springNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        AsyncNodeAction<InterviewState> awsAction = state -> {
            var result = awsNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        AsyncNodeAction<InterviewState> fallbackAction = state -> {
            var result = fallbackNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        // Router: returns category name, Map translates to node ID
        // category() returns String now (e.g., "JAVA", "SPRING", "AWS")
        AsyncCommandAction<InterviewState> router = (state, config) -> {
            String category = state.category();
            log.info("Routing decision: category='{}'", category);
            return CompletableFuture.completedFuture(new Command(category));
        };

        StateGraph<InterviewState> graph = new StateGraph<>(InterviewState.SCHEMA, InterviewState::new)
                // Add nodes
                .addNode("classifier", classifierAction)
                .addNode("java_agent", javaAction)
                .addNode("spring_agent", springAction)
                .addNode("aws_agent", awsAction)
                .addNode("general_agent", fallbackAction)

                // Entry point → classifier
                .addEdge(StateGraph.START, "classifier")

                // Conditional routing from classifier
                .addConditionalEdges("classifier", router,
                        Map.of(
                                "JAVA", "java_agent",
                                "SPRING", "spring_agent",
                                "AWS", "aws_agent",
                                "UNKNOWN", "general_agent"
                        ))

                // All expert agents terminate at END
                .addEdge("java_agent", StateGraph.END)
                .addEdge("spring_agent", StateGraph.END)
                .addEdge("aws_agent", StateGraph.END)
                .addEdge("general_agent", StateGraph.END);

        return graph.compile();
    }
}

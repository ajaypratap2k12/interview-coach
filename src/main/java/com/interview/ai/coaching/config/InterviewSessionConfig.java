package com.interview.ai.coaching.config;

import com.interview.ai.coaching.graph.InterviewSessionState;
import com.interview.ai.coaching.nodes.GenerateFollowUpNode;
import com.interview.ai.coaching.nodes.GenerateQuestionNode;
import com.interview.ai.coaching.nodes.SessionEvaluateAnswerNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Configuration class for the Interview Session Graph.
 * 
 * This class defines two graphs for managing multi-turn interview sessions:
 * 
 * <h3>1. Question Generation Graph</h3>
 * <pre>
 * START → generate_question → END
 * </pre>
 * Generates the initial or next interview question.
 * 
 * <h3>2. Evaluation Graph</h3>
 * <pre>
 * START → evaluate_answer → generate_followup → END
 * </pre>
 * Evaluates the candidate's answer and generates a follow-up question.
 * 
 * <h3>Session Flow</h3>
 * <pre>
 * 1. Invoke questionGraph → get currentQuestion
 * 2. Candidate provides candidateAnswer
 * 3. Invoke evaluationGraph with candidateAnswer
 * 4. Get feedback, score, and nextQuestion
 * 5. Repeat from step 1 with nextQuestion
 * </pre>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see InterviewSessionState
 * @see GenerateQuestionNode
 * @see SessionEvaluateAnswerNode
 * @see GenerateFollowUpNode
 */
@Slf4j
@Configuration
public class InterviewSessionConfig {

    // ==================== Node Beans ====================

    @Bean
    public GenerateQuestionNode generateQuestionNode(ChatClient chatClient) {
        return new GenerateQuestionNode(chatClient);
    }

    @Bean
    public SessionEvaluateAnswerNode sessionEvaluateAnswerNode(ChatClient chatClient) {
        return new SessionEvaluateAnswerNode(chatClient);
    }

    @Bean
    public GenerateFollowUpNode generateFollowUpNode(ChatClient chatClient) {
        return new GenerateFollowUpNode(chatClient);
    }

    // ==================== Graph Compilation ====================

    /**
     * Creates and compiles the question generation graph.
     * 
     * <p>Graph structure:</p>
     * <pre>
     * START → generate_question → END
     * </pre>
     * 
     * @param generateQuestionNode the question generation node
     * @return the compiled graph for generating questions
     * @throws org.bsc.langgraph4j.GraphStateException if graph is invalid
     */
    @Bean("questionGraph")
    public CompiledGraph<InterviewSessionState> questionGraph(
            GenerateQuestionNode generateQuestionNode) throws org.bsc.langgraph4j.GraphStateException {

        AsyncNodeAction<InterviewSessionState> generateAction = state -> {
            var result = generateQuestionNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        StateGraph<InterviewSessionState> graph = new StateGraph<>(InterviewSessionState.SCHEMA, InterviewSessionState::new)
                .addNode("generate_question", generateAction)
                .addEdge(StateGraph.START, "generate_question")
                .addEdge("generate_question", StateGraph.END);

        return graph.compile();
    }

    /**
     * Creates and compiles the evaluation and follow-up graph.
     * 
     * <p>Graph structure:</p>
     * <pre>
     * START → evaluate_answer → generate_followup → END
     * </pre>
     * 
     * @param evaluateNode the evaluation node
     * @param followUpNode the follow-up generation node
     * @return the compiled graph for evaluation and follow-up
     * @throws org.bsc.langgraph4j.GraphStateException if graph is invalid
     */
    @Bean("evaluationGraph")
    public CompiledGraph<InterviewSessionState> evaluationGraph(
            SessionEvaluateAnswerNode evaluateNode,
            GenerateFollowUpNode followUpNode) throws org.bsc.langgraph4j.GraphStateException {

        AsyncNodeAction<InterviewSessionState> evaluateAction = state -> {
            var result = evaluateNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        AsyncNodeAction<InterviewSessionState> followUpAction = state -> {
            var result = followUpNode.execute(state);
            return CompletableFuture.completedFuture(result);
        };

        StateGraph<InterviewSessionState> graph = new StateGraph<>(InterviewSessionState.SCHEMA, InterviewSessionState::new)
                .addNode("evaluate_answer", evaluateAction)
                .addNode("generate_followup", followUpAction)
                .addEdge(StateGraph.START, "evaluate_answer")
                .addEdge("evaluate_answer", "generate_followup")
                .addEdge("generate_followup", StateGraph.END);

        return graph.compile();
    }
}

package com.interview.ai.coaching.config;

import com.interview.ai.coaching.graph.InterviewState;
import com.interview.ai.coaching.nodes.*;
import com.interview.ai.coaching.service.TraceCollector;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.Command;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Graph configuration for the planning-based multi-agent interview flow.
 *
 * <h3>Graph Topology</h3>
 * <pre>
 * START → planner → supervisor → [expert(s)] → aggregator → evaluator → END
 * </pre>
 *
 * @author Interview AI Coaching Team
 * @version 3.0
 */
@Slf4j
@Configuration
public class InterviewGraphConfig {

    private static final Map<String, String> AGENT_NODE_MAP = Map.of(
            "JAVA", "java_agent",
            "SPRING", "spring_agent",
            "AWS", "aws_agent",
            "MICROSERVICES", "microservice_agent",
            "KAFKA", "kafka_agent"
    );

    // ════════════════════════════════════════════════════════════════════════
    //  Node Beans
    // ════════════════════════════════════════════════════════════════════════

    @Bean
    public PlannerAgentNode plannerAgentNode(ChatClient chatClient) {
        return new PlannerAgentNode(chatClient);
    }

    @Bean
    public SupervisorAgentNode supervisorAgentNode() {
        return new SupervisorAgentNode();
    }

    @Bean
    public JavaExpertNode javaExpertNode(ChatClient chatClient) {
        return new JavaExpertNode(chatClient);
    }

    @Bean
    public SpringExpertNode springExpertNode(ChatClient chatClient) {
        return new SpringExpertNode(chatClient);
    }

    @Bean
    public AwsExpertNode awsExpertNode(ChatClient chatClient) {
        return new AwsExpertNode(chatClient);
    }

    @Bean
    public MicroserviceExpertNode microserviceExpertNode(ChatClient chatClient) {
        return new MicroserviceExpertNode(chatClient);
    }

    @Bean
    public KafkaExpertNode kafkaExpertNode(ChatClient chatClient) {
        return new KafkaExpertNode(chatClient);
    }

    @Bean
    public AggregatorAgentNode aggregatorAgentNode(ChatClient chatClient) {
        return new AggregatorAgentNode(chatClient);
    }

    @Bean
    public EvaluatorAgentNode evaluatorAgentNode(ChatClient chatClient) {
        return new EvaluatorAgentNode(chatClient);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Graph Compilation
    // ════════════════════════════════════════════════════════════════════════

    @Bean
    public CompiledGraph<InterviewState> compiledGraph(
            TraceCollector traceCollector,
            PlannerAgentNode plannerNode,
            SupervisorAgentNode supervisorNode,
            JavaExpertNode javaNode,
            SpringExpertNode springNode,
            AwsExpertNode awsNode,
            MicroserviceExpertNode microserviceNode,
            KafkaExpertNode kafkaNode,
            AggregatorAgentNode aggregatorNode,
            EvaluatorAgentNode evaluatorNode) throws org.bsc.langgraph4j.GraphStateException {

        // ── Wrap nodes with tracing ──────────────────────────────────────
        AsyncNodeAction<InterviewState> plannerAction =
                traced("planner", traceCollector, state -> plannerNode.execute(state));

        AsyncNodeAction<InterviewState> supervisorAction =
                traced("supervisor", traceCollector, state -> supervisorNode.execute(state));

        AsyncNodeAction<InterviewState> javaAction =
                traced("java_agent", traceCollector, state -> javaNode.execute(state));

        AsyncNodeAction<InterviewState> springAction =
                traced("spring_agent", traceCollector, state -> springNode.execute(state));

        AsyncNodeAction<InterviewState> awsAction =
                traced("aws_agent", traceCollector, state -> awsNode.execute(state));

        AsyncNodeAction<InterviewState> microserviceAction =
                traced("microservice_agent", traceCollector, state -> microserviceNode.execute(state));

        AsyncNodeAction<InterviewState> kafkaAction =
                traced("kafka_agent", traceCollector, state -> kafkaNode.execute(state));

        AsyncNodeAction<InterviewState> aggregatorAction =
                traced("aggregator", traceCollector, state -> aggregatorNode.execute(state));

        AsyncNodeAction<InterviewState> evaluatorAction =
                traced("evaluator", traceCollector, state -> evaluatorNode.execute(state));

        // ── Routers ──────────────────────────────────────────────────────
        AsyncCommandAction<InterviewState> supervisorRouter = (state, config) -> {
            List<String> plan = state.executionPlan();
            String firstAgent = plan.isEmpty() ? "JAVA" : plan.get(0);
            String nodeId = AGENT_NODE_MAP.getOrDefault(firstAgent, "aggregator");
            log.info("Supervisor router: plan={}, firstAgent={}, nodeId={}", plan, firstAgent, nodeId);
            return CompletableFuture.completedFuture(new Command(nodeId));
        };

        AsyncCommandAction<InterviewState> expertRouter = (state, config) -> {
            List<String> plan = state.executionPlan();
            List<String> completed = state.completedAgents();

            String nextAgent = plan.stream()
                    .filter(agent -> !completed.contains(agent))
                    .findFirst()
                    .orElse(null);

            if (nextAgent == null) {
                log.info("All experts completed, routing to aggregator");
                return CompletableFuture.completedFuture(new Command("aggregator"));
            }

            String nodeId = AGENT_NODE_MAP.getOrDefault(nextAgent, "aggregator");
            log.info("Expert router: nextAgent={}, nodeId={}, completed={}", nextAgent, nodeId, completed);
            return CompletableFuture.completedFuture(new Command(nodeId));
        };

        Map<String, String> expertRoutingMap = Map.of(
                "java_agent", "java_agent",
                "spring_agent", "spring_agent",
                "aws_agent", "aws_agent",
                "microservice_agent", "microservice_agent",
                "kafka_agent", "kafka_agent",
                "aggregator", "aggregator"
        );

        // ── Build Graph ──────────────────────────────────────────────────
        StateGraph<InterviewState> graph = new StateGraph<>(InterviewState.SCHEMA, InterviewState::new)
                .addNode("planner", plannerAction)
                .addNode("supervisor", supervisorAction)
                .addNode("java_agent", javaAction)
                .addNode("spring_agent", springAction)
                .addNode("aws_agent", awsAction)
                .addNode("microservice_agent", microserviceAction)
                .addNode("kafka_agent", kafkaAction)
                .addNode("aggregator", aggregatorAction)
                .addNode("evaluator", evaluatorAction)

                .addEdge(StateGraph.START, "planner")
                .addEdge("planner", "supervisor")
                .addConditionalEdges("supervisor", supervisorRouter, expertRoutingMap)

                .addConditionalEdges("java_agent", expertRouter, expertRoutingMap)
                .addConditionalEdges("spring_agent", expertRouter, expertRoutingMap)
                .addConditionalEdges("aws_agent", expertRouter, expertRoutingMap)
                .addConditionalEdges("microservice_agent", expertRouter, expertRoutingMap)
                .addConditionalEdges("kafka_agent", expertRouter, expertRoutingMap)

                .addEdge("aggregator", "evaluator")
                .addEdge("evaluator", StateGraph.END);

        return graph.compile();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tracing Helper
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Wraps a node action with execution timing and trace recording.
     *
     * <p>Captures start/end time, input state snapshot, and output map.
     * Records the trace via {@link TraceCollector}.</p>
     */
    private AsyncNodeAction<InterviewState> traced(
            String nodeName,
            TraceCollector collector,
            Function<InterviewState, Map<String, Object>> nodeFunction) {

        return state -> {
            Instant startedAt = Instant.now();

            // Snapshot relevant input fields
            Map<String, Object> input = Map.of(
                    "question", state.question(),
                    "executionPlan", state.executionPlan(),
                    "completedAgents", state.completedAgents()
            );

            // Execute the node
            Map<String, Object> result = nodeFunction.apply(state);

            Instant endedAt = Instant.now();

            // Record trace
            collector.recordNode(nodeName, startedAt, endedAt, input, result);

            return CompletableFuture.completedFuture(result);
        };
    }
}

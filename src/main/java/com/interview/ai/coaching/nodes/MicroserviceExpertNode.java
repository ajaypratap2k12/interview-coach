package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * Microservices expert agent node.
 *
 * <p>Answers ONLY from the microservices domain. Must not mention other technologies.
 * Writes {@code {"MICROSERVICES": answer}} into the shared {@code expertResponses} map
 * and appends {@code "MICROSERVICES"} to {@code completedAgents}.</p>
 *
 * <p>This node is Open/Closed-compliant: adding new experts requires no changes
 * to this class, {@code InterviewState}, or {@code AggregatorAgentNode}.</p>
 *
 * @author Interview AI Coaching Team
 * @version 3.0
 */
@Slf4j
@RequiredArgsConstructor
public class MicroserviceExpertNode {

    private static final String NODE_NAME = "microservice_agent";
    private static final String AGENT_ID = "MICROSERVICES";

    private final ChatClient chatClient;

    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input: question='{}'", NODE_NAME, state.question());

        String answer = chatClient.prompt()
                .user("""
                    You are a senior microservices architect. Answer ONLY from the microservices domain.

                    RULES:
                    - Answer exclusively about distributed systems and microservices patterns
                    - Do NOT mention Java, Spring, AWS, Kafka, or any other technology
                    - Do NOT provide a complete end-to-end answer that covers multiple domains
                    - Focus only on the microservices-specific parts of the question
                    - If the question has no microservices component, state that briefly

                    Microservices topics you cover:
                    Saga Pattern, CQRS, Event Sourcing, Service Mesh, API Gateway,
                    Circuit Breaker, Distributed Transactions, Service Discovery,
                    Eventual Consistency, Idempotency, Resilience Patterns

                    The candidate has 10 years of experience.
                    Provide concise, technically accurate answers with real-world examples.

                    Question: %s
                    """.formatted(state.question()))
                .call()
                .content();

        log.info("[{}] Output: expertResponses[{}] ({} chars)", NODE_NAME, AGENT_ID, answer.length());
        return Map.of("expertResponses", Map.of(AGENT_ID, answer), "completedAgents", List.of(AGENT_ID));
    }
}

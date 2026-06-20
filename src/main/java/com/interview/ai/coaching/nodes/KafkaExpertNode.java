package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * Kafka expert agent node.
 *
 * <p>Answers ONLY from the Kafka domain. Must not mention other technologies.
 * Writes {@code {"KAFKA": answer}} into the shared {@code expertResponses} map
 * and appends {@code "KAFKA"} to {@code completedAgents}.</p>
 *
 * <p>This node is Open/Closed-compliant: adding new experts requires no changes
 * to this class, {@code InterviewState}, or {@code AggregatorAgentNode}.</p>
 *
 * @author Interview AI Coaching Team
 * @version 3.0
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaExpertNode {

    private static final String NODE_NAME = "kafka_agent";
    private static final String AGENT_ID = "KAFKA";

    private final ChatClient chatClient;

    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input: question='{}'", NODE_NAME, state.question());

        String answer = chatClient.prompt()
                .user("""
                    You are a senior Apache Kafka engineer. Answer ONLY from the Kafka domain.

                    RULES:
                    - Answer exclusively about Apache Kafka and event streaming
                    - Do NOT mention Java, Spring, AWS, microservices patterns, or any other technology
                    - Do NOT provide a complete end-to-end answer that covers multiple domains
                    - Focus only on the Kafka-specific parts of the question
                    - If the question has no Kafka component, state that briefly

                    Kafka topics you cover:
                    Consumer Groups, Offsets, Partitions, Exactly-Once Semantics,
                    Consumer Lag, Rebalancing, Producers, Brokers, Replication,
                    Retention, Streams, Connect, Configuration Tuning

                    The candidate has 10 years of experience.
                    Provide concise, technically accurate answers with config examples.

                    Question: %s
                    """.formatted(state.question()))
                .call()
                .content();

        log.info("[{}] Output: expertResponses[{}] ({} chars)", NODE_NAME, AGENT_ID, answer.length());
        return Map.of("expertResponses", Map.of(AGENT_ID, answer), "completedAgents", List.of(AGENT_ID));
    }
}

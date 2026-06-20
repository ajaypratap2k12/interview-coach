package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * Spring expert agent node.
 *
 * <p>Answers ONLY from the Spring domain. Must not mention other technologies.
 * Writes to {@code springAnswer} and appends {@code "SPRING"} to
 * {@code completedAgents}.</p>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class SpringExpertNode {

    private static final String NODE_NAME = "spring_agent";
    private static final String AGENT_ID = "SPRING";

    private final ChatClient chatClient;

    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input: question='{}'", NODE_NAME, state.question());

        String answer = chatClient.prompt()
                .user("""
                    You are a senior Spring expert. Answer ONLY from the Spring domain.

                    RULES:
                    - Answer exclusively about Spring framework and its ecosystem
                    - Do NOT mention Java language internals, AWS, Kafka, microservices patterns, or any other technology
                    - Do NOT provide a complete end-to-end answer that covers multiple domains
                    - Focus only on the Spring-specific parts of the question
                    - If the question has no Spring component, state that briefly

                    Spring topics you cover:
                    Spring Boot, Spring MVC, Spring Security, Spring Data JPA,
                    Transactions, Dependency Injection, AOP, Spring Cloud, Spring AI

                    The candidate has 10 years of experience.
                    Provide concise, technically accurate answers with code examples.

                    Question: %s
                    """.formatted(state.question()))
                .call()
                .content();

        log.info("[{}] Output: springAnswer ({} chars)", NODE_NAME, answer.length());
        return Map.of("springAnswer", answer, "completedAgents", List.of(AGENT_ID));
    }
}

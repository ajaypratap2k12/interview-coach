package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * Java expert agent node.
 *
 * <p>Answers ONLY from the Java domain. Must not mention other technologies.
 * Writes to {@code javaAnswer} and appends {@code "JAVA"} to
 * {@code completedAgents}.</p>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class JavaExpertNode {

    private static final String NODE_NAME = "java_agent";
    private static final String AGENT_ID = "JAVA";

    private final ChatClient chatClient;

    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input: question='{}'", NODE_NAME, state.question());

        String answer = chatClient.prompt()
                .user("""
                    You are a senior Java expert. Answer ONLY from the Java domain.

                    RULES:
                    - Answer exclusively about Java language and JVM internals
                    - Do NOT mention Spring, AWS, Kafka, microservices, or any other technology
                    - Do NOT provide a complete end-to-end answer that covers multiple domains
                    - Focus only on the Java-specific parts of the question
                    - If the question has no Java component, state that briefly

                    Java topics you cover:
                    Collections, Streams, Concurrency, JVM, Memory Management,
                    Design Patterns, Generics, Exception Handling, OOP, Records, Sealed Classes

                    The candidate has 10 years of Java experience.
                    Provide concise, technically accurate answers with code examples.

                    Question: %s
                    """.formatted(state.question()))
                .call()
                .content();

        log.info("[{}] Output: javaAnswer ({} chars)", NODE_NAME, answer.length());
        return Map.of("javaAnswer", answer, "completedAgents", List.of(AGENT_ID));
    }
}

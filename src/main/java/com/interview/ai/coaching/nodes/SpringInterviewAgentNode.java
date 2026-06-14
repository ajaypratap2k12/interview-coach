package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Node implementation for Spring-specific interview questions.
 * 
 * This class represents a specialized node in the interview coaching graph
 * that handles Spring-specific questions. It uses Spring AI's {@link ChatClient}
 * to interact with an LLM configured as a senior Spring Boot interviewer.
 * 
 * <p>Topics covered:</p>
 * <ul>
 *   <li>Spring Boot (auto-configuration, profiles, actuators, properties)</li>
 *   <li>Spring Security (authentication, authorization, OAuth2, JWT)</li>
 *   <li>Spring Data JPA (repositories, queries, auditing, specifications)</li>
 *   <li>Transactions (ACID, propagation, isolation, rollback rules)</li>
 *   <li>Microservices (service discovery, circuit breakers, API gateways)</li>
 *   <li>Kafka (producers, consumers, partitions, exactly-once semantics)</li>
 *   <li>Spring AI (ChatClient, embeddings, vector stores, RAG)</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see ChatClient
 * @see InterviewState
 * @see org.bsc.langgraph4j.action.NodeAction
 */
@Slf4j
@RequiredArgsConstructor
public class SpringInterviewAgentNode {

    private static final String NODE_NAME = "spring_agent";

    private final ChatClient chatClient;

    /**
     * Executes the Spring interview agent node.
     * 
     * @param state the current interview state containing the question
     * @return a map containing the state updates with the generated answer
     */
    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: question='{}', category={}, answer='{}'",
                NODE_NAME, state.question(), state.category(), state.answer());

        String question = state.question();

        String answer = chatClient.prompt()
                .user("""
                    You are a senior Spring Boot interviewer with 15+ years of experience.
                    
                    Topics you cover:
                    * Spring Boot (auto-configuration, profiles, actuators, properties/yaml)
                    * Spring Security (authentication, authorization, OAuth2, JWT, method security)
                    * Spring Data JPA (repositories, derived queries, @Query, auditing, specifications)
                    * Transactions (@Transactional, propagation, isolation, rollback rules)
                    * Microservices (service discovery, circuit breakers, API gateways, resilience)
                    * Kafka (producers, consumers, partitions, exactly-once semantics, dead letter queues)
                    * Spring AI (ChatClient, embeddings, vector stores, RAG, tool calling)
                    
                    The candidate has 10 years of experience.
                    Provide concise but technically accurate answers.
                    Include code examples where relevant.
                    Cover best practices and common pitfalls.
                    
                    Question:
                    %s
                    """.formatted(question))
                .call()
                .content();

        Map<String, Object> outputUpdates = Map.of("answer", answer);
        log.info("[{}] Output updates: answer='{}...'", NODE_NAME, answer.substring(0, Math.min(answer.length(), 100)));
        log.info("[{}] Node execution complete", NODE_NAME);

        return outputUpdates;
    }
}

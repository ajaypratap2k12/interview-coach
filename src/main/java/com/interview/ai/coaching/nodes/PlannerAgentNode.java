package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Planner agent that analyzes an interview question and produces an execution plan
 * listing which domain expert agents should be consulted.
 *
 * <p>This node sits at the start of the graph. It does NOT perform any domain work —
 * it only determines the routing plan. The LLM classifies the question and returns
 * a JSON array of relevant expert agent IDs.</p>
 *
 * <h3>Output Fields</h3>
 * <ul>
 *   <li>{@code executionPlan} — ordered list of agent IDs to invoke (appended via reducer)</li>
 * </ul>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Must NEVER answer the interview question</li>
 *   <li>Must return ONLY a JSON array of agent IDs</li>
 *   <li>Must validate all agent IDs against the allowed set</li>
 * </ul>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 * @see ChatClient
 * @see InterviewState
 */
@Slf4j
@RequiredArgsConstructor
public class PlannerAgentNode {

    private static final String NODE_NAME = "planner";
    private static final Set<String> VALID_AGENTS = Set.of("JAVA", "SPRING", "AWS", "MICROSERVICES", "KAFKA");
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[.*\\]");

    private final ChatClient chatClient;

    /**
     * Executes the planner node.
     *
     * <p>Analyzes the question and produces an {@code executionPlan} — an ordered list
     * of agent IDs that should be consulted. The plan is appended to state via the
     * appender reducer.</p>
     *
     * @param state the current interview state containing the question
     * @return state updates with {@code executionPlan}
     */
    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: question='{}'", NODE_NAME, state.question());

        String question = state.question();

        if (question == null || question.isBlank()) {
            log.warn("[{}] Question is empty or null, defaulting to JAVA agent", NODE_NAME);
            return buildResult(List.of("JAVA"));
        }

        log.info("[{}] Analyzing question to build execution plan: '{}'", NODE_NAME, question);

        String rawResponse = chatClient.prompt()
                .user("""
                    You are a planning agent for an interview coaching system.

                    Your ONLY job is to analyze the interview question and determine which expert agents
                    are needed to answer it. You must NOT answer the question yourself.

                    Available expert agents:
                    - JAVA: Core Java concepts (OOP, Collections, Streams, Concurrency, JVM, Design Patterns, Generics, Exception Handling)
                    - SPRING: Spring framework (Spring Boot, Spring MVC, Spring Security, Spring Data, Spring Cloud, Dependency Injection, AOP)
                    - AWS: Amazon Web Services (EC2, S3, Lambda, IAM, VPC, ECS, EKS, RDS, DynamoDB, SQS, SNS, CloudFormation)
                    - MICROSERVICES: Distributed systems (Saga Pattern, CQRS, Event Sourcing, Service Mesh, API Gateway, Circuit Breaker, Distributed Transactions)
                    - KAFKA: Event streaming (Topics, Partitions, Consumer Groups, Offsets, Consumer Lag, Exactly-Once Semantics, Streams, Connect)

                    Return ONLY a JSON array of agent IDs in order of relevance (most relevant first).
                    Do NOT include any explanation, reasoning, or answer to the question.
                    Do NOT wrap the array in markdown code blocks.

                    Examples:
                    "Explain HashMap internals" -> ["JAVA"]
                    "What is @Transactional?" -> ["SPRING"]
                    "How does ECS differ from EKS?" -> ["AWS"]
                    "Explain Saga Pattern" -> ["MICROSERVICES"]
                    "What is consumer lag?" -> ["KAFKA"]
                    "Transaction handling in Spring Boot microservices using Kafka" -> ["SPRING","MICROSERVICES","KAFKA"]
                    "Secure AWS Lambda with API Gateway and DynamoDB" -> ["AWS","MICROSERVICES"]
                    "ConcurrentHashMap in multithreaded Spring service" -> ["JAVA","SPRING"]
                    "Event-driven microservices with Kafka on AWS" -> ["KAFKA","MICROSERVICES","AWS"]

                    Question: %s
                    """.formatted(question))
                .call()
                .content();

        log.info("[{}] Raw planner response: '{}'", NODE_NAME, rawResponse);

        List<String> plan = parseJsonArray(rawResponse);

        log.info("[{}] Execution plan: question='{}' -> agents='{}'", NODE_NAME, question, plan);

        return buildResult(plan);
    }

    /**
     * Parses a JSON array of agent IDs from the LLM response.
     *
     * <p>Handles various response formats:</p>
     * <ul>
     *   <li>Clean JSON: {@code ["JAVA","SPRING"]}</li>
     *   <li>With markdown: {@code ```json ["JAVA","SPRING"] ```}</li>
     *   <li>Fallback: comma-separated or single agent name</li>
     * </ul>
     *
     * @param raw the raw LLM response
     * @return the parsed list of agent IDs
     */
    private List<String> parseJsonArray(String raw) {
        List<String> result = new ArrayList<>();

        if (raw == null || raw.isBlank()) {
            log.warn("[{}] LLM returned null/blank response, defaulting to JAVA agent", NODE_NAME);
            result.add("JAVA");
            return result;
        }

        // Strip markdown code fences if present
        String cleaned = raw.replaceAll("```[a-z]*\\s*", "").replaceAll("```", "").trim();

        // Try to extract JSON array using regex
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            String jsonArray = matcher.group();
            log.info("[{}] Extracted JSON array: '{}'", NODE_NAME, jsonArray);

            // Strip brackets and split by comma
            String inner = jsonArray.substring(1, jsonArray.length() - 1);
            String[] parts = inner.split(",");

            for (String part : parts) {
                // Remove quotes and whitespace
                String agentId = part.trim().replaceAll("^\"|\"$", "").trim().toUpperCase();
                if (agentId.isEmpty()) continue;

                if (VALID_AGENTS.contains(agentId)) {
                    log.info("[{}] Valid agent found: '{}'", NODE_NAME, agentId);
                    result.add(agentId);
                } else {
                    // Try partial match
                    for (String validAgent : VALID_AGENTS) {
                        if (agentId.contains(validAgent)) {
                            log.info("[{}] Partial match: '{}' contains '{}'", NODE_NAME, agentId, validAgent);
                            if (!result.contains(validAgent)) {
                                result.add(validAgent);
                            }
                            break;
                        }
                    }
                }
            }
        } else {
            // Fallback: try comma-separated parsing
            log.warn("[{}] No JSON array found in response, attempting comma-separated fallback", NODE_NAME);
            String[] parts = cleaned.split(",");
            for (String part : parts) {
                String normalized = part.trim().replaceAll("^\"|\"$", "").toUpperCase();
                if (normalized.isEmpty()) continue;

                if (VALID_AGENTS.contains(normalized)) {
                    result.add(normalized);
                } else {
                    for (String validAgent : VALID_AGENTS) {
                        if (normalized.contains(validAgent)) {
                            if (!result.contains(validAgent)) {
                                result.add(validAgent);
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (result.isEmpty()) {
            log.warn("[{}] No valid agents parsed from '{}', defaulting to JAVA", NODE_NAME, raw);
            result.add("JAVA");
        }

        return result;
    }

    private Map<String, Object> buildResult(List<String> plan) {
        Map<String, Object> result = Map.of("executionPlan", plan);
        log.info("[{}] Output: executionPlan={}", NODE_NAME, plan);
        log.info("[{}] Node execution complete", NODE_NAME);
        return result;
    }
}

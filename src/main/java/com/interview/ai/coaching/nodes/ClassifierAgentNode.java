package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.Category;
import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Node implementation for classifying interview questions.
 * 
 * This class represents a routing node in the interview coaching graph that
 * classifies questions into categories (JAVA, SPRING, AWS) using an LLM.
 * It uses Spring AI's {@link ChatClient} to interact with the AI model.
 * 
 * <p>The node follows the langgraph4j node action pattern:</p>
 * <ol>
 *   <li>Receives current {@link InterviewState} with interview question</li>
 *   <li>Classifies the question into a category using the AI model</li>
 *   <li>Returns updated state with the classified category (as String)</li>
 * </ol>
 * 
 * <p>The classification is deterministic - the prompt instructs the model
 * to return only a single word (JAVA, SPRING, or AWS) with no explanation.</p>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see ChatClient
 * @see InterviewState
 * @see Category
 * @see org.bsc.langgraph4j.action.NodeAction
 */
@Slf4j
@RequiredArgsConstructor
public class ClassifierAgentNode {

    private static final String NODE_NAME = "classifier";

    private final ChatClient chatClient;

    /**
     * Executes the classifier node.
     * 
     * This method processes the current state, extracts the interview
     * question, classifies it into a category using the AI model,
     * and returns the updated state with the category.
     * 
     * @param state the current interview state containing the question
     * @return a map containing the state updates with the classified category (String)
     */
    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: question='{}', category={}, answer='{}'",
                NODE_NAME, state.question(), state.category(), state.answer());

        String question = state.question();

        String categoryWord = chatClient.prompt()
                .user("""
                    You are a routing agent.
                    
                    Given an interview question, return only one word:
                    
                    JAVA
                    SPRING
                    AWS
                    
                    No explanation.
                    
                    Examples:
                    
                    "Explain HashMap internals" -> JAVA
                    "Explain @Transactional" -> SPRING
                    "What is an S3 bucket?" -> AWS
                    
                    Question:
                    %s
                    """.formatted(question))
                .call()
                .content();

        Category category = parseCategory(categoryWord);

        // Return as String to avoid classloader issues with DevTools
        Map<String, Object> outputUpdates = Map.of("category", category.name());
        log.info("[{}] Output updates: {}", NODE_NAME, outputUpdates);
        log.info("[{}] Node execution complete", NODE_NAME);

        return outputUpdates;
    }

    /**
     * Parses the LLM response into a Category enum.
     * 
     * Handles trimming, uppercasing, and defaults to UNKNOWN
     * if the response doesn't match expected categories.
     * 
     * @param raw the raw LLM response
     * @return the parsed Category
     */
    private Category parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return Category.UNKNOWN;
        }

        String normalized = raw.trim().toUpperCase();

        try {
            return Category.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return Category.UNKNOWN;
        }
    }
}

package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewSessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Node implementation for generating interview questions.
 * 
 * This class represents a node in the interview session graph that
 * generates interview questions based on the interview context.
 * It uses Spring AI's {@link ChatClient} to interact with an LLM.
 * 
 * <p>The node generates questions for:</p>
 * <ul>
 *   <li>Initial interview questions</li>
 *   <li>Technical topics (Java, Spring, AWS)</li>
 *   <li>Candidate experience level (10 years)</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see ChatClient
 * @see InterviewSessionState
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateQuestionNode {

    private static final String NODE_NAME = "generate_question";

    private final ChatClient chatClient;

    /**
     * Executes the question generation node.
     * 
     * @param state the current interview session state
     * @return a map containing the state updates with the generated question
     */
    public Map<String, Object> execute(InterviewSessionState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: currentQuestion='{}', candidateAnswer='{}', score={}",
                NODE_NAME, state.currentQuestion(), state.candidateAnswer(), state.score());

        String question = chatClient.prompt()
                .user("""
                    You are a senior technical interviewer conducting an interview.
                    
                    Generate ONE interview question for a candidate with 10 years of experience.
                    Focus on Java, Spring Boot topics.
                    
                    Return ONLY the question, no explanations or prefixes.
                    
                    Previous context:
                    - Previous question: %s
                    - Previous answer: %s
                    - Previous score: %d/10
                    
                    Generate a new question:
                    """.formatted(
                        state.currentQuestion().isEmpty() ? "None (first question)" : state.currentQuestion(),
                        state.candidateAnswer().isEmpty() ? "None" : state.candidateAnswer(),
                        state.score()))
                .call()
                .content();

        Map<String, Object> outputUpdates = Map.of("currentQuestion", question.trim());
        log.info("[{}] Output updates: currentQuestion='{}'", NODE_NAME, question.trim());
        log.info("[{}] Node execution complete", NODE_NAME);

        return outputUpdates;
    }
}

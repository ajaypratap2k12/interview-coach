package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewSessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Node implementation for generating follow-up questions in a session.
 * 
 * This class represents a node in the interview session graph that
 * generates follow-up questions based on the previous answer and evaluation.
 * It uses Spring AI's {@link ChatClient} to interact with an LLM.
 * 
 * <p>The node generates follow-up questions that:</p>
 * <ul>
 *   <li>Address areas where the candidate scored low</li>
 *   <li>Dive deeper into specific topics</li>
 *   <li>Challenge the candidate with more advanced questions</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see ChatClient
 * @see InterviewSessionState
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateFollowUpNode {

    private static final String NODE_NAME = "generate_followup";

    private final ChatClient chatClient;

    /**
     * Executes the follow-up question generation node.
     * 
     * @param state the current interview session state
     * @return a map containing the state updates with the next question
     */
    public Map<String, Object> execute(InterviewSessionState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: currentQuestion='{}', feedback='{}', score={}",
                NODE_NAME, state.currentQuestion(), state.feedback(), state.score());

        String followUp = chatClient.prompt()
                .user("""
                    You are a senior technical interviewer conducting an interview.
                    
                    Based on the candidate's previous answer and evaluation, generate a follow-up question.
                    
                    Previous Question: %s
                    Candidate Answer: %s
                    Evaluation: %s
                    Score: %d/10
                    
                    Guidelines:
                    - If score < 5: Ask a simpler follow-up to clarify fundamentals
                    - If score 5-7: Ask a related question to test deeper understanding
                    - If score > 7: Ask a more advanced question on the same topic
                    
                    Return ONLY the follow-up question, no explanations or prefixes.
                    """.formatted(
                        state.currentQuestion(),
                        state.candidateAnswer(),
                        state.feedback(),
                        state.score()))
                .call()
                .content();

        Map<String, Object> outputUpdates = Map.of("nextQuestion", followUp.trim());
        log.info("[{}] Output updates: nextQuestion='{}'", NODE_NAME, followUp.trim());
        log.info("[{}] Node execution complete", NODE_NAME);

        return outputUpdates;
    }
}

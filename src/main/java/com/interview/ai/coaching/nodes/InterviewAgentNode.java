package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Node implementation for the interview agent.
 * 
 * This class represents a node in the interview coaching graph that
 * processes interview questions and generates appropriate answers or
 * coaching responses. It uses Spring AI's {@link ChatClient} to
 * interact with an LLM for generating responses.
 * 
 * <p>The node follows the langgraph4j node action pattern:</p>
 * <ol>
 *   <li>Receives current {@link InterviewState} with interview question</li>
 *   <li>Processes the question using the AI model</li>
 *   <li>Returns updated state with the generated answer</li>
 * </ol>
 * 
 * <p>This class is designed to be used as a node action in the
 * interview coaching graph. It should be registered with the
 * {@link org.bsc.langgraph4j.StateGraph} during graph construction.</p>
 * 
 * <p>Dependencies:</p>
 * <ul>
 *   <li>{@link ChatClient} - Injected via constructor for AI model interaction</li>
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
public class InterviewAgentNode {

    private static final String NODE_NAME = "general_agent";

    private final ChatClient chatClient;

    /**
     * Executes the interview agent node.
     * 
     * This method processes the current state, extracts the interview
     * question, generates an answer using the AI model, and returns
     * the updated state with the answer.
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
                    You are an experienced Java interview coach.
                    Provide concise but technically accurate answers suitable for a 10-year experienced Java developer.

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

package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Aggregator agent node that merges all expert responses into a single answer.
 *
 * <p>Iterates over the shared {@code expertResponses} map (keyed by expert ID)
 * and builds a prompt that merges all domain-specific answers into one coherent,
 * interview-quality explanation. Stores the result in {@code finalAnswer}.</p>
 *
 * <h3>Open/Closed Principle</h3>
 * <p>This node operates generically on {@code expertResponses} — it never references
 * individual expert fields by name. Adding a new expert (e.g. DATABASE, SECURITY)
 * requires no changes to this class.</p>
 *
 * @author Interview AI Coaching Team
 * @version 3.0
 */
@Slf4j
@RequiredArgsConstructor
public class AggregatorAgentNode {

    private static final String NODE_NAME = "aggregator";

    private final ChatClient chatClient;

    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input: question='{}'", NODE_NAME, state.question());

        String assembledInput = collectExpertAnswers(state);
        log.info("[{}] Collected expert input ({} chars)", NODE_NAME, assembledInput.length());

        if (assembledInput.isBlank()) {
            log.warn("[{}] No expert answers found", NODE_NAME);
            return Map.of("finalAnswer", "No expert answers available.");
        }

        String finalAnswer = chatClient.prompt()
                .user("""
                    You are a technical interview answer synthesizer.

                    You will receive answers from multiple domain experts.
                    Your task is to merge them into ONE comprehensive, interview-quality explanation.

                    RULES:
                    - Do NOT introduce any new technical concepts not present in the expert answers
                    - Do NOT repeat the same information from different experts
                    - Do NOT add your own knowledge or opinions
                    - Remove duplicate explanations — keep the best version
                    - Preserve the logical flow: start with fundamentals, build to advanced
                    - Use clear section headings if multiple domains are involved
                    - Write as if giving a single, polished interview answer
                    - Maintain technical depth — do not simplify or dumb down

                    The original interview question was:
                    %s

                    Expert answers to merge:

                    %s

                    Produce ONE comprehensive answer that covers all domains without duplication.
                    """.formatted(state.question(), assembledInput))
                .call()
                .content();

        log.info("[{}] Output: finalAnswer ({} chars)", NODE_NAME, finalAnswer.length());
        return Map.of("finalAnswer", finalAnswer);
    }

    /**
     * Collects all expert responses from the shared map into a formatted string.
     *
     * <p>Iterates generically over {@code expertResponses} — no hardcoded expert
     * references. New experts are automatically included.</p>
     */
    private String collectExpertAnswers(InterviewState state) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> responses = state.expertResponses();

        for (Map.Entry<String, String> entry : responses.entrySet()) {
            String expertId = entry.getKey();
            String answer = entry.getValue();
            if (answer != null && !answer.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append("\n\n");
                }
                sb.append("[").append(expertId).append("]\n").append(answer);
            }
        }

        return sb.toString();
    }
}

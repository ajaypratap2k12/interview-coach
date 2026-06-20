package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Aggregator agent node that merges all expert answers into a single
 * comprehensive interview-quality explanation.
 *
 * <p>Reads answers from all expert fields, removes duplicates, preserves
 * logical flow, and stores the result in {@code finalAnswer}.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Must NEVER introduce new technical concepts absent from expert responses</li>
 *   <li>Must only synthesize and organize existing expert content</li>
 *   <li>Must produce one coherent, interview-quality explanation</li>
 * </ul>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class AggregatorAgentNode {

    private static final String NODE_NAME = "aggregator";

    private final ChatClient chatClient;

    /**
     * Executes the aggregator node.
     *
     * <p>Collects non-empty expert answers, sends them to the LLM for merging,
     * and returns the combined {@code finalAnswer}.</p>
     *
     * @param state the current interview state with expert answers
     * @return state update with {@code finalAnswer}
     */
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
     * Collects non-empty expert answers into a single labeled string.
     *
     * @param state the current interview state
     * @return formatted string of all expert answers
     */
    private String collectExpertAnswers(InterviewState state) {
        StringBuilder sb = new StringBuilder();

        appendIfPresent(sb, "Java", state.javaAnswer());
        appendIfPresent(sb, "Spring", state.springAnswer());
        appendIfPresent(sb, "AWS", state.awsAnswer());
        appendIfPresent(sb, "Microservices", state.microserviceAnswer());
        appendIfPresent(sb, "Kafka", state.kafkaAnswer());

        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, String answer) {
        if (answer != null && !answer.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("[").append(label).append("]\n").append(answer);
        }
    }
}

package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluator agent node that scores the aggregated answer.
 *
 * <p>Reads {@code finalAnswer} (produced by AggregatorAgentNode) and evaluates
 * it for technical accuracy and completeness. Produces {@code feedback} and
 * {@code score}.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Reads {@code finalAnswer} — does NOT assemble or modify it</li>
 *   <li>Produces {@code feedback} and {@code score}</li>
 * </ul>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class EvaluatorAgentNode {

    private static final String NODE_NAME = "evaluator";
    private static final Pattern TECHNICAL_PATTERN = Pattern.compile("Technical Accuracy:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPLETENESS_PATTERN = Pattern.compile("Completeness:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final ChatClient chatClient;

    /**
     * Executes the evaluator node.
     *
     * @param state the current interview state with finalAnswer
     * @return state updates with {@code feedback} and {@code score}
     */
    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input: question='{}', finalAnswer={} chars",
                NODE_NAME, state.question(), state.finalAnswer().length());

        String question = state.question();
        String finalAnswer = state.finalAnswer();

        String evaluation = chatClient.prompt()
                .user("""
                    You are a technical interview evaluator.

                    Review the following interview answer and provide:
                    1. Technical Accuracy (1-10): How technically correct is the answer?
                    2. Completeness (1-10): How complete and thorough is the answer?
                    3. Suggestions: Specific improvements for the answer.

                    Format your response EXACTLY as:
                    Technical Accuracy: X
                    Completeness: Y
                    Suggestions: [your suggestions here]

                    Question:
                    %s

                    Answer:
                    %s
                    """.formatted(question, finalAnswer))
                .call()
                .content();

        int technicalScore = parseScore(evaluation, TECHNICAL_PATTERN);
        int completenessScore = parseScore(evaluation, COMPLETENESS_PATTERN);
        int combinedScore = (technicalScore + completenessScore) / 2;

        String feedback = String.format("Technical Accuracy: %d/10, Completeness: %d/10\n%s",
                technicalScore, completenessScore, extractSuggestions(evaluation));

        log.info("[{}] Output: score={}, feedback='{}...'",
                NODE_NAME, combinedScore, feedback.substring(0, Math.min(feedback.length(), 100)));
        log.info("[{}] Node execution complete", NODE_NAME);

        return Map.of("feedback", feedback, "score", combinedScore);
    }

    private int parseScore(String text, Pattern pattern) {
        if (text == null) {
            return 5;
        }
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                int score = Integer.parseInt(matcher.group(1));
                return Math.max(1, Math.min(10, score));
            } catch (NumberFormatException e) {
                return 5;
            }
        }
        return 5;
    }

    private String extractSuggestions(String text) {
        if (text == null) {
            return "No suggestions available.";
        }
        int suggestionsIndex = text.indexOf("Suggestions:");
        if (suggestionsIndex >= 0) {
            return text.substring(suggestionsIndex).trim();
        }
        return "No suggestions available.";
    }
}

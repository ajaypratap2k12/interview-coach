package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewSessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node implementation for evaluating candidate answers in a session.
 * 
 * This class represents an evaluation node in the interview session graph
 * that reviews candidate answers and provides feedback with scores.
 * It uses Spring AI's {@link ChatClient} to interact with an LLM.
 * 
 * <p>The node evaluates:</p>
 * <ul>
 *   <li>Technical accuracy (1-10)</li>
 *   <li>Completeness (1-10)</li>
 *   <li>Provides suggestions for improvement</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see ChatClient
 * @see InterviewSessionState
 */
@Slf4j
@RequiredArgsConstructor
public class SessionEvaluateAnswerNode {

    private static final String NODE_NAME = "evaluate_answer";
    private static final Pattern TECHNICAL_PATTERN = Pattern.compile("Technical Accuracy:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPLETENESS_PATTERN = Pattern.compile("Completeness:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final ChatClient chatClient;

    /**
     * Executes the evaluation node.
     * 
     * @param state the current interview session state
     * @return a map containing the state updates with feedback and score
     */
    public Map<String, Object> execute(InterviewSessionState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: currentQuestion='{}', candidateAnswer='{}'",
                NODE_NAME, state.currentQuestion(), state.candidateAnswer());

        String evaluation = chatClient.prompt()
                .user("""
                    You are a technical interview evaluator.
                    
                    Review the candidate's answer to the interview question.
                    
                    Question: %s
                    Candidate Answer: %s
                    
                    Provide:
                    1. Technical Accuracy (1-10): How technically correct is the answer?
                    2. Completeness (1-10): How complete and thorough is the answer?
                    3. Suggestions: Specific improvements for the answer.
                    
                    Format your response EXACTLY as:
                    Technical Accuracy: X
                    Completeness: Y
                    Suggestions: [your suggestions here]
                    """.formatted(state.currentQuestion(), state.candidateAnswer()))
                .call()
                .content();

        int technicalScore = parseScore(evaluation, TECHNICAL_PATTERN);
        int completenessScore = parseScore(evaluation, COMPLETENESS_PATTERN);
        int combinedScore = (technicalScore + completenessScore) / 2;

        String feedback = String.format("Technical Accuracy: %d/10, Completeness: %d/10\n%s",
                technicalScore, completenessScore, extractSuggestions(evaluation));

        Map<String, Object> outputUpdates = Map.of(
                "feedback", feedback,
                "score", combinedScore
        );
        log.info("[{}] Output updates: score={}, feedback='{}...'",
                NODE_NAME, combinedScore, feedback.substring(0, Math.min(feedback.length(), 100)));
        log.info("[{}] Node execution complete", NODE_NAME);

        return outputUpdates;
    }

    /**
     * Parses a score from the evaluation text using the given pattern.
     * 
     * @param text the evaluation text
     * @param pattern the pattern to match
     * @return the parsed score, or 5 (default) if not found
     */
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

    /**
     * Extracts suggestions from the evaluation text.
     * 
     * @param text the evaluation text
     * @return the suggestions string
     */
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

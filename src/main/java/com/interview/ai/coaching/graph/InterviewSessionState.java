package com.interview.ai.coaching.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * Represents the state of a multi-turn interview session.
 * 
 * This class extends {@link AgentState} to manage the shared state
 * across all nodes in the interview session graph. It tracks the
 * current question, candidate's answer, evaluation, and next question.
 * 
 * <p>The state contains the following properties:</p>
 * <ul>
 *   <li><b>currentQuestion</b> - The current interview question being asked</li>
 *   <li><b>candidateAnswer</b> - The candidate's response to the current question</li>
 *   <li><b>feedback</b> - Evaluation feedback with suggestions</li>
 *   <li><b>score</b> - Combined evaluation score (1-10)</li>
 *   <li><b>nextQuestion</b> - The follow-up question generated based on evaluation</li>
 * </ul>
 * 
 * <h3>Session Flow</h3>
 * <pre>
 * 1. GenerateQuestion → sets currentQuestion
 * 2. Candidate provides answer → sets candidateAnswer
 * 3. EvaluateAnswer → sets feedback, score, nextQuestion
 * 4. Return to step 1 with nextQuestion → sets currentQuestion
 * </pre>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see AgentState
 * @see Channel
 * @see Channels#base(java.util.function.Supplier)
 */
public class InterviewSessionState extends AgentState {

    /**
     * Schema definition for the interview session state.
     * 
     * All properties use {@link Channels#base(java.util.function.Supplier)}
     * which creates a channel with a default value provider but no reducer.
     * Without a reducer, the channel uses the default behavior:
     * the new value completely replaces the old value.
     */
    public static final Map<String, Channel<?>> SCHEMA =
            Map.of(
                    "currentQuestion", Channels.base(() -> ""),
                    "candidateAnswer", Channels.base(() -> ""),
                    "feedback", Channels.base(() -> ""),
                    "score", Channels.base(() -> 0),
                    "nextQuestion", Channels.base(() -> "")
            );

    /**
     * Constructs a new InterviewSessionState with the given initial data.
     * 
     * @param initData the initial state data as a map of key-value pairs
     */
    public InterviewSessionState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * Retrieves the current interview question.
     * 
     * @return the current question string, or empty string if not set
     */
    public String currentQuestion() {
        return this.<String>value("currentQuestion").orElse("");
    }

    /**
     * Retrieves the candidate's answer to the current question.
     * 
     * @return the candidate's answer, or empty string if not set
     */
    public String candidateAnswer() {
        return this.<String>value("candidateAnswer").orElse("");
    }

    /**
     * Retrieves the evaluation feedback.
     * 
     * @return the feedback string, or empty string if not set
     */
    public String feedback() {
        return this.<String>value("feedback").orElse("");
    }

    /**
     * Retrieves the evaluation score.
     * 
     * @return the score as Integer, or 0 if not set
     */
    public Integer score() {
        return this.<Integer>value("score").orElse(0);
    }

    /**
     * Retrieves the next question to ask.
     * 
     * @return the next question string, or empty string if not set
     */
    public String nextQuestion() {
        return this.<String>value("nextQuestion").orElse("");
    }
}

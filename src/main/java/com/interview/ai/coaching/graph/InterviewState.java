package com.interview.ai.coaching.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * Represents the state of an interview coaching session.
 * 
 * This class extends {@link AgentState} to manage the shared state
 * across all nodes in the interview coaching graph. It defines the
 * schema for the state properties and provides accessor methods
 * for retrieving values.
 * 
 * <p>The state contains the following properties:</p>
 * <ul>
 *   <li><b>question</b> - The interview question being processed</li>
 *   <li><b>category</b> - The category as String (JAVA, SPRING, AWS, UNKNOWN)</li>
 *   <li><b>answer</b> - The generated answer or coaching response</li>
 *   <li><b>feedback</b> - Evaluation feedback with suggestions</li>
 *   <li><b>score</b> - Combined evaluation score (technical accuracy + completeness)</li>
 * </ul>
 * 
 * <p><b>Note:</b> Category is stored as String to avoid classloader issues
 * with Spring Boot DevTools. Use {@link #categoryAs()} to get the enum value.</p>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see AgentState
 * @see Channel
 * @see Category
 * @see Channels#base(java.util.function.Supplier)
 */
public class InterviewState extends AgentState {

    /**
     * Schema definition for the interview state.
     * 
     * All properties use {@link Channels#base(java.util.function.Supplier)}
     * which creates a channel with a default value provider but no reducer.
     * Without a reducer, the channel uses the default behavior:
     * the new value completely replaces the old value.
     */
    public static final Map<String, Channel<?>> SCHEMA =
            Map.of(
                    "question", Channels.base(() -> ""),
                    "category", Channels.base(() -> Category.UNKNOWN.name()),
                    "answer", Channels.base(() -> ""),
                    "feedback", Channels.base(() -> ""),
                    "score", Channels.base(() -> 0)
            );

    /**
     * Constructs a new InterviewState with the given initial data.
     * 
     * @param initData the initial state data as a map of key-value pairs
     */
    public InterviewState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * Retrieves the current interview question.
     * 
     * @return the question string, or empty string if not set
     */
    public String question() {
        return this.<String>value("question").orElse("");
    }

    /**
     * Retrieves the category as a String.
     * 
     * @return the category string, or "UNKNOWN" if not set
     */
    public String category() {
        return this.<String>value("category").orElse(Category.UNKNOWN.name());
    }

    /**
     * Retrieves the category as a Category enum.
     * 
     * @return the Category enum, or {@link Category#UNKNOWN} if not set
     */
    public Category categoryAs() {
        String cat = category();
        try {
            return Category.valueOf(cat);
        } catch (IllegalArgumentException e) {
            return Category.UNKNOWN;
        }
    }

    /**
     * Retrieves the current answer or coaching response.
     * 
     * @return the answer string, or empty string if not set
     */
    public String answer() {
        return this.<String>value("answer").orElse("");
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
}

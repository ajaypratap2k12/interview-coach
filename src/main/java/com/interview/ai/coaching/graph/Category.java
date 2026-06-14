package com.interview.ai.coaching.graph;

/**
 * Enum representing the category of an interview question.
 * 
 * <p>This enum is used to classify questions into specific technical domains,
 * allowing the coaching system to route questions to specialized agents
 * or apply domain-specific processing.</p>
 * 
 * <p>The categories are:</p>
 * <ul>
 *   <li>{@link #JAVA} - Core Java language questions (OOP, concurrency, collections, etc.)</li>
 *   <li>{@link #SPRING} - Spring framework questions (Spring Boot, Spring MVC, Spring Security, etc.)</li>
 *   <li>{@link #AWS} - Amazon Web Services questions (EC2, S3, Lambda, etc.)</li>
 *   <li>{@link #UNKNOWN} - Questions that don't fit into the above categories</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see InterviewState
 */
public enum Category {

    /**
     * Core Java language questions.
     * Covers topics like OOP, concurrency, collections, generics, JVM, etc.
     */
    JAVA,

    /**
     * Spring framework questions.
     * Covers Spring Boot, Spring MVC, Spring Security, Spring Data, etc.
     */
    SPRING,

    /**
     * Amazon Web Services questions.
     * Covers EC2, S3, Lambda, IAM, CloudFormation, etc.
     */
    AWS,

    /**
     * Unknown or uncategorized questions.
     * Used as default when a question doesn't fit other categories.
     */
    UNKNOWN
}

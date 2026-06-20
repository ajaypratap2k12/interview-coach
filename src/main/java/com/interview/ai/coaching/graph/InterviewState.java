package com.interview.ai.coaching.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the state of an interview coaching session using a planning-based
 * multi-agent workflow.
 *
 * <p>The workflow follows a planner → experts → aggregator pattern:</p>
 * <ol>
 *   <li><b>Planner</b> analyzes the question and produces an {@code executionPlan}
 *       listing which expert agents should be consulted.</li>
 *   <li><b>Experts</b> each write their domain-specific answer to their dedicated
 *       field (e.g. {@code javaAnswer}, {@code springAnswer}).</li>
 *   <li><b>Aggregator</b> combines all expert answers into {@code finalAnswer} and
 *       produces feedback/score.</li>
 * </ol>
 *
 * <h3>Field Categories</h3>
 * <ul>
 *   <li><b>Input:</b> {@code question} — the interview question being processed</li>
 *   <li><b>Plan:</b> {@code executionPlan}, {@code completedAgents} — planning and tracking</li>
 *   <li><b>Expert outputs:</b> dedicated answer fields per domain expert</li>
 *   <li><b>Aggregated output:</b> {@code finalAnswer}, {@code feedback}, {@code score}</li>
 * </ul>
 *
 * <h3>Channel Types</h3>
 * <ul>
 *   <li>List fields ({@code executionPlan}, {@code completedAgents}) use
 *       {@link Channels#appender} — values are appended, not overwritten.</li>
 *   <li>All other fields use {@link Channels#base} — new values overwrite old values.</li>
 * </ul>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 * @see AgentState
 * @see Channel
 * @see Channels
 */
public class InterviewState extends AgentState {

    /**
     * Schema definition for the interview state.
     *
     * <p>List fields use {@link Channels#appender(java.util.function.Supplier)} which
     * creates a channel that appends new values to the existing list rather than
     * replacing it. All other fields use {@link Channels#base(java.util.function.Supplier)}
     * which overwrites the previous value.</p>
     */
    public static final Map<String, Channel<?>> SCHEMA =
            Map.ofEntries(
                    // ── Input ──────────────────────────────────────────────
                    Map.entry("question", Channels.base(() -> "")),

                    // ── Planning ───────────────────────────────────────────
                    // Ordered list of agent IDs the planner has scheduled (e.g. ["SPRING","KAFKA"]).
                    // Appender reducer: each plan step is added, never overwritten.
                    Map.entry("executionPlan", Channels.appender(() -> new ArrayList<>())),

                    // Agent IDs that have already completed their work.
                    // Appender reducer: agents are added as they finish.
                    Map.entry("completedAgents", Channels.appender(() -> new ArrayList<>())),

                    // ── Domain Expert Answers ──────────────────────────────
                    // Each expert writes to its own dedicated field.
                    // Base (overwrite) reducer: each expert sets its answer exactly once.
                    Map.entry("javaAnswer", Channels.base(() -> "")),
                    Map.entry("springAnswer", Channels.base(() -> "")),
                    Map.entry("microserviceAnswer", Channels.base(() -> "")),
                    Map.entry("kafkaAnswer", Channels.base(() -> "")),
                    Map.entry("awsAnswer", Channels.base(() -> "")),

                    // ── Aggregated Output ──────────────────────────────────
                    // Combined answer from all experts, produced by the aggregator.
                    Map.entry("finalAnswer", Channels.base(() -> "")),

                    // Evaluation feedback produced by the evaluator/aggregator.
                    Map.entry("feedback", Channels.base(() -> "")),

                    // Numeric score (1-10) produced by the evaluator/aggregator.
                    Map.entry("score", Channels.base(() -> 0))
            );

    /**
     * Constructs a new InterviewState with the given initial data.
     *
     * @param initData the initial state data as a map of key-value pairs
     */
    public InterviewState(Map<String, Object> initData) {
        super(initData);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Input
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The interview question being processed.
     *
     * @return the question string, or empty string if not set
     */
    public String question() {
        return this.<String>value("question").orElse("");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Planning
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The execution plan produced by the planner agent.
     *
     * <p>Contains an ordered list of agent IDs (e.g. {@code ["JAVA", "SPRING", "KAFKA"]})
     * that should be consulted to answer the question. The planner writes this list
     * once, and expert nodes consume it to determine which agents to invoke.</p>
     *
     * <p>Uses an appender reducer — plan steps are accumulated, not overwritten.</p>
     *
     * @return the list of planned agent IDs, or an empty list if not set
     */
    public List<String> executionPlan() {
        return this.<List<String>>value("executionPlan").orElse(List.of());
    }

    /**
     * The list of agent IDs that have already completed their execution.
     *
     * <p>Used by the routing logic to determine which agent to invoke next
     * from the execution plan. Each expert node appends its agent ID to this
     * list after completing its work.</p>
     *
     * <p>Uses an appender reducer — completed agents are accumulated.</p>
     *
     * @return the list of completed agent IDs, or an empty list if not set
     */
    public List<String> completedAgents() {
        return this.<List<String>>value("completedAgents").orElse(List.of());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Domain Expert Answers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Answer produced by the Java expert agent.
     *
     * <p>Covers: Collections, Streams, Concurrency, JVM, Memory Management,
     * Design Patterns. Written once by the Java expert node.</p>
     *
     * @return the Java-specific answer, or empty string if not yet produced
     */
    public String javaAnswer() {
        return this.<String>value("javaAnswer").orElse("");
    }

    /**
     * Answer produced by the Spring expert agent.
     *
     * <p>Covers: Spring Boot, Spring Security, Spring Data JPA, Transactions,
     * Microservices, Kafka, Spring AI. Written once by the Spring expert node.</p>
     *
     * @return the Spring-specific answer, or empty string if not yet produced
     */
    public String springAnswer() {
        return this.<String>value("springAnswer").orElse("");
    }

    /**
     * Answer produced by the Microservices expert agent.
     *
     * <p>Covers: Saga Pattern, CQRS, Event Sourcing, API Gateway,
     * Service Discovery, Distributed Transactions. Written once by the
     * microservices expert node.</p>
     *
     * @return the microservices-specific answer, or empty string if not yet produced
     */
    public String microserviceAnswer() {
        return this.<String>value("microserviceAnswer").orElse("");
    }

    /**
     * Answer produced by the Kafka expert agent.
     *
     * <p>Covers: Consumer Groups, Offsets, Partitions, Exactly-Once Semantics,
     * Consumer Lag, Rebalancing. Written once by the Kafka expert node.</p>
     *
     * @return the Kafka-specific answer, or empty string if not yet produced
     */
    public String kafkaAnswer() {
        return this.<String>value("kafkaAnswer").orElse("");
    }

    /**
     * Answer produced by the AWS expert agent.
     *
     * <p>Covers: EC2, S3, IAM, VPC, ECS, EKS, Cloud Architecture.
     * Written once by the AWS expert node.</p>
     *
     * @return the AWS-specific answer, or empty string if not yet produced
     */
    public String awsAnswer() {
        return this.<String>value("awsAnswer").orElse("");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Aggregated Output
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The combined final answer produced by the aggregator agent.
     *
     * <p>Assembled from all relevant expert answers (e.g. {@code javaAnswer},
     * {@code springAnswer}, etc.) into a single coherent response. This is the
     * primary output of the workflow.</p>
     *
     * @return the combined answer, or empty string if not yet produced
     */
    public String finalAnswer() {
        return this.<String>value("finalAnswer").orElse("");
    }

    /**
     * Evaluation feedback produced by the evaluator/aggregator.
     *
     * <p>Contains structured feedback including technical accuracy score,
     * completeness score, and improvement suggestions.</p>
     *
     * @return the feedback string, or empty string if not set
     */
    public String feedback() {
        return this.<String>value("feedback").orElse("");
    }

    /**
     * Numeric evaluation score (1-10) produced by the evaluator/aggregator.
     *
     * <p>Typically calculated as the average of technical accuracy and
     * completeness scores.</p>
     *
     * @return the score, or 0 if not set
     */
    public Integer score() {
        return this.<Integer>value("score").orElse(0);
    }
}

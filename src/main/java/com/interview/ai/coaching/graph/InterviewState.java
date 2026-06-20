package com.interview.ai.coaching.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.state.Reducer;

/**
 * Represents the state of an interview coaching session using a planning-based
 * multi-agent workflow.
 *
 * <p>The workflow follows a planner → experts → aggregator pattern:</p>
 * <ol>
 *   <li><b>Planner</b> analyzes the question and produces an {@code executionPlan}
 *       listing which expert agents should be consulted.</li>
 *   <li><b>Experts</b> each contribute their domain-specific answer to the shared
 *       {@code expertResponses} map under their own key.</li>
 *   <li><b>Aggregator</b> iterates over all entries in {@code expertResponses} and
 *       combines them into {@code finalAnswer}.</li>
 * </ol>
 *
 * <h3>Extensibility (Open/Closed Principle)</h3>
 * <p>Adding a new expert (e.g. DATABASE, SECURITY, SYSTEM_DESIGN) requires only:</p>
 * <ul>
 *   <li>Creating a new expert node class</li>
 *   <li>Registering it in the graph configuration</li>
 * </ul>
 * <p>No changes are required in this state class or in the AggregatorAgentNode,
 * because both operate on the generic {@code expertResponses} map rather than
 * hardcoded per-expert fields.</p>
 *
 * <h3>Channel Types</h3>
 * <ul>
 *   <li>{@code expertResponses} uses {@link Channels#base(org.bsc.langgraph4j.state.Reducer, java.util.function.Supplier)}
 *       with a merge reducer — multiple expert nodes can each write their own entry
 *       without overwriting entries from other experts.</li>
 *   <li>List fields ({@code executionPlan}, {@code completedAgents}) use
 *       {@link Channels#appender(java.util.function.Supplier)} — values are appended.</li>
 *   <li>All other fields use {@link Channels#base(java.util.function.Supplier)} — overwrite.</li>
 * </ul>
 *
 * @author Interview AI Coaching Team
 * @version 3.0
 * @see AgentState
 * @see Channel
 * @see Channels
 */
public class InterviewState extends AgentState {

    /**
     * Reducer that merges two maps by copying the current map and adding/overwriting
     * entries from the update map. Each expert node contributes a single-entry map
     * {@code {AGENT_ID: answer}}, and this reducer accumulates them into the shared
     * {@code expertResponses} map without losing entries from other experts.
     *
     * <p>A Map-based shared state is preferable to individual per-expert fields because:</p>
     * <ul>
     *   <li>New experts can be added without modifying the state schema (Open/Closed Principle)</li>
     *   <li>The aggregator can iterate generically over all responses without hardcoded field references</li>
     *   <li>The reducer logic is centralized and consistent — every expert follows the same contract</li>
     * </ul>
     */
    private static final Reducer<Map<String, String>> MERGE_REDUCER = (current, update) -> {
        var merged = new HashMap<>(current);
        merged.putAll(update);
        return Map.copyOf(merged);
    };

    /**
     * Schema definition for the interview state.
     */
    public static final Map<String, Channel<?>> SCHEMA =
            Map.ofEntries(
                    // ── Input ──────────────────────────────────────────────
                    Map.entry("question", Channels.base(() -> "")),

                    // ── Planning ───────────────────────────────────────────
                    Map.entry("executionPlan", Channels.appender(() -> new ArrayList<>())),
                    Map.entry("completedAgents", Channels.appender(() -> new ArrayList<>())),

                    // ── Domain Expert Answers ──────────────────────────────
                    // Shared map: each expert writes {AGENT_ID: answer} under its own key.
                    // Merge reducer accumulates entries from all experts without overwriting.
                    Map.entry("expertResponses", Channels.base(MERGE_REDUCER, Map::of)),

                    // ── Aggregated Output ──────────────────────────────────
                    Map.entry("finalAnswer", Channels.base(() -> "")),
                    Map.entry("feedback", Channels.base(() -> "")),
                    Map.entry("score", Channels.base(() -> 0))
            );

    public InterviewState(Map<String, Object> initData) {
        super(initData);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Input
    // ════════════════════════════════════════════════════════════════════════

    public String question() {
        return this.<String>value("question").orElse("");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Planning
    // ════════════════════════════════════════════════════════════════════════

    public List<String> executionPlan() {
        return this.<List<String>>value("executionPlan").orElse(List.of());
    }

    public List<String> completedAgents() {
        return this.<List<String>>value("completedAgents").orElse(List.of());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Domain Expert Answers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Shared map of expert responses keyed by expert ID (e.g. "JAVA", "SPRING").
     *
     * <p>Each expert node writes a single-entry map {@code {AGENT_ID: answer}}.
     * The merge reducer accumulates entries from all experts into this shared map.
     * The aggregator iterates over all entries to produce {@code finalAnswer}.</p>
     *
     * <p>This design follows the Open/Closed Principle: adding a new expert requires
     * no changes to this class or to the aggregator — the new expert's entry is
     * automatically included.</p>
     *
     * @return unmodifiable map of expert ID to answer, empty if no experts have run yet
     */
    public Map<String, String> expertResponses() {
        return this.<Map<String, String>>value("expertResponses").orElse(Map.of());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Aggregated Output
    // ════════════════════════════════════════════════════════════════════════

    public String finalAnswer() {
        return this.<String>value("finalAnswer").orElse("");
    }

    public String feedback() {
        return this.<String>value("feedback").orElse("");
    }

    public Integer score() {
        return this.<Integer>value("score").orElse(0);
    }
}

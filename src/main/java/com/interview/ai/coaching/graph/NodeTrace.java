package com.interview.ai.coaching.graph;

import java.time.Instant;
import java.util.Map;

/**
 * Trace data for a single node execution within the graph.
 *
 * @param nodeName  the graph node name (e.g. "planner", "java_agent")
 * @param startedAt when execution started
 * @param endedAt   when execution ended
 * @param durationMs execution duration in milliseconds
 * @param input     input state snapshot (question, executionPlan, etc.)
 * @param output    output map returned by the node
 */
public record NodeTrace(
        String nodeName,
        Instant startedAt,
        Instant endedAt,
        long durationMs,
        Map<String, Object> input,
        Map<String, Object> output
) {
    /**
     * Returns a human-readable summary of this node trace.
     */
    public String summary() {
        return String.format("[%s] %dms", nodeName, durationMs);
    }
}

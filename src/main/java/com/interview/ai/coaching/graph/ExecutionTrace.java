package com.interview.ai.coaching.graph;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Complete execution trace for a single graph invocation.
 *
 * @param question        the original interview question
 * @param plannerOutput   raw LLM response from the planner
 * @param executionPlan   parsed execution plan (list of agent IDs)
 * @param nodeTraces      ordered list of node execution traces
 * @param aggregatorOutput the merged answer from the aggregator
 * @param evaluatorScore  numeric score from the evaluator
 * @param evaluatorFeedback the feedback from the evaluator
 */
public record ExecutionTrace(
        String question,
        String plannerOutput,
        List<String> executionPlan,
        List<NodeTrace> nodeTraces,
        String aggregatorOutput,
        int evaluatorScore,
        String evaluatorFeedback
) {
    /**
     * Prints the trace in a readable format for debugging.
     */
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        String line = "═".repeat(70);
        String thin = "─".repeat(70);

        sb.append("\n").append(line).append("\n");
        sb.append("  EXECUTION TRACE\n");
        sb.append(line).append("\n\n");

        sb.append("  Question:\n");
        sb.append("  ").append(question).append("\n\n");

        sb.append(thin).append("\n");
        sb.append("  PLANNER OUTPUT:\n");
        sb.append(thin).append("\n");
        sb.append("  ").append(plannerOutput).append("\n\n");

        sb.append("  Execution Plan: ").append(executionPlan).append("\n\n");

        sb.append(thin).append("\n");
        sb.append("  NODE EXECUTIONS:\n");
        sb.append(thin).append("\n");

        for (NodeTrace trace : nodeTraces) {
            sb.append(String.format("  %-20s %6d ms", trace.nodeName(), trace.durationMs()));
            if (trace.output().containsKey("executionPlan")) {
                sb.append("  → plan=").append(trace.output().get("executionPlan"));
            }
            if (trace.output().containsKey("completedAgents")) {
                sb.append("  → completed=").append(trace.output().get("completedAgents"));
            }
            sb.append("\n");
        }

        sb.append("\n").append(thin).append("\n");
        sb.append("  AGGREGATOR OUTPUT:\n");
        sb.append(thin).append("\n");
        if (aggregatorOutput != null && !aggregatorOutput.isBlank()) {
            // Truncate long outputs for readability
            String display = aggregatorOutput.length() > 500
                    ? aggregatorOutput.substring(0, 500) + "..."
                    : aggregatorOutput;
            for (String line2 : display.split("\n")) {
                sb.append("  ").append(line2).append("\n");
            }
        } else {
            sb.append("  (none)\n");
        }

        sb.append("\n").append(thin).append("\n");
        sb.append("  EVALUATOR:\n");
        sb.append(thin).append("\n");
        sb.append("  Score: ").append(evaluatorScore).append("/10\n");
        if (evaluatorFeedback != null && !evaluatorFeedback.isBlank()) {
            for (String line2 : evaluatorFeedback.split("\n")) {
                sb.append("  ").append(line2).append("\n");
            }
        }

        long totalMs = nodeTraces.stream().mapToLong(NodeTrace::durationMs).sum();
        sb.append("\n").append(line).append("\n");
        sb.append(String.format("  TOTAL: %d ms (%.1f s)\n", totalMs, totalMs / 1000.0));
        sb.append(line).append("\n");

        return sb.toString();
    }

    /**
     * Returns the trace as a structured map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "question", question,
                "plannerOutput", plannerOutput,
                "executionPlan", executionPlan,
                "nodeTraces", nodeTraces.stream().map(t -> Map.of(
                        "nodeName", t.nodeName(),
                        "durationMs", t.durationMs(),
                        "startedAt", t.startedAt().toString(),
                        "endedAt", t.endedAt().toString()
                )).toList(),
                "aggregatorOutput", aggregatorOutput != null ? aggregatorOutput : "",
                "evaluatorScore", evaluatorScore,
                "evaluatorFeedback", evaluatorFeedback != null ? evaluatorFeedback : "",
                "totalDurationMs", nodeTraces.stream().mapToLong(NodeTrace::durationMs).sum()
        );
    }
}

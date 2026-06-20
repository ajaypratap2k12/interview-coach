package com.interview.ai.coaching.service;

import com.interview.ai.coaching.graph.ExecutionTrace;
import com.interview.ai.coaching.graph.NodeTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects execution trace data during graph invocations.
 *
 * <p>Uses a {@link ThreadLocal} to store per-request trace data. Each graph
 * invocation creates a new trace context that nodes record into. After
 * execution, the trace is retrieved and stored in a history map.</p>
 *
 * @author Interview AI Coaching Team
 * @version 1.0
 */
@Slf4j
@Component
public class TraceCollector {

    private final ThreadLocal<TraceContext> currentContext = new ThreadLocal<>();
    private final ConcurrentHashMap<String, ExecutionTrace> traceHistory = new ConcurrentHashMap<>();

    /**
     * Starts a new trace for the given question.
     *
     * @param question the interview question
     */
    public void startTrace(String question) {
        TraceContext ctx = new TraceContext(question);
        currentContext.set(ctx);
        log.debug("Trace started for question: {}", question);
    }

    /**
     * Records a node execution. Called by the wrapper in InterviewGraphConfig.
     *
     * @param nodeName   the node name
     * @param startedAt  execution start time
     * @param endedAt    execution end time
     * @param input      input state snapshot
     * @param output     output map from the node
     */
    public void recordNode(String nodeName, Instant startedAt, Instant endedAt,
                           Map<String, Object> input, Map<String, Object> output) {
        TraceContext ctx = currentContext.get();
        if (ctx == null) return;

        long durationMs = endedAt.toEpochMilli() - startedAt.toEpochMilli();
        NodeTrace trace = new NodeTrace(nodeName, startedAt, endedAt, durationMs, input, output);
        ctx.nodeTraces.add(trace);

        log.debug("Recorded node '{}': {}ms", nodeName, durationMs);
    }

    /**
     * Finalizes the trace and stores it in history.
     *
     * @param aggregatorOutput the aggregator's merged answer
     * @param evaluatorScore   the evaluator's score
     * @param evaluatorFeedback the evaluator's feedback
     * @return the finalized trace
     */
    public ExecutionTrace finishTrace(String aggregatorOutput, int evaluatorScore, String evaluatorFeedback) {
        TraceContext ctx = currentContext.get();
        if (ctx == null) {
            log.warn("No trace context found when finishing trace");
            return null;
        }

        ExecutionTrace trace = new ExecutionTrace(
                ctx.question,
                ctx.plannerOutput,
                List.copyOf(ctx.executionPlan),
                List.copyOf(ctx.nodeTraces),
                aggregatorOutput,
                evaluatorScore,
                evaluatorFeedback
        );

        String traceId = "trace-" + System.currentTimeMillis();
        traceHistory.put(traceId, trace);
        currentContext.remove();

        log.debug("Trace finalized: {} nodes, {}ms total",
                trace.nodeTraces().size(),
                trace.nodeTraces().stream().mapToLong(NodeTrace::durationMs).sum());

        return trace;
    }

    /**
     * Retrieves a trace by ID.
     */
    public ExecutionTrace getTrace(String traceId) {
        return traceHistory.get(traceId);
    }

    /**
     * Returns all stored traces.
     */
    public Map<String, ExecutionTrace> getAllTraces() {
        return Map.copyOf(traceHistory);
    }

    /**
     * Clears all stored traces.
     */
    public void clearTraces() {
        traceHistory.clear();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Internal context for building a trace during execution
    // ════════════════════════════════════════════════════════════════════════

    private static class TraceContext {
        final String question;
        String plannerOutput = "";
        final List<String> executionPlan = new ArrayList<>();
        final List<NodeTrace> nodeTraces = new ArrayList<>();

        TraceContext(String question) {
            this.question = question;
        }
    }
}

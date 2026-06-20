package com.interview.ai.coaching.service;

import com.interview.ai.coaching.graph.ExecutionTrace;
import com.interview.ai.coaching.graph.InterviewState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Service for executing the interview coaching graph with execution tracing.
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 */
@Slf4j
@Service
public class InterviewGraphService {

    private final CompiledGraph<InterviewState> compiledGraph;
    private final TraceCollector traceCollector;

    public InterviewGraphService(CompiledGraph<InterviewState> compiledGraph,
                                 TraceCollector traceCollector) {
        this.compiledGraph = compiledGraph;
        this.traceCollector = traceCollector;
    }

    /**
     * Processes an interview question through the graph pipeline.
     *
     * @param question the interview question
     * @return the generated answer
     */
    public String ask(String question) {
        return askWithTrace(question).answer();
    }

    /**
     * Processes an interview question and returns both the answer and execution trace.
     *
     * @param question the interview question
     * @return result containing the answer and execution trace
     */
    public AskResult askWithTrace(String question) {
        log.info("Graph invocation started: question='{}'", question);

        // Start trace collection
        traceCollector.startTrace(question);

        try {
            Map<String, Object> initialState = Map.of("question", question);

            Optional<InterviewState> result = compiledGraph.invoke(
                    initialState,
                    RunnableConfig.builder().build()
            );

            InterviewState finalState = result.orElseThrow(
                    () -> new RuntimeException("Graph execution failed")
            );

            // Finalize trace with evaluator outputs
            ExecutionTrace trace = traceCollector.finishTrace(
                    finalState.finalAnswer(),
                    finalState.score(),
                    finalState.feedback()
            );

            log.info("Graph invocation completed: score={}, {} nodes traced",
                    finalState.score(),
                    trace != null ? trace.nodeTraces().size() : 0);

            return new AskResult(finalState.finalAnswer(), trace);

        } catch (Exception e) {
            traceCollector.finishTrace("ERROR: " + e.getMessage(), 0, e.getMessage());
            throw e;
        }
    }

    /**
     * Generates a Mermaid diagram of the graph structure.
     */
    public String getMermaidDiagram() {
        GraphRepresentation representation = compiledGraph.getGraph(
                GraphRepresentation.Type.MERMAID,
                "Interview Coaching Graph"
        );
        return representation.content();
    }

    /**
     * Result of a graph invocation including the answer and trace.
     */
    public record AskResult(String answer, ExecutionTrace trace) {}
}

package com.interview.ai.coaching.service;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Service for executing the interview coaching graph.
 * 
 * This service provides a simple interface for processing interview
 * questions through the langgraph4j graph pipeline. It manages the
 * lifecycle of graph execution and provides logging for debugging.
 * 
 * <p>The service follows these steps for each request:</p>
 * <ol>
 *   <li>Creates an initial {@link InterviewState} with the question</li>
 *   <li>Executes the compiled graph with the initial state</li>
 *   <li>Extracts and returns the answer from the final state</li>
 * </ol>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see CompiledGraph
 * @see InterviewState
 */
@Slf4j
@Service
public class InterviewGraphService {

    private final CompiledGraph<InterviewState> compiledGraph;

    /**
     * Constructs the service with the compiled graph dependency.
     * 
     * @param compiledGraph the compiled interview coaching graph
     */
    public InterviewGraphService(CompiledGraph<InterviewState> compiledGraph) {
        this.compiledGraph = compiledGraph;
    }

    /**
     * Processes an interview question through the graph pipeline.
     * 
     * <p>This method creates an initial state with the question,
     * executes the graph, and returns the generated answer.</p>
     * 
     * @param question the interview question to process
     * @return the generated answer from the graph
     */
    public String ask(String question) {
        Map<String, Object> initialState = Map.of("question", question);
        InterviewState inputState = new InterviewState(initialState);

        log.info("Initial state: question='{}'", inputState.question());

        Optional<InterviewState> result = compiledGraph.invoke(
                initialState,
                RunnableConfig.builder().build()
        );

        InterviewState finalState = result.orElseThrow(
                () -> new RuntimeException("Graph execution failed")
        );

        log.info("Final state: answer='{}'", finalState.answer());

        return finalState.answer();
    }

    /**
     * Generates a Mermaid diagram of the graph structure.
     * 
     * <p>This method uses LangGraph4j's built-in diagram generation
     * to create a Mermaid flowchart representation of the graph.</p>
     * 
     * @return Mermaid diagram string
     */
    public String getMermaidDiagram() {
        GraphRepresentation representation = compiledGraph.getGraph(
                GraphRepresentation.Type.MERMAID,
                "Interview Coaching Graph"
        );
        return representation.content();
    }
}

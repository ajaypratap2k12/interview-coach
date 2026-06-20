package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Supervisor orchestration node that reads the execution plan and determines
 * which expert agent should execute next.
 *
 * <p>This node is purely stateless — it does NOT call any LLM. It reads
 * {@code executionPlan} and {@code completedAgents} from state, then
 * decides the next action:</p>
 * <ul>
 *   <li>If uncompleted agents remain → returns empty map (conditional edge handles routing)</li>
 *   <li>If all agents completed → returns empty map (conditional edge routes to evaluator)</li>
 * </ul>
 *
 * <p>The actual routing is handled by conditional edges in the graph configuration.
 * This node's only job is to log the current orchestration state.</p>
 *
 * <h3>Logged Information</h3>
 * <ul>
 *   <li>Execution Plan — full ordered list of planned agents</li>
 *   <li>Current Agent — the next agent to execute</li>
 *   <li>Remaining Agents — agents still pending</li>
 *   <li>Completed Agents — agents that have finished</li>
 * </ul>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 * @see InterviewState
 */
@Slf4j
public class SupervisorAgentNode {

    private static final String NODE_NAME = "supervisor";

    /**
     * Executes the supervisor orchestration node.
     *
     * <p>Reads the current state, logs orchestration details, and returns an empty
     * map. This node produces no state updates — routing is handled entirely by
     * conditional edges in the graph.</p>
     *
     * @param state the current interview state
     * @return empty map (no state updates)
     */
    public Map<String, Object> execute(InterviewState state) {
        List<String> plan = state.executionPlan();
        List<String> completed = state.completedAgents();

        String currentAgent = plan.stream()
                .filter(agent -> !completed.contains(agent))
                .findFirst()
                .orElse(null);

        List<String> remaining = currentAgent == null
                ? List.of()
                : plan.subList(plan.indexOf(currentAgent) + 1, plan.size());

        log.info("[{}] ─────────────────────────────────────────────", NODE_NAME);
        log.info("[{}] Execution Plan   : {}", NODE_NAME, plan);
        log.info("[{}] Current Agent    : {}", NODE_NAME, currentAgent == null ? "(none — all done)" : currentAgent);
        log.info("[{}] Remaining Agents : {}", NODE_NAME, remaining.isEmpty() ? "(none)" : remaining);
        log.info("[{}] Completed Agents : {}", NODE_NAME, completed.isEmpty() ? "(none)" : completed);
        log.info("[{}] expertResponses  : {}", NODE_NAME, state.expertResponses());
        log.info("[{}] ─────────────────────────────────────────────", NODE_NAME);

        return Map.of();
    }
}

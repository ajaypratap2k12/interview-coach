package com.interview.ai.coaching.service;

import com.interview.ai.coaching.graph.InterviewSessionState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing multi-turn interview sessions.
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 */
@Slf4j
@Service
public class InterviewSessionService {

    private final CompiledGraph<InterviewSessionState> questionGraph;
    private final CompiledGraph<InterviewSessionState> evaluationGraph;

    public InterviewSessionService(
            @Qualifier("questionGraph") CompiledGraph<InterviewSessionState> questionGraph,
            @Qualifier("evaluationGraph") CompiledGraph<InterviewSessionState> evaluationGraph) {
        this.questionGraph = questionGraph;
        this.evaluationGraph = evaluationGraph;
    }

    /**
     * Result of starting a new session.
     */
    public record SessionStartResult(String sessionId, InterviewSessionState state) {}

    /**
     * Starts a new interview session and generates the first question.
     * 
     * @return result containing sessionId and initial state
     */
    public SessionStartResult startSession() {
        String sessionId = UUID.randomUUID().toString();
        log.info("Starting new interview session: {}", sessionId);

        Map<String, Object> initialState = Map.of(
                "currentQuestion", "",
                "candidateAnswer", "",
                "feedback", "",
                "score", 0,
                "nextQuestion", ""
        );

        RunnableConfig config = RunnableConfig.builder()
                .threadId(sessionId)
                .build();

        Optional<InterviewSessionState> result = questionGraph.invoke(initialState, config);
        InterviewSessionState state = result.orElseThrow(() ->
                new RuntimeException("Failed to generate first question"));

        log.info("Session {} started. First question: {}", sessionId, state.currentQuestion());
        return new SessionStartResult(sessionId, state);
    }

    /**
     * Submits a candidate's answer and returns evaluation with next question.
     * 
     * @param sessionId the session ID from startSession
     * @param currentQuestion the current question being answered
     * @param candidateAnswer the candidate's answer
     * @return the updated state with feedback, score, and next question
     */
    public InterviewSessionState submitAnswer(String sessionId, String currentQuestion, String candidateAnswer) {
        log.info("Session {}: Submitting answer for question '{}'", sessionId, currentQuestion);

        Map<String, Object> stateUpdate = Map.of(
                "currentQuestion", currentQuestion,
                "candidateAnswer", candidateAnswer
        );

        RunnableConfig config = RunnableConfig.builder()
                .threadId(sessionId)
                .build();

        Optional<InterviewSessionState> result = evaluationGraph.invoke(stateUpdate, config);
        InterviewSessionState state = result.orElseThrow(() ->
                new RuntimeException("Failed to evaluate answer"));

        log.info("Session {}: Score={}, Next question='{}'",
                sessionId, state.score(), state.nextQuestion());
        return state;
    }
}

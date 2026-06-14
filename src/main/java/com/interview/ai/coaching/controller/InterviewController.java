package com.interview.ai.coaching.controller;

import com.interview.ai.coaching.graph.InterviewSessionState;
import com.interview.ai.coaching.service.InterviewGraphService;
import com.interview.ai.coaching.service.InterviewSessionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Interview Coaching application.
 * 
 * This controller provides HTTP endpoints for interacting with the
 * interview coaching system. It handles incoming requests and routes
 * them to the appropriate graph execution logic.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET /interview} - Process interview question through graph</li>
 *   <li>{@code GET /graph/diagram} - Get Mermaid diagram of graph structure</li>
 *   <li>{@code POST /session/start} - Start a new interview session</li>
 *   <li>{@code POST /session/answer} - Submit answer and get next question</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see InterviewGraphService
 * @see InterviewSessionService
 */
@RestController
public class InterviewController {

    private final InterviewGraphService interviewGraphService;
    private final InterviewSessionService interviewSessionService;

    /**
     * Constructs the controller with service dependencies.
     * 
     * @param interviewGraphService the service for single-question graph execution
     * @param interviewSessionService the service for multi-turn session execution
     */
    public InterviewController(InterviewGraphService interviewGraphService,
                               InterviewSessionService interviewSessionService) {
        this.interviewGraphService = interviewGraphService;
        this.interviewSessionService = interviewSessionService;
    }

    /**
     * Processes an interview question through the graph pipeline.
     * 
     * @param question the interview question to process
     * @return a map containing the question and generated answer
     */
    @GetMapping("/interview")
    public Map<String, String> interview(@RequestParam String question) {
        String answer = interviewGraphService.ask(question);
        return Map.of(
                "question", question,
                "answer", answer
        );
    }

    /**
     * Returns a Mermaid diagram of the graph structure.
     * 
     * @return Mermaid diagram string
     */
    @GetMapping("/graph/diagram")
    public String graphDiagram() {
        return interviewGraphService.getMermaidDiagram();
    }

    // ==================== Session Endpoints ====================

    /**
     * Starts a new interview session and returns the first question.
     * 
     * @return a map containing sessionId and first question
     */
    @PostMapping("/session/start")
    public Map<String, String> startSession() {
        InterviewSessionService.SessionStartResult result = interviewSessionService.startSession();
        return Map.of(
                "sessionId", result.sessionId(),
                "currentQuestion", result.state().currentQuestion()
        );
    }

    /**
     * Submits a candidate's answer and returns evaluation with next question.
     * 
     * @param request the request containing sessionId, currentQuestion, and candidateAnswer
     * @return a map containing feedback, score, and nextQuestion
     */
    @PostMapping("/session/answer")
    public Map<String, Object> submitAnswer(@RequestBody AnswerRequest request) {
        InterviewSessionState state = interviewSessionService.submitAnswer(
                request.sessionId(),
                request.currentQuestion(),
                request.candidateAnswer()
        );

        return Map.of(
                "feedback", state.feedback(),
                "score", state.score(),
                "nextQuestion", state.nextQuestion()
        );
    }

    /**
     * Request body for submitting an answer.
     */
    public record AnswerRequest(
            String sessionId,
            String currentQuestion,
            String candidateAnswer
    ) {}
}

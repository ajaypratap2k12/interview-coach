package com.interview.ai.coaching.controller;

import com.interview.ai.coaching.graph.InterviewSessionState;
import com.interview.ai.coaching.service.InterviewGraphService;
import com.interview.ai.coaching.service.InterviewSessionService;
import com.interview.ai.coaching.service.TraceCollector;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Interview Coaching application.
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 */
@RestController
public class InterviewController {

    private final InterviewGraphService interviewGraphService;
    private final InterviewSessionService interviewSessionService;
    private final TraceCollector traceCollector;

    public InterviewController(InterviewGraphService interviewGraphService,
                               InterviewSessionService interviewSessionService,
                               TraceCollector traceCollector) {
        this.interviewGraphService = interviewGraphService;
        this.interviewSessionService = interviewSessionService;
        this.traceCollector = traceCollector;
    }

    /**
     * Processes an interview question through the graph pipeline.
     */
    @GetMapping("/interview")
    public Map<String, String> interview(@RequestParam String question) {
        String answer = interviewGraphService.ask(question);
        return Map.of(
                "question", question,
                "finalAnswer", answer
        );
    }

    /**
     * Processes an interview question and returns the full execution trace.
     *
     * <p>Returns JSON containing: question, plannerOutput, executionPlan,
     * nodeTraces (with timing), aggregatorOutput, evaluatorScore,
     * evaluatorFeedback, and totalDurationMs.</p>
     */
    @GetMapping("/interview/trace")
    public Map<String, Object> interviewWithTrace(@RequestParam String question) {
        InterviewGraphService.AskResult result = interviewGraphService.askWithTrace(question);
        return result.trace().toMap();
    }

    /**
     * Processes an interview question and returns the trace in readable text format.
     */
    @GetMapping("/interview/trace/text")
    public String interviewWithTraceText(@RequestParam String question) {
        InterviewGraphService.AskResult result = interviewGraphService.askWithTrace(question);
        return result.trace().prettyPrint();
    }

    /**
     * Returns a Mermaid diagram of the graph structure.
     */
    @GetMapping("/graph/diagram")
    public String graphDiagram() {
        return interviewGraphService.getMermaidDiagram();
    }

    // ==================== Session Endpoints ====================

    @PostMapping("/session/start")
    public Map<String, String> startSession() {
        InterviewSessionService.SessionStartResult result = interviewSessionService.startSession();
        return Map.of(
                "sessionId", result.sessionId(),
                "currentQuestion", result.state().currentQuestion()
        );
    }

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

    public record AnswerRequest(
            String sessionId,
            String currentQuestion,
            String candidateAnswer
    ) {}
}

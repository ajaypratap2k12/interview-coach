package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Node implementation for Java-specific interview questions.
 * 
 * This class represents a specialized node in the interview coaching graph
 * that handles Java-specific questions. It uses Spring AI's {@link ChatClient}
 * to interact with an LLM configured as a senior Java interviewer.
 * 
 * <p>Topics covered:</p>
 * <ul>
 *   <li>Collections (HashMap, ArrayList, ConcurrentHashMap, etc.)</li>
 *   <li>Streams (stream API, collectors, parallel streams)</li>
 *   <li>Concurrency (threads, locks, atomic variables, executors)</li>
 *   <li>JVM (garbage collection, class loading, JIT compilation)</li>
 *   <li>Memory Management (heap, stack, memory leaks, profiling)</li>
 *   <li>Design Patterns (singleton, factory, observer, etc.)</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see ChatClient
 * @see InterviewState
 * @see org.bsc.langgraph4j.action.NodeAction
 */
@Slf4j
@RequiredArgsConstructor
public class JavaInterviewAgentNode {

    private static final String NODE_NAME = "java_agent";

    private final ChatClient chatClient;

    /**
     * Executes the Java interview agent node.
     * 
     * @param state the current interview state containing the question
     * @return a map containing the state updates with the generated answer
     */
    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: question='{}', category={}, answer='{}'",
                NODE_NAME, state.question(), state.category(), state.answer());

        String question = state.question();

        String answer = chatClient.prompt()
                .user("""
                    You are a senior Java interviewer with 15+ years of experience.
                    
                    Topics you cover:
                    * Collections (HashMap internals, ConcurrentHashMap, ArrayList vs LinkedList)
                    * Streams (stream API, collectors, parallel streams, lazy evaluation)
                    * Concurrency (ReentrantLock, AtomicReference, CompletableFuture, Fork/Join)
                    * JVM (GC algorithms, JIT compilation, class loading, bytecode)
                    * Memory Management (heap/stack, memory leaks, JFR, heap dumps)
                    * Design Patterns (singleton, factory, strategy, observer)
                    
                    The candidate has 10 years of Java experience.
                    Provide concise but technically accurate answers.
                    Include code examples where relevant.
                    Cover edge cases and common pitfalls.
                    
                    Question:
                    %s
                    """.formatted(question))
                .call()
                .content();

        Map<String, Object> outputUpdates = Map.of("answer", answer);
        log.info("[{}] Output updates: answer='{}...'", NODE_NAME, answer.substring(0, Math.min(answer.length(), 100)));
        log.info("[{}] Node execution complete", NODE_NAME);

        return outputUpdates;
    }
}

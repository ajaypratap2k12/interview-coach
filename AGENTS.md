# AGENTS.md - Interview Coaching Application

## Quick Reference

```bash
# Build
.\mvnw compile

# Run
.\mvnw spring-boot:run

# Check graph diagram
curl "http://localhost:8080/graph/diagram"
```

## Tech Stack

- **Java 21**, **Spring Boot 4.1.0**, **Spring AI 2.0.0-RC2** (Release Candidate)
- **LangGraph4j 1.8.17** (pinned - newer versions have breaking API changes)
- OpenRouter API with `OPEN_ROUTER_API_KEY` env var

## Architecture

```
src/main/java/com/interview/ai/coaching/
├── config/
│   ├── AIConfig.java              # ChatClient bean
│   ├── InterviewGraphConfig.java  # Single-question graph
│   └── InterviewSessionConfig.java # Multi-turn session graphs
├── controller/
│   └── InterviewController.java   # GET /interview, GET /graph/diagram
├── graph/
│   ├── Category.java              # Enum: JAVA, SPRING, AWS, UNKNOWN
│   ├── InterviewState.java        # State for single-question flow
│   └── InterviewSessionState.java # State for multi-turn session
├── nodes/
│   ├── ClassifierAgentNode.java   # Classifies question → JAVA/SPRING/AWS
│   ├── EvaluatorAgentNode.java    # Evaluates answer → feedback + score
│   ├── GenerateFollowUpNode.java  # Generates follow-up question
│   ├── GenerateQuestionNode.java  # Generates initial question
│   ├── InterviewAgentNode.java    # General answer node (fallback)
│   ├── JavaInterviewAgentNode.java # Java-specific: Collections, Streams, Concurrency, JVM
│   ├── SessionEvaluateAnswerNode.java # Session evaluation with feedback
│   ├── SpringInterviewAgentNode.java # Spring-specific: Boot, Security, JPA, Kafka, AI
│   └── AwsInterviewAgentNode.java # AWS-specific: EC2, S3, IAM, VPC, ECS, EKS
└── service/
    └── InterviewGraphService.java # Graph execution wrapper
```

## Multi-Turn Session Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Interview Session                        │
├─────────────────────────────────────────────────────────────┤
│  1. questionGraph: START → generate_question → END         │
│     → Returns currentQuestion                              │
│                                                             │
│  2. External: Candidate provides candidateAnswer           │
│                                                             │
│  3. evaluationGraph: START → evaluate_answer →             │
│     generate_followup → END                                │
│     → Returns feedback, score, nextQuestion                │
│                                                             │
│  4. Repeat from step 1 with nextQuestion                   │
└─────────────────────────────────────────────────────────────┘
```

## State Schema

```java
public static final Map<String, Channel<?>> SCHEMA = Map.of(
    "question", Channels.base(() -> ""),
    "category", Channels.base(() -> Category.UNKNOWN),
    "answer",   Channels.base(() -> ""),
    "feedback", Channels.base(() -> ""),
    "score",    Channels.base(() -> 0)
);
```

All fields use `Channels.base()` - new values overwrite old values completely.

**Merging behavior**: When multiple nodes return different keys, LangGraph4j merges by union. If two nodes return the same key, the later node's value wins.

## Critical LangGraph4j 1.8.17 API Notes

### State Construction

```java
// Must pass SCHEMA + state factory
new StateGraph<>(InterviewState.SCHEMA, InterviewState::new)
```

### Adding Nodes

```java
// NodeAction is NOT directly accepted by addNode - must wrap in AsyncNodeAction
AsyncNodeAction<InterviewState> agentAction = state -> {
    var result = node.execute(state);
    return CompletableFuture.completedFuture(result);
};
graph.addNode("node_id", agentAction);
```

### State Channels

```java
// Channels.base(() -> "") = overwrite (no reducer)
// Access with type-safe: state.<String>value("key").orElse("")
```

### Conditional Edges

```java
// Router returns Command with target node name
AsyncCommandAction<InterviewState> router = (state, config) -> {
    Category category = state.category();
    String targetNode = category.name().toLowerCase() + "_agent";
    return CompletableFuture.completedFuture(new Command(targetNode));
};

// Map category names to node IDs
graph.addConditionalEdges("classifier", router,
    Map.of(
        "JAVA", "java_agent",
        "SPRING", "spring_agent",
        "AWS", "aws_agent",
        "UNKNOWN", "general_agent"
    ));
```

### Graph Execution

```java
// invoke() returns Optional<InterviewState>, NOT InterviewState
Optional<InterviewState> result = graph.invoke(initialState, RunnableConfig.builder().build());
InterviewState finalState = result.orElseThrow();
```

## Environment

Set `OPEN_ROUTER_API_KEY` before running. Config in `application.properties`:
```properties
spring.ai.openai.api-key=${OPEN_ROUTER_API_KEY}
spring.ai.openai.chat.options.model=nvidia/nemotron-3-super-120b-a12b:free
spring.ai.openai.base-url=https://openrouter.ai/api/v1
```

## Common Mistakes

1. Using `StateGraph.compile()` bean instead of `CompiledGraph` - Spring injection requires `CompiledGraph<InterviewState>`
2. Passing `NodeAction` directly to `addNode()` - must wrap in `AsyncNodeAction` with `CompletableFuture`
3. Using `Channels.lastValue()` (doesn't exist) - use `Channels.base(() -> "")` for overwrite behavior
4. Using `./mvnw` (Linux) - use `.\mvnw` on Windows

# AGENTS.md - Interview Coaching Application

## Quick Reference

```bash
# Build
.\mvnw compile

# Run
.\mvnw spring-boot:run

# Check graph diagram
curl "http://localhost:8080/graph/diagram"

# Check execution trace
curl "http://localhost:8080/interview/trace?question=Explain+HashMap+internals"
```

## Tech Stack

- **Java 21**, **Spring Boot 4.1.0**, **Spring AI 2.0.0-RC2** (Release Candidate)
- **LangGraph4j 1.8.17** (pinned - newer versions have breaking API changes)
- OpenRouter API with `OPEN_ROUTER_API_KEY` env var

## Architecture

```
src/main/java/com/interview/ai/coaching/
├── config/
│   ├── AIConfig.java                # ChatClient bean
│   ├── InterviewGraphConfig.java    # Graph topology + routing + tracing
│   └── InterviewSessionConfig.java  # Multi-turn session graphs
├── controller/
│   └── InterviewController.java     # REST endpoints (incl. /interview/trace)
├── graph/
│   ├── InterviewState.java          # State schema (planner-based flow)
│   ├── InterviewSessionState.java   # State for multi-turn session
│   ├── ExecutionTrace.java          # Trace data + prettyPrint()
│   └── NodeTrace.java              # Per-node execution trace record
├── nodes/
│   ├── PlannerAgentNode.java        # LLM planner — returns JSON array of agent IDs
│   ├── SupervisorAgentNode.java     # Orchestrator — no LLM, logs state
│   ├── JavaExpertNode.java          # Java domain only: Collections, Streams, Concurrency, JVM
│   ├── SpringExpertNode.java        # Spring domain only: Boot, Security, JPA, AI
│   ├── AwsExpertNode.java           # AWS domain only: EC2, S3, IAM, VPC, ECS, EKS
│   ├── MicroserviceExpertNode.java  # Microservices domain only: Saga, CQRS, Event Sourcing
│   ├── KafkaExpertNode.java         # Kafka domain only: Topics, Partitions, Consumer Groups
│   ├── AggregatorAgentNode.java     # Merges expert answers into one coherent answer
│   ├── EvaluatorAgentNode.java      # Scores finalAnswer → feedback + score
│   ├── GenerateQuestionNode.java    # Generates interview question (session)
│   ├── GenerateFollowUpNode.java    # Generates follow-up question (session)
│   └── SessionEvaluateAnswerNode.java # Evaluates answer in session context
└── service/
    ├── InterviewGraphService.java   # Graph execution + trace capture
    ├── InterviewSessionService.java # Multi-turn session orchestration
    └── TraceCollector.java          # ThreadLocal trace collection per request
```

## Graph Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Planning-Based Multi-Agent Flow                   │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  START → planner                                                     │
│            │                                                         │
│            ↓                                                         │
│         supervisor  (logs orchestration state, no LLM)               │
│            │                                                         │
│            ↓  (conditional: executionPlan[0])                        │
│            ├── JAVA          → java_agent                            │
│            ├── SPRING        → spring_agent                          │
│            ├── AWS           → aws_agent                             │
│            ├── MICROSERVICES → microservice_agent                    │
│            └── KAFKA         → kafka_agent                           │
│                                  │                                   │
│                                  ↓  (expertRouter: next or done)     │
│                             aggregator  (merges all answers)         │
│                                  │                                   │
│                                  ↓                                   │
│                             evaluator  (scores finalAnswer)          │
│                                  │                                   │
│                                  ↓                                   │
│                                END                                   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Example Trace

Question: "Transaction handling in Spring Boot microservices using Kafka"

```
Planner output: ["SPRING","MICROSERVICES","KAFKA"]
Execution Plan: SPRING → MICROSERVICES → KAFKA

Node Executions:
  planner             1234ms
  supervisor             2ms
  spring_agent        4567ms
  microservice_agent  3891ms
  kafka_agent         3210ms
  aggregator          5678ms
  evaluator           2345ms

Total: 20927ms (20.9s)
Score: 8/10
```

## State Schema

```java
public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
    // Input
    Map.entry("question",        Channels.base(() -> "")),

    // Planning (appender reducer — values accumulate, never overwrite)
    Map.entry("executionPlan",   Channels.appender(() -> new ArrayList<>())),
    Map.entry("completedAgents", Channels.appender(() -> new ArrayList<>())),

    // Domain Expert Answers (base reducer — each expert sets once)
    Map.entry("javaAnswer",          Channels.base(() -> "")),
    Map.entry("springAnswer",        Channels.base(() -> "")),
    Map.entry("microserviceAnswer",  Channels.base(() -> "")),
    Map.entry("kafkaAnswer",         Channels.base(() -> "")),
    Map.entry("awsAnswer",           Channels.base(() -> "")),

    // Aggregated Output
    Map.entry("finalAnswer",     Channels.base(() -> "")),
    Map.entry("feedback",        Channels.base(() -> "")),
    Map.entry("score",           Channels.base(() -> 0))
);
```

Key fields:
- `executionPlan` — ordered list of agent IDs from the planner (appender reducer)
- `completedAgents` — agents that have finished execution (appender reducer)
- `*Answer` — each expert writes to its own dedicated field
- `finalAnswer` — assembled by the aggregator from all expert answers
- `feedback` / `score` — produced by the evaluator

## REST API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/interview?question=...` | Process single question through graph |
| `GET` | `/interview/trace?question=...` | Process question with full execution trace (JSON) |
| `GET` | `/interview/trace/text?question=...` | Process question with trace (readable text) |
| `GET` | `/graph/diagram` | Get Mermaid diagram of graph structure |
| `POST` | `/session/start` | Start new interview session, returns first question |
| `POST` | `/session/answer` | Submit answer, returns feedback + next question |

## Critical LangGraph4j 1.8.17 API Notes

### State Construction

```java
new StateGraph<>(InterviewState.SCHEMA, InterviewState::new)
```

### Adding Nodes

```java
AsyncNodeAction<InterviewState> agentAction = state -> {
    var result = node.execute(state);
    return CompletableFuture.completedFuture(result);
};
graph.addNode("node_id", agentAction);
```

### State Channels

```java
// Channels.base(() -> "") = overwrite (no reducer)
// Channels.appender(() -> new ArrayList<>()) = append to list (reducer)
// Access with type-safe: state.<String>value("key").orElse("")
```

### Conditional Edges with Fan-Out

```java
// Expert router: finds next uncompleted agent or dispatches to aggregator
AsyncCommandAction<InterviewState> expertRouter = (state, config) -> {
    List<String> plan = state.executionPlan();
    List<String> completed = state.completedAgents();
    String nextAgent = plan.stream()
            .filter(agent -> !completed.contains(agent))
            .findFirst()
            .orElse(null);
    if (nextAgent == null) {
        return CompletableFuture.completedFuture(new Command("aggregator"));
    }
    String nodeId = AGENT_NODE_MAP.getOrDefault(nextAgent, "aggregator");
    return CompletableFuture.completedFuture(new Command(nodeId));
};
```

### Graph Execution

```java
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

1. Using `StateGraph.compile()` bean instead of `CompiledGraph` — Spring injection requires `CompiledGraph<InterviewState>`
2. Passing `NodeAction` directly to `addNode()` — must wrap in `AsyncNodeAction` with `CompletableFuture`
3. Using `Channels.lastValue()` (doesn't exist) — use `Channels.base(() -> "")` for overwrite behavior
4. Using `./mvnw` (Linux) — use `.\mvnw` on Windows
5. Using `Map.of()` with more than 10 entries — use `Map.ofEntries()` with `Map.entry()` for 11+ schema fields
6. Forgetting to update `expertRoutingMap` when adding new expert nodes — all expert node IDs must be in the map

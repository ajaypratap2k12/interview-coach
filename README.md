# Interview Coach

AI-powered interview coaching application built with Spring Boot and LangGraph4j. Uses a planning-based multi-agent architecture with domain experts, aggregation, and evaluation.

## Features

- **Planning-based multi-agent workflow** — Planner → Supervisor → Experts → Aggregator → Evaluator
- **Domain-specific experts** — Java, Spring, AWS, Microservices, Kafka (each answers only from its own domain)
- **Answer aggregation** — merges expert answers into one coherent interview-quality explanation
- **Evaluation scoring** — technical accuracy + completeness (1-10)
- **Execution tracing** — full trace of every graph invocation with per-node timing
- **Multi-turn interview sessions** with follow-up questions
- **Graph visualization** at `/graph/diagram`

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 4.1.0 |
| Spring AI | 2.0.0-RC2 |
| LangGraph4j | 1.8.17 |
| LLM | nvidia/nemotron-3-super-120b-a12b (via OpenRouter) |

## Prerequisites

- Java 21+
- OpenRouter API key

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/ajaypratap2k12/interview-coach.git
   cd interview-coach
   ```

2. Set environment variable:
   ```bash
   export OPEN_ROUTER_API_KEY=your_api_key_here
   ```

3. Run the application:
   ```bash
   .\mvnw spring-boot:run
   ```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/interview?question=...` | Process single question (returns answer) |
| `GET` | `/interview/trace?question=...` | Process question with full execution trace (JSON) |
| `GET` | `/interview/trace/text?question=...` | Process question with trace (readable text) |
| `GET` | `/graph/diagram` | Mermaid diagram of graph structure |
| `POST` | `/session/start` | Start new interview session |
| `POST` | `/session/answer` | Submit answer, returns feedback + next question |

## Graph Flow

```
START → planner → supervisor → [expert(s)] → aggregator → evaluator → END
```

1. **Planner** — LLM analyzes question, returns JSON array of agent IDs
2. **Supervisor** — logs orchestration state, no LLM call
3. **Experts** — each answers from its own domain only
4. **Aggregator** — merges expert answers into one coherent answer
5. **Evaluator** — scores the aggregated answer

## Project Structure

```
src/main/java/com/interview/ai/coaching/
├── config/
│   ├── AIConfig.java                # ChatClient bean
│   ├── InterviewGraphConfig.java    # Graph topology + routing
│   └── InterviewSessionConfig.java  # Multi-turn session graphs
├── controller/
│   └── InterviewController.java     # REST endpoints
├── graph/
│   ├── InterviewState.java          # State schema (planner-based flow)
│   ├── InterviewSessionState.java   # State for multi-turn session
│   ├── ExecutionTrace.java          # Trace data + pretty print
│   └── NodeTrace.java              # Per-node execution trace
├── nodes/
│   ├── PlannerAgentNode.java        # LLM planner — returns JSON array
│   ├── SupervisorAgentNode.java     # Orchestrator — no LLM, logs state
│   ├── JavaExpertNode.java          # Java domain only
│   ├── SpringExpertNode.java        # Spring domain only
│   ├── AwsExpertNode.java           # AWS domain only
│   ├── MicroserviceExpertNode.java  # Microservices domain only
│   ├── KafkaExpertNode.java         # Kafka domain only
│   ├── AggregatorAgentNode.java     # Merges expert answers
│   ├── EvaluatorAgentNode.java      # Scores finalAnswer
│   ├── GenerateQuestionNode.java    # Session question generation
│   ├── GenerateFollowUpNode.java    # Session follow-up generation
│   └── SessionEvaluateAnswerNode.java # Session answer evaluation
└── service/
    ├── InterviewGraphService.java   # Graph execution + trace capture
    ├── InterviewSessionService.java # Multi-turn session orchestration
    └── TraceCollector.java          # ThreadLocal trace collection
```

## License

MIT

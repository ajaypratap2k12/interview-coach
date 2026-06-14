# Interview Coach

AI-powered interview coaching application built with Spring Boot and LangGraph4j. Practice technical interviews with multi-turn conversations covering Java, Spring, and AWS topics.

## Features

- **Multi-turn interview sessions** with follow-up questions
- **Topic classification** - questions categorized as JAVA, SPRING, AWS, or UNKNOWN
- **Domain-specific agents** - specialized knowledge for each technology area
- **Answer evaluation** with detailed feedback and scoring
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
   ./mvnw spring-boot:run
   ```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/interview/start` | Start a new interview session |
| POST | `/interview/{sessionId}/answer` | Submit an answer |
| GET | `/graph/diagram` | View graph structure |

## Project Structure

```
src/main/java/com/interview/ai/coaching/
├── config/           # Graph and AI configuration
├── controller/       # REST endpoints
├── graph/            # State definitions
├── nodes/            # Graph node implementations
└── service/          # Business logic
```

## License

MIT

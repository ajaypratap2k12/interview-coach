# LangGraph4j 1.8.17 API Reference

## Main Classes

### Core Package (`org.bsc.langgraph4j`)
- **StateGraph** - Main graph builder class for constructing state machines
- **CompiledGraph** - Compiled graph ready for execution
- **GraphDefinition** - Interface defining graph structure
- **CompileConfig** - Configuration for graph compilation
- **RunnableConfig** - Runtime configuration for graph execution
- **NodeOutput** - Output from node execution
- **GraphRepresentation** - Visual representation of graph

### State Package (`org.bsc.langgraph4j.state`)
- **AgentState** - Base class for graph state
- **Channel** - Interface for state property management
- **AppenderChannel** - Channel implementation for list accumulation
- **Channels** - Utility class for creating channels

### Action Package (`org.bsc.langgraph4j.action`)
- **NodeAction** - Synchronous node action interface
- **AsyncNodeAction** - Asynchronous node action interface
- **CommandAction** - Conditional edge action interface
- **NodeActionWithConfig** - Node action with configuration access

## StateGraph API

```java
public class StateGraph<State extends AgentState> implements GraphDefinition<State>
```

### Constructors
- `StateGraph(AgentStateFactory<State> stateFactory)` - Creates graph with state factory
- `StateGraph(StateSerializer<State> stateSerializer)` - Creates graph with serializer
- `StateGraph(Map<String, Channel<?>> channels, AgentStateFactory<State> stateFactory)` - Creates graph with schema

### Key Methods
- `addNode(String id, AsyncNodeAction<State> action)` - Adds executable node
- `addNode(String id, NodeAction<State> action)` - Adds synchronous node
- `addNode(String id, CompiledGraph<State> subGraph)` - Adds subgraph
- `addEdge(String sourceId, String targetId)` - Adds unconditional edge
- `addConditionalEdges(String sourceId, AsyncCommandAction<State, String> action, Map<String, String> mappings)` - Adds conditional edges
- `addConditionalEdges(String sourceId, AsyncCommandAction<State, String> action, String... mappings)` - Adds conditional edges
- `setEntryPoint(String nodeId)` - Sets start node
- `setFinishPoint(String nodeId)` - Sets end node
- `compile()` - Compiles graph to executable form
- `compile(CompileConfig config)` - Compiles with configuration

### Constants
- `START` - Entry point identifier
- `END` - Exit point identifier

## CompiledGraph API

```java
public final class CompiledGraph<State extends AgentState> implements GraphDefinition<State>
```

### Key Methods
- `invoke(Map<String, Object> inputs, RunnableConfig config)` - Executes graph synchronously
- `invoke(RunnableConfig config)` - Executes with empty inputs
- `stream(Map<String, Object> inputs, RunnableConfig config)` - Streams execution results
- `stream(RunnableConfig config)` - Streams with empty inputs
- `updateState(RunnableConfig config, Map<String, Object> values, String asNode)` - Updates graph state
- `getGraph(GraphRepresentation.Type type, String title)` - Generates visual representation
- `getGraph(GraphRepresentation.Type type)` - Generates with default title

### Stream Modes
- `StreamMode.VALUES` - Returns full state after each step
- `StreamMode.UPDATES` - Returns only state changes

## NodeAction API

```java
@FunctionalInterface
public interface NodeAction<State extends AgentState> {
    Map<String, Object> apply(State state) throws Exception;
}
```

### Usage
```java
NodeAction<SimpleState> myNode = state -> {
    String input = state.value("input").orElse("");
    return Map.of("result", "Processed: " + input);
};
```

### Related Interfaces
- **AsyncNodeAction** - Async version returning `CompletableFuture<Map<String, Object>>`
- **NodeActionWithConfig** - Includes `RunnableConfig` parameter
- **CommandAction** - For conditional edges, returns `Command` object

## AgentState API

```java
public class AgentState {
    public AgentState(Map<String, Object> initData)
    public final Map<String, Object> data()
    public final <T> Optional<T> value(String key)
    public static Map<String, Object> updateState(AgentState state, Map<String, Object> partialState, Map<String, Channel<?>> channels)
}
```

### Usage Pattern
```java
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

public class InterviewState extends AgentState {
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
        "question", Channels.lastValue(),
        "answer", Channels.lastValue()
    );

    public InterviewState(Map<String, Object> initData) {
        super(initData);
    }

    public String question() {
        return value("question").orElse("");
    }

    public String answer() {
        return value("answer").orElse("");
    }
}
```

**Note:** The existing project code uses `Channel.overwrite()` and `Channel.Reducer` which appear to be outdated API. The correct API uses `Channels.lastValue()` and `Channel<?>` with the `Reducer` interface from `org.bsc.langgraph4j.state.Channel.Reducer`.

## Channel API

```java
public interface Channel<T> {
    Optional<Channel.Reducer<T>> getReducer();
    Optional<Supplier<T>> getDefault();
    Object update(String key, Object oldValue, Object newValue);
    
    @FunctionalInterface
    interface Reducer<T> {
        T apply(T oldValue, T newValue);
    }
}
```

### Channel Types
- **Channels.lastValue()** - Overwrites with new value (default behavior)
- **Channels.appender(Supplier<List<T>>)** - Appends to list (no duplicates)
- **Channels.appenderWithDuplicate(Supplier<List<T>>)** - Appends to list (allows duplicates)
- **Channels.base(Supplier<T>)** - Default value only
- **Channels.base(Reducer<T>)** - Custom reducer only
- **Channels.base(Reducer<T>, Supplier<T>)** - Custom reducer with default
- **Channel.of(Reducer<T>)** - Creates channel with custom reducer

### Reducer Interface
```java
// Nested in Channel interface
@FunctionalInterface
public interface Channel.Reducer<T> {
    T apply(T oldValue, T newValue);
}
```

### AppenderChannel
```java
public class AppenderChannel<T> implements Channel<List<T>> {
    public static <T> AppenderChannel<T> of(Supplier<List<T>> defaultProvider)
    public static <T> AppenderChannel<T> withDuplicate(Supplier<List<T>> defaultProvider)
}
```

## Minimal Example Graph

```java
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

// 1. Define State
public class SimpleState extends AgentState {
    public static final String MESSAGES_KEY = "messages";
    
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
        MESSAGES_KEY, Channels.appender(ArrayList::new)
    );
    
    public SimpleState(Map<String, Object> initData) {
        super(initData);
    }
    
    public List<String> messages() {
        return this.<List<String>>value(MESSAGES_KEY).orElse(List.of());
    }
}

// 2. Define Nodes
NodeAction<SimpleState> greeter = state -> {
    return Map.of(SimpleState.MESSAGES_KEY, "Hello!");
};

NodeAction<SimpleState> responder = state -> {
    List<String> msgs = state.messages();
    if (msgs.contains("Hello!")) {
        return Map.of(SimpleState.MESSAGES_KEY, "Hi there!");
    }
    return Map.of(SimpleState.MESSAGES_KEY, "No greeting found.");
};

// 3. Build Graph
StateGraph<SimpleState> workflow = new StateGraph<>(SimpleState.SCHEMA, SimpleState::new)
    .addNode("greeter", greeter)
    .addNode("responder", responder)
    .addEdge(StateGraph.START, "greeter")
    .addEdge("greeter", "responder")
    .addEdge("responder", StateGraph.END);

// 4. Compile and Execute
CompiledGraph<SimpleState> graph = workflow.compile();

Map<String, Object> initialState = Map.of(
    SimpleState.MESSAGES_KEY, new ArrayList<>()
);

// Execute synchronously
SimpleState finalState = graph.invoke(initialState, RunnableConfig.builder().build());

// Or stream execution
graph.stream(initialState, RunnableConfig.builder().build())
    .forEach(item -> System.out.println(item));
```

## Key Constants

- `StateGraph.START` = "START"
- `StateGraph.END` = "END"
- `CompiledGraph.StreamMode.VALUES`
- `CompiledGraph.StreamMode.UPDATES`

## RunnableConfig

```java
RunnableConfig config = RunnableConfig.builder()
    .threadId("user-123")
    .streamMode(CompiledGraph.StreamMode.UPDATES)
    .putMetadata("userId", "user-123")
    .putMetadata("model", "gpt-4")
    .build();
```

### Configuration Attributes
- **threadId** - Unique identifier for execution thread/session
- **checkPointId** - Specific checkpoint identifier within a thread
- **nextNode** - Specifies which node should execute next
- **streamMode** - Controls how results are streamed (VALUES or UPDATES)
- **metadata** - Custom key-value pairs available throughout execution

## CompileConfig

```java
CompileConfig config = CompileConfig.builder()
    .build();
```

Used to pass configuration parameters that control runtime behaviors of your graph.

## Dependencies

```xml
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-core</artifactId>
    <version>1.8.17</version>
</dependency>
```

Transitive dependencies:
- `org.bsc.async:async-generator:4.3.1`
- `org.slf4j:slf4j-api:2.0.18`
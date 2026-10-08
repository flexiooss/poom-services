package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpTaskNotFoundException;
import org.codingmatters.poom.mcp.McpTaskState;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.McpToolTasks;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.codingmatters.rest.tests.api.TestSseChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorTasksTest {

    private static final McpTimings FAST = new McpTimings(Duration.ofMillis(50), Duration.ofMillis(30),
            Duration.ofMillis(200), Duration.ofMillis(400), Duration.ofMillis(10), Duration.ofHours(1), Duration.ofSeconds(2));
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant CREATED = Instant.ofEpochMilli(1_760_000_000_123L);

    private final ExecutorService pool = Executors.newCachedThreadPool();

    /** Stockage durable simulé : une tâche finit quand le test la déclare finie. */
    static class FakeTasks implements McpToolTasks {
        final ConcurrentHashMap<String, McpTaskState> states = new ConcurrentHashMap<>();
        final List<String> cancelled = new CopyOnWriteArrayList<>();

        public Start start(CallToolParams params) {
            if (params.arguments() != null && params.arguments().property("refuse") != null) {
                return new Start.Done(CallToolResult.builder().isError(true)
                        .content(ToolContent.builder().type(ToolContent.Type.text).text("refused").build()).build());
            }
            this.states.put("exec-1", McpTaskState.working());
            return new Start.Running("exec-1");
        }

        public McpTaskState get(String id) {
            McpTaskState state = this.states.get(id);
            if (state == null) throw new McpTaskNotFoundException("unknown " + id);
            return state;
        }

        public void cancel(String id) {
            this.cancelled.add(id);
        }
    }

    private final FakeTasks tasks = new FakeTasks();
    private final McpProcessor processor = new McpProcessor(new JsonFactory(),
            McpServerDescriptor.builder().name("t").version("1")
                    .tools(McpToolDescriptor.builder().name("slow").tasks(this.tasks).build(),
                           McpToolDescriptor.builder().name("plain").handler(p -> null).build())
                    .build(), this.pool, FAST);

    @AfterEach
    void tearDown() {
        this.pool.shutdownNow();
    }

    private RecordingResponse callSlow(boolean clientTasks, String args) throws Exception {
        RecordingResponse response = new RecordingResponse();
        this.processor.process(post("tools/call", "slow",
                body("5", "tools/call", "{\"name\":\"slow\",\"arguments\":" + args + "}", clientTasks)), response);
        return response;
    }

    private JsonNode tasksCall(String method, String taskId) throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.processor.process(post(method, taskId, body("6", method, "{\"taskId\":\"" + taskId + "\"}", true)), response);
        assertThat(response.status(), is(200));
        return MAPPER.readTree(response.payload());
    }

    private JsonNode lastMessage(RecordingResponse response) throws Exception {
        List<TestSseChannel.SseEvent> events = response.sseChannel().drainEvents();
        assertThat(events, hasSize(1));
        return MAPPER.readTree(events.get(0).data());
    }

    @Test
    void givenClientWithTasksAndWorkStillRunningAtTaskAfter__whenCall__thenCreateTaskResultEndsTheStream() throws Exception {
        Instant before = Instant.now();
        JsonNode result = this.lastMessage(this.callSlow(true, "{}")).path("result");

        assertThat(result.path("resultType").asText(), is("task"));
        assertThat(result.path("status").asText(), is("working"));
        assertThat(result.path("ttlMs").asLong(), is(3_600_000L));
        assertThat(result.path("pollIntervalMs").asLong(), is(2_000L));
        TaskIds.Ref ref = TaskIds.decode(result.path("taskId").asText()).orElseThrow();
        assertThat(ref.toolTaskId(), is("exec-1"));
        assertThat(Instant.parse(result.path("createdAt").asText()), is(ref.createdAt()));
        assertThat(ref.createdAt().isBefore(before.minusMillis(1)), is(false));
        assertThat(Instant.parse(result.path("lastUpdatedAt").asText()), is(notNullValue()));
        assertThat(this.tasks.cancelled, is(empty())); // la tâche survit à la fin du flux
    }

    @Test
    void givenClientWithoutTasks__whenWorkOutlivesStreamMax__thenNoResultAndNoCancel() throws Exception {
        JsonNode result = this.lastMessage(this.callSlow(false, "{}")).path("result");

        assertThat(result.path("isError").asBoolean(), is(true));
        assertThat(result.toString(), containsString("no_result"));
        assertThat(this.tasks.cancelled, is(empty()));
    }

    @Test
    void givenWorkCompletesBeforeTaskAfter__whenCall__thenPlainCallToolResult() throws Exception {
        this.pool.submit(() -> {
            Thread.sleep(100);
            this.tasks.states.put("exec-1", McpTaskState.completed(CallToolResult.builder().isError(false)
                    .content(ToolContent.builder().type(ToolContent.Type.text).text("{\"out\":1}").build()).build()));
            return null;
        });

        JsonNode result = this.lastMessage(this.callSlow(true, "{}")).path("result");

        assertThat(result.has("resultType"), is(false));
        assertThat(result.path("content").get(0).path("text").asText(), is("{\"out\":1}"));
    }

    @Test
    void givenStartRefuses__whenCall__thenToolErrorAsJson() throws Exception {
        RecordingResponse response = this.callSlow(true, "{\"refuse\":true}");

        assertThat(response.sseChannel(), is(nullValue()));
        assertThat(new String(response.payload()), containsString("refused"));
    }

    @Test
    void givenTask__whenTasksGet__thenWorkingThenCompletedWithResult() throws Exception {
        String taskId = TaskIds.encode("slow", CREATED, "exec-1");
        this.tasks.states.put("exec-1", McpTaskState.working());

        JsonNode working = this.tasksCall("tasks/get", taskId).path("result");
        assertThat(working.path("status").asText(), is("working"));
        assertThat(working.path("resultType").asText(), is("complete"));
        assertThat(working.path("taskId").asText(), is(taskId));
        assertThat(Instant.parse(working.path("createdAt").asText()), is(CREATED));
        assertThat(Instant.parse(working.path("lastUpdatedAt").asText()).isBefore(CREATED), is(false));

        this.tasks.states.put("exec-1", McpTaskState.completed(CallToolResult.builder().isError(true)
                .content(ToolContent.builder().type(ToolContent.Type.text).text("execution_failed").build()).build()));
        JsonNode done = this.tasksCall("tasks/get", taskId).path("result");

        assertThat(done.path("status").asText(), is("completed"));
        assertThat(done.path("resultType").asText(), is("complete"));
        assertThat(done.path("result").path("isError").asBoolean(), is(true));
    }

    @Test
    void givenFailedTask__whenTasksGet__thenFailedWithJsonRpcError() throws Exception {
        this.tasks.states.put("exec-1", McpTaskState.failed(-32603, "lost"));

        JsonNode result = this.tasksCall("tasks/get", TaskIds.encode("slow", CREATED, "exec-1")).path("result");

        assertThat(result.path("status").asText(), is("failed"));
        assertThat(result.path("error").path("code").asInt(), is(-32603));
    }

    @Test
    void givenGarbageTaskId__whenTasksGet__thenUnknownTask() throws Exception {
        assertThat(this.tasksCall("tasks/get", "%%%").path("error").path("code").asInt(), is(-32602));
        assertThat(this.tasksCall("tasks/get", TaskIds.encode("nope", CREATED, "x")).path("error").path("code").asInt(), is(-32602));
        assertThat(this.tasksCall("tasks/get", TaskIds.encode("plain", CREATED, "x")).path("error").path("code").asInt(), is(-32602));
        assertThat(this.tasksCall("tasks/get", TaskIds.encode("slow", CREATED, "unknown")).path("error").path("code").asInt(), is(-32602));
    }

    @Test
    void givenTask__whenTasksCancel__thenCompleteAckAndToolAsked() throws Exception {
        JsonNode response = this.tasksCall("tasks/cancel", TaskIds.encode("slow", CREATED, "exec-1"));

        assertThat(response.path("result").path("resultType").asText(), is("complete"));
        assertThat(this.tasks.cancelled, contains("exec-1"));
    }
}

package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.poom.mcp.types.InputRequired;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.codingmatters.rest.tests.api.TestSseChannel;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.body;
import static org.codingmatters.poom.mcp.processor.McpTestRequests.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorInputRequiredTest {

    private static final String ELICITATION = "{\"elicitation\":{\"form\":{}}}";

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final AtomicReference<CallToolParams> received = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        this.pool.shutdownNow();
    }

    private McpProcessor recording() {
        return new McpProcessor(new JsonFactory(), McpServerDescriptor.builder().name("t").version("1")
                .tools(McpToolDescriptor.builder().name("rec").handler(p -> {
                    this.received.set((CallToolParams) p);
                    return CallToolResult.builder()
                            .content(ToolContent.builder().type(ToolContent.Type.text).text("ok").build())
                            .isError(false).build();
                }).build()).build(), this.pool);
    }

    private void call(McpProcessor processor, String paramsJson, String capabilities) throws Exception {
        processor.process(post("tools/call", "rec", body("1", "tools/call", paramsJson, capabilities)),
                new TestResponseDeleguate());
    }

    @Test
    void givenRetryWithInputResponsesAndState__whenToolsCall__thenHandlerReceivesThem() throws Exception {
        this.call(this.recording(),
                "{\"name\":\"rec\",\"arguments\":{},\"inputResponses\":{\"confirm\":{\"action\":\"accept\"}},\"requestState\":\"st-1\"}",
                ELICITATION);

        CallToolParams params = this.received.get();
        assertThat(params.requestState(), is("st-1"));
        assertThat(params.inputResponses().property("confirm").single().objectValue()
                .property("action").single().stringValue(), is("accept"));
    }

    @Test
    void givenPlainCall__whenToolsCall__thenNoInputResponsesNorState() throws Exception {
        this.call(this.recording(), "{\"name\":\"rec\",\"arguments\":{}}", "{}");

        assertThat(this.received.get().inputResponses(), is(nullValue()));
        assertThat(this.received.get().requestState(), is(nullValue()));
    }

    @Test
    void givenInputResponsesNotAnObject__whenToolsCall__thenIgnored() throws Exception {
        this.call(this.recording(), "{\"name\":\"rec\",\"arguments\":{},\"inputResponses\":\"yes\"}", ELICITATION);

        assertThat(this.received.get().inputResponses(), is(nullValue()));
    }

    @Test
    void givenClientCapabilities__whenToolsCall__thenHandlerSeesThem() throws Exception {
        this.call(this.recording(), "{\"name\":\"rec\",\"arguments\":{}}", ELICITATION);

        assertThat(this.received.get().clientCapabilities().has("elicitation"), is(true));
    }

    @Test
    void givenEmptyElicitationCapability__whenToolsCall__thenDeclared() throws Exception {
        this.call(this.recording(), "{\"name\":\"rec\",\"arguments\":{}}", "{\"elicitation\":{}}");

        assertThat(this.received.get().clientCapabilities().has("elicitation"), is(true));
    }

    private static final McpTimings FAST = new McpTimings(Duration.ofMillis(50), Duration.ofMillis(30),
            Duration.ofMillis(300), Duration.ofMillis(400), Duration.ofMillis(10), Duration.ofHours(1), Duration.ofSeconds(2));

    private static CallToolResult confirmation() {
        return CallToolResult.builder().inputRequired(InputRequired.builder()
                .inputRequests(ObjectValue.builder().property("confirm", c -> c.objectValue(ObjectValue.builder()
                        .property("method", m -> m.stringValue("elicitation/create"))
                        .property("params", p -> p.objectValue(ObjectValue.builder()
                                .property("mode", m -> m.stringValue("form"))
                                .property("message", m -> m.stringValue("Supprimer le champ X ?"))
                                .property("requestedSchema", s -> s.objectValue(ObjectValue.builder()
                                        .property("type", t -> t.stringValue("object"))
                                        .property("properties", t -> t.objectValue(ObjectValue.builder().build()))
                                        .build()))
                                .build()))
                        .build())).build())
                .requestState("st-1")
                .build()).build();
    }

    private McpProcessor answering(Function<CallToolParams, CallToolResult> handler, McpTimings timings) {
        return new McpProcessor(new JsonFactory(), McpServerDescriptor.builder().name("t").version("1")
                .tools(McpToolDescriptor.builder().name("del").handler(handler).build()).build(), this.pool, timings);
    }

    private String jsonCall(McpProcessor processor, String capabilities) throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(post("tools/call", "del", body("7", "tools/call", "{\"name\":\"del\",\"arguments\":{}}", capabilities)), response);
        return new String(response.payload());
    }

    @Test
    void givenInputRequired__whenToolsCall__thenInputRequiredResult() throws Exception {
        String body = this.jsonCall(this.answering(p -> confirmation(), McpTimings.defaults()), ELICITATION);

        assertThat(body, allOf(
                containsString("\"resultType\":\"input_required\""),
                containsString("\"requestState\":\"st-1\""),
                containsString("\"method\":\"elicitation/create\""),
                containsString("Supprimer le champ X ?"),
                not(containsString("\"isError\""))));
    }

    @Test
    void givenInputRequiredWithStateOnly__whenToolsCall__thenNoInputRequests() throws Exception {
        String body = this.jsonCall(this.answering(p -> CallToolResult.builder()
                .inputRequired(InputRequired.builder().requestState("st-2").build()).build(), McpTimings.defaults()), "{}");

        assertThat(body, allOf(containsString("\"resultType\":\"input_required\""),
                containsString("\"requestState\":\"st-2\""), not(containsString("inputRequests"))));
    }

    @Test
    void givenInputRequiredWithNothing__whenToolsCall__thenInternalError() throws Exception {
        String body = this.jsonCall(this.answering(p -> CallToolResult.builder()
                .inputRequired(InputRequired.builder().build()).build(), McpTimings.defaults()), ELICITATION);

        assertThat(body, allOf(containsString("-32603"), not(containsString("input_required"))));
    }

    @Test
    void givenElicitationToClientWithoutCapability__whenToolsCall__thenInternalError() throws Exception {
        String body = this.jsonCall(this.answering(p -> confirmation(), McpTimings.defaults()), "{}");

        assertThat(body, allOf(containsString("-32603"), not(containsString("elicitation/create"))));
    }

    @Test
    void givenSlowInputRequired__whenToolsCall__thenLastSseEventIsInputRequired() throws Exception {
        RecordingResponse response = new RecordingResponse();
        this.answering(p -> {
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            return confirmation();
        }, FAST).process(post("tools/call", "del", body("8", "tools/call", "{\"name\":\"del\",\"arguments\":{}}", ELICITATION)), response);

        List<TestSseChannel.SseEvent> events = response.sseChannel().drainEvents();
        assertThat(events, hasSize(1));
        assertThat(events.get(0).data(), allOf(containsString("\"id\":\"8\""), containsString("\"resultType\":\"input_required\"")));
    }
}

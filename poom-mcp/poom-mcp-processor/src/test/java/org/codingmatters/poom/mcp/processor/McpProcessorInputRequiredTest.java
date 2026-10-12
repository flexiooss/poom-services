package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
}

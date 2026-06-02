package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorToolCallTest {

    private static final String URL = "http://test/mcp";
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private String sessionId;
    private McpProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        McpToolDescriptor echoTool = McpToolDescriptor.builder()
                .name("echo")
                .description("Returns its input")
                .handler(params -> CallToolResult.builder()
                        .content(ToolContent.builder().type("text").text("echo").build())
                        .isError(false)
                        .build())
                .build();
        processor = new McpProcessor("/mcp", jsonFactory,
                McpServerDescriptor.builder().name("test").version("1.0")
                        .tools(echoTool).build(),
                pool, 500);

        TestResponseDeleguate initResp = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{},\"id\":\"1\"}"))
                        .build(),
                initResp
        );
        sessionId = initResp.headers().get("Mcp-Session-Id")[0];
    }

    @Test
    void givenEchoTool__whenToolsCallWithFastHandler__then200WithResult() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                                + "\"params\":{\"name\":\"echo\",\"arguments\":{}},\"id\":\"2\"}"))
                        .build(),
                response
        );
        assertThat(response.status(), is(200));
        assertThat(response.contentType(), containsString("application/json"));
        String body = new String(response.payload());
        assertThat(body, containsString("\"result\""));
        assertThat(body, containsString("echo"));
    }

    @Test
    void givenUnknownTool__whenToolsCall__thenMethodNotFoundError() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                                + "\"params\":{\"name\":\"unknown\",\"arguments\":{}},\"id\":\"3\"}"))
                        .build(),
                response
        );
        assertThat(response.status(), is(200));
        String body = new String(response.payload());
        assertThat(body, containsString("\"error\""));
        assertThat(body, containsString("-32601"));
    }

    private ByteArrayInputStream asStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}

package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.codingmatters.rest.tests.api.TestSseChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorAsyncToolCallTest {

    private static final String URL = "http://test/mcp";
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private String sessionId;
    private McpProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        McpToolDescriptor slowTool = McpToolDescriptor.builder()
                .name("slow")
                .description("A slow tool")
                .handler(params -> {
                    try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return CallToolResult.builder()
                            .content(ToolContent.builder().type(ToolContent.Type.text).text("slow-result").build())
                            .isError(false)
                            .build();
                })
                .build();
        processor = new McpProcessor("/mcp", jsonFactory,
                McpServerDescriptor.builder().name("test").version("1.0")
                        .tools(slowTool).build(),
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
    void givenSlowTool__whenToolsCall__then202AndResultPushedToSse() throws Exception {
        TestResponseDeleguate sseResponse = new TestResponseDeleguate();
        Thread sseThread = new Thread(() -> {
            try {
                processor.process(
                        TestRequestDeleguate.request(RequestDelegate.Method.GET, URL)
                                .addHeader("Accept", "text/event-stream")
                                .addHeader("Mcp-Session-Id", sessionId)
                                .build(),
                        sseResponse
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        sseThread.setDaemon(true);
        sseThread.start();

        Eventually.timeout(2000).assertThat(() -> sseResponse.sseChannel(), notNullValue());
        // Drain initial ping
        sseResponse.sseChannel().poll(2000);

        TestResponseDeleguate toolResponse = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                                + "\"params\":{\"name\":\"slow\",\"arguments\":{}},\"id\":\"42\"}"))
                        .build(),
                toolResponse
        );
        assertThat(toolResponse.status(), is(202));

        TestSseChannel.SseEvent event = sseResponse.sseChannel().poll(3000);
        assertThat(event, notNullValue());
        assertThat(event.event(), is("message"));
        assertThat(event.data(), containsString("slow-result"));
        assertThat(event.data(), containsString("\"id\":\"42\""));

        sseResponse.sseChannel().close();
    }

    private ByteArrayInputStream asStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}

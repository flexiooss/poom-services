package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.codingmatters.rest.tests.api.TestSseChannel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorGetTest {

    private static final String URL = "http://test/mcp";
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    @Test
    void givenSession__whenGetWithSseAccept__thenOpensSSEChannel() throws Exception {
        McpServerDescriptor descriptor = McpServerDescriptor.builder()
                .name("test-server").version("1.0").build();
        McpProcessor processor = new McpProcessor("/mcp", jsonFactory, descriptor, pool);

        TestResponseDeleguate initResponse = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{},\"id\":\"1\"}"))
                        .build(),
                initResponse
        );
        String sessionId = initResponse.headers().get("Mcp-Session-Id")[0];
        assertThat(sessionId, notNullValue());

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
        TestSseChannel channel = sseResponse.sseChannel();
        TestSseChannel.SseEvent ping = channel.poll(2000);
        assertThat(ping, notNullValue());
        assertThat(ping.event(), is("ping"));

        channel.close();
    }

    @Test
    void givenUnknownSession__whenGet__then404() throws Exception {
        McpServerDescriptor descriptor = McpServerDescriptor.builder()
                .name("test-server").version("1.0").build();
        McpProcessor processor = new McpProcessor("/mcp", jsonFactory, descriptor, pool);

        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.GET, URL)
                        .addHeader("Accept", "text/event-stream")
                        .addHeader("Mcp-Session-Id", "unknown-id")
                        .build(),
                response
        );
        assertThat(response.status(), is(404));
    }

    private ByteArrayInputStream asStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}

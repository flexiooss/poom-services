package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
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

class McpProcessorValidationTest {

    private static final String URL = "http://test/mcp";
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private McpProcessor processor;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        processor = new McpProcessor("/mcp", jsonFactory,
                McpServerDescriptor.builder().name("test").version("1.0").build(), pool);

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
    void whenPostWithoutSessionAndNotInitialize__then400() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"2\"}"))
                        .build(),
                response
        );
        assertThat(response.status(), is(400));
    }

    @Test
    void whenPostWithUnknownSession__then400() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", "bad-session-id")
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"2\"}"))
                        .build(),
                response
        );
        assertThat(response.status(), is(400));
    }

    @Test
    void whenPostWithBadJson__thenParseError() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(asStream("not json at all"))
                        .build(),
                response
        );
        String body = new String(response.payload());
        assertThat(body, containsString("-32700"));
    }

    @Test
    void whenPostWithUnknownMethod__thenMethodNotFound() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"totally/unknown\",\"id\":\"3\"}"))
                        .build(),
                response
        );
        String body = new String(response.payload());
        assertThat(body, containsString("-32601"));
    }

    @Test
    void whenDelete__thenSessionIsRemoved() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.DELETE, URL)
                        .addHeader("Mcp-Session-Id", sessionId)
                        .build(),
                response
        );
        assertThat(response.status(), is(200));

        TestResponseDeleguate afterResponse = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"4\"}"))
                        .build(),
                afterResponse
        );
        assertThat(afterResponse.status(), is(400));
    }

    @Test
    void whenToolsList__thenReturnsEmptyList() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"5\"}"))
                        .build(),
                response
        );
        assertThat(response.status(), is(200));
        String body = new String(response.payload());
        assertThat(body, containsString("\"tools\""));
    }

    private ByteArrayInputStream asStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}

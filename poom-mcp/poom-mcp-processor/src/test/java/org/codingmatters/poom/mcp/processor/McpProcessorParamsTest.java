package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codingmatters.poom.mcp.McpResourceDescriptor;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.processor.McpProcessorTasksTest.FakeTasks;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ReadResourceResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** Paramètres du client d'un mauvais type : toujours une réponse JSON-RPC, jamais une exception (donc un 500). */
class McpProcessorParamsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String META = "\"" + McpProtocol.META_PROTOCOL_VERSION + "\":\"" + McpProtocol.VERSION + "\"";

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final McpProcessor processor = new McpProcessor(new JsonFactory(),
            McpServerDescriptor.builder().name("t").version("1")
                    .tools(McpToolDescriptor.builder().name("echo").handler(p -> CallToolResult.builder().isError(false)
                                    .content(ToolContent.builder().type(ToolContent.Type.text).text("ok").build()).build()).build(),
                            McpToolDescriptor.builder().name("slow").tasks(new FakeTasks()).build())
                    .resources(McpResourceDescriptor.builder().uri("note://{id}").name("note")
                            .handler(p -> ReadResourceResult.builder().build()).build())
                    .build(), this.pool);

    record Probe(String method, String name, String body, Integer expectedCode) {}

    private static String raw(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"" + method + "\",\"params\":" + params + "}";
    }

    @AfterEach
    void tearDown() {
        this.pool.shutdownNow();
    }

    @Test
    void givenWrongTypedParams__whenProcess__thenJsonRpcAnswerAndNoException() throws Exception {
        List<Probe> probes = List.of(
                new Probe("tools/list", null, raw("tools/list", "{\"_meta\":\"x\"}"), McpProtocol.HEADER_MISMATCH),
                new Probe("tools/list", null, raw("tools/list", "{\"_meta\":[]}"), McpProtocol.HEADER_MISMATCH),
                new Probe("tools/list", null, raw("tools/list", "{\"_meta\":{\"" + McpProtocol.META_PROTOCOL_VERSION + "\":42}}"), McpProtocol.HEADER_MISMATCH),
                new Probe("tools/list", null, raw("tools/list", "{\"_meta\":{\"" + McpProtocol.META_PROTOCOL_VERSION + "\":[]}}"), McpProtocol.HEADER_MISMATCH),
                new Probe("tools/call", "42", body("1", "tools/call", "{\"name\":42}", false), McpProtocol.INVALID_PARAMS),
                new Probe("tools/call", "x", body("1", "tools/call", "{\"name\":[]}", false), McpProtocol.INVALID_PARAMS),
                new Probe("tools/call", "x", body("1", "tools/call", "{\"name\":{\"a\":1}}", false), McpProtocol.INVALID_PARAMS),
                new Probe("tools/call", "echo", body("1", "tools/call", "{\"name\":\"echo\",\"arguments\":\"x\"}", false), McpProtocol.INVALID_PARAMS),
                new Probe("tools/call", "echo", body("1", "tools/call", "{\"name\":\"echo\",\"arguments\":[1]}", false), McpProtocol.INVALID_PARAMS),
                new Probe("tools/call", "echo", raw("tools/call", "{\"name\":\"echo\",\"_meta\":{" + META + ",\""
                        + McpProtocol.META_CLIENT_CAPABILITIES + "\":{\"extensions\":\"x\"}}}"), null),
                new Probe("tools/call", "echo", raw("tools/call", "{\"name\":\"echo\",\"_meta\":{" + META + ",\""
                        + McpProtocol.META_CLIENT_CAPABILITIES + "\":\"x\"}}"), null),
                new Probe("tasks/get", "1", body("1", "tasks/get", "{\"taskId\":1}", true), McpProtocol.INVALID_PARAMS),
                new Probe("tasks/get", "x", body("1", "tasks/get", "{\"taskId\":[]}", true), McpProtocol.INVALID_PARAMS),
                new Probe("tasks/cancel", "x", body("1", "tasks/cancel", "{\"taskId\":{}}", true), McpProtocol.INVALID_PARAMS),
                new Probe("resources/read", "1", body("1", "resources/read", "{\"uri\":1}", false), McpProtocol.INVALID_PARAMS),
                new Probe("resources/read", "x", body("1", "resources/read", "{\"uri\":[\"note://1\"]}", false), McpProtocol.INVALID_PARAMS),
                new Probe("prompts/get", "true", body("1", "prompts/get", "{\"name\":true}", false), McpProtocol.METHOD_NOT_FOUND)
        );

        for (Probe probe : probes) {
            TestResponseDeleguate response = new TestResponseDeleguate();
            try {
                this.processor.process(post(probe.method(), probe.name(), probe.body()), response);
            } catch (Throwable t) {
                throw new AssertionError("exception escaped process for " + probe.body(), t);
            }
            assertThat(probe.body(), response.status(), anyOf(is(200), is(400)));
            JsonNode answer = MAPPER.readTree(response.payload());
            assertThat(probe.body(), answer.path("jsonrpc").asText(), is("2.0"));
            if (probe.expectedCode() == null) {
                assertThat(probe.body(), answer.has("result"), is(true));
            } else {
                assertThat(probe.body(), answer.path("error").path("code").asInt(), is(probe.expectedCode()));
            }
        }
    }
}

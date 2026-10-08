package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpTaskState;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.McpToolTasks;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorDiscoverTest {

    private JsonNode discover(McpServerDescriptor descriptor) throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        new McpProcessor(new JsonFactory(), descriptor, Executors.newSingleThreadExecutor())
                .process(post("server/discover", null, body("1", "server/discover", "{}", false)), response);
        assertThat(response.status(), is(200));
        return new ObjectMapper().readTree(response.payload()).path("result");
    }

    @Test
    void givenToolsOnly__whenDiscover__thenVersionsToolsAndServerInfo() throws Exception {
        JsonNode result = this.discover(McpServerDescriptor.builder().name("srv").version("2.0")
                .tools(McpToolDescriptor.builder().name("echo").build()).build());

        assertThat(result.path("resultType").asText(), is("complete"));
        assertThat(result.path("supportedVersions").get(0).asText(), is(McpProtocol.VERSION));
        assertThat(result.path("capabilities").has("tools"), is(true));
        assertThat(result.path("capabilities").has("resources"), is(false));
        assertThat(result.path("capabilities").has("extensions"), is(false));
        assertThat(result.has("serverInfo"), is(false));
        JsonNode serverInfo = result.path("_meta").path(McpProtocol.META_SERVER_INFO);
        assertThat(serverInfo.path("name").asText(), is("srv"));
        assertThat(serverInfo.path("version").asText(), is("2.0"));
    }

    @Test
    void givenATaskTool__whenDiscover__thenTasksExtensionDeclared() throws Exception {
        McpToolTasks tasks = new McpToolTasks() {
            public Start start(CallToolParams params) { return new Start.Running("t"); }
            public McpTaskState get(String id) { return McpTaskState.working(); }
            public void cancel(String id) {}
        };
        JsonNode result = this.discover(McpServerDescriptor.builder().name("srv").version("2.0")
                .tools(McpToolDescriptor.builder().name("slow").tasks(tasks).build()).build());

        assertThat(result.path("capabilities").path("extensions").has(McpProtocol.TASKS_EXTENSION), is(true));
    }
}

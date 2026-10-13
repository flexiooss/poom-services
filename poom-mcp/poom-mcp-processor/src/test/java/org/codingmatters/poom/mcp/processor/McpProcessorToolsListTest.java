package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolAnnotations;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorToolsListTest {

    private JsonNode tool(McpToolDescriptor tool) throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        new McpProcessor(new JsonFactory(), McpServerDescriptor.builder().name("srv").version("1").tools(tool).build(),
                Executors.newSingleThreadExecutor())
                .process(post("tools/list", null, body("1", "tools/list", "{}", false)), response);
        assertThat(response.status(), is(200));
        return new ObjectMapper().readTree(response.payload()).path("result").path("tools").get(0);
    }

    @Test
    void givenNoAnnotations__whenToolsList__thenNoAnnotationsProperty() throws Exception {
        JsonNode tool = this.tool(McpToolDescriptor.builder().name("echo").build());

        assertThat(tool.has("annotations"), is(false));
    }

    @Test
    void givenAllAnnotations__whenToolsList__thenTheyAreListed() throws Exception {
        JsonNode tool = this.tool(McpToolDescriptor.builder().name("delete_row")
                .annotations(McpToolAnnotations.builder()
                        .title("Delete a row").readOnlyHint(false).destructiveHint(true)
                        .idempotentHint(true).openWorldHint(false).build())
                .build());

        JsonNode annotations = tool.path("annotations");
        assertThat(annotations.path("title").asText(), is("Delete a row"));
        assertThat(annotations.path("readOnlyHint").isBoolean(), is(true));
        assertThat(annotations.path("readOnlyHint").asBoolean(), is(false));
        assertThat(annotations.path("destructiveHint").asBoolean(), is(true));
        assertThat(annotations.path("idempotentHint").asBoolean(), is(true));
        assertThat(annotations.path("openWorldHint").isBoolean(), is(true));
        assertThat(annotations.path("openWorldHint").asBoolean(), is(false));
    }

    @Test
    void givenSomeAnnotations__whenToolsList__thenOnlyThoseAreListed() throws Exception {
        JsonNode tool = this.tool(McpToolDescriptor.builder().name("list_rows")
                .annotations(McpToolAnnotations.builder().readOnlyHint(true).build())
                .build());

        JsonNode annotations = tool.path("annotations");
        assertThat(annotations.path("readOnlyHint").asBoolean(), is(true));
        assertThat(annotations.size(), is(1));
    }

    @Test
    void givenEmptyAnnotations__whenToolsList__thenNoAnnotationsProperty() throws Exception {
        JsonNode tool = this.tool(McpToolDescriptor.builder().name("echo")
                .annotations(McpToolAnnotations.builder().build()).build());

        assertThat(tool.has("annotations"), is(false));
    }
}

package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorToolCallTest {

    private final JsonFactory jsonFactory = new JsonFactory();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final McpProcessor processor = new McpProcessor(this.jsonFactory, descriptor(), this.pool);

    private static McpServerDescriptor descriptor() {
        McpToolDescriptor echoTool = McpToolDescriptor.builder()
                .name("echo")
                .description("Returns its input")
                .handler(params -> CallToolResult.builder()
                        .content(ToolContent.builder().type(ToolContent.Type.text).text("echo").build())
                        .isError(false)
                        .build())
                .build();
        McpToolDescriptor imageTool = McpToolDescriptor.builder()
                .name("image")
                .description("Returns an image")
                .handler(params -> CallToolResult.builder()
                        .content(ToolContent.builder().type(ToolContent.Type.image)
                                .data("iVBORw0K").mimeType("image/png").build())
                        .isError(false)
                        .build())
                .build();
        return McpServerDescriptor.builder().name("test").version("1.0").tools(echoTool, imageTool).build();
    }

    @Test
    void givenEchoTool__whenToolsCallWithFastHandler__then200WithResult() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.processor.process(post("tools/call", "echo",
                body("2", "tools/call", "{\"name\":\"echo\",\"arguments\":{}}", false)), response);

        assertThat(response.status(), is(200));
        assertThat(response.contentType(), containsString("application/json"));
        String body = new String(response.payload());
        assertThat(body, containsString("\"result\""));
        assertThat(body, containsString("echo"));
    }

    @Test
    void givenUnknownTool__whenToolsCall__thenInvalidParamsError() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.processor.process(post("tools/call", "unknown",
                body("3", "tools/call", "{\"name\":\"unknown\",\"arguments\":{}}", false)), response);

        assertThat(response.status(), is(200));
        String body = new String(response.payload());
        assertThat(body, containsString("\"error\""));
        assertThat(body, containsString("-32602"));
    }

    @Test
    void givenImageTool__whenToolsCall__thenContentCarriesDataAndMimeTypeWithoutText() throws Exception {
        String body = this.callTool("image", "4");
        assertThat(body, containsString("\"type\":\"image\""));
        assertThat(body, containsString("\"data\":\"iVBORw0K\""));
        assertThat(body, containsString("\"mimeType\":\"image/png\""));
        assertThat(body, not(containsString("\"text\"")));
    }

    @Test
    void givenEchoTool__whenToolsCall__thenTextContentCarriesNoDataNorMimeType() throws Exception {
        String body = this.callTool("echo", "5");
        assertThat(body, containsString("\"text\":\"echo\""));
        assertThat(body, not(containsString("\"data\"")));
        assertThat(body, not(containsString("\"mimeType\"")));
    }

    private String callTool(String name, String id) throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.processor.process(post("tools/call", name,
                body(id, "tools/call", "{\"name\":\"" + name + "\",\"arguments\":{}}", false)), response);
        assertThat(response.status(), is(200));
        return new String(response.payload());
    }
}

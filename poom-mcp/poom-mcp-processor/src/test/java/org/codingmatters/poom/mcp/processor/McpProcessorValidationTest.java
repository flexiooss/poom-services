package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorValidationTest {

    private final McpProcessor processor = new McpProcessor(new JsonFactory(),
            McpServerDescriptor.builder().name("test").version("1.0").build(), Executors.newSingleThreadExecutor());

    private String send(RequestDelegate request, TestResponseDeleguate response) throws Exception {
        this.processor.process(request, response);
        return response.payload() == null ? "" : new String(response.payload(), StandardCharsets.UTF_8);
    }

    @Test
    void givenValidRequest__whenToolsList__then200Json() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("tools/list", null, body("1", "tools/list", "{}", false)), response);

        assertThat(response.status(), is(200));
        assertThat(response.contentType(), containsString("application/json"));
        assertThat(body, containsString("\"tools\""));
        assertThat(response.headers().get("Mcp-Session-Id"), is(nullValue()));
    }

    @Test
    void givenJsonContentTypeWithCharset__whenPost__thenAccepted() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.send(TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                .contentType("application/json; charset=utf-8")
                .addHeader(McpProtocol.HEADER_PROTOCOL_VERSION, McpProtocol.VERSION)
                .addHeader(McpProtocol.HEADER_METHOD, "tools/list")
                .payload(stream(body("1", "tools/list", "{}", false))).build(), response);

        assertThat(response.status(), is(200));
    }

    @Test
    void givenGet__whenProcessed__then405() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.send(TestRequestDeleguate.request(RequestDelegate.Method.GET, URL).build(), response);
        assertThat(response.status(), is(405));
    }

    @Test
    void givenDelete__whenProcessed__then405() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.send(TestRequestDeleguate.request(RequestDelegate.Method.DELETE, URL).build(), response);
        assertThat(response.status(), is(405));
    }

    @Test
    void givenTextPlain__whenPost__then415() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        this.send(TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                .contentType("text/plain").payload(stream("x")).build(), response);
        assertThat(response.status(), is(415));
    }

    @Test
    void givenInvalidJson__whenPost__then400ParseError() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("tools/list", null, "{not json"), response);

        assertThat(response.status(), is(400));
        assertThat(body, containsString("-32700"));
    }

    @Test
    void givenNotification__whenPost__then202WithoutBody() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("notifications/cancelled", null,
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{}}"), response);

        assertThat(response.status(), is(202));
        assertThat(body, is(""));
    }

    @Test
    void givenUnknownProtocolVersionHeader__whenPost__then400UnsupportedWithSupportedList() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                .contentType("application/json")
                .addHeader(McpProtocol.HEADER_PROTOCOL_VERSION, "1900-01-01")
                .addHeader(McpProtocol.HEADER_METHOD, "tools/list")
                .payload(stream(body("1", "tools/list", "{}", false))).build(), response);

        assertThat(response.status(), is(400));
        assertThat(body, containsString("-32022"));
        assertThat(body, containsString("\"supported\""));
        assertThat(body, containsString(McpProtocol.VERSION));
    }

    @Test
    void givenLegacyInitialize__whenPost__then400Unsupported() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                .contentType("application/json")
                .payload(stream("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\"}}"))
                .build(), response);

        assertThat(response.status(), is(400));
        assertThat(body, containsString("-32022"));
    }

    @Test
    void givenMethodHeaderNotMatchingBody__whenPost__then400HeaderMismatch() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("tools/call", "x", body("1", "tools/list", "{}", false)), response);

        assertThat(response.status(), is(400));
        assertThat(body, containsString("-32020"));
    }

    @Test
    void givenNameHeaderNotMatchingTool__whenToolsCall__then400HeaderMismatch() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("tools/call", "other",
                body("1", "tools/call", "{\"name\":\"echo\",\"arguments\":{}}", false)), response);

        assertThat(response.status(), is(400));
        assertThat(body, containsString("-32020"));
    }

    @Test
    void givenBase64EncodedNameHeader__whenToolsCall__thenDecodedBeforeComparison() throws Exception {
        String encoded = "=?base64?" + java.util.Base64.getEncoder().encodeToString("échos".getBytes(StandardCharsets.UTF_8)) + "?=";
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("tools/call", encoded,
                body("1", "tools/call", "{\"name\":\"échos\",\"arguments\":{}}", false)), response);

        assertThat(body, not(containsString("-32020")));
        assertThat(body, containsString("-32602")); // outil inconnu : l'en-tête, lui, a été accepté
    }

    @Test
    void givenUnknownVersion__whenPost__thenRequestedVersionEchoed() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                .contentType("application/json")
                .addHeader(McpProtocol.HEADER_PROTOCOL_VERSION, "1900-01-01")
                .addHeader(McpProtocol.HEADER_METHOD, "tools/list")
                .payload(stream(body("1", "tools/list", "{}", false))).build(), response);

        assertThat(body, containsString("\"requested\":\"1900-01-01\""));
    }

    @Test
    void givenMissingMetaVersion__whenPost__then400HeaderMismatch() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("tools/list", null,
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\",\"params\":{}}"), response);

        assertThat(response.status(), is(400));
        assertThat(body, containsString("-32020"));
    }

    @Test
    void givenUnknownMethod__whenPost__thenMethodNotFound() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("nope/nope", null, body("1", "nope/nope", "{}", false)), response);

        assertThat(response.status(), is(200));
        assertThat(body, containsString("-32601"));
    }

    @Test
    void givenNumericId__whenPost__thenIdEchoedAsString() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();
        String body = this.send(post("tools/list", null,
                body("1", "tools/list", "{}", false).replace("\"id\":\"1\"", "\"id\":7")), response);

        assertThat(body, containsString("\"id\":\"7\""));
    }
}

package org.codingmatters.poom.mcp.processor;

import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Requêtes MCP 2026-07-28 conformes, à dégrader au besoin dans un test. */
final class McpTestRequests {

    static final String URL = "http://test/mcp";

    private McpTestRequests() {}

    static RequestDelegate post(String method, String name, String body) {
        TestRequestDeleguate.Builder builder = TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                .contentType("application/json")
                .addHeader(McpProtocol.HEADER_PROTOCOL_VERSION, McpProtocol.VERSION)
                .addHeader(McpProtocol.HEADER_METHOD, method)
                .payload(stream(body));
        if (name != null) builder.addHeader(McpProtocol.HEADER_NAME, name);
        return builder.build();
    }

    /** {@code paramsJson} : un objet JSON sans {@code _meta}, ou {@code "{}"}. */
    static String body(String id, String method, String paramsJson, boolean tasks) {
        String capabilities = tasks ? "{\"extensions\":{\"" + McpProtocol.TASKS_EXTENSION + "\":{}}}" : "{}";
        String meta = "\"_meta\":{"
                + "\"" + McpProtocol.META_PROTOCOL_VERSION + "\":\"" + McpProtocol.VERSION + "\","
                + "\"" + McpProtocol.META_CLIENT_INFO + "\":{\"name\":\"test\",\"version\":\"1\"},"
                + "\"" + McpProtocol.META_CLIENT_CAPABILITIES + "\":" + capabilities + "}";
        String rest = paramsJson.trim().substring(1).trim();
        String params = rest.equals("}") ? "{" + meta + "}" : "{" + meta + "," + rest;
        return "{\"jsonrpc\":\"2.0\",\"id\":\"" + id + "\",\"method\":\"" + method + "\",\"params\":" + params + "}";
    }

    static ByteArrayInputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}

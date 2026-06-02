package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.types.McpError;
import org.codingmatters.poom.mcp.types.McpRequest;
import org.codingmatters.poom.mcp.types.McpResponse;
import org.codingmatters.poom.mcp.types.json.McpRequestReader;
import org.codingmatters.poom.mcp.types.json.McpResponseWriter;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.api.SseChannel;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class McpProcessor implements Processor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(McpProcessor.class);

    private static final String MCP_SESSION_HEADER = "Mcp-Session-Id";
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final String apiPath;
    private final JsonFactory jsonFactory;
    private final McpServerDescriptor descriptor;
    private final ExecutorService toolExecutor;
    private final long syncTimeoutMillis;
    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();

    public McpProcessor(String apiPath, JsonFactory jsonFactory, McpServerDescriptor descriptor, ExecutorService toolExecutor) {
        this(apiPath, jsonFactory, descriptor, toolExecutor, 500);
    }

    public McpProcessor(String apiPath, JsonFactory jsonFactory, McpServerDescriptor descriptor, ExecutorService toolExecutor, long syncTimeoutMillis) {
        this.apiPath = apiPath;
        this.jsonFactory = jsonFactory;
        this.descriptor = descriptor;
        this.toolExecutor = toolExecutor;
        this.syncTimeoutMillis = syncTimeoutMillis;
    }

    @Override
    public void process(RequestDelegate request, ResponseDelegate response) throws IOException {
        String sessionId = headerValue(request, MCP_SESSION_HEADER);
        switch (request.method()) {
            case GET -> handleGet(request, response, sessionId);
            case POST -> handlePost(request, response, sessionId);
            case DELETE -> handleDelete(request, response, sessionId);
            default -> {
                response.status(405);
                response.contenType("text/plain");
                response.payload("Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleGet(RequestDelegate request, ResponseDelegate response, String sessionId) throws IOException {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            response.status(404);
            response.contenType("text/plain");
            response.payload("Session not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        McpSession session = sessions.get(sessionId);
        SseChannel channel = response.openSse();
        session.setSseChannel(channel);

        while (channel.isOpen()) {
            try {
                channel.send("ping", "{}");
                Thread.sleep(30_000);
            } catch (IOException e) {
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handlePost(RequestDelegate request, ResponseDelegate response, String sessionId) throws IOException {
        if (!"application/json".equals(request.contentType())) {
            response.status(415);
            response.contenType("text/plain");
            response.payload("Unsupported Media Type".getBytes(StandardCharsets.UTF_8));
            return;
        }

        McpRequest mcpRequest;
        try (JsonParser parser = jsonFactory.createParser(request.payload())) {
            mcpRequest = new McpRequestReader().read(parser);
        } catch (IOException e) {
            writeJsonRpcError(response, null, -32700, "Parse error");
            return;
        }

        String method = mcpRequest.method();

        if ("initialize".equals(method)) {
            String newSessionId = UUID.randomUUID().toString();
            sessions.put(newSessionId, new McpSession(newSessionId));
            McpResponse resp = McpResponse.builder()
                    .jsonrpc("2.0")
                    .id(mcpRequest.id())
                    .result(ObjectValue.builder()
                            .property("protocolVersion", v -> v.stringValue(PROTOCOL_VERSION))
                            .property("capabilities", v -> v.objectValue(ObjectValue.builder().build()))
                            .property("serverInfo", v -> v.objectValue(ObjectValue.builder()
                                    .property("name", n -> n.stringValue(descriptor.name()))
                                    .property("version", ver -> ver.stringValue(descriptor.version()))
                                    .build()))
                            .build())
                    .build();
            response.addHeader(MCP_SESSION_HEADER, newSessionId);
            writeJsonRpcResponse(response, resp);
            return;
        }

        if (sessionId == null || !sessions.containsKey(sessionId)) {
            response.status(400);
            response.contenType("text/plain");
            response.payload("Missing or unknown Mcp-Session-Id".getBytes(StandardCharsets.UTF_8));
            return;
        }

        McpSession session = sessions.get(sessionId);
        dispatchMethod(response, session, mcpRequest);
    }

    private void dispatchMethod(ResponseDelegate response, McpSession session, McpRequest mcpRequest) throws IOException {
        switch (mcpRequest.method()) {
            case "tools/list" -> writeJsonRpcResponse(response, buildListToolsResult(mcpRequest));
            case "tools/call" -> handleToolCall(response, session, mcpRequest);
            case "resources/list" -> writeJsonRpcResponse(response, buildListResourcesResult(mcpRequest));
            case "resources/read" -> handleResourceRead(response, mcpRequest);
            case "prompts/list" -> writeJsonRpcResponse(response, buildListPromptsResult(mcpRequest));
            default -> writeJsonRpcError(response, mcpRequest.id(), -32601, "Method not found");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleToolCall(ResponseDelegate response, McpSession session, McpRequest mcpRequest) throws IOException {
        String toolName = mcpRequest.params() != null && mcpRequest.params().property("name") != null
                ? mcpRequest.params().property("name").single().stringValue()
                : null;
        Optional<org.codingmatters.poom.mcp.McpToolDescriptor> tool = descriptor.opt().tools().safe().stream()
                .filter(t -> toolName != null && toolName.equals(t.name()))
                .findFirst();

        if (tool.isEmpty()) {
            writeJsonRpcError(response, mcpRequest.id(), -32601, "Tool not found: " + toolName);
            return;
        }

        ObjectValue arguments = mcpRequest.params() != null && mcpRequest.params().property("arguments") != null
                ? mcpRequest.params().property("arguments").single().objectValue()
                : ObjectValue.builder().build();
        if (arguments == null) {
            arguments = ObjectValue.builder().build();
        }

        org.codingmatters.poom.mcp.types.CallToolParams params =
                org.codingmatters.poom.mcp.types.CallToolParams.builder()
                        .name(toolName)
                        .arguments(arguments)
                        .build();

        CompletableFuture<org.codingmatters.poom.mcp.types.CallToolResult> future =
                CompletableFuture.supplyAsync(
                        () -> (org.codingmatters.poom.mcp.types.CallToolResult) tool.get().handler().apply(params),
                        toolExecutor
                );

        try {
            org.codingmatters.poom.mcp.types.CallToolResult result =
                    future.get(syncTimeoutMillis, TimeUnit.MILLISECONDS);
            writeJsonRpcResponse(response, McpResponse.builder()
                    .jsonrpc("2.0").id(mcpRequest.id())
                    .result(buildCallToolResultObject(result))
                    .build());
        } catch (TimeoutException e) {
            handleAsyncToolCall(response, session, mcpRequest, future);
        } catch (Exception e) {
            writeJsonRpcError(response, mcpRequest.id(), -32603, "Internal error: " + e.getMessage());
        }
    }

    private void handleAsyncToolCall(ResponseDelegate response, McpSession session, McpRequest mcpRequest,
            CompletableFuture<org.codingmatters.poom.mcp.types.CallToolResult> future) throws IOException {
        response.status(202);
        response.payload(new byte[0]);
        future.thenAccept(result -> {
            if (!session.hasSseChannel()) {
                log.error("async tool result ready but no SSE channel for session {}", session.id());
                return;
            }
            try {
                McpResponse mcpResponse = McpResponse.builder()
                        .jsonrpc("2.0").id(mcpRequest.id())
                        .result(buildCallToolResultObject(result))
                        .build();
                session.sseChannel().send("message", new String(serializeResponse(mcpResponse), StandardCharsets.UTF_8));
            } catch (IOException ex) {
                log.error("failed to push async result to SSE channel for session {}", session.id(), ex);
            }
        });
    }

    private ObjectValue buildCallToolResultObject(org.codingmatters.poom.mcp.types.CallToolResult result) {
        List<ObjectValue> contentList = result.opt().content().safe().stream()
                .map(c -> ObjectValue.builder()
                        .property("type", v -> v.stringValue(c.type()))
                        .property("text", v -> v.stringValue(c.text()))
                        .build())
                .toList();
        return ObjectValue.builder()
                .property("content", PropertyValue.multipleObject(contentList.toArray(new ObjectValue[0])))
                .property("isError", v -> v.booleanValue(result.isError() != null && result.isError()))
                .build();
    }

    private void handleDelete(RequestDelegate request, ResponseDelegate response, String sessionId) throws IOException {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            response.status(404);
            response.contenType("text/plain");
            response.payload("Session not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        McpSession session = sessions.remove(sessionId);
        if (session.hasSseChannel()) {
            session.sseChannel().close();
        }
        response.status(200);
        response.payload(new byte[0]);
    }

    private McpResponse buildListToolsResult(McpRequest request) {
        List<ObjectValue> tools = descriptor.opt().tools().safe().stream()
                .map(t -> ObjectValue.builder()
                        .property("name", v -> v.stringValue(t.name()))
                        .property("description", v -> v.stringValue(t.description() != null ? t.description() : ""))
                        .property("inputSchema", v -> v.objectValue(
                                t.inputSchema() != null ? t.inputSchema() : ObjectValue.builder().build()))
                        .build())
                .toList();
        return McpResponse.builder()
                .jsonrpc("2.0").id(request.id())
                .result(ObjectValue.builder()
                        .property("tools", PropertyValue.multipleObject(tools.toArray(new ObjectValue[0])))
                        .build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private void handleResourceRead(ResponseDelegate response, McpRequest mcpRequest) throws IOException {
        String uri = mcpRequest.params() != null && mcpRequest.params().property("uri") != null
                ? mcpRequest.params().property("uri").single().stringValue()
                : null;
        if (uri == null) {
            writeJsonRpcError(response, mcpRequest.id(), -32602, "Invalid params: uri required");
            return;
        }
        // Prefix routing: match by the literal portion before the first template variable.
        // The handler receives the full URI and is responsible for precise extraction.
        Optional<org.codingmatters.poom.mcp.McpResourceDescriptor> resource =
                descriptor.opt().resources().safe().stream()
                        .filter(r -> r.uri() != null && uri.startsWith(uriPrefix(r.uri())))
                        .findFirst();
        if (resource.isEmpty()) {
            writeJsonRpcError(response, mcpRequest.id(), -32601, "Resource not found: " + uri);
            return;
        }
        org.codingmatters.poom.mcp.types.ReadResourceParams params =
                org.codingmatters.poom.mcp.types.ReadResourceParams.builder().uri(uri).build();
        try {
            org.codingmatters.poom.mcp.types.ReadResourceResult result =
                    (org.codingmatters.poom.mcp.types.ReadResourceResult) resource.get().handler().apply(params);
            writeJsonRpcResponse(response, McpResponse.builder()
                    .jsonrpc("2.0").id(mcpRequest.id())
                    .result(buildReadResourceResultObject(result))
                    .build());
        } catch (RuntimeException e) {
            log.error("resource handler threw for uri {}", uri, e);
            writeJsonRpcError(response, mcpRequest.id(), -32603, "Internal error");
        }
    }

    private String uriPrefix(String uriTemplate) {
        int idx = uriTemplate.indexOf('{');
        return idx >= 0 ? uriTemplate.substring(0, idx) : uriTemplate;
    }

    private ObjectValue buildReadResourceResultObject(org.codingmatters.poom.mcp.types.ReadResourceResult result) {
        List<ObjectValue> contents = result.opt().contents().safe().stream()
                .map(c -> {
                    ObjectValue.Builder b = ObjectValue.builder()
                            .property("uri", v -> v.stringValue(c.uri()))
                            .property("mimeType", v -> v.stringValue(c.mimeType() != null ? c.mimeType() : ""));
                    if (c.text() != null) {
                        b.property("text", v -> v.stringValue(c.text()));
                    }
                    return b.build();
                })
                .toList();
        return ObjectValue.builder()
                .property("contents", PropertyValue.multipleObject(contents.toArray(new ObjectValue[0])))
                .build();
    }

    private McpResponse buildListResourcesResult(McpRequest request) {
        List<ObjectValue> resources = descriptor.opt().resources().safe().stream()
                .map(r -> ObjectValue.builder()
                        .property("uri", v -> v.stringValue(r.uri()))
                        .property("name", v -> v.stringValue(r.name() != null ? r.name() : ""))
                        .property("mimeType", v -> v.stringValue(r.mimeType() != null ? r.mimeType() : ""))
                        .build())
                .toList();
        return McpResponse.builder()
                .jsonrpc("2.0").id(request.id())
                .result(ObjectValue.builder()
                        .property("resources", PropertyValue.multipleObject(resources.toArray(new ObjectValue[0])))
                        .build())
                .build();
    }

    private McpResponse buildListPromptsResult(McpRequest request) {
        List<ObjectValue> prompts = descriptor.opt().prompts().safe().stream()
                .map(p -> ObjectValue.builder()
                        .property("name", v -> v.stringValue(p.name()))
                        .property("description", v -> v.stringValue(p.description() != null ? p.description() : ""))
                        .build())
                .toList();
        return McpResponse.builder()
                .jsonrpc("2.0").id(request.id())
                .result(ObjectValue.builder()
                        .property("prompts", PropertyValue.multipleObject(prompts.toArray(new ObjectValue[0])))
                        .build())
                .build();
    }

    private void writeJsonRpcResponse(ResponseDelegate response, McpResponse mcpResponse) throws IOException {
        response.status(200);
        response.contenType("application/json");
        response.payload(serializeResponse(mcpResponse));
    }

    private void writeJsonRpcError(ResponseDelegate response, String id, int code, String message) throws IOException {
        McpResponse errorResponse = McpResponse.builder()
                .jsonrpc("2.0")
                .id(id)
                .error(McpError.builder().code(code).message(message).build())
                .build();
        response.status(200);
        response.contenType("application/json");
        response.payload(serializeResponse(errorResponse));
    }

    private byte[] serializeResponse(McpResponse mcpResponse) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             JsonGenerator gen = jsonFactory.createGenerator(out)) {
            new McpResponseWriter().write(gen, mcpResponse);
            gen.flush();
            return out.toByteArray();
        }
    }

    private String headerValue(RequestDelegate request, String name) {
        List<String> values = request.headers().get(name);
        if (values == null || values.isEmpty()) return null;
        return values.get(0);
    }
}

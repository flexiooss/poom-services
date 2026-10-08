package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.types.McpRequest;
import org.codingmatters.poom.mcp.types.McpResponse;
import org.codingmatters.poom.mcp.types.json.McpIdNormalizingParser;
import org.codingmatters.poom.mcp.types.json.McpRequestReader;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class McpProcessor implements Processor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(McpProcessor.class);

    private final JsonFactory jsonFactory;
    private final McpServerDescriptor descriptor;
    private final ExecutorService toolExecutor;
    private final McpTimings timings;
    private final JsonRpcWriter writer;
    private final ToolCallResponder responder;

    public McpProcessor(JsonFactory jsonFactory, McpServerDescriptor descriptor, ExecutorService toolExecutor) {
        this(jsonFactory, descriptor, toolExecutor, McpTimings.defaults());
    }

    public McpProcessor(JsonFactory jsonFactory, McpServerDescriptor descriptor, ExecutorService toolExecutor, McpTimings timings) {
        this.jsonFactory = jsonFactory;
        this.descriptor = descriptor;
        this.toolExecutor = toolExecutor;
        this.timings = timings;
        this.writer = new JsonRpcWriter(jsonFactory);
        ScheduledExecutorService keepalives = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mcp-sse-keepalive");
            t.setDaemon(true);
            return t;
        });
        this.responder = new ToolCallResponder(this.writer, timings, keepalives);
    }

    @Override
    public void process(RequestDelegate request, ResponseDelegate response) throws IOException {
        if (request.method() != RequestDelegate.Method.POST) {
            response.status(405);
            response.addHeader("Allow", "POST");
            response.contenType("text/plain");
            response.payload("Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String contentType = request.contentType();
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            response.status(415);
            response.contenType("text/plain");
            response.payload("Unsupported Media Type".getBytes(StandardCharsets.UTF_8));
            return;
        }

        McpRequest mcpRequest;
        try (JsonParser parser = new McpIdNormalizingParser(this.jsonFactory.createParser(request.payload()))) {
            mcpRequest = new McpRequestReader().read(parser);
        } catch (IOException e) {
            this.writer.error(response, 400, null, McpProtocol.PARSE_ERROR, "Parse error", null);
            return;
        }
        if (mcpRequest == null || mcpRequest.method() == null) {
            this.writer.error(response, 400, null, McpProtocol.INVALID_REQUEST, "Invalid Request", null);
            return;
        }
        if (mcpRequest.id() == null) {
            // Notification : rien à répondre (notifications/cancelled, notifications/initialized…).
            response.status(202);
            response.payload(new byte[0]);
            return;
        }

        Optional<RequestCheck.Rejection> rejection = RequestCheck.check(request, mcpRequest);
        if (rejection.isPresent()) {
            RequestCheck.Rejection r = rejection.get();
            this.writer.error(response, r.httpStatus(), mcpRequest.id(), r.code(), r.message(), r.data());
            return;
        }
        this.dispatch(response, mcpRequest);
    }

    private void dispatch(ResponseDelegate response, McpRequest mcpRequest) throws IOException {
        switch (mcpRequest.method()) {
            case "server/discover" -> this.writer.json(response, this.writer.result(mcpRequest.id(), this.discoverResult()));
            case "tools/list" -> this.writer.json(response, this.buildListToolsResult(mcpRequest));
            case "tools/call" -> this.handleToolCall(response, mcpRequest);
            case "resources/list" -> this.writer.json(response, this.buildListResourcesResult(mcpRequest));
            case "resources/read" -> this.handleResourceRead(response, mcpRequest);
            case "prompts/list" -> this.writer.json(response, this.buildListPromptsResult(mcpRequest));
            case "prompts/get" -> this.handlePromptGet(response, mcpRequest);
            default -> this.writer.json(response,
                    this.writer.errorResponse(mcpRequest.id(), McpProtocol.METHOD_NOT_FOUND, "Method not found"));
        }
    }

    private ObjectValue discoverResult() {
        ObjectValue empty = ObjectValue.builder().build();
        ObjectValue.Builder capabilities = ObjectValue.builder();
        if (!this.descriptor.opt().tools().safe().isEmpty()) capabilities.property("tools", v -> v.objectValue(empty));
        if (!this.descriptor.opt().resources().safe().isEmpty()) capabilities.property("resources", v -> v.objectValue(empty));
        if (!this.descriptor.opt().prompts().safe().isEmpty()) capabilities.property("prompts", v -> v.objectValue(empty));
        if (this.descriptor.opt().tools().safe().stream().anyMatch(t -> t.tasks() != null)) {
            capabilities.property("extensions", v -> v.objectValue(ObjectValue.builder()
                    .property(McpProtocol.TASKS_EXTENSION, e -> e.objectValue(empty)).build()));
        }
        ObjectValue serverInfo = ObjectValue.builder()
                .property("name", n -> n.stringValue(this.descriptor.name()))
                .property("version", n -> n.stringValue(this.descriptor.version()))
                .build();
        return ObjectValue.builder()
                .property("resultType", v -> v.stringValue("complete"))
                .property("supportedVersions", PropertyValue.multipleString(McpProtocol.VERSION))
                .property("capabilities", v -> v.objectValue(capabilities.build()))
                .property(McpProtocol.META, v -> v.objectValue(ObjectValue.builder()
                        .property(McpProtocol.META_SERVER_INFO, s -> s.objectValue(serverInfo))
                        .build()))
                .build();
    }

    @SuppressWarnings("unchecked")
    private void handleToolCall(ResponseDelegate response, McpRequest mcpRequest) throws IOException {
        String toolName = mcpRequest.params() != null && mcpRequest.params().property("name") != null
                ? mcpRequest.params().property("name").single().stringValue()
                : null;
        Optional<org.codingmatters.poom.mcp.McpToolDescriptor> tool = descriptor.opt().tools().safe().stream()
                .filter(t -> toolName != null && toolName.equals(t.name()))
                .findFirst();

        if (tool.isEmpty()) {
            this.writer.json(response, this.writer.errorResponse(mcpRequest.id(), McpProtocol.INVALID_PARAMS, "Tool not found: " + toolName));
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

        ToolRun run = ToolRun.start(tool.get(), params, this.toolExecutor, this.timings.toolPollInterval());
        this.responder.respond(response, mcpRequest, toolName, run, RequestCheck.clientDeclaresTasks(mcpRequest));
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
            this.writer.json(response, this.writer.errorResponse(mcpRequest.id(), McpProtocol.INVALID_PARAMS, "Invalid params: uri required"));
            return;
        }
        // Prefix routing: match by the literal portion before the first template variable.
        // The handler receives the full URI and is responsible for precise extraction.
        Optional<org.codingmatters.poom.mcp.McpResourceDescriptor> resource =
                descriptor.opt().resources().safe().stream()
                        .filter(r -> r.uri() != null && uri.startsWith(uriPrefix(r.uri())))
                        .findFirst();
        if (resource.isEmpty()) {
            this.writer.json(response, this.writer.errorResponse(mcpRequest.id(), McpProtocol.METHOD_NOT_FOUND, "Resource not found: " + uri));
            return;
        }
        org.codingmatters.poom.mcp.types.ReadResourceParams params =
                org.codingmatters.poom.mcp.types.ReadResourceParams.builder().uri(uri).build();
        try {
            org.codingmatters.poom.mcp.types.ReadResourceResult result =
                    (org.codingmatters.poom.mcp.types.ReadResourceResult) resource.get().handler().apply(params);
            this.writer.json(response, McpResponse.builder()
                    .jsonrpc("2.0").id(mcpRequest.id())
                    .result(buildReadResourceResultObject(result))
                    .build());
        } catch (RuntimeException e) {
            log.error("resource handler threw for uri {}", uri, e);
            this.writer.json(response, this.writer.errorResponse(mcpRequest.id(), McpProtocol.INTERNAL_ERROR, "Internal error"));
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

    @SuppressWarnings("unchecked")
    private void handlePromptGet(ResponseDelegate response, McpRequest mcpRequest) throws IOException {
        String name = mcpRequest.params() != null && mcpRequest.params().property("name") != null
                ? mcpRequest.params().property("name").single().stringValue()
                : null;
        Optional<org.codingmatters.poom.mcp.McpPromptDescriptor> prompt =
                descriptor.opt().prompts().safe().stream()
                        .filter(p -> name != null && name.equals(p.name()))
                        .findFirst();
        if (prompt.isEmpty()) {
            this.writer.json(response, this.writer.errorResponse(mcpRequest.id(), McpProtocol.METHOD_NOT_FOUND, "Prompt not found: " + name));
            return;
        }
        ObjectValue arguments = mcpRequest.params() != null && mcpRequest.params().property("arguments") != null
                ? mcpRequest.params().property("arguments").single().objectValue()
                : ObjectValue.builder().build();
        if (arguments == null) arguments = ObjectValue.builder().build();

        org.codingmatters.poom.mcp.types.GetPromptParams params =
                org.codingmatters.poom.mcp.types.GetPromptParams.builder()
                        .name(name).arguments(arguments).build();
        try {
            org.codingmatters.poom.mcp.types.GetPromptResult result =
                    (org.codingmatters.poom.mcp.types.GetPromptResult) prompt.get().handler().apply(params);
            this.writer.json(response, McpResponse.builder()
                    .jsonrpc("2.0").id(mcpRequest.id())
                    .result(buildGetPromptResultObject(result))
                    .build());
        } catch (RuntimeException e) {
            log.error("prompt handler threw for name {}", name, e);
            this.writer.json(response, this.writer.errorResponse(mcpRequest.id(), McpProtocol.INTERNAL_ERROR, "Internal error"));
        }
    }

    private ObjectValue buildGetPromptResultObject(org.codingmatters.poom.mcp.types.GetPromptResult result) {
        List<ObjectValue> messages = result.opt().messages().safe().stream()
                .map(m -> ObjectValue.builder()
                        .property("role", v -> v.stringValue(m.role() != null ? m.role().name() : null))
                        .property("content", v -> v.objectValue(m.content() != null ? m.content() : ObjectValue.builder().build()))
                        .build())
                .toList();
        return ObjectValue.builder()
                .property("description", v -> v.stringValue(result.description() != null ? result.description() : ""))
                .property("messages", PropertyValue.multipleObject(messages.toArray(new ObjectValue[0])))
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
}

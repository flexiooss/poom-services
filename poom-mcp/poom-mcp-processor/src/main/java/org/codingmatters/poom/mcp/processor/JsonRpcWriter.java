package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.mcp.McpTaskState;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.McpError;
import org.codingmatters.poom.mcp.types.McpResponse;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.poom.mcp.types.json.McpResponseWriter;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

final class JsonRpcWriter {

    private final JsonFactory jsonFactory;

    JsonRpcWriter(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    McpResponse result(String id, ObjectValue result) {
        return McpResponse.builder().jsonrpc("2.0").id(id).result(result).build();
    }

    McpResponse errorResponse(String id, int code, String message) {
        return this.errorResponse(id, code, message, null);
    }

    McpResponse errorResponse(String id, int code, String message, ObjectValue data) {
        McpError.Builder error = McpError.builder().code(code).message(message);
        if (data != null) error.data(data);
        return McpResponse.builder().jsonrpc("2.0").id(id).error(error.build()).build();
    }

    void json(ResponseDelegate response, McpResponse mcpResponse) throws IOException {
        response.status(200);
        response.contenType("application/json");
        response.payload(this.serialize(mcpResponse));
    }

    void error(ResponseDelegate response, int httpStatus, String id, int code, String message, ObjectValue data) throws IOException {
        response.status(httpStatus);
        response.contenType("application/json");
        response.payload(this.serialize(this.errorResponse(id, code, message, data)));
    }

    byte[] serialize(McpResponse mcpResponse) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             JsonGenerator gen = this.jsonFactory.createGenerator(out)) {
            new McpResponseWriter().write(gen, mcpResponse);
            gen.flush();
            return out.toByteArray();
        }
    }

    ObjectValue callToolResult(CallToolResult result) {
        List<ObjectValue> contentList = result.opt().content().safe().stream()
                .map(this::toolContent)
                .toList();
        return ObjectValue.builder()
                .property("content", PropertyValue.multipleObject(contentList.toArray(new ObjectValue[0])))
                .property("isError", v -> v.booleanValue(result.isError() != null && result.isError()))
                .build();
    }

    ObjectValue createTaskResult(String taskId, Instant createdAt, McpTimings timings) {
        return ObjectValue.builder()
                .property("resultType", v -> v.stringValue("task"))
                .property("taskId", v -> v.stringValue(taskId))
                .property("status", v -> v.stringValue("working"))
                .property("createdAt", v -> v.stringValue(createdAt.toString()))
                .property("lastUpdatedAt", v -> v.stringValue(Instant.now().toString()))
                .property("ttlMs", v -> v.longValue(timings.taskTtl().toMillis()))
                .property("pollIntervalMs", v -> v.longValue(timings.clientPollInterval().toMillis()))
                .build();
    }

    /** {@code lastUpdatedAt} vaut l'instant de la lecture : l'outil ne fournit pas de date de mise à jour. */
    ObjectValue taskState(String taskId, Instant createdAt, McpTaskState state, McpTimings timings) {
        ObjectValue.Builder b = ObjectValue.builder()
                .property("resultType", v -> v.stringValue("complete"))
                .property("taskId", v -> v.stringValue(taskId))
                .property("status", v -> v.stringValue(state.status().name().toLowerCase(java.util.Locale.ROOT)))
                .property("createdAt", v -> v.stringValue(createdAt.toString()))
                .property("lastUpdatedAt", v -> v.stringValue(Instant.now().toString()))
                .property("ttlMs", v -> v.longValue(timings.taskTtl().toMillis()))
                .property("pollIntervalMs", v -> v.longValue(timings.clientPollInterval().toMillis()));
        if (state.result() != null) b.property("result", v -> v.objectValue(this.callToolResult(state.result())));
        if (state.error() != null) b.property("error", v -> v.objectValue(ObjectValue.builder()
                .property("code", c -> c.longValue((long) state.error().code()))
                .property("message", m -> m.stringValue(state.error().message()))
                .build()));
        return b.build();
    }

    /** Résultat rendu quand un flux atteint {@code streamMax} : l'outil tourne peut-être encore. */
    CallToolResult noResult() {
        return CallToolResult.builder()
                .content(ToolContent.builder().type(ToolContent.Type.text)
                        .text("{\"error\":\"no_result\",\"message\":\"no result within the stream time limit; "
                                + "the call may still be running, do not call it again\"}")
                        .build())
                .isError(true)
                .build();
    }

    // Seuls les champs renseignés sont émis : un contenu image n'a pas de text, un contenu texte
    // n'a ni data ni mimeType, et un client MCP strict refuse un champ à null.
    private ObjectValue toolContent(ToolContent c) {
        ObjectValue.Builder content = ObjectValue.builder()
                .property("type", v -> v.stringValue(c.type() != null ? c.type().name() : null));
        if (c.text() != null) content.property("text", v -> v.stringValue(c.text()));
        if (c.data() != null) content.property("data", v -> v.stringValue(c.data()));
        if (c.mimeType() != null) content.property("mimeType", v -> v.stringValue(c.mimeType()));
        return content.build();
    }
}

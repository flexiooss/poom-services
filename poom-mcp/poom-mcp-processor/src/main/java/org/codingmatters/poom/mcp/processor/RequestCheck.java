package org.codingmatters.poom.mcp.processor;

import org.codingmatters.poom.mcp.types.McpRequest;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contrôle d'une requête 2026-07-28 : version annoncée, en-têtes cohérents avec le corps. */
final class RequestCheck {

    record Rejection(int httpStatus, int code, String message, ObjectValue data) {}

    private RequestCheck() {}

    static Optional<Rejection> check(RequestDelegate request, McpRequest mcpRequest) {
        String version = header(request, McpProtocol.HEADER_PROTOCOL_VERSION);
        if (!McpProtocol.VERSION.equals(version)) {
            ObjectValue.Builder data = ObjectValue.builder()
                    .property("supported", PropertyValue.multipleString(McpProtocol.VERSION));
            if (version != null) data.property("requested", v -> v.stringValue(version));
            return Optional.of(new Rejection(400, McpProtocol.UNSUPPORTED_PROTOCOL_VERSION,
                    "Unsupported protocol version: " + version, data.build()));
        }
        String metaVersion = metaString(mcpRequest, McpProtocol.META_PROTOCOL_VERSION);
        if (!version.equals(metaVersion)) {
            return mismatch(McpProtocol.HEADER_PROTOCOL_VERSION, version, metaVersion);
        }
        String method = header(request, McpProtocol.HEADER_METHOD);
        if (!mcpRequest.method().equals(method)) {
            return mismatch(McpProtocol.HEADER_METHOD, method, mcpRequest.method());
        }
        String expectedName = expectedName(mcpRequest);
        String name = decodeName(header(request, McpProtocol.HEADER_NAME));
        if (expectedName != null && !expectedName.equals(name)) {
            return mismatch(McpProtocol.HEADER_NAME, name, expectedName);
        }
        return Optional.empty();
    }

    /** Le client a-t-il déclaré l'extension des tâches dans ses capacités ? */
    static boolean clientDeclaresTasks(McpRequest mcpRequest) {
        ObjectValue capabilities = metaObject(mcpRequest, McpProtocol.META_CLIENT_CAPABILITIES);
        if (capabilities == null || capabilities.property("extensions") == null) return false;
        ObjectValue extensions = capabilities.property("extensions").single().objectValue();
        return extensions != null && extensions.property(McpProtocol.TASKS_EXTENSION) != null;
    }

    /** Lecture d'en-tête insensible à la casse : le délégué de test ne normalise pas les noms. */
    static String header(RequestDelegate request, String name) {
        for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    /** Ce que doit porter {@code Mcp-Name} : l'outil, l'URI, le prompt ou la tâche visés, rien pour les autres méthodes. */
    private static String expectedName(McpRequest mcpRequest) {
        return switch (mcpRequest.method()) {
            case "tools/call", "prompts/get" -> paramString(mcpRequest, "name");
            case "resources/read" -> paramString(mcpRequest, "uri");
            case "tasks/get", "tasks/update", "tasks/cancel" -> paramString(mcpRequest, "taskId");
            default -> null;
        };
    }

    /** {@code Mcp-Name} peut être encodé {@code =?base64?<base64 utf-8>?=} : on le décode avant de comparer au corps. */
    static String decodeName(String raw) {
        if (raw == null || !raw.startsWith("=?base64?") || !raw.endsWith("?=") || raw.length() < 11) return raw;
        try {
            return new String(java.util.Base64.getDecoder().decode(raw.substring(9, raw.length() - 2)),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return raw; // valeur illisible : la comparaison échouera, d'où un -32020
        }
    }

    private static Optional<Rejection> mismatch(String header, String got, String expected) {
        return Optional.of(new Rejection(400, McpProtocol.HEADER_MISMATCH,
                "Header " + header + " (" + got + ") does not match the request body (" + expected + ")", null));
    }

    private static String paramString(McpRequest mcpRequest, String name) {
        ObjectValue params = mcpRequest.params();
        if (params == null || params.property(name) == null) return null;
        return params.property(name).single().stringValue();
    }

    private static ObjectValue meta(McpRequest mcpRequest) {
        ObjectValue params = mcpRequest.params();
        if (params == null || params.property(McpProtocol.META) == null) return null;
        return params.property(McpProtocol.META).single().objectValue();
    }

    private static String metaString(McpRequest mcpRequest, String key) {
        ObjectValue meta = meta(mcpRequest);
        if (meta == null || meta.property(key) == null) return null;
        return meta.property(key).single().stringValue();
    }

    private static ObjectValue metaObject(McpRequest mcpRequest, String key) {
        ObjectValue meta = meta(mcpRequest);
        if (meta == null || meta.property(key) == null) return null;
        return meta.property(key).single().objectValue();
    }
}

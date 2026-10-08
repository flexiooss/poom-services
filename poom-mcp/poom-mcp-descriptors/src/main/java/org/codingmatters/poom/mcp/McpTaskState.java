package org.codingmatters.poom.mcp;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.McpError;

/**
 * État d'une tâche lu par {@link McpToolTasks#get}. Un outil dont le travail a abouti en erreur
 * métier est {@code COMPLETED} avec un résultat {@code isError: true} ; {@code FAILED} est
 * réservé à l'échec de la tâche elle-même, porté par une erreur JSON-RPC.
 */
public record McpTaskState(Status status, CallToolResult result, McpError error) {

    public enum Status { WORKING, COMPLETED, FAILED }

    public static McpTaskState working() {
        return new McpTaskState(Status.WORKING, null, null);
    }

    public static McpTaskState completed(CallToolResult result) {
        return new McpTaskState(Status.COMPLETED, result, null);
    }

    public static McpTaskState failed(int code, String message) {
        return new McpTaskState(Status.FAILED, null, McpError.builder().code(code).message(message).build());
    }
}

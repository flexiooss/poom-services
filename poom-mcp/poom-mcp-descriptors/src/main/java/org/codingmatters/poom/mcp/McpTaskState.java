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

    /** Un état incohérent (FAILED sans erreur codée, COMPLETED sans résultat, WORKING avec l'un ou l'autre) est refusé. */
    public McpTaskState {
        if (status == null) throw new IllegalArgumentException("status is required");
        switch (status) {
            case WORKING -> {
                if (result != null || error != null) throw new IllegalArgumentException("a WORKING task has neither result nor error");
            }
            case COMPLETED -> {
                if (result == null) throw new IllegalArgumentException("a COMPLETED task requires a result");
                if (error != null) throw new IllegalArgumentException("a COMPLETED task has no error");
            }
            case FAILED -> {
                if (error == null || error.code() == null) throw new IllegalArgumentException("a FAILED task requires an error with a code");
                if (result != null) throw new IllegalArgumentException("a FAILED task has no result");
            }
        }
    }

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

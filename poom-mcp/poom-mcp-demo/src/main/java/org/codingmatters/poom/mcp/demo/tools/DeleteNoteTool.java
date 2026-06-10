package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;

import java.util.function.Function;

public class DeleteNoteTool implements Function<CallToolParams, CallToolResult> {

    private final NoteService noteService;

    public DeleteNoteTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String id = ToolHelper.arg(params.arguments(), "id");

        if (id == null || id.isBlank()) {
            return ToolHelper.error("Missing required argument: id");
        }

        try {
            boolean deleted = noteService.delete(id);
            if (deleted) {
                return ToolHelper.success("Note " + id + " deleted.");
            } else {
                return ToolHelper.error("Note not found: " + id);
            }
        } catch (RepositoryException e) {
            return ToolHelper.error("Failed to delete note: " + e.getMessage());
        }
    }
}

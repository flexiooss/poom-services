package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class UpdateNoteTool implements Function<CallToolParams, CallToolResult> {

    private final NoteService noteService;

    public UpdateNoteTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String id = ToolHelper.arg(params.arguments(), "id");

        if (id == null || id.isBlank()) {
            return ToolHelper.error("Missing required argument: id");
        }

        String title = ToolHelper.arg(params.arguments(), Note.names_().title());
        String content = ToolHelper.arg(params.arguments(), Note.names_().content());
        List<String> tags = ToolHelper.argList(params.arguments(), Note.names_().tags());

        try {
            Optional<Entity<Note>> updated = noteService.update(id, title, content, tags);
            if (updated.isEmpty()) {
                return ToolHelper.error("Note not found: " + id);
            }
            return ToolHelper.success("Note " + id + " updated.");
        } catch (RepositoryException e) {
            return ToolHelper.error("Failed to update note: " + e.getMessage());
        }
    }
}

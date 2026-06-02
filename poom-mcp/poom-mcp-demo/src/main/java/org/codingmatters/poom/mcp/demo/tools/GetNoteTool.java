package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;

import java.util.Optional;
import java.util.function.Function;

public class GetNoteTool implements Function<CallToolParams, CallToolResult> {

    private final NoteService noteService;

    public GetNoteTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String id = ToolHelper.arg(params.arguments(), "id");

        if (id == null || id.isBlank()) {
            return ToolHelper.error("Missing required argument: id");
        }

        try {
            Optional<Entity<Note>> found = noteService.get(id);
            if (found.isEmpty()) {
                return ToolHelper.error("Note not found: " + id);
            }
            Note note = found.get().value();
            String tags = note.opt().tags().isPresent() ? String.join(", ", note.tags()) : "";
            String text = "# " + note.title() + "\n\n"
                    + "**id:** " + found.get().id() + "\n"
                    + "**tags:** " + tags + "\n"
                    + "**created:** " + note.createdAt() + "\n"
                    + "**updated:** " + note.updatedAt() + "\n\n"
                    + note.content();
            return ToolHelper.success(text);
        } catch (RepositoryException e) {
            return ToolHelper.error("Failed to get note: " + e.getMessage());
        }
    }
}

package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;

import java.util.List;
import java.util.function.Function;

public class ListNotesTool implements Function<CallToolParams, CallToolResult> {

    private final NoteService noteService;

    public ListNotesTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String tag = ToolHelper.arg(params.arguments(), "tag");

        try {
            List<Entity<Note>> notes = noteService.list(tag);
            if (notes.isEmpty()) {
                return ToolHelper.success("No notes found.");
            }
            StringBuilder sb = new StringBuilder();
            for (Entity<Note> entity : notes) {
                Note note = entity.value();
                String tags = note.opt().tags().isPresent() ? String.join(", ", note.tags()) : "";
                sb.append(entity.id())
                        .append(" — ")
                        .append(note.title())
                        .append(" [")
                        .append(tags)
                        .append("]\n");
            }
            return ToolHelper.success(sb.toString().stripTrailing());
        } catch (RepositoryException e) {
            return ToolHelper.error("Failed to list notes: " + e.getMessage());
        }
    }
}

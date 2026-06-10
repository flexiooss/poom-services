package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;

import java.util.List;
import java.util.function.Function;

public class SearchNotesTool implements Function<CallToolParams, CallToolResult> {

    private final NoteService noteService;

    public SearchNotesTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String query = ToolHelper.arg(params.arguments(), "query");

        if (query == null || query.isBlank()) {
            return ToolHelper.error("Missing required argument: query");
        }

        try {
            List<Entity<Note>> results = noteService.search(query);
            if (results.isEmpty()) {
                return ToolHelper.success("No notes match \"" + query + "\".");
            }
            StringBuilder sb = new StringBuilder();
            for (Entity<Note> entity : results) {
                Note note = entity.value();
                String content = note.content() != null ? note.content() : "";
                String excerpt = content.length() > 80 ? content.substring(0, 80) : content;
                sb.append(entity.id())
                        .append(": ")
                        .append(note.title())
                        .append(" — ")
                        .append(excerpt)
                        .append("\n");
            }
            return ToolHelper.success(sb.toString().stripTrailing());
        } catch (RepositoryException e) {
            return ToolHelper.error("Failed to search notes: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolHelper.error("Search interrupted");
        }
    }
}

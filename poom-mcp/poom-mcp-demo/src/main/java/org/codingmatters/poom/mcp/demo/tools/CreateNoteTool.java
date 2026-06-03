package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;

import java.util.List;
import java.util.function.Function;

public class CreateNoteTool implements Function<CallToolParams, CallToolResult> {

    private final NoteService noteService;

    public CreateNoteTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String title = ToolHelper.arg(params.arguments(), Note.names_().title());
        String content = ToolHelper.arg(params.arguments(), Note.names_().content());
        List<String> tags = ToolHelper.argList(params.arguments(), Note.names_().tags());

        if (title == null || title.isBlank()) {
            return ToolHelper.error("Missing required argument: title");
        }
        if (content == null) {
            return ToolHelper.error("Missing required argument: content");
        }

        try {
            Entity<Note> entity = noteService.create(title, content, tags);
            return ToolHelper.success("Note created with id: " + entity.id());
        } catch (RepositoryException e) {
            return ToolHelper.error("Failed to create note: " + e.getMessage());
        }
    }
}

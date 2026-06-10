package org.codingmatters.poom.mcp.demo.prompts;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.GetPromptParams;
import org.codingmatters.poom.mcp.types.GetPromptResult;

import java.util.function.Function;

public class SummarizeNotePromptHandler implements Function<GetPromptParams, GetPromptResult> {

    private final NoteService noteService;

    public SummarizeNotePromptHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public GetPromptResult apply(GetPromptParams params) {
        String noteId = PromptHelper.stringArg(params, "note_id");
        if (noteId == null) {
            return PromptHelper.error("note_id argument is required");
        }
        try {
            return noteService.get(noteId)
                    .map(e -> {
                        var n = e.value();
                        var promptText = "Please summarize the following note in 2-3 sentences:\n\n"
                                + "Title: " + n.title() + "\n\n"
                                + n.content();
                        return GetPromptResult.builder()
                                .description("Summarize note '" + n.title() + "'")
                                .messages(PromptHelper.userMessage(promptText))
                                .build();
                    })
                    .orElseGet(() -> PromptHelper.error("Note not found: " + noteId));
        } catch (Exception e) {
            return PromptHelper.error("Error: " + e.getMessage());
        }
    }
}

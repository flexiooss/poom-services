package org.codingmatters.poom.mcp.demo.prompts;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.GetPromptParams;
import org.codingmatters.poom.mcp.types.GetPromptResult;

import java.util.function.Function;

public class CompareNotesPromptHandler implements Function<GetPromptParams, GetPromptResult> {

    private final NoteService noteService;

    public CompareNotesPromptHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public GetPromptResult apply(GetPromptParams params) {
        String id1 = PromptHelper.stringArg(params, "note_id_1");
        String id2 = PromptHelper.stringArg(params, "note_id_2");
        if (id1 == null) return PromptHelper.error("note_id_1 argument is required");
        if (id2 == null) return PromptHelper.error("note_id_2 argument is required");
        try {
            var note1 = noteService.get(id1).orElse(null);
            var note2 = noteService.get(id2).orElse(null);
            if (note1 == null) return PromptHelper.error("Note not found: " + id1);
            if (note2 == null) return PromptHelper.error("Note not found: " + id2);

            var promptText = "Compare and contrast the following two notes:\n\n"
                    + "## Note 1: " + note1.value().title() + "\n\n"
                    + note1.value().content() + "\n\n"
                    + "## Note 2: " + note2.value().title() + "\n\n"
                    + note2.value().content();
            return GetPromptResult.builder()
                    .description("Compare '" + note1.value().title() + "' with '" + note2.value().title() + "'")
                    .messages(PromptHelper.userMessage(promptText))
                    .build();
        } catch (Exception e) {
            return PromptHelper.error("Error: " + e.getMessage());
        }
    }
}

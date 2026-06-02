package org.codingmatters.poom.mcp.demo.prompts;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.GetPromptParams;
import org.codingmatters.poom.mcp.types.GetPromptResult;
import org.codingmatters.poom.mcp.types.PromptMessage;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

public class SummarizeNotePromptHandler implements Function<GetPromptParams, GetPromptResult> {

    private final NoteService noteService;

    public SummarizeNotePromptHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public GetPromptResult apply(GetPromptParams params) {
        String noteId = stringArg(params, "note_id");
        if (noteId == null) {
            return error("note_id argument is required");
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
                                .messages(userMessage(promptText))
                                .build();
                    })
                    .orElseGet(() -> error("Note not found: " + noteId));
        } catch (Exception e) {
            return error("Error: " + e.getMessage());
        }
    }

    static PromptMessage userMessage(String text) {
        return PromptMessage.builder()
                .role("user")
                .content(ObjectValue.builder()
                        .property("type", v -> v.stringValue("text"))
                        .property("text", v -> v.stringValue(text))
                        .build())
                .build();
    }

    private static GetPromptResult error(String msg) {
        return GetPromptResult.builder()
                .description("Error")
                .messages(userMessage(msg))
                .build();
    }

    private static String stringArg(GetPromptParams params, String name) {
        if (params.arguments() == null || params.arguments().property(name) == null) return null;
        var pv = params.arguments().property(name);
        return pv.isSingle() ? pv.single().stringValue() : null;
    }
}

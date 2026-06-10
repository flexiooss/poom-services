package org.codingmatters.poom.mcp.demo.prompts;

import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.GetPromptParams;
import org.codingmatters.poom.mcp.types.GetPromptResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class SummarizeNotePromptHandlerTest {

    private NoteService service;
    private SummarizeNotePromptHandler handler;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        handler = new SummarizeNotePromptHandler(service);
    }

    @Test
    void givenExistingNote__thenPromptContainsTitleAndContent() throws Exception {
        Entity<Note> created = service.create("Annual report", "Revenue increased 20%", null);

        GetPromptResult result = handler.apply(GetPromptParams.builder()
                .name("summarize_note")
                .arguments(ObjectValue.builder()
                        .property("note_id", v -> v.stringValue(created.id()))
                        .build())
                .build());

        assertThat(result.description(), not(equalTo("Error")));
        String text = result.messages().get(0).content().property(ToolContent.names_().text()).single().stringValue();
        assertThat(text, containsString("Annual report"));
        assertThat(text, containsString("Revenue increased"));
    }

    @Test
    void givenMissingNoteId__thenErrorDescription() {
        GetPromptResult result = handler.apply(GetPromptParams.builder()
                .name("summarize_note")
                .build());

        assertThat(result.description(), equalTo("Error"));
    }

    @Test
    void givenUnknownNoteId__thenErrorDescription() {
        GetPromptResult result = handler.apply(GetPromptParams.builder()
                .name("summarize_note")
                .arguments(ObjectValue.builder()
                        .property("note_id", v -> v.stringValue("no-such-id"))
                        .build())
                .build());

        assertThat(result.description(), equalTo("Error"));
    }
}

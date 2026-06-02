package org.codingmatters.poom.mcp.demo.prompts;

import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.GetPromptParams;
import org.codingmatters.poom.mcp.types.GetPromptResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CompareNotesPromptHandlerTest {

    private NoteService service;
    private CompareNotesPromptHandler handler;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        handler = new CompareNotesPromptHandler(service);
    }

    @Test
    void givenTwoNotes__thenPromptContainsBoth() throws Exception {
        Entity<Note> note1 = service.create("Note Alpha", "Content of alpha note", null);
        Entity<Note> note2 = service.create("Note Beta", "Content of beta note", null);

        GetPromptResult result = handler.apply(GetPromptParams.builder()
                .name("compare_notes")
                .arguments(ObjectValue.builder()
                        .property("note_id_1", v -> v.stringValue(note1.id()))
                        .property("note_id_2", v -> v.stringValue(note2.id()))
                        .build())
                .build());

        assertThat(result.description(), not(equalTo("Error")));
        String text = result.messages().get(0).content().property("text").single().stringValue();
        assertThat(text, containsString("Content of alpha note"));
        assertThat(text, containsString("Content of beta note"));
    }

    @Test
    void givenMissingFirstId__thenErrorDescription() throws Exception {
        Entity<Note> note2 = service.create("Note Beta", "Content of beta note", null);

        GetPromptResult result = handler.apply(GetPromptParams.builder()
                .name("compare_notes")
                .arguments(ObjectValue.builder()
                        .property("note_id_2", v -> v.stringValue(note2.id()))
                        .build())
                .build());

        assertThat(result.description(), equalTo("Error"));
    }

    @Test
    void givenOneNoteNotFound__thenErrorDescription() throws Exception {
        Entity<Note> note1 = service.create("Note Alpha", "Content of alpha note", null);

        GetPromptResult result = handler.apply(GetPromptParams.builder()
                .name("compare_notes")
                .arguments(ObjectValue.builder()
                        .property("note_id_1", v -> v.stringValue(note1.id()))
                        .property("note_id_2", v -> v.stringValue("no-such-id"))
                        .build())
                .build());

        assertThat(result.description(), equalTo("Error"));
    }
}

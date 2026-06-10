package org.codingmatters.poom.mcp.demo.resources;

import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.ReadResourceParams;
import org.codingmatters.poom.mcp.types.ReadResourceResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TaggedNotesResourceHandlerTest {

    private NoteService service;
    private TaggedNotesResourceHandler handler;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        handler = new TaggedNotesResourceHandler(service);
    }

    @Test
    void givenTaggedNotes__whenRead__thenListReturned() throws Exception {
        Entity<Note> note1 = service.create("First Demo Note", "content1", List.of("demo"));
        Entity<Note> note2 = service.create("Second Demo Note", "content2", List.of("demo"));

        ReadResourceResult result = handler.apply(
                ReadResourceParams.builder().uri("notes://tagged/demo").build());

        String text = result.contents().get(0).text();
        assertThat(text, containsString("First Demo Note"));
        assertThat(text, containsString("Second Demo Note"));
    }

    @Test
    void givenNoNotesWithTag__whenRead__thenEmptyMessage() {
        ReadResourceResult result = handler.apply(
                ReadResourceParams.builder().uri("notes://tagged/missing").build());

        String text = result.contents().get(0).text();
        assertThat(text, containsString("No notes tagged"));
    }

    @Test
    void givenMixedTags__whenRead__thenOnlyMatchingReturned() throws Exception {
        service.create("Work Note", "work content", List.of("work"));
        service.create("Personal Note", "personal content", List.of("personal"));

        ReadResourceResult result = handler.apply(
                ReadResourceParams.builder().uri("notes://tagged/work").build());

        String text = result.contents().get(0).text();
        assertThat(text, containsString("Work Note"));
        assertThat(text, not(containsString("Personal Note")));
    }
}

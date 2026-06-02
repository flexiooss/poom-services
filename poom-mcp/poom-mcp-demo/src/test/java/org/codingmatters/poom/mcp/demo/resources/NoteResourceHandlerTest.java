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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

class NoteResourceHandlerTest {

    private NoteService service;
    private NoteResourceHandler handler;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        handler = new NoteResourceHandler(service);
    }

    @Test
    void givenExistingNote__whenRead__thenMarkdownReturned() throws Exception {
        Entity<Note> created = service.create("My Title", "My content body", null);
        String uri = "note://" + created.id();

        ReadResourceResult result = handler.apply(ReadResourceParams.builder().uri(uri).build());

        assertThat(result.contents().size(), is(1));
        String mimeType = result.contents().get(0).mimeType();
        String text = result.contents().get(0).text();
        assertThat(mimeType, is("text/markdown"));
        assertThat(text, containsString("My Title"));
        assertThat(text, containsString("My content body"));
    }

    @Test
    void givenNoteWithTags__whenRead__thenTagsInMarkdown() throws Exception {
        Entity<Note> created = service.create("Tagged Note", "Some content", List.of("alpha", "beta"));
        String uri = "note://" + created.id();

        ReadResourceResult result = handler.apply(ReadResourceParams.builder().uri(uri).build());

        String text = result.contents().get(0).text();
        assertThat(text, containsString("alpha"));
        assertThat(text, containsString("beta"));
    }

    @Test
    void givenUnknownId__whenRead__thenNotFoundText() {
        ReadResourceResult result = handler.apply(
                ReadResourceParams.builder().uri("note://unknown-id").build());

        String text = result.contents().get(0).text();
        assertThat(text.toLowerCase(), containsString("not found"));
    }
}

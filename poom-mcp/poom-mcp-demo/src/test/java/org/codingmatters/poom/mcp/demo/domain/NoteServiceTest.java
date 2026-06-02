package org.codingmatters.poom.mcp.demo.domain;

import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class NoteServiceTest {

    private NoteService service;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
    }

    @Test
    void create_returnsEntityWithId() throws Exception {
        Entity<Note> note = service.create("Hello", "World content", List.of("greetings"));
        assertThat(note.id(), notNullValue());
        assertThat(note.value().title(), is("Hello"));
        assertThat(note.value().content(), is("World content"));
        assertThat(note.value().createdAt(), notNullValue());
        assertThat(note.value().updatedAt(), notNullValue());
    }

    @Test
    void create_withTags_tagsStored() throws Exception {
        Entity<Note> note = service.create("T", "C", List.of("work", "meeting"));
        assertThat(note.value().tags().toArray(), arrayContaining("work", "meeting"));
    }

    @Test
    void create_withNullTags_emptyTagList() throws Exception {
        Entity<Note> note = service.create("T", "C", null);
        assertThat(note.value().tags().toArray(), emptyArray());
    }

    @Test
    void get_existingNote_returnsIt() throws Exception {
        Entity<Note> created = service.create("T", "C", null);
        Optional<Entity<Note>> found = service.get(created.id());
        assertThat(found.isPresent(), is(true));
        assertThat(found.get().value().title(), is("T"));
    }

    @Test
    void get_unknownId_returnsEmpty() throws Exception {
        assertThat(service.get("no-such-id").isPresent(), is(false));
    }

    @Test
    void update_allFields_allChanged() throws Exception {
        Entity<Note> created = service.create("Original", "Body", List.of("tag1"));
        Optional<Entity<Note>> updated = service.update(created.id(), "New title", "New body", List.of("tag2"));
        assertThat(updated.isPresent(), is(true));
        assertThat(updated.get().value().title(), is("New title"));
        assertThat(updated.get().value().content(), is("New body"));
        assertThat(updated.get().value().tags().toArray(), arrayContaining("tag2"));
        assertThat(updated.get().value().createdAt(), is(created.value().createdAt()));
    }

    @Test
    void update_titleOnly_contentAndTagsUnchanged() throws Exception {
        Entity<Note> created = service.create("Original", "Body", List.of("tag1"));
        Optional<Entity<Note>> updated = service.update(created.id(), "New title", null, null);
        assertThat(updated.get().value().title(), is("New title"));
        assertThat(updated.get().value().content(), is("Body"));
        assertThat(updated.get().value().tags().toArray(), arrayContaining("tag1"));
    }

    @Test
    void update_unknownId_returnsEmpty() throws Exception {
        assertThat(service.update("no-such-id", "T", "C", null).isPresent(), is(false));
    }

    @Test
    void delete_existingNote_returnsTrue_andNoteGone() throws Exception {
        Entity<Note> created = service.create("T", "C", null);
        assertThat(service.delete(created.id()), is(true));
        assertThat(service.get(created.id()).isPresent(), is(false));
    }

    @Test
    void delete_unknownId_returnsFalse() throws Exception {
        assertThat(service.delete("no-such-id"), is(false));
    }

    @Test
    void list_noFilter_returnsAll() throws Exception {
        service.create("A", "c", List.of("x"));
        service.create("B", "c", List.of("y"));
        assertThat(service.list(null), hasSize(2));
    }

    @Test
    void list_withTag_returnsOnlyMatching() throws Exception {
        service.create("A", "c", List.of("work"));
        service.create("B", "c", List.of("personal"));
        List<Entity<Note>> result = service.list("work");
        assertThat(result, hasSize(1));
        assertThat(result.get(0).value().title(), is("A"));
    }

    @Test
    void list_withTagNoMatch_returnsEmpty() throws Exception {
        service.create("A", "c", List.of("work"));
        assertThat(service.list("personal"), empty());
    }

    @Test
    void search_matchesInTitle() throws Exception {
        service.create("Meeting notes", "agenda for thursday", null);
        service.create("Shopping list", "milk eggs bread", null);
        List<Entity<Note>> result = service.search("meeting");
        assertThat(result, hasSize(1));
        assertThat(result.get(0).value().title(), is("Meeting notes"));
    }

    @Test
    void search_matchesInContent() throws Exception {
        service.create("Random title", "contains the keyword here", null);
        List<Entity<Note>> result = service.search("keyword");
        assertThat(result, hasSize(1));
    }

    @Test
    void search_caseInsensitive() throws Exception {
        service.create("UPPERCASE TITLE", "content", null);
        assertThat(service.search("uppercase"), hasSize(1));
    }

    @Test
    void search_noMatch_returnsEmpty() throws Exception {
        service.create("T", "C", null);
        assertThat(service.search("xyzzy"), empty());
    }
}

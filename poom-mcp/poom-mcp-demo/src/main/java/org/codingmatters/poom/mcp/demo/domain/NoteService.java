package org.codingmatters.poom.mcp.demo.domain;

import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteService {

    private final Repository<Note, PropertyQuery> repository;

    public NoteService(Repository<Note, PropertyQuery> repository) {
        this.repository = repository;
    }

    public Entity<Note> create(String title, String content, List<String> tags) throws RepositoryException {
        Note note = Note.builder()
                .title(title)
                .content(content)
                .tags(tags != null ? tags : new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.create(note);
    }

    public Optional<Entity<Note>> get(String id) throws RepositoryException {
        return Optional.ofNullable(repository.retrieve(id));
    }

    /** Partial update — null fields are left unchanged. */
    public Optional<Entity<Note>> update(String id, String title, String content, List<String> tags)
            throws RepositoryException {
        Entity<Note> existing = repository.retrieve(id);
        if (existing == null) return Optional.empty();
        Note.Builder builder = Note.builder()
                .title(title != null ? title : existing.value().title())
                .content(content != null ? content : existing.value().content())
                .createdAt(existing.value().createdAt())
                .updatedAt(LocalDateTime.now());
        if (tags != null) {
            builder.tags(tags);
        } else if (existing.value().opt().tags().isPresent()) {
            builder.tags(existing.value().tags());
        } else {
            builder.tags(new ArrayList<String>());
        }
        return Optional.of(repository.update(existing, builder.build()));
    }

    public boolean delete(String id) throws RepositoryException {
        Entity<Note> existing = repository.retrieve(id);
        if (existing == null) return false;
        repository.delete(existing);
        return true;
    }

    /** Lists all notes, optionally filtered by tag (case-insensitive). */
    public List<Entity<Note>> list(String tag) throws RepositoryException {
        PagedEntityList<Note> all = repository.all(0, Integer.MAX_VALUE);
        List<Entity<Note>> result = new ArrayList<>();
        for (Entity<Note> entity : all) {
            if (tag == null || containsTag(entity.value(), tag)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * Full-text search in title and content (case-insensitive).
     * Sleeps 600ms intentionally to simulate a slow operation and
     * demonstrate the MCP async tool call path (202 + SSE push).
     */
    public List<Entity<Note>> search(String query) throws RepositoryException, InterruptedException {
        Thread.sleep(600);
        String lq = query.toLowerCase();
        PagedEntityList<Note> all = repository.all(0, Integer.MAX_VALUE);
        List<Entity<Note>> result = new ArrayList<>();
        for (Entity<Note> entity : all) {
            Note n = entity.value();
            if ((n.title() != null && n.title().toLowerCase().contains(lq))
                    || (n.content() != null && n.content().toLowerCase().contains(lq))) {
                result.add(entity);
            }
        }
        return result;
    }

    private boolean containsTag(Note note, String tag) {
        if (note.opt().tags().isEmpty()) return false;
        for (String t : note.tags()) {
            if (tag.equalsIgnoreCase(t)) return true;
        }
        return false;
    }
}

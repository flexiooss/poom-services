package org.codingmatters.poom.mcp.demo.domain;

import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;

public class NoteRepository {
    private NoteRepository() {}

    public static Repository<Note, PropertyQuery> create() {
        return InMemoryRepositoryWithPropertyQuery.validating(Note.class);
    }
}

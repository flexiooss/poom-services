package org.codingmatters.poom.services.test.utils;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

/**
 * Created by nelt on 7/17/17.
 */
public class StringInMemoryRepository extends InMemoryRepository<String, String> {
    @Override
    public PagedEntityList<String> search(String query, long startIndex, long endIndex) throws RepositoryException {
        return this.paged(this.stream().filter(entity -> entity.value().startsWith(query)), startIndex, endIndex);
    }
}

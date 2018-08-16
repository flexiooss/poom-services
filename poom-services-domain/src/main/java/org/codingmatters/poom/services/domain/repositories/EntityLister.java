package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

public interface EntityLister<V, Q> {
    PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException;
    PagedEntityList<V> search(Q query, long startIndex, long endIndex) throws RepositoryException;
}

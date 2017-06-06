package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

/**
 * Created by nelt on 6/2/17.
 */
public interface Repository<V, Q> {
    Entity<V> create(V withValue) throws RepositoryException;
    Entity<V> retrieve(String id) throws RepositoryException;
    void update(Entity<V> entity, V withValue) throws RepositoryException;
    void delete(Entity<V> entity) throws RepositoryException;
    PagedEntityList<V> search(Q query, int page, int pageSize) throws RepositoryException;
}

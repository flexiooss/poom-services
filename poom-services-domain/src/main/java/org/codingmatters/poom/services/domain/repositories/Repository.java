package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.math.BigInteger;

/**
 * Created by nelt on 6/2/17.
 */
public interface Repository<V, Q> {
    Entity<V> create(V withValue) throws RepositoryException;
    Entity<V> retrieve(String id) throws RepositoryException;
    Entity<V>  update(Entity<V> entity, V withValue) throws RepositoryException;
    void delete(Entity<V> entity) throws RepositoryException;
    PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException;
    PagedEntityList<V> search(Q query, long startIndex, long endIndex) throws RepositoryException;

    default Entity<V> createWithId(String id, V withValue) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    default Entity<V> createWithIdAndVersion(String id, BigInteger version, V withValue) throws RepositoryException {
        throw new UnsupportedOperationException();
    }
}

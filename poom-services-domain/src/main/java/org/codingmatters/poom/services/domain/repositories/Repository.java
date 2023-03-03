package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.entities.Entity;

import java.math.BigInteger;

/**
 * Created by nelt on 6/2/17.
 */
public interface Repository<V, Q> extends EntityLister<V, Q> {
    Entity<V> create(V withValue) throws RepositoryException;
    Entity<V> retrieve(String id) throws RepositoryException;
    Entity<V>  update(Entity<V> entity, V withValue) throws RepositoryException;
    void delete(Entity<V> entity) throws RepositoryException;

    default void deleteFrom(Q query) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    default Entity<V> createWithId(String id, V withValue) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    default Entity<V> createWithIdAndVersion(String id, BigInteger version, V withValue) throws RepositoryException {
        throw new UnsupportedOperationException();
    }
}

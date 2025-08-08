package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.entities.Entity;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nelt on 6/2/17.
 */
public interface Repository<V, Q> extends EntityLister<V, Q> {
    Entity<V> create(V withValue) throws RepositoryException;
    Entity<V> retrieve(String id) throws RepositoryException;
    Entity<V> update(Entity<V> entity, V withValue) throws RepositoryException;
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

    default List<String> createMany(V ... values) throws RepositoryException {
        List<String> result = new LinkedList<>();
        if(values != null) {
            for (V value : values) {
                Entity<V> created = this.create(value);
                result.add(created.id());
            }
        }
        return result;
    }
}

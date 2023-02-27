package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

import java.math.BigInteger;

public class RepositoryWithDelegate<V, Q> implements Repository<V, Q> {
    private final Repository<V, Q> delegate;

    public RepositoryWithDelegate(Repository<V, Q> delegate) {
        this.delegate = delegate;
    }

    protected Repository<V, Q> delegate() {
        return delegate;
    }

    @Override
    public PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException {
        return delegate.all(startIndex, endIndex);
    }

    @Override
    public PagedEntityList<V> search(Q query, long startIndex, long endIndex) throws RepositoryException {
        return delegate.search(query, startIndex, endIndex);
    }

    @Override
    public Entity<V> create(V withValue) throws RepositoryException {
        return delegate.create(withValue);
    }

    @Override
    public Entity<V> retrieve(String id) throws RepositoryException {
        return delegate.retrieve(id);
    }

    @Override
    public Entity<V> update(Entity<V> entity, V withValue) throws RepositoryException {
        return delegate.update(entity, withValue);
    }

    @Override
    public void delete(Entity<V> entity) throws RepositoryException {
        delegate.delete(entity);
    }

    @Override
    public void deleteFrom(Q query) throws RepositoryException {
        delegate.deleteFrom(query);
    }

    @Override
    public Entity<V> createWithId(String id, V withValue) throws RepositoryException {
        return delegate.createWithId(id, withValue);
    }

    @Override
    public Entity<V> createWithIdAndVersion(String id, BigInteger version, V withValue) throws RepositoryException {
        return delegate.createWithIdAndVersion(id, version, withValue);
    }
}

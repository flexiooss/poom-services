package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.MutableEntity;

import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by nelt on 6/5/17.
 */
public abstract class InMemoryRepository<V, Q> implements Repository<V, Q> {
    private final TreeMap<String, MutableEntity<V>> store = new TreeMap<>();

    @Override
    public Entity<V> create(V withValue) throws RepositoryException {
        MutableEntity<V> entity = new MutableEntity<>(UUID.randomUUID().toString(), withValue);
        this.store.put(entity.id(), entity);
        return entity;
    }

    @Override
    public Entity<V> retrieve(String id) throws RepositoryException {
        return this.store.get(id);
    }

    @Override
    public void update(Entity<V> entity, V withValue) throws RepositoryException {
        if(this.store.containsKey(entity.id())) {
            this.store.get(entity.id()).changeValue(withValue);
        } else {
            throw new RepositoryException("cannot update entity, no such entity in store : " + entity.id());
        }
    }

    @Override
    public void delete(Entity<V> entity) throws RepositoryException {
        if(this.store.containsKey(entity.id())) {
            this.store.remove(entity.id());
        } else {
            throw new RepositoryException("cannot delete entity, no such entity in store : " + entity.id());
        }
    }
}

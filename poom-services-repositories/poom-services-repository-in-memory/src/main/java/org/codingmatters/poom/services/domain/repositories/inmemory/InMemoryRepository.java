package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.MutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Created by nelt on 6/5/17.
 */
public abstract class InMemoryRepository<V, Q> implements Repository<V, Q> {
    private final LinkedHashMap<String, MutableEntity<V>> store = new LinkedHashMap<>();

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

    @Override
    public PagedEntityList<V> all(int page, int pageSize) throws RepositoryException {
        if(this.store.isEmpty()) {
            return new PagedEntityList.DefaultPagedEntityList<>(page, false, Collections.emptyList());
        }

        int start = page * pageSize;
        if(start >= this.store.size()) {
            return new PagedEntityList.DefaultPagedEntityList<>(page, false, Collections.emptyList());
        }

        int end = Math.min((page + 1) * pageSize, this.store.size());

        boolean hasNextPage = (page + 1) * pageSize < this.store.size();

        Entity[] entities = this.store.values().toArray(new Entity[this.store.values().size()]);
        Entity[] results = new Entity[end - start];

        System.arraycopy(entities, start, results, 0, results.length);

        return new PagedEntityList.DefaultPagedEntityList<>(page, hasNextPage, Arrays.asList(results));
    }
}

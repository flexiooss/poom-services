package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.MutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Created by nelt on 6/5/17.
 */
public abstract class InMemoryRepository<V, Q> implements Repository<V, Q> {
    private final LinkedHashMap<String, MutableEntity<V>> store = new LinkedHashMap<>();

    @Override
    public Entity<V> create(V withValue) throws RepositoryException {
        MutableEntity<V> entity = new MutableEntity<>(UUID.randomUUID().toString(), withValue);
        this.store.put(entity.id(), entity);
        return ImmutableEntity.from(entity);
    }

    @Override
    public Entity<V> retrieve(String id) throws RepositoryException {
        return this.store.get(id);
    }

    @Override
    public Entity<V> update(Entity<V> entity, V withValue) throws RepositoryException {
        if(this.store.containsKey(entity.id())) {
            MutableEntity<V> storedEntity = this.store.get(entity.id());
            storedEntity.changeValue(withValue);
            return ImmutableEntity.from(storedEntity);
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
    public PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException {
        if(this.store.isEmpty()) {
            return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, Collections.emptyList());
        }
        if(startIndex >= this.store.size()) {
            return new PagedEntityList.DefaultPagedEntityList<>(0, 0, this.store.size(), Collections.emptyList());
        }
        return this.slice(this.store.values().toArray(new Entity[this.store.values().size()]), startIndex, endIndex);
    }

    protected Stream<Entity<V>> stream() {
        return this.store.values().stream().map(e -> (Entity<V>)e);
    }

    protected PagedEntityList<V> paged(Stream<Entity<V>> stream, long startIndex, long endIndex) {
        return this.slice(stream.<Entity>toArray(length -> new Entity[length]), startIndex, endIndex);
    }

    private PagedEntityList<V> slice(Entity<V>[] entities, long startIndex, long endIndex) {
        int start = (int) startIndex;
        int end = (int) Math.min(endIndex + 1, entities.length);

        Entity[] results = new Entity[end - start];
        System.arraycopy(entities, start, results, 0, results.length);

        for (int i = 0; i < results.length; i++) {
            results[i] = ImmutableEntity.from(results[i]);
        }

        return new PagedEntityList.DefaultPagedEntityList<>(startIndex, end - 1, entities.length, Arrays.asList(results));
    }
}

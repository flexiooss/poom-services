package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.codingmatters.poom.services.domain.entities.MutableEntity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Created by nelt on 6/5/17.
 */
public abstract class InMemoryRepository<V, Q> implements Repository<V, Q> {

    private final Store<V> store;

    public InMemoryRepository() {
        this(false);
    }

    public InMemoryRepository(boolean withOptimisticLocking) {
        this.store = new Store<>(withOptimisticLocking);
    }

    @Override
    public Entity<V> create(V withValue) throws RepositoryException {
        return this.quickCreate(UUID.randomUUID().toString(), withValue);
    }

    @Override
    public Entity<V> createWithId(String id, V withValue) throws RepositoryException {
        if(this.store.get(id) == null) {
            return this.quickCreate(id, withValue);
        } else {
            throw new RepositoryException("entity already exists : " + id);
        }
    }

    @Override
    public Entity<V> createWithIdAndVersion(String id, BigInteger version, V withValue) throws RepositoryException {
        if(this.store.get(id) == null) {
            return this.quickCreate(id, version, withValue);
        } else {
            throw new RepositoryException("entity already exists : " + id);
        }
    }

    private Entity<V> quickCreate(String id, V withValue) {
        return this.quickCreate(id, BigInteger.ONE, withValue);
    }
    private Entity<V> quickCreate(String id, BigInteger version, V withValue) {
        MutableEntity<V> entity = new MutableEntity<>(id, version, withValue);
        this.store.store(entity);
        return ImmutableEntity.from(entity);
    }

    @Override
    public Entity<V> retrieve(String id) throws RepositoryException {
        return ImmutableEntity.from(this.store.get(id));
    }

    @Override
    public Entity<V> update(Entity<V> entity, V withValue) throws RepositoryException {
        if(this.store.isStored(entity)) {
            return ImmutableEntity.from(this.store.update(entity, withValue));
        } else {
            throw new RepositoryException("cannot update entity, no such entity in store : " + entity.id());
        }
    }

    @Override
    public void delete(Entity<V> entity) throws RepositoryException {
        if(this.store.isStored(entity)) {
            this.store.remove(entity);
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
        return this.slice(this.store.contents(), startIndex, endIndex);
    }

    protected Stream<Entity<V>> stream() {
        return this.store.stream();
    }

    protected PagedEntityList<V> paged(Stream<Entity<V>> stream, long startIndex, long endIndex) {
        return this.slice(stream.<Entity>toArray(length -> new Entity[length]), startIndex, endIndex);
    }

    private PagedEntityList<V> slice(Entity<V>[] entities, long startIndex, long endIndex) {
        int start = (int) startIndex;
        int end = (int) Math.min(endIndex + 1, entities.length);

        if(end - start <= 0) {
            return new PagedEntityList.DefaultPagedEntityList<>(startIndex, 0, entities.length, Arrays.asList(new Entity[0]));
        }

        Entity[] results = new Entity[end - start];
        System.arraycopy(entities, start, results, 0, results.length);

        for (int i = 0; i < results.length; i++) {
            results[i] = ImmutableEntity.from(results[i]);
        }

        return new PagedEntityList.DefaultPagedEntityList<>(startIndex, end - 1, entities.length, Arrays.asList(results));
    }
}

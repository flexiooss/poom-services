package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.MutableEntity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

public class Store<V> {

    private final LinkedHashMap<String, MutableEntity<V>> store = new LinkedHashMap<>();
    private final boolean withOptimisticLocking;

    public Store(boolean withOptimisticLocking) {
        this.withOptimisticLocking = withOptimisticLocking;
    }

    public synchronized void store(MutableEntity<V> entity) {
        this.store.put(entity.id(), entity);
    }

    public synchronized MutableEntity<V> get(String id) {
        return this.store.get(id);
    }

    public synchronized boolean isStored(Entity<V> entity) {
        return this.store.containsKey(entity.id());
    }

    public synchronized void remove(Entity<V> entity) {
        this.store.remove(entity.id());
    }

    public synchronized boolean isEmpty() {
        return this.store.isEmpty();
    }

    public synchronized int size() {
        return this.store.size();
    }

    public synchronized Stream<Entity<V>> stream() {
        return Arrays.stream(this.contents());
    }

    public synchronized Entity<V>[] contents() {
        return this.store.values().toArray(new Entity[this.store.size()]);
    }

    public synchronized MutableEntity<V> update(Entity<V> entity, V withValue) throws RepositoryException {
        if(this.isStored(entity)) {
            MutableEntity<V> mutableEntity = this.get(entity.id());
            if(this.withOptimisticLocking) {
                this.ensureLockGranted(entity, mutableEntity);
            }
            mutableEntity.changeValue(withValue);
            return mutableEntity;
        }
        return null;
    }

    private void ensureLockGranted(Entity<V> entity, MutableEntity<V> mutableEntity) throws RepositoryException {
        if(entity.version() == null) {
            throw new RepositoryException("cannot update entity : since optimistic locking activated, must provide a version");
        } else {
            if(! mutableEntity.version().equals(entity.version())) {
                throw new RepositoryException(String.format("cannot update entity : optimistic locking error, version %s does not match", entity.version()));
            }
        }
    }
}

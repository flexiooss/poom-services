package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.MutableEntity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

public class Store<V> {

    private final LinkedHashMap<String, MutableEntity<V>> store = new LinkedHashMap<>();

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

    public synchronized MutableEntity<V> update(Entity<V> entity, V withValue) {
        if(this.isStored(entity)) {
            MutableEntity<V> mutableEntity = this.get(entity.id());
            mutableEntity.changeValue(withValue);
            return mutableEntity;
        }
        return null;
    }
}

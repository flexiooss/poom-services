package org.codingmatters.poom.caches.in.memory.stores;

import java.util.*;
import java.util.function.Function;

public class MapCacheStore<K, V> implements CacheStore<K, V> {
    private final Map<K, Optional<V>> delegate = new HashMap<>();

    public Optional<V> get(K key) {
        return this.delegate.get(key);
    }

    @Override
    public void store(K key, V value) {
        this.delegate.put(key, Optional.ofNullable(value));
    }

    public boolean has(K key) {
        return this.delegate.containsKey(key);
    }

    public void remove(K key) {
        this.delegate.remove(key);
    }

    public void clear() {
        this.delegate.clear();
    }

    public Set<K> keys() {
        return new HashSet<>(this.delegate.keySet());
    }
}

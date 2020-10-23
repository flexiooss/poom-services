package org.codingmatters.poom.caches.management.stores;

import java.util.Optional;
import java.util.Set;

public interface CacheStore<K, V> {
    Optional<V> get(K key);
    void store(K key, V value);
    boolean has(K key);
    void remove(K key);
    void clear();
    Set<K> keys();
}

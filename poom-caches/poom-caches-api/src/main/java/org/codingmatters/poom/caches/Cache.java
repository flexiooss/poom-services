package org.codingmatters.poom.caches;

import org.codingmatters.poom.caches.invalidation.Invalidation;

public interface Cache<K, V> {
    V get(K key);
    void prune(K key);
    void pruneAll();

    void addPruneListener(PruneListener<K> listener);
    void addAccessListener(AccessListener<K> listener);

    @FunctionalInterface
    interface ValueRetriever<K, V> {
        V retrieve(K key) throws Exception;
    }

    @FunctionalInterface
    interface PruneListener<K> {
        void pruned(K key);
    }

    interface AccessListener<K> {
        void accessed(K key);
    }

    interface ValueInvalidator<K, V> {
        Invalidation check(K key, V value) throws Exception;

        static <K, V> ValueInvalidator<K, V> alwaysValid() {
            return (key, value) -> Invalidation.valid();
        }
    }
}

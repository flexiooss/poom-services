package org.codingmatters.poom.caches.management.caches;

import org.codingmatters.poom.caches.Cache;
import org.codingmatters.poom.caches.management.stores.CacheStore;
import org.codingmatters.poom.caches.invalidation.Invalidation;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CacheWithStore<K, V> implements Cache<K, V> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(CacheWithStore.class);

    private final ValueRetriever<K, V> retriever;
    private final CacheStore<K, V> delegate;
    private final ValueInvalidator<K, V> invalidator;
    private final List<PruneListener<K>> pruneListeners = new LinkedList<>();
    private final List<AccessListener<K>> accessListeners = new LinkedList<>();

    public CacheWithStore(
            ValueRetriever<K, V> retriever,
            CacheStore<K, V> store,
            Cache.ValueInvalidator<K, V> invalidator
    ) {
        this.retriever = retriever;
        this.delegate = store;
        this.invalidator = invalidator;
    }

    @Override
    public synchronized V get(K key) throws Exception {
        Optional<V> value;
        if(this.delegate.has(key)) {
            value = this.delegate.get(key);

            Invalidation<V> invalidation = this.invalidator.check(key, value.get());
            if(invalidation.isInvalid()) {
                this.prune(key);
                if(invalidation.newValue().isPresent()) {
                    value = invalidation.newValue();
                    this.delegate.store(key, value.get());
                } else {
                    value = this.retrieve(key);
                    if(value.isPresent()) {
                        this.delegate.store(key, value.get());
                    }
                }
            }
        } else {
            value = this.retrieve(key);
            if(value.isPresent()) {
                this.delegate.store(key, value.get());
            }
        }
        this.accessListeners.forEach(listener -> listener.accessed(key));
        return value.orElse(null);
    }

    @Override
    public void insert(K key, V value) {
        this.delegate.store(key, value);
    }

    private Optional<V> retrieve(K key) throws Exception {
        V newValue = this.retriever.retrieve(key);
        return newValue != null ? Optional.of(newValue) : Optional.empty();
    }

    @Override
    public synchronized void prune(K key) {
        if(this.delegate.has(key)) {
            this.delegate.remove(key);
            this.pruneListeners.forEach(listener -> listener.pruned(key));
        }
    }

    @Override
    public synchronized void pruneAll() {
        Set<K> keys = this.delegate.keys();
        this.delegate.clear();
        keys.forEach(key -> this.pruneListeners.forEach(listener -> listener.pruned(key)));
    }

    @Override
    public synchronized void addPruneListener(PruneListener<K> listener) {
        if(listener != null) {
            this.pruneListeners.add(listener);
        }
    }

    @Override
    public void addAccessListener(AccessListener<K> listener) {
        if(listener != null) {
            this.accessListeners.add(listener);
        }
    }
}

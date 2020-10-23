package org.codingmatters.poom.caches.management;

import org.codingmatters.poom.caches.Cache;
import org.codingmatters.poom.caches.management.caches.CacheWithStore;
import org.codingmatters.poom.caches.management.lru.LRUManager;
import org.codingmatters.poom.caches.management.stores.MapCacheStore;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CacheManager<K, V> implements Closeable {

    static public <K, V> CacheManager<K, V> newHashMapBackedCache(
            Cache.ValueRetriever<K, V> retriever, Cache.ValueInvalidator<K, V> invalidator,
            int capacity,
            ScheduledExecutorService scheduler, long delay, TimeUnit delayUnit) {
        return createMapBackedCache(new HashMap<>(), retriever, invalidator, capacity, scheduler, delay, delayUnit);
    }

    static private <K, V> CacheManager<K, V> createMapBackedCache(HashMap<K, Optional<V>> map, Cache.ValueRetriever<K, V> retriever, Cache.ValueInvalidator<K, V> invalidator, int capacity, ScheduledExecutorService scheduler, long delay, TimeUnit delayUnit) {
        return new CacheManager<K, V>(
                new CacheWithStore<>(retriever, new MapCacheStore<>(map), invalidator),
                capacity,
                scheduler, delay, delayUnit
                );
    }

    private final Cache<K, V> cache;
    private final ScheduledExecutorService scheduler;
    private final LRUManager<K> lruManager;
    private final long delay;
    private final TimeUnit timeUnit;
    private final ScheduledFuture<?> task;

    public CacheManager(Cache<K, V> cache, int capacity, ScheduledExecutorService scheduler, long delay, TimeUnit timeUnit) {
        this.cache = cache;
        this.scheduler = scheduler;
        this.lruManager = new LRUManager<>(capacity);
        this.delay = delay;
        this.timeUnit = timeUnit;

        this.cache.addAccessListener(key -> this.lruManager.accessed(key));
        this.task = this.scheduler.scheduleWithFixedDelay(this::cleanup, this.delay, this.delay, this.timeUnit);
    }

    public Cache<K, V> cache() {
        return cache;
    }

    private void cleanup() {
        List<K> overload = this.lruManager.overload();
        System.out.println("overload :: " + overload);
        for (K key : overload) {
            this.cache.prune(key);
        }
        for (K key : overload) {
            this.lruManager.removed(key);
        }
    }

    @Override
    public void close() throws IOException {
        this.task.cancel(false);
    }
}

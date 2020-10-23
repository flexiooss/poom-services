package org.codingmatters.poom.caches.management.caches;


import org.codingmatters.poom.caches.Cache;
import org.codingmatters.poom.caches.management.stores.CacheStore;
import org.codingmatters.poom.caches.management.stores.MapCacheStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(Parameterized.class)
public class CacheWithStoreTest {

    @Parameterized.Parameters
    public static Object[] cacheStores() {
        return new Object[]{
                (Supplier<Object>) () -> new MapCacheStore<String, String>()
        };
    };

    @Parameterized.Parameter
    public Supplier<CacheStore<String, String>> storeSupplier;

    @Test
    public void givenNoKeyRetrieved__whenGetting__thenRetrieverCalled() throws Exception {
        Cache<String, String> actual = this.createCache(key -> key);
        assertThat(actual.get("test"), is("test"));
    }

    @Test
    public void givenKeyAlreadyRetrieved__whenGettingSameKey__thenRetrieverNotCalled() throws Exception {
        AtomicReference<String> nextVal = new AtomicReference<>("initial");
        Cache<String, String> actual = this.createCache(key -> nextVal.get());

        actual.get("test");
        nextVal.set("new");

        assertThat(actual.get("test"), is("initial"));
    }

    @Test
    public void givenKeyAlreadyRetrieved__whenGettingOtherKey__thenRetrieverCalled() throws Exception {
        AtomicReference<String> nextVal = new AtomicReference<>("initial");
        Cache<String, String> actual = this.createCache(key -> nextVal.get());

        actual.get("test");
        nextVal.set("new");

        assertThat(actual.get("other"), is("new"));
    }

    @Test
    public void givenNoKeyRetrieved__whenRetrieverRaisesAnException__thenReturnsNull() throws Exception {
        Cache<String, String> actual = this.createCache(key -> {throw new IOException("failed retrieving value");});
        assertThat(actual.get("test"), is(nullValue()));
    }

    @Test
    public void givenPruneListenerRegistered__whenNoKeyRetrieved_andPruningKey__thenPruneListenerNotCalled() throws Exception {
        Cache<String, String> actual = this.createCache(key -> key);

        List<Object> pruned = Collections.synchronizedList(new LinkedList<>());
        actual.addPruneListener(key -> pruned.add(key));

        actual.prune("test");

        assertThat(pruned, is(empty()));
    }

    @Test
    public void givenPruneListenerRegistered__whenKeyRetrieved_andPruningKey__thenPruneListenerCalled() throws Exception {
        Cache<String, String> actual = this.createCache(key -> key);
        actual.get("test");

        List<Object> pruned = Collections.synchronizedList(new LinkedList<>());
        actual.addPruneListener(key -> pruned.add(key));

        actual.prune("test");

        assertThat(pruned, contains("test"));
    }

    @Test
    public void givenPruneListenerRegistered__whenNoKeyRetrieved_andPruningAll__thenPruneListenerNotCalled() throws Exception {
        Cache<String, String> actual = this.createCache(key -> key);

        List<Object> pruned = Collections.synchronizedList(new LinkedList<>());
        actual.addPruneListener(key -> pruned.add(key));

        actual.pruneAll();

        assertThat(pruned, is(empty()));
    }

    @Test
    public void givenPruneListenerRegistered__whenKeyRetrieved_andPruningAll__thenPruneListenerCalled() throws Exception {
        Cache<String, String> actual = this.createCache(key -> key);
        actual.get("test");

        List<Object> pruned = Collections.synchronizedList(new LinkedList<>());
        actual.addPruneListener(key -> pruned.add(key));

        actual.pruneAll();

        assertThat(pruned, contains("test"));
    }

    @Test
    public void givenAccessListenerRegisterd__when__then() throws Exception {
        Cache<String, String> actual = this.createCache(key -> key);
        LinkedList<String> accessed = new LinkedList<>();
        actual.addAccessListener(key -> accessed.add(key));

        actual.get("test");

        assertThat(accessed, contains("test"));
    }

    private Cache<String, String> createCache(Cache.ValueRetriever<String, String> retriever) {
        return new CacheWithStore<>(retriever, this.storeSupplier.get(), Cache.ValueInvalidator.alwaysValid());
    }
}
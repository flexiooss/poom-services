package org.codingmatters.poom.caches.management.caches;


import org.codingmatters.poom.caches.Cache;
import org.codingmatters.poom.caches.invalidation.Invalidation;
import org.codingmatters.poom.caches.management.stores.CacheStore;
import org.codingmatters.poom.caches.management.stores.MapCacheStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
                (Supplier<Object>) MapCacheStore::new
        };
    }

    @Parameterized.Parameter
    public Supplier<CacheStore<String, String>> storeSupplier;

    @Test
    public void givenNoKeyRetrieved__whenGetting__thenRetrieverCalled() throws Exception {
        Cache<String, String> actual = this.createCache(key -> key);
        assertThat(actual.get("test"), is("test"));
    }

    @Test
    public void whenInsert_thenRetrieveInserted() throws Exception {
        Cache<String, String> actual = this.createCache(key -> null);
        assertThat(actual.get("test"), nullValue());
        actual.insert("test", "ok");
        assertThat(actual.get("test"), is("ok"));
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

    @Test
    public void givenRetrieverThrowsError__whenGetting__thenReturnsNull_andThrownErrorIsRecoverable() throws Exception {
        Cache<String, String> actual = this.createCache(key -> {
            throw new AssertionError("error while retrieving " + key);
        });

        try {
            actual.get("test");
        } catch (Throwable e) {
            assertThat(e, is(notNullValue()));
            assertThat(e, isA(AssertionError.class));
            assertThat(e.getMessage(), is("error while retrieving test"));
        }
    }

    @Test
    public void givenInvalidatorThrowsError__whenGetting__thenReturnsPreviousValue_andThrownErrorIsRecoverable() throws Exception {
        Cache<String, String> actual = this.createCache(key -> "new value", (key, value) -> {
            throw new AssertionError("error while invalidating " + key);
        });
        actual.insert("test", "previous value");

        try {
            actual.get("test");
        } catch (Throwable e) {
            assertThat(e, is(notNullValue()));
            assertThat(e, isA(AssertionError.class));
            assertThat(e.getMessage(), is("error while invalidating test"));
        }
    }

    @Test
    public void givenValid__withNewValue() throws Exception {
        Cache<String, String> cache = this.createCache(key -> "new value", (key, value) -> Invalidation.replaced("plok"));
        cache.insert("key", "value");
        assertThat(cache.get("key"), is("plok"));
    }

    private Cache<String, String> createCache(Cache.ValueRetriever<String, String> retriever) {
        return new CacheWithStore<>(retriever, this.storeSupplier.get(), Cache.ValueInvalidator.alwaysValid());
    }

    private Cache<String, String> createCache(Cache.ValueRetriever<String, String> retriever, Cache.ValueInvalidator<String, String> invalidator) {
        return new CacheWithStore<>(retriever, this.storeSupplier.get(), invalidator);
    }
}

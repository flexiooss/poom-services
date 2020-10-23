package org.codingmatters.poom.caches.in.memory;

import org.codingmatters.poom.caches.Cache;
import org.codingmatters.poom.caches.invalidation.Invalidation;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CacheWithStoreInvalidationTest {

    private final AtomicReference<Invalidation<String>> nextInvalidation = new AtomicReference<>();
    private final List<String> invalidationChecked = new LinkedList<>();
    private final Cache.ValueInvalidator<String, String> invalidator = (key, value) -> {
        this.invalidationChecked.add(key);
        return this.nextInvalidation.get();
    };

    private final TestCacheStore<String, String> store = new TestCacheStore<>();

    private final AtomicReference<String> nextRetrieved = new AtomicReference<>("test");
    private final List<String> retrieved = new LinkedList<>();
    private Cache.ValueRetriever<String, String> retriever = key -> {
        this.retrieved.add(key);
        return this.nextRetrieved.get();
    };

    private final CacheWithStore<String, String> cache = new CacheWithStore<>(this.retriever, this.store, this.invalidator);

    private final List<String> pruned = new LinkedList<>();

    @Before
    public void setUp() throws Exception {
        this.cache.addPruneListener(key -> this.pruned.add(key));
    }

    @Test
    public void givenEmptyCache__whenGettingKey__thenInvalidationNotChecked() throws Exception {
        this.nextInvalidation.set(Invalidation.invalid());
        this.cache.get("test");

        assertThat(this.retrieved, contains("test"));
        assertThat(this.invalidationChecked, is(empty()));
        assertThat(this.pruned, is(empty()));

        assertThat(this.store.actions(), contains(
                TestCacheStore.ActionType.HAS.action("test").returning(false),
                TestCacheStore.ActionType.STORE.action("test", "test").returning(null)
        ));
    }

    @Test
    public void givenValueCached__whenGetting_andValueIsValid__thenValidationChecked_andValueReturnedWithoutRetrieving() throws Exception {
        this.cache.get("test");

        this.nextInvalidation.set(Invalidation.valid());
        this.cache.get("test");

        assertThat(this.retrieved, contains("test"));
        assertThat(this.invalidationChecked, contains("test"));
        assertThat(this.pruned, is(empty()));

        assertThat(this.store.actions(), contains(
                TestCacheStore.ActionType.HAS.action("test").returning(false),
                TestCacheStore.ActionType.STORE.action("test", "test").returning(null),
                TestCacheStore.ActionType.HAS.action("test").returning(true),
                TestCacheStore.ActionType.GET.action("test").returning(Optional.of("test"))
        ));
    }
    @Test
    public void givenValueCached__whenGetting_andValueIsInvalidated__thenValidationChecked_andValueRetrieved_andNewValueReturned() throws Exception {
        this.cache.get("test");

        this.nextInvalidation.set(Invalidation.invalid());
        this.nextRetrieved.set("new value");

        assertThat(this.cache.get("test"), is("new value"));

        assertThat(this.retrieved, contains("test", "test"));
        assertThat(this.invalidationChecked, contains("test"));
        assertThat(this.pruned, contains("test"));

        assertThat(this.store.actions(), contains(
                TestCacheStore.ActionType.HAS.action("test").returning(false),
                TestCacheStore.ActionType.STORE.action("test", "test").returning(null),
                TestCacheStore.ActionType.HAS.action("test").returning(true),
                TestCacheStore.ActionType.GET.action("test").returning(Optional.of("test")),
                TestCacheStore.ActionType.HAS.action("test").returning(true),
                TestCacheStore.ActionType.REMOVE.action("test").returning(null),
                TestCacheStore.ActionType.STORE.action("test", "new value").returning(null)
        ));
    }
    @Test
    public void givenValueCached__whenGetting_andValueInvalidated_andNewValueWithValidation__thenValidationChecked_andValueNotRetrieved_andNewValueReturned_andNewValueStored() throws Exception {
        this.cache.get("test");

        this.nextInvalidation.set(Invalidation.replaced("new value"));

        assertThat(this.cache.get("test"), is("new value"));

        assertThat(this.retrieved, contains("test"));
        assertThat(this.invalidationChecked, contains("test"));
        assertThat(this.pruned, contains("test"));

        assertThat(this.store.actions(), contains(
                TestCacheStore.ActionType.HAS.action("test").returning(false),
                TestCacheStore.ActionType.STORE.action("test", "test").returning(null),
                TestCacheStore.ActionType.HAS.action("test").returning(true),
                TestCacheStore.ActionType.GET.action("test").returning(Optional.of("test")),
                TestCacheStore.ActionType.HAS.action("test").returning(true),
                TestCacheStore.ActionType.REMOVE.action("test").returning(null),
                TestCacheStore.ActionType.STORE.action("test", "new value").returning(null)
        ));
    }
}

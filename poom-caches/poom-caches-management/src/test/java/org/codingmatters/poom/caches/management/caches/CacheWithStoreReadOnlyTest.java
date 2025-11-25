package org.codingmatters.poom.caches.management.caches;

import org.codingmatters.poom.caches.invalidation.Invalidation;
import org.codingmatters.poom.caches.management.stores.MapCacheStore;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

class CacheWithStoreReadOnlyTest {
    @Test
    void whenReadonlyIsFalse__thenRetrievedValueIsStored() throws Exception {
        Map<String, String> backup = Collections.synchronizedMap(new HashMap<>());
        Map<String, Optional<String>> deleguate = Collections.synchronizedMap(new HashMap<>());

        CacheWithStore<String, String> cache = new CacheWithStore<>(
                key -> backup.get(key),
                new MapCacheStore<String, String>(deleguate),
                (key, value) -> Invalidation.invalid(),
                false
        );

        backup.put("k", "v");

        assertThat(cache.get("k"), is("v"));
        assertThat(deleguate.get("k"), is(Optional.of("v")));
    }

    @Test
    void whenReadonlyIsTrue__thenRetrievedValueIsNotStored() throws Exception {
        Map<String, String> backup = Collections.synchronizedMap(new HashMap<>());
        Map<String, Optional<String>> deleguate = Collections.synchronizedMap(new HashMap<>());

        CacheWithStore<String, String> cache = new CacheWithStore<>(
                key -> backup.get(key),
                new MapCacheStore<String, String>(deleguate),
                (key, value) -> Invalidation.invalid(),
                true
        );

        backup.put("k", "v");

        assertThat(cache.get("k"), is("v"));
        assertThat(deleguate.get("k"), is(nullValue()));
    }

    @Test
    void whenReadonlyNotSet__thenRetrievedValueIsStored() throws Exception {
        Map<String, String> backup = Collections.synchronizedMap(new HashMap<>());
        Map<String, Optional<String>> deleguate = Collections.synchronizedMap(new HashMap<>());

        CacheWithStore<String, String> cache = new CacheWithStore<>(
                key -> backup.get(key),
                new MapCacheStore<String, String>(deleguate),
                (key, value) -> Invalidation.invalid()
        );

        backup.put("k", "v");

        assertThat(cache.get("k"), is("v"));
        assertThat(deleguate.get("k"), is(Optional.of("v")));
    }
}
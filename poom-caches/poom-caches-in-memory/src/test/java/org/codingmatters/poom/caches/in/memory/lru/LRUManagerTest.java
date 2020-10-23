package org.codingmatters.poom.caches.in.memory.lru;


import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LRUManagerTest {

    private final LRUManager<String> lruManager = new LRUManager<>(3);

    @Test
    public void givenManagerEmpty__whenGettingOverload__thenEmpty() throws Exception {
        assertThat(lruManager.overload(), is(empty()));
    }

    @Test
    public void givenManagerHasLessAccessedKeysThanCapacity__whenGettingOverload__thenEmpty() throws Exception {
        this.lruManager.accessed("yop");
        this.lruManager.accessed("yip");

        assertThat(lruManager.overload(), is(empty()));
    }

    @Test
    public void givenManagerHasExactlyAccessedCapacityKeys__whenGettingOverload__thenEmpty() throws Exception {
        this.lruManager.accessed("yop");
        this.lruManager.accessed("yip");
        this.lruManager.accessed("yup");

        assertThat(lruManager.overload(), is(empty()));
    }

    @Test
    public void givenManagerHasAccessedMoreThanCapacityKeys__whenGettingOverload__thenContainsFirstAccessedKey() throws Exception {
        this.lruManager.accessed("yop");
        this.lruManager.accessed("yip");
        this.lruManager.accessed("yup");
        this.lruManager.accessed("yap");

        assertThat(lruManager.overload(), contains("yop"));
    }

    @Test
    public void givenManagerHasAccessedMoreThanCapacityKeys__whenGettingOverload_andSomeKeysWhereReAccessed__thenContainsLeastRecentAccessedKey() throws Exception {
        this.lruManager.accessed("yop");
        this.lruManager.accessed("yip");
        this.lruManager.accessed("yup");
        this.lruManager.accessed("yap");
        this.lruManager.accessed("yop");
        this.lruManager.accessed("yip");

        assertThat(lruManager.overload(), contains("yup"));
    }

    @Test
    public void givenManagerHasSomeKeys_andOneElementOverload__whenKeyRemoved__thenOverloadIsEmpty() throws Exception {
        this.lruManager.accessed("yop");
        this.lruManager.accessed("yip");
        this.lruManager.accessed("yup");
        this.lruManager.accessed("yap");

        this.lruManager.removed("yop");

        assertThat(lruManager.overload(), is(empty()));
    }
}
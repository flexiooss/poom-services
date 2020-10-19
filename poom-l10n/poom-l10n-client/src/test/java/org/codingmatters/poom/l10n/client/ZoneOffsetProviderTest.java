package org.codingmatters.poom.l10n.client;

import org.junit.Test;

import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

public class ZoneOffsetProviderTest {

    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final ExecutorService otherPool = Executors.newSingleThreadExecutor();

    @Test
    public void givenGettingOffsetFromAThread__whenNoOffsetSetted__thenDefaultOffsetIsParisTimezones() throws Exception {
        AtomicReference<ZoneOffset> actual = new AtomicReference<>();

        pool.submit(() -> {
            actual.set(ZoneOffsetProvider.at());
        });
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(actual.get().getId(), is(oneOf("+01:00", "+02:00")));
    }

    @Test
    public void givenGettingOffsetFromAThread__whenOffsetSettedInOtherThread__thenDefaultOffsetIsReturned() throws Exception {
        AtomicReference<ZoneOffset> actual = new AtomicReference<>();

        otherPool.submit(() -> ZoneOffsetProvider.set(ZoneOffset.of("+12:42")));
        otherPool.shutdown();
        otherPool.awaitTermination(1, TimeUnit.MINUTES);

        pool.submit(() -> {
            actual.set(ZoneOffsetProvider.at());
        });
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(actual.get().getId(), is(oneOf("+01:00", "+02:00")));
    }

    @Test
    public void givenGettingOffsetFromAThread__whenOffsetSettedInSameThread__thenSettedOffsetIsReturned() throws Exception {
        AtomicReference<ZoneOffset> actual = new AtomicReference<>();

        pool.submit(() -> ZoneOffsetProvider.set(ZoneOffset.of("+12:42")));

        pool.submit(() -> {
            actual.set(ZoneOffsetProvider.at());
        });
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(actual.get().getId(), is("+12:42"));
    }

    @Test
    public void givenGettingOffsetFromAThread__whenOffsetSettedInParentThread__thenSettedOffsetIsReturned() throws Exception {
        AtomicReference<ZoneOffset> actual = new AtomicReference<>();

        ZoneOffsetProvider.set(ZoneOffset.of("+12:42"));

        pool.submit(() -> {
            actual.set(ZoneOffsetProvider.at());
        });
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(actual.get().getId(), is("+12:42"));
    }
}
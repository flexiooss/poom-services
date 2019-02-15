package org.codingmatters.poom.services.tests;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Eventually {

    public static Eventually defaults() {
        return timeout(2 * 1000L);
    }

    public static Eventually timeout(long t) {
        return timeout(t, TimeUnit.MILLISECONDS);
    }

    public static Eventually timeout(long t, TimeUnit tu) {
        return new Eventually(tu.toMillis(t));
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private final long timeout;

    public Eventually(long withTimmeout) {
        this.timeout = withTimmeout;
    }

    public <T> void assertThat(CheckedSupplier<T> actual, Matcher<? super T> matcher) {
        assertThat("", actual, matcher);
    }

    public <T> void assertThat(String reason, CheckedSupplier<T> actual, Matcher<? super T> matcher) {
        long start = System.currentTimeMillis();
        AssertionError last = null;
        do {
            T actualValue = this.getActualValue(actual);
            try {
                MatcherAssert.assertThat(reason, actualValue, matcher);
                return;
            } catch (AssertionError ae) {
                last = ae;
            }
            this.sleepAWhile();
        } while (System.currentTimeMillis() - start < this.timeout);

        throw last;
    }

    private void sleepAWhile() {
        try {
            Thread.sleep(this.timeout / 10);
        } catch (InterruptedException e) {
            throw new AssertionError("failed while eventually asserting", e);
        }
    }

    private <T> T getActualValue(CheckedSupplier<T> actual) {
        AssertionError last;
        T actualValue = null;
        try {
            actualValue = actual.get();
        } catch (Exception e) {
            last = new AssertionError("failed invoking actual value supplier", e);
        }
        return actualValue;
    }

}

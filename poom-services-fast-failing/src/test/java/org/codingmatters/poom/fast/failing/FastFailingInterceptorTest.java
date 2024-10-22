package org.codingmatters.poom.fast.failing;

import org.codingmatters.poom.fast.failing.exceptions.FailFastException;
import org.codingmatters.poom.fast.failing.exceptions.RecoverableFFException;
import org.codingmatters.poom.fast.failing.exceptions.UnrecoverableFFException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

class FastFailingInterceptorTest {

    private final List<Failure> failures = new LinkedList<>();

    private FastFailer failer = new FastFailer() {
        @Override
        public void failAndStop(UnrecoverableFFException e) {
            failures.add(new Failure(e, FailureConsequence.STOP));
        }

        @Override
        public void failAndContinue(RecoverableFFException e) {
            failures.add(new Failure(e, FailureConsequence.CONTINUE));
        }
    };
    private FastFailingInterceptor interceptor = new FastFailingInterceptor(this.failer);

    @Test
    void whenNotAFailFastException__thenExceptionRethrown_andNotIntercepted() throws Exception {
        IOException rethrown = assertThrows(
                IOException.class,
                () -> {
                    try {
                        throw new IOException("not a fail fast exception");
                    } catch (Throwable e) {
                        this.interceptor.failFast(e);
                    }
                }
        );

        assertThat(rethrown.getMessage(), is("not a fail fast exception"));
        assertThat(this.failures, is(empty()));
    }

    @Test
    void givenFailFastException__whenUnrecoverable__thenIntercepted_andStopped() throws Throwable {
        UnrecoverableFFException failFastException = new UnrecoverableFFException("unrecoverable") {};
        try {
            throw failFastException;
        } catch (Throwable e) {
            this.interceptor.failFast(e);
        }

        assertThat(this.failures, hasSize(1));
        assertThat(this.failures.get(0).exception, is(failFastException));
        assertThat(this.failures.get(0).consequence, is(FailureConsequence.STOP));
    }

    @Test
    void givenFailFastException__whenRecoverable__thenIntercepted_andStopped() throws Throwable {
        RecoverableFFException failFastException = new RecoverableFFException("recoverable") {};
        try {
            throw failFastException;
        } catch (Throwable e) {
            this.interceptor.failFast(e);
        }

        assertThat(this.failures, hasSize(1));
        assertThat(this.failures.get(0).exception, is(failFastException));
        assertThat(this.failures.get(0).consequence, is(FailureConsequence.CONTINUE));
    }

    enum FailureConsequence {
        STOP, CONTINUE
    }
    class Failure {
        public final FailFastException exception;
        public final FailureConsequence consequence;

        Failure(FailFastException exception, FailureConsequence consequence) {
            this.exception = exception;
            this.consequence = consequence;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Failure failure = (Failure) o;
            return Objects.equals(exception, failure.exception) && consequence == failure.consequence;
        }

        @Override
        public int hashCode() {
            return Objects.hash(exception, consequence);
        }
    }
}
package org.codingmatters.poom.services.tests.mock.support.internals;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ExpectedReturnValue {
    private final Method method;
    private final Optional<Object[]> checkedArguments;
    private final Object value;
    private final Supplier<Throwable> thrown;


    public ExpectedReturnValue(Method method, Object[] objects, Object value, Supplier<Throwable> thrown) {
        this.method = method;
        this.value = value;
        this.checkedArguments = Optional.ofNullable(objects);
        this.thrown = thrown;
    }

    public Optional<Object[]> checkedArguments() {
        return checkedArguments;
    }

    public Object value() {
        return value;
    }

    public Supplier<Throwable> thrown() {
        return thrown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedReturnValue that = (ExpectedReturnValue) o;
        return Objects.equals(method, that.method) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, value);
    }

    @Override
    public String toString() {
        return "ExpectedReturnValue{" +
                "method=" + method +
                ", value=" + value +
                '}';
    }
}

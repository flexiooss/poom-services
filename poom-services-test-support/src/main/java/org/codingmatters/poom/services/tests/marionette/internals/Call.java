package org.codingmatters.poom.services.tests.marionette.internals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class Call {
    private final Method method;
    private final Object [] arguments;

    public Call(Method method, Object ... arguments) {
        this.method = method;
        this.arguments = arguments != null ? arguments : new Object[0];
    }

    public Object[] arguments() {
        return arguments;
    }

    public Method method() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Call that = (Call) o;
        return Objects.equals(method, that.method) && Objects.deepEquals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, Arrays.hashCode(arguments));
    }

    @Override
    public String toString() {
        return "Call{" +
                method.getDeclaringClass().getSimpleName() +
                "#" + method.getName() +
                "(" + Arrays.toString(arguments) + ")" +
                '}';
    }
}

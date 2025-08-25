package org.codingmatters.poom.services.tests.marionette.internals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.LinkedList;

public class CallCountAssertions<I> implements InvocationHandler {
    private final Class<I> clazz;
    private final LinkedList<Call> calls;
    private final int expected;

    public CallCountAssertions(Class<I> clazz, LinkedList<Call> calls, int expected) {
        this.clazz = clazz;
        this.calls = calls;
        this.expected = expected;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if (method.getName().equals("toString")) return this.toString();

        Call callTemplate = new Call(method, objects);
        int actual = this.calls.stream().filter(call -> call.method().equals(callTemplate.method())).toList().size();
        if (expected != actual) {
            throw new AssertionError(String.format(
                    "expected %s calls of %s but was %s",
                    expected, callTemplate, actual
            ));
        }
        return null;
    }

    @Override
    public String toString() {
        return "CallCountAssertions{" +
                "calls=" + calls +
                ", clazz=" + clazz +
                ", expected=" + expected +
                '}';
    }
}

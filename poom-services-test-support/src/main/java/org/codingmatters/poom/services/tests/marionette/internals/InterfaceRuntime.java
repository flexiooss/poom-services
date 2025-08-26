package org.codingmatters.poom.services.tests.marionette.internals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InterfaceRuntime implements InvocationHandler {

    private final List<Call> calls;
    private final Map<Method, List<ExpectedReturnValue>> nextResults;
    private final Map<Method, List<ExpectedReturnValue>> defaultResults;

    public InterfaceRuntime(List<Call> calls, Map<Method, List<ExpectedReturnValue>> nextResults, Map<Method, List<ExpectedReturnValue>> defaultResults) {
        this.calls = calls;
        this.nextResults = nextResults;
        this.defaultResults = defaultResults;
    }

    @Override
    public synchronized Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if (method.getName().equals("toString")) return this.toString();
        if (method.getName().equals("equals")) return this.equals(o);

        Call call = new Call(method, objects);
        this.calls.add(call);
        return this.resolveResult(call);
    }

    private Object resolveResult(Call call) throws Throwable {
        ExpectedReturnValue result;
        List<ExpectedReturnValue> methodResults = this.nextResults.getOrDefault(call.method(), Collections.emptyList());
        if (methodResults.isEmpty()) {
            List<ExpectedReturnValue> methodDefaultResults = this.defaultResults.getOrDefault(call.method(), Collections.emptyList());
            if (methodDefaultResults.isEmpty()) {
                throw new AssertionError("unexpected call " + call);
            } else {
                result = methodDefaultResults.getLast();
            }
        } else {
            result = methodResults.removeFirst();
        }
        if (result.checkedArguments().isPresent()) {
            if (!Arrays.equals(call.arguments(), result.checkedArguments().get())) {
                throw new AssertionError(String.format(
                        "argument expected : %s but was : %s",
                        Arrays.toString(result.checkedArguments().get()),
                        Arrays.toString(call.arguments())
                ));
            }
        }
        if (result.thrown() != null) {
            throw result.thrown().get();
        }
        return result.value();
    }

    @Override
    public String toString() {
        return "InterfaceRuntime{" +
                "nextResults=" + nextResults +
                ", defaultResults=" + defaultResults +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}

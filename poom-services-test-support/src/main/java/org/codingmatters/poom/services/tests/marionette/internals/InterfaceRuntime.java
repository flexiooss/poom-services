package org.codingmatters.poom.services.tests.marionette.internals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

public class InterfaceRuntime implements InvocationHandler {

    private final List<Call> calls;
    private final Map<Method, List<ExpectedReturnValue>> nextResults;

    public InterfaceRuntime(List<Call> calls, Map<Method, List<ExpectedReturnValue>> nextResults) {
        this.calls = calls;
        this.nextResults = nextResults;
    }

    @Override
    public synchronized Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        Call call = new Call(method, objects);
        this.calls.add(call);
        return this.resolveResult(call);
    }

    private Object resolveResult(Call call) throws Throwable {
        List<ExpectedReturnValue> methodResults = this.nextResults.getOrDefault(call.method(), Collections.emptyList());
        if(methodResults.isEmpty()) {
            throw new AssertionError("unexpected call " + call);
        }
        ExpectedReturnValue result = methodResults.removeFirst();
        if(result.checkedArguments().isPresent()) {
            if(! Arrays.equals(call.arguments(), result.checkedArguments().get())) {
                throw new AssertionError(String.format(
                        "argument expected : %s but was : %s",
                        Arrays.toString(result.checkedArguments().get()),
                        Arrays.toString(call.arguments())
                ));
            }
        }
        if(result.thrown() != null) {
            throw result.thrown().get();
        }
        return result.value();
    }
}

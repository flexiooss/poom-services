package org.codingmatters.poom.services.tests.marionette.internals;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Function;

public class InterfaceAssertions<I> implements InvocationHandler {

    static public <I> InterfaceAssertions<I> onLastCall(Class<I> clazz, List<Call> callList) {
        return new InterfaceAssertions<>(clazz, callList, calls -> calls.size() > 0 ? calls.getLast() : null);
    }

    static public <I> InterfaceAssertions<I> onNthCall(Class<I> clazz, List<Call> callList, int index) {
        return new InterfaceAssertions<>(clazz, callList, calls -> calls.size() >= index + 1 ? calls.get(index) : null);
    }

    private final Class<I> clazz;
    private final List<Call> calls;
    private final Function<List<Call>, Call> actualCallProvider;

    private InterfaceAssertions(Class<I> clazz, List<Call> calls, Function<List<Call>, Call> actualCallProvider) {
        this.clazz = clazz;
        this.calls = calls;
        this.actualCallProvider = actualCallProvider;
    }

    public I was() {
        return (I) Proxy.newProxyInstance(this.clazz.getClassLoader(), new Class[]{this.clazz}, this);
    }

    public Call getFor(String methodName, Class<?> ... argTypes) {
        List<Call> methodCalls = this.calls.stream().filter(call -> {
            if(call.method().getName().equals(methodName)) {
                if(argTypes != null) {
                    for (int i = 0; i < argTypes.length; i++) {
                        Class<?> parameterType = call.method().getParameterTypes()[i];
                        if(! parameterType.isAssignableFrom(argTypes[i])) {
                            return false;
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        }).toList();
        Call call = this.actualCallProvider.apply(methodCalls);
        return call;
    }


    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if(method.getName().equals("toString")) return o.toString();

        Call expected = new Call(method, objects);

        List<Call> methodCalls = this.calls.stream().filter(call -> call.method().equals(method)).toList();
        Call call = this.actualCallProvider.apply(methodCalls);

        MatcherAssert.assertThat(call, Matchers.is(expected));
        return null;
    }
}

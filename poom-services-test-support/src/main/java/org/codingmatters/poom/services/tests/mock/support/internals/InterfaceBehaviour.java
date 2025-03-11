package org.codingmatters.poom.services.tests.mock.support.internals;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InterfaceBehaviour<I> {

    private final Object value;
    private final Class<I> clazz;
    private final Map<Method, List<ExpectedReturnValue>> nextResults;
    private final Supplier<Throwable> thrown;

    public InterfaceBehaviour(Object value, Class<I> clazz, Map<Method, List<ExpectedReturnValue>> nextResults) {
        this(value, null, clazz, nextResults);
    }

    public InterfaceBehaviour(Class<I> clazz, Map<Method, List<ExpectedReturnValue>> nextResults, Supplier<Throwable> thrown) {
        this(null, thrown, clazz, nextResults);
    }

    private InterfaceBehaviour(Object value, Supplier<Throwable> thrown, Class<I> clazz, Map<Method, List<ExpectedReturnValue>> nextResults) {
        this.value = value;
        this.clazz = clazz;
        this.nextResults = nextResults;
        this.thrown = thrown;
    }


    public I whenAnyArgs() {
        return (I) Proxy.newProxyInstance(this.clazz.getClassLoader(), new Class[]{this.clazz}, new InterfaceBehaviourHandler<>(this.value, this.thrown, this.clazz, this.nextResults, false));
    }

    public I whenCheckedArgs() {
        return (I) Proxy.newProxyInstance(this.clazz.getClassLoader(), new Class[]{this.clazz}, new InterfaceBehaviourHandler<>(this.value, this.thrown, this.clazz, this.nextResults, true));
    }
}

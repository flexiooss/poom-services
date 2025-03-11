package org.codingmatters.poom.services.tests.mock.support.internals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InterfaceAssertions<I> implements InvocationHandler {
    private final Call actual;
    private final Class<I> clazz;

    public InterfaceAssertions(Class<I> clazz, Call actual) {
        this.clazz = clazz;
        this.actual = actual;
    }

    public I was() {
        return (I) Proxy.newProxyInstance(this.clazz.getClassLoader(), new Class[]{this.clazz}, this);
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if(! method.getDeclaringClass().equals(this.clazz)) return method.invoke(o, objects);
        Call expected = new Call(method, objects);
        if(this.actual == null || ! this.actual.equals(expected)) {
            throw new AssertionError("expected : " + expected + " but was : " + this.actual);
        }
        return null;
    }
}

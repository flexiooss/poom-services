package org.codingmatters.poom.services.tests.marionette.internals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Optional;

public class NeverCalledAssertions<I> implements InvocationHandler {
    private final Class<I> clazz;
    private final LinkedList<Call> calls;

    public NeverCalledAssertions(Class<I> clazz, LinkedList<Call> calls) {
        super();

        this.clazz = clazz;
        this.calls = calls;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if(! method.getDeclaringClass().equals(this.clazz)) return method.invoke(o, objects);
        Call notExcepted = new Call(method, objects);
        Optional<Call> called = this.calls.stream().filter(call -> call.method().equals(notExcepted.method())).findAny();
        if(called.isPresent())  {
            throw new AssertionError("was not expecting to be called : " + called.get());
        }
        return null;
    }
}

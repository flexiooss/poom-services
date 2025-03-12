package org.codingmatters.poom.services.tests.marionette.internals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InterfaceBehaviourHandler<I> implements InvocationHandler {

    private final Object value;
    private final Supplier<Throwable> thrown;
    private final Class<I> clazz;
    private final Map<Method, List<ExpectedReturnValue>> nextResults;
    private final boolean checked;

    public InterfaceBehaviourHandler(Object value, Supplier<Throwable> thrown, Class<I> clazz, Map<Method, List<ExpectedReturnValue>> nextResults, boolean checked) {
        this.value = value;
        this.thrown = thrown;
        this.clazz = clazz;
        this.nextResults = nextResults;
        this.checked = checked;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if(! method.getDeclaringClass().equals(this.clazz)) return method.invoke(o, objects);
        if(! this.nextResults.containsKey(method)) {
            this.nextResults.put(method, Collections.synchronizedList(new LinkedList<>()));
        }
        this.nextResults.get(method).add(new ExpectedReturnValue(method, this.checked ? objects : null, this.value, this.thrown));
        return null;
    }
}

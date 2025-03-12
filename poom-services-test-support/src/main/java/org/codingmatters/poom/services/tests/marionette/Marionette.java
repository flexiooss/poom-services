package org.codingmatters.poom.services.tests.marionette;

import org.codingmatters.poom.services.tests.marionette.internals.*;
import org.codingmatters.poom.services.tests.mock.support.internals.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Supplier;

public class Marionette<I> {

    static public <I> Marionette<I> of(Class<I> clazz) {
        return new Marionette<>(clazz);
    }

    private final Class<I> clazz;
    private final List<Call> calls = new LinkedList<>();
    private final Map<Method, List<ExpectedReturnValue>> nextResults = Collections.synchronizedMap(new HashMap<>());

    private final InterfaceRuntime component = new InterfaceRuntime(this.calls, this.nextResults);

    public Marionette(Class<I> clazz) {
        this.clazz = clazz;
    }

    public I component() {
        return (I) Proxy.newProxyInstance(this.clazz.getClassLoader(), new Class[]{this.clazz}, this.component);
    }

    public InterfaceAssertions<I> assertLastCall() {
        Call call = this.calls.isEmpty() ? null : this.calls.getLast();
        return new InterfaceAssertions<>(this.clazz, call);
    }

    public InterfaceAssertions<I> assertNthCall(int index) {
        Call call = this.calls.size() >= index + 1 ? this.calls.get(index) : null;
        return new InterfaceAssertions<>(this.clazz, call);
    }

    public InterfaceBehaviour<I> nextCallReturns(Object value) {
        return new InterfaceBehaviour<>(value, this.clazz, this.nextResults);
    }

    public InterfaceBehaviour<I> nextCallThrows(Supplier<Throwable> thrown) {
        return new InterfaceBehaviour<>(this.clazz, this.nextResults, thrown);
    }


}

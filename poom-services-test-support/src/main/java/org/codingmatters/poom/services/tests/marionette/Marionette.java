package org.codingmatters.poom.services.tests.marionette;

import org.codingmatters.poom.services.tests.marionette.internals.*;

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
    private final Map<Method, List<ExpectedReturnValue>> defaultResults = Collections.synchronizedMap(new HashMap<>());

    private final InterfaceRuntime component = new InterfaceRuntime(this.calls, this.nextResults, this.defaultResults);

    public Marionette(Class<I> clazz) {
        this.clazz = clazz;
    }

    public I component() {
        return (I) Proxy.newProxyInstance(this.clazz.getClassLoader(), new Class[]{this.clazz}, this.component);
    }

    public InterfaceAssertions<I> assertLastCall() {
        return InterfaceAssertions.onLastCall(this.clazz, new LinkedList<>(this.calls));
    }

    public InterfaceAssertions<I> assertNthCall(int index) {
        return InterfaceAssertions.onNthCall(this.clazz, this.calls, index);
    }

    public InterfaceBehaviour<I> nextCallReturns(Object value) {
        return new InterfaceBehaviour<>(value, this.clazz, this.nextResults);
    }

    public InterfaceBehaviour<I> defaultCallReturns(Object value) {
        return new InterfaceBehaviour<>(value, this.clazz, this.defaultResults);
    }

    public InterfaceBehaviour<I> nextCallThrows(Supplier<Throwable> thrown) {
        return new InterfaceBehaviour<>(this.clazz, this.nextResults, thrown);
    }


}

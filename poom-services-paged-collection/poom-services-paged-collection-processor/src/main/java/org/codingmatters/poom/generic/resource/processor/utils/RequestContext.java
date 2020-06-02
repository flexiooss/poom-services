package org.codingmatters.poom.generic.resource.processor.utils;

import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.util.concurrent.atomic.AtomicReference;

public class RequestContext {
    private final ThreadLocal<AtomicReference<RequestDelegate>> requestDelegateHolder;
    private final ThreadLocal<AtomicReference<ResponseDelegate>> responseDelegateHolder;

    public RequestContext() {
        this.requestDelegateHolder = new ThreadLocal<>() {
            @Override
            protected AtomicReference<RequestDelegate> initialValue() {
                return new AtomicReference<>();
            }
        };
        this.responseDelegateHolder = new ThreadLocal<>() {
            @Override
            protected AtomicReference<ResponseDelegate> initialValue() {
                return new AtomicReference<>();
            }
        };
    }

    public void setup(RequestDelegate requestDelegate, ResponseDelegate responseDelegate) {
        this.requestDelegateHolder.get().set(requestDelegate);
        this.responseDelegateHolder.get().set(responseDelegate);
    }

    public RequestDelegate requestDelegate() {
        return this.requestDelegateHolder.get().get();
    }

    public ResponseDelegate responseDelegate() {
        return this.responseDelegateHolder.get().get();
    }
}

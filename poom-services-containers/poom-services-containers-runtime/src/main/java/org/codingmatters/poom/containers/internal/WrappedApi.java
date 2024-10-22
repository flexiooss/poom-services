package org.codingmatters.poom.containers.internal;

import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;

public class WrappedApi implements Api {
    private final Api wrapped;
    private final Processor processor;

    public WrappedApi(Api wrapped, Processor processor) {
        this.wrapped = wrapped;
        this.processor = processor;
    }

    @Override
    public String name() {
        return this.wrapped.name();
    }

    @Override
    public String version() {
        return this.wrapped.version();
    }

    @Override
    public Processor processor() {
        return this.processor;
    }

    @Override
    public String docResource() {
        return this.wrapped.docResource();
    }

    @Override
    public String path() {
        return this.wrapped.path();
    }
}

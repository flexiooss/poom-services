package org.codingmatters.poom.containers;

import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class ApiContainerRuntimeBuilder {

    private final List<Api> apis = new LinkedList<>();
    private Function<Processor, Processor> apiProcessorWrapper;

    public ApiContainerRuntimeBuilder withApi(Api api) {
        this.apis.add(api);
        return this;
    }

    public ApiContainerRuntimeBuilder apiProcessorWrapper(Function<Processor, Processor> wrapper) {
        this.apiProcessorWrapper = wrapper;
        return this;
    }

    public ApiContainerRuntime build(ApiContainerRuntime runtime) {
        List<Api> wrappedApis = new LinkedList<>();
        for (Api api : this.apis) {
            wrappedApis.add(new WrappedApi(api, this.apiProcessorWrapper.apply(api.processor())));
        }
        return runtime.apis(wrappedApis.toArray(new Api[0]));
    }

    private class WrappedApi implements Api {
        private final Api wrapped;
        private final Processor processor;


        WrappedApi(Api wrapped, Processor processor) {
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

}

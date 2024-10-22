package org.codingmatters.poom.containers.internal;


import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.fast.failing.FastFailingInterceptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.IOException;

public class FastFailingProcessor implements Processor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(FastFailingProcessor.class);

    private final Processor delegate;
    private final ExternallyStoppable runtime;
    private final JsonFactory jsonFactory;

    public FastFailingProcessor(Processor delegate, ExternallyStoppable runtime, JsonFactory jsonFactory) {
        this.delegate = delegate;
        this.runtime = runtime;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public void process(RequestDelegate requestDelegate, ResponseDelegate responseDelegate) throws IOException {
        try {
            this.delegate.process(requestDelegate, responseDelegate);
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            try {
                FastFailingInterceptor interceptor = ApiContainerFastFailer.fastFailingInterceptor(this.runtime, responseDelegate, this.jsonFactory);
                interceptor.failFast(t);
            } catch (Throwable e) {
                log.error("uncatched throwable in processor : processor should catch or throw a fail fast exception", e);
            }
        }
    }
}

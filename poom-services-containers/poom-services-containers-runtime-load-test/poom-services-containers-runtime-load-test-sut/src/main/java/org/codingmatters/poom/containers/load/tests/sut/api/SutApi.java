package org.codingmatters.poom.containers.load.tests.sut.api;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.IOException;
import java.io.InputStream;

public class SutApi implements Api {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(SutApi.class);

    private final Processor processor;

    public SutApi() {
        processor = this::process;
    }

    private void process(RequestDelegate request, ResponseDelegate response) {
        if(request.method().equals(RequestDelegate.Method.POST)) {
            try(InputStream in = request.payload()) {
                byte [] buffer = new byte[1024];
                for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer));
            } catch (IOException e) {
                log.error("error reading body", e);
            }
        }
        response.status(200);
        response.contenType("plain/text");
        response.payload("ok", "utf-8");
    }

    @Override
    public String name() {
        return "sut";
    }

    @Override
    public String version() {
        return Api.versionFrom(SutApi.class);
    }

    @Override
    public Processor processor() {
        return this.processor;
    }
}

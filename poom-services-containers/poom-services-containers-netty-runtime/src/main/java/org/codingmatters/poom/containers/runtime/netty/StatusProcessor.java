package org.codingmatters.poom.containers.runtime.netty;

import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class StatusProcessor implements Processor {
    private final AtomicReference<STATUS> status;

    public StatusProcessor() {
        status = new AtomicReference<>(STATUS.STARTING);
    }

    public static enum STATUS {
        UP,
        STARTING,
        STOPPING
    }

    @Override
    public void process(RequestDelegate requestDelegate, ResponseDelegate responseDelegate) throws IOException {
        responseDelegate.status(200);
        responseDelegate.contenType("text/plain");
        responseDelegate.payload(status.get().name(), StandardCharsets.UTF_8.name());
    }

    public void start(){
        status.set(STATUS.UP);
    }
}

package org.codingmatters.poom.containers.runtime.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.util.concurrent.atomic.AtomicReference;

public class StatusHandler implements HttpHandler {

    private final AtomicReference<STATUS> status;

    public StatusHandler() {
        status = new AtomicReference<>(STATUS.STARTING);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.setStatusCode(StatusCodes.OK);
        exchange.getResponseSender().send(status.get().name());
    }

    public void started() {
        this.status.set(STATUS.UP);
    }

    public static enum STATUS {
        UP,
        STARTING,
        STOPPING
    }

}

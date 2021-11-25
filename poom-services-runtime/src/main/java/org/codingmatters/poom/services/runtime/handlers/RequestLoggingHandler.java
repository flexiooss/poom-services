package org.codingmatters.poom.services.runtime.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class RequestLoggingHandler implements HttpHandler {
    private final HttpHandler delegate;

    public RequestLoggingHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            this.delegate.handleRequest(exchange);
            this.safelyLogSuccess(exchange);
        } catch (Exception e) {
            this.safelyLogError(exchange, e);
            throw e;
        }
    }

    private void safelyLogSuccess(HttpServerExchange exchange) {

    }

    private void safelyLogError(HttpServerExchange exchange, Exception e) {

    }
}

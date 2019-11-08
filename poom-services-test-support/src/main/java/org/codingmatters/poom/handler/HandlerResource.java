package org.codingmatters.poom.handler;

import org.junit.rules.ExternalResource;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class HandlerResource <Req, Resp> extends ExternalResource implements Function<Req, Resp> {

    private final AtomicReference<Req> lastRequest = new AtomicReference<>();
    private final AtomicReference<Function<Req,Resp>> nextResponse = new AtomicReference<>();

    private Resp handle(Req request) {
        this.lastRequest.set(request);
        if(this.nextResponse.get() != null) {
            return this.nextResponse.get().apply(request);
        } else {
            return this.defaultResponse(request);
        }
    }

    protected abstract Resp defaultResponse(Req request);

    @Override
    protected void before() throws Throwable {
        super.before();
        this.nextResponse.set(null);
        this.lastRequest.set(null);
    }

    @Override
    protected void after() {
        this.nextResponse.set(null);
        this.lastRequest.set(null);
        super.after();
    }

    public Req lastRequest() {
        return lastRequest.get();
    }

    public HandlerResource <Req, Resp> nextResponse(Function<Req,Resp> responder) {
        nextResponse.set(responder);
        return this;
    }

    public HandlerResource <Req, Resp> reset() {
        this.lastRequest.set(null);
        return this;
    }

    @Override
    public Resp apply(Req req) {
        return this.handle(req);
    }
}

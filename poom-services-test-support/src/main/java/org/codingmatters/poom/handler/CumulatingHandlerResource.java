package org.codingmatters.poom.handler;

import org.junit.rules.ExternalResource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class CumulatingHandlerResource<Req, Resp> extends ExternalResource implements Function<Req, Resp> {

    private final List<Req> requests = Collections.synchronizedList(new LinkedList<>());
    private final List<Function<Req, Resp>> nextResponses = Collections.synchronizedList(new LinkedList<>());
    private final AtomicInteger currentResponseIndex = new AtomicInteger(0);

    protected abstract Resp defaultResponse(Req request);

    @Override
    public synchronized Resp apply(Req req) {
        this.requests.add(req);
        if(this.nextResponses.size() > this.currentResponseIndex.get()) {
            return this.nextResponses.get(this.currentResponseIndex.getAndIncrement()).apply(req);
        } else if(! this.nextResponses.isEmpty()) {
            return this.nextResponses.get(this.nextResponses.size() - 1).apply(req);
        } else {
            return this.defaultResponse(req);
        }
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        this.requests.clear();
        this.nextResponses.clear();
    }

    @Override
    protected void after() {
        this.requests.clear();
        this.nextResponses.clear();
        super.after();
    }

    public synchronized CumulatingHandlerResource<Req, Resp> nextResponse(Function<Req,Resp> responder) {
        this.nextResponses.add(responder);
        return this;
    }

    public synchronized CumulatingHandlerResource<Req, Resp> reset() {
        this.requests.clear();
        return this;
    }

    public synchronized List<Req> requests() {
        return new LinkedList<>(this.requests);
    }

    public synchronized Req lastRequest() {
        if(this.requests.isEmpty()) return null;
        return this.requests.get(this.requests.size() - 1);
    }
}

package org.codingmatters.poom.handler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class CumulatingTestHandler<Req, Resp> implements Function<Req, Resp> {

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

    public void initialize() {
        this.requests.clear();
        this.nextResponses.clear();
        this.currentResponseIndex.set(0);
    }

    public void cleanup() {
        this.requests.clear();
        this.nextResponses.clear();
        this.currentResponseIndex.set(0);
    }

    public synchronized CumulatingTestHandler<Req, Resp> nextResponse(Function<Req,Resp> responder) {
        this.nextResponses.add(responder);
        return this;
    }

    public synchronized CumulatingTestHandler<Req, Resp> nthResponse(int index, Function<Req,Resp> responder) {
        this.nextResponses.set(index, responder);
        return this;
    }

    public synchronized CumulatingTestHandler<Req, Resp> reset() {
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

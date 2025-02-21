package org.codingmatters.poom.handler;

import org.junit.rules.ExternalResource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class CumulatingHandlerResource<Req, Resp> extends ExternalResource implements Function<Req, Resp> {

    private final CumulatingTestHandler<Req, Resp> deleguate = new CumulatingTestHandler<>() {
        @Override
        protected Resp defaultResponse(Req request) {
            return CumulatingHandlerResource.this.defaultResponse(request);
        }
    };

    private final List<Req> requests = Collections.synchronizedList(new LinkedList<>());
    private final List<Function<Req, Resp>> nextResponses = Collections.synchronizedList(new LinkedList<>());
    private final AtomicInteger currentResponseIndex = new AtomicInteger(0);

    protected abstract Resp defaultResponse(Req request);

    @Override
    public synchronized Resp apply(Req req) {
        return this.deleguate.apply(req);
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        this.deleguate.initialize();
    }

    @Override
    protected void after() {
        this.deleguate.cleanup();
        super.after();
    }

    public synchronized CumulatingHandlerResource<Req, Resp> nextResponse(Function<Req,Resp> responder) {
        this.deleguate.nextResponse(responder);
        return this;
    }

    public synchronized CumulatingHandlerResource<Req, Resp> reset() {
        this.deleguate.reset();
        return this;
    }

    public synchronized List<Req> requests() {
        return this.deleguate.requests();
    }

    public synchronized Req lastRequest() {
        return this.deleguate.lastRequest();
    }
}

package org.codingmatters.poom.mcp.processor;

import org.codingmatters.rest.api.SseChannel;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.codingmatters.rest.tests.api.TestSseChannel;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link TestResponseDeleguate} dont le canal SSE compte les commentaires (le {@link TestSseChannel}
 * de cdm-rest les ignore), et peut simuler un client qui a fermé le flux : toute écriture lève alors
 * une {@link IOException}, comme sur Undertow.
 */
class RecordingResponse extends TestResponseDeleguate {

    final AtomicInteger comments = new AtomicInteger();
    final AtomicBoolean clientGone = new AtomicBoolean(false);
    final AtomicInteger writesAfterGone = new AtomicInteger();
    private RecordingChannel channel;

    @Override
    public SseChannel openSse() {
        this.channel = new RecordingChannel();
        return this.channel;
    }

    @Override
    public TestSseChannel sseChannel() {
        return this.channel;
    }

    class RecordingChannel extends TestSseChannel {
        @Override
        public SseChannel comment(String comment) {
            if (RecordingResponse.this.clientGone.get()) {
                RecordingResponse.this.writesAfterGone.incrementAndGet();
                throw new UncheckedGone();
            }
            RecordingResponse.this.comments.incrementAndGet();
            return this;
        }

        @Override
        public SseChannel send(String event, String data) {
            if (RecordingResponse.this.clientGone.get()) {
                RecordingResponse.this.writesAfterGone.incrementAndGet();
                throw new UncheckedGone();
            }
            return super.send(event, data);
        }
    }

    /** {@link TestSseChannel} ne déclare pas IOException : on la porte dans une exception non vérifiée, que le répondeur doit traiter comme une IOException. */
    static class UncheckedGone extends java.io.UncheckedIOException {
        UncheckedGone() { super(new IOException("client gone")); }
    }
}

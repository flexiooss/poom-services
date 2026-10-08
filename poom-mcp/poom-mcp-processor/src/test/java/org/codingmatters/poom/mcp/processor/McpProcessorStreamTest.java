package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.rest.tests.api.TestSseChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.codingmatters.poom.mcp.processor.McpTestRequests.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpProcessorStreamTest {

    // jsonWindow 50 ms, keepalive 30 ms, taskAfter 300 ms, streamMax 400 ms
    private static final McpTimings FAST = new McpTimings(Duration.ofMillis(50), Duration.ofMillis(30),
            Duration.ofMillis(300), Duration.ofMillis(400), Duration.ofMillis(10), Duration.ofHours(1), Duration.ofSeconds(2));

    private final ExecutorService pool = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        this.pool.shutdownNow();
    }

    private McpProcessor processorWith(String name, Function<CallToolParams, CallToolResult> handler) {
        return new McpProcessor(new JsonFactory(), McpServerDescriptor.builder().name("t").version("1")
                .tools(McpToolDescriptor.builder().name(name).handler(handler).build()).build(), this.pool, FAST);
    }

    private static CallToolResult text(String t) {
        return CallToolResult.builder().content(ToolContent.builder().type(ToolContent.Type.text).text(t).build()).isError(false).build();
    }

    private static Function<CallToolParams, CallToolResult> sleeping(long millis, String out) {
        return p -> {
            try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            return text(out);
        };
    }

    private RecordingResponse call(McpProcessor processor, String tool) throws Exception {
        RecordingResponse response = new RecordingResponse();
        processor.process(post("tools/call", tool, body("9", "tools/call", "{\"name\":\"" + tool + "\",\"arguments\":{}}", false)), response);
        return response;
    }

    @Test
    void givenFastTool__whenCall__thenJsonNotSse() throws Exception {
        RecordingResponse response = this.call(this.processorWith("fast", p -> text("ok")), "fast");

        assertThat(response.contentType(), containsString("application/json"));
        assertThat(response.sseChannel(), is(nullValue()));
        assertThat(new String(response.payload()), containsString("ok"));
    }

    @Test
    void givenToolSlowerThanJsonWindow__whenCall__thenSseWithKeepalivesThenOneMessage() throws Exception {
        RecordingResponse response = this.call(this.processorWith("mid", sleeping(150, "done")), "mid");

        assertThat(response.status(), is(200));
        assertThat(response.headers().get("X-Accel-Buffering")[0], is("no"));
        assertThat(response.comments.get(), greaterThanOrEqualTo(2));
        List<TestSseChannel.SseEvent> events = response.sseChannel().drainEvents();
        assertThat(events, hasSize(1));
        assertThat(events.get(0).event(), is("message"));
        assertThat(events.get(0).data(), allOf(containsString("\"id\":\"9\""), containsString("done")));
        assertThat(response.sseChannel().isOpen(), is(false));
    }

    @Test
    void givenToolLongerThanStreamMax__whenCall__thenNoResultErrorAndToolInterrupted() throws Exception {
        AtomicBoolean interrupted = new AtomicBoolean(false);
        RecordingResponse response = this.call(this.processorWith("long", p -> {
            try { Thread.sleep(5_000); } catch (InterruptedException e) { interrupted.set(true); }
            return text("late");
        }), "long");

        List<TestSseChannel.SseEvent> events = response.sseChannel().drainEvents();
        assertThat(events, hasSize(1));
        assertThat(events.get(0).data(), allOf(containsString("no_result"), containsString("\"isError\":true")));
        Thread.sleep(100);
        assertThat(interrupted.get(), is(true));
    }

    @Test
    void givenToolThrowsAfterJsonWindow__whenCall__thenStreamEndsWithInternalError() throws Exception {
        RecordingResponse response = this.call(this.processorWith("boom", p -> {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            throw new IllegalStateException("kaput");
        }), "boom");

        List<TestSseChannel.SseEvent> events = response.sseChannel().drainEvents();
        assertThat(events, hasSize(1));
        assertThat(events.get(0).data(), allOf(containsString("-32603"), containsString("kaput")));
    }

    @Test
    void givenClientGone__whenKeepaliveFails__thenToolInterruptedAndNothingMoreWritten() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);
        McpProcessor processor = this.processorWith("long", p -> {
            started.countDown();
            try { Thread.sleep(5_000); } catch (InterruptedException e) { interrupted.set(true); }
            return text("late");
        });
        RecordingResponse response = new RecordingResponse();
        Thread client = new Thread(() -> {
            try {
                processor.process(post("tools/call", "long", body("9", "tools/call", "{\"name\":\"long\",\"arguments\":{}}", false)), response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        client.start();
        started.await(1, TimeUnit.SECONDS);
        Thread.sleep(80); // le flux est ouvert
        response.clientGone.set(true);
        client.join(2_000);

        assertThat(client.isAlive(), is(false));
        assertThat(interrupted.get(), is(true));
        assertThat(response.writesAfterGone.get(), is(1)); // le keepalive qui a révélé le départ, et rien d'autre
    }
}

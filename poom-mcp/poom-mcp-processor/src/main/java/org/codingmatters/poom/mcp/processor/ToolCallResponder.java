package org.codingmatters.poom.mcp.processor;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.McpRequest;
import org.codingmatters.poom.mcp.types.McpResponse;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.api.SseChannel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Forme de la réponse d'un {@code tools/call} : JSON si l'outil rend avant {@code jsonWindow},
 * sinon un flux SSE tenu en vie par des commentaires planifiés, et clos par une réponse unique.
 * Le thread de la requête reste bloqué jusqu'à cette réponse : sur Undertow, l'échange se termine
 * quand {@code process} rend la main.
 */
final class ToolCallResponder {

    private static final CategorizedLogger log = CategorizedLogger.getLogger(ToolCallResponder.class);

    private final JsonRpcWriter writer;
    private final McpTimings timings;
    private final ScheduledExecutorService keepalives;

    ToolCallResponder(JsonRpcWriter writer, McpTimings timings, ScheduledExecutorService keepalives) {
        this.writer = writer;
        this.timings = timings;
        this.keepalives = keepalives;
    }

    void respond(ResponseDelegate response, McpRequest request, String toolName, ToolRun run, boolean clientTasks) throws IOException {
        try {
            CallToolResult result = run.result().get(this.timings.jsonWindow().toMillis(), TimeUnit.MILLISECONDS);
            McpResponse built;
            try {
                built = this.writer.result(request.id(), this.writer.callToolResult(result));
            } catch (RuntimeException e) {
                built = this.internalError(request.id(), e);
            }
            this.writer.json(response, built);
            return;
        } catch (TimeoutException e) {
            // bascule en SSE
        } catch (ExecutionException e) {
            this.writer.json(response, this.failure(request.id(), e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            run.stopWaiting();
            return;
        }

        response.status(200);
        response.addHeader("X-Accel-Buffering", "no");
        SseChannel channel = response.openSse();
        // Un seul moniteur pour les deux écritures (keepalive / message final) : jamais d'écriture après un échec.
        Object lock = new Object();
        boolean[] state = new boolean[2]; // 0 = gone, 1 = done (gardés par lock)
        long period = this.timings.keepalive().toMillis();
        ScheduledFuture<?> keepalive = this.keepalives.scheduleAtFixedRate(() -> {
            synchronized (lock) {
                if (state[0] || state[1]) return;
                try {
                    channel.comment("");
                } catch (IOException | UncheckedIOException e) {
                    state[0] = true;
                    run.stopWaiting();
                }
            }
        }, period, period, TimeUnit.MILLISECONDS);
        try {
            McpResponse message = this.awaitInStream(request, toolName, run, clientTasks);
            boolean abandoned;
            synchronized (lock) {
                abandoned = state[0] || message == null;
                if (!abandoned) {
                    try {
                        channel.send("message", new String(this.writer.serialize(message), StandardCharsets.UTF_8));
                    } catch (IOException | UncheckedIOException e) {
                        state[0] = true;
                        abandoned = true;
                    }
                }
                state[1] = true;
            }
            if (abandoned) {
                run.stopWaiting();
                if (state[0]) run.cancelTask();
            }
        } finally {
            keepalive.cancel(false);
            channel.close();
        }
    }

    /** La réponse finale du flux, ou null si l'attente a été abandonnée (client parti). */
    private McpResponse awaitInStream(McpRequest request, String toolName, ToolRun run, boolean clientTasks) {
        boolean asTask = clientTasks && run.taskCapable();
        long elapsed = this.timings.jsonWindow().toMillis();
        try {
            if (asTask) {
                try {
                    return this.writer.result(request.id(), this.writer.callToolResult(
                            run.result().get(Math.max(0, this.timings.taskAfter().toMillis() - elapsed), TimeUnit.MILLISECONDS)));
                } catch (TimeoutException e) {
                    McpResponse task = this.onTaskAfter(request, toolName, run);
                    if (task != null) return task;
                    elapsed = this.timings.taskAfter().toMillis(); // pas encore démarré : on attend jusqu'à streamMax
                }
            }
            try {
                return this.writer.result(request.id(), this.writer.callToolResult(
                        run.result().get(Math.max(0, this.timings.streamMax().toMillis() - elapsed), TimeUnit.MILLISECONDS)));
            } catch (TimeoutException e) {
                run.stopWaiting();
                return this.writer.result(request.id(), this.writer.callToolResult(this.writer.noResult()));
            }
        } catch (CancellationException e) {
            return null;
        } catch (RuntimeException e) {
            return this.internalError(request.id(), e);
        } catch (ExecutionException e) {
            return this.failure(request.id(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            run.stopWaiting();
            return null;
        }
    }

    /** Remplie par la Task 5 : rend le CreateTaskResult, ou null si la tâche n'a pas encore d'identifiant. */
    McpResponse onTaskAfter(McpRequest request, String toolName, ToolRun run) {
        return null;
    }

    private McpResponse internalError(String id, RuntimeException e) {
        log.error("tool call response failed", e);
        return this.writer.errorResponse(id, McpProtocol.INTERNAL_ERROR, "Internal error: " + e.getMessage());
    }

    private McpResponse failure(String id, ExecutionException e) {
        if (e.getCause() instanceof ToolRun.TaskFailedException failed) {
            return this.writer.errorResponse(id, failed.error.code(), failed.error.message());
        }
        log.error("tool call failed", e.getCause());
        return this.writer.errorResponse(id, McpProtocol.INTERNAL_ERROR, "Internal error: "
                + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
    }
}

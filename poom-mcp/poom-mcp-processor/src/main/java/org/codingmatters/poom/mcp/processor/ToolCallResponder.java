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
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

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
        // Un verrou par flux pour les deux écritures (keepalive / message final) : jamais d'écriture après
        // un échec, jamais de keepalive après le message final. Le keepalive ne fait que tryLock : un
        // client lent qui bloque l'envoi final ne doit pas affamer le thread partagé des autres flux.
        ReentrantLock lock = new ReentrantLock();
        boolean[] state = new boolean[2]; // 0 = gone, 1 = done (gardés par lock)
        long period = this.timings.keepalive().toMillis();
        ScheduledFuture<?> keepalive = this.keepalives.scheduleAtFixedRate(() -> {
            if (!lock.tryLock()) return; // écriture en cours sur ce flux : ce battement est sauté
            try {
                if (state[0] || state[1]) return;
                try {
                    channel.comment("");
                } catch (IOException | RuntimeException e) {
                    // RuntimeException aussi : une tâche périodique qui lève est annulée sans bruit.
                    state[0] = true;
                    run.stopWaiting();
                }
            } finally {
                lock.unlock();
            }
        }, 0, period, TimeUnit.MILLISECONDS); // délai initial nul : un premier commentaire valide tout de suite les en-têtes
        try {
            McpResponse message = this.awaitInStream(request, toolName, run, clientTasks);
            String payload = message != null ? this.serialize(request.id(), message) : null;
            boolean abandoned;
            lock.lock();
            try {
                abandoned = state[0] || payload == null;
                if (!abandoned) {
                    try {
                        channel.send("message", payload);
                    } catch (IOException | UncheckedIOException e) {
                        state[0] = true;
                        abandoned = true;
                    }
                }
                state[1] = true;
            } finally {
                lock.unlock();
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

    /**
     * Sérialise le message final hors verrou ; un échec ici n'est pas un départ du client : il devient une
     * erreur interne (et null seulement si même celle-ci ne se sérialise pas).
     */
    private String serialize(String id, McpResponse message) {
        try {
            return new String(this.writer.serialize(message), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            log.error("tool call response serialization failed", e);
            try {
                return new String(this.writer.serialize(this.writer.errorResponse(id, McpProtocol.INTERNAL_ERROR, "Internal error")),
                        StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException again) {
                log.error("tool call internal error serialization failed", again);
                return null; // le flux se clôt sans message
            }
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

    /** Rend le CreateTaskResult, ou null si la tâche n'a pas encore d'identifiant. */
    McpResponse onTaskAfter(McpRequest request, String toolName, ToolRun run) {
        Optional<String> id = run.toolTaskId();
        Optional<Instant> createdAt = run.createdAt();
        if (id.isEmpty() || createdAt.isEmpty()) return null;
        run.stopWaiting(); // l'attente locale s'arrête, le travail durable continue
        return this.writer.result(request.id(), this.writer.createTaskResult(
                TaskIds.encode(toolName, createdAt.get(), id.get()), createdAt.get(), this.timings));
    }

    private McpResponse internalError(String id, RuntimeException e) {
        log.error("tool call response failed", e);
        return this.writer.errorResponse(id, McpProtocol.INTERNAL_ERROR, "Internal error");
    }

    private McpResponse failure(String id, ExecutionException e) {
        if (e.getCause() instanceof ToolRun.TaskFailedException failed) {
            return this.writer.errorResponse(id, failed.error.code(), failed.error.message());
        }
        log.error("tool call failed", e.getCause());
        return this.writer.errorResponse(id, McpProtocol.INTERNAL_ERROR, "Internal error");
    }
}

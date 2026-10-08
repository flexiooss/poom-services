package org.codingmatters.poom.mcp.processor;

import org.codingmatters.poom.mcp.McpTaskState;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.McpToolTasks;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.McpError;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Un appel d'outil en cours. Soumis par {@code submit} (et non {@code CompletableFuture.supplyAsync})
 * pour que {@link #stopWaiting()} interrompe vraiment le thread de l'outil.
 */
final class ToolRun {

    static final class TaskFailedException extends RuntimeException {
        final McpError error;
        TaskFailedException(McpError error) {
            super(error.message());
            this.error = error;
        }
    }

    private final McpToolTasks tasks;
    private final AtomicReference<String> toolTaskId = new AtomicReference<>();
    private Future<CallToolResult> result;

    private ToolRun(McpToolTasks tasks) {
        this.tasks = tasks;
    }

    @SuppressWarnings("unchecked")
    static ToolRun start(McpToolDescriptor tool, CallToolParams params, ExecutorService executor, Duration pollInterval) {
        ToolRun run = new ToolRun(tool.tasks());
        if (tool.tasks() == null) {
            Function<CallToolParams, CallToolResult> handler = (Function<CallToolParams, CallToolResult>) tool.handler();
            run.result = executor.submit(() -> handler.apply(params));
        } else {
            run.result = executor.submit(() -> run.startAndAwait(params, pollInterval));
        }
        return run;
    }

    private CallToolResult startAndAwait(CallToolParams params, Duration pollInterval) throws InterruptedException {
        McpToolTasks.Start start = this.tasks.start(params);
        if (start instanceof McpToolTasks.Start.Done done) return done.result();
        String id = ((McpToolTasks.Start.Running) start).toolTaskId();
        this.toolTaskId.set(id);
        while (true) {
            McpTaskState state = this.tasks.get(id);
            switch (state.status()) {
                case COMPLETED -> { return state.result(); }
                case FAILED -> throw new TaskFailedException(state.error());
                case WORKING -> Thread.sleep(pollInterval.toMillis());
            }
        }
    }

    Future<CallToolResult> result() {
        return this.result;
    }

    boolean taskCapable() {
        return this.tasks != null;
    }

    Optional<String> toolTaskId() {
        return Optional.ofNullable(this.toolTaskId.get());
    }

    /** Arrête l'attente locale. Pour un outil à tâches, le travail durable continue. */
    void stopWaiting() {
        this.result.cancel(true);
    }

    /** Demande l'arrêt du travail durable, s'il a démarré. À appeler depuis le thread de la requête (contexte propagé). */
    void cancelTask() {
        String id = this.toolTaskId.get();
        if (this.tasks != null && id != null) this.tasks.cancel(id);
    }
}

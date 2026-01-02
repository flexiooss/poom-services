package org.codingmatters.poom.containers.runtime.undertow;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.ServerShutdownException;
import org.codingmatters.poom.containers.ServerStartupException;
import org.codingmatters.poom.containers.internal.FastFailingProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.processors.StaticResourceProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.xnio.Options.*;

public class UndertowApiContainerRuntime extends ApiContainerRuntime {

    public static final String UNDERTOW_IO_THREAD_COUNT = "UNDERTOW_IO_THREAD_COUNT";
    public static final String UNDERTOW_WORKER_THREAD_COUNT = "UNDERTOW_WORKER_THREAD_COUNT";
    public static final String UNDERTOW_USE_DIRECT_BUFFER = "UNDERTOW_USE_DIRECT_BUFFER";
    public static final String UNDERTOW_GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS = "UNDERTOW_GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS";
    private final String host;
    private final int port;

    private Undertow undertow;
    private final JsonFactory jsonFactory = new JsonFactory();
    private GracefulShutdownHandler gracefulShutdownHandler;

    public UndertowApiContainerRuntime(String host, int port, CategorizedLogger log) {
        super(log);
        this.host = host;
        this.port = port;
    }

    @Override
    protected void startupServer(Api[] apis) throws ServerStartupException {
        PathHandler handlers = Handlers.path();

        for (Api api : this.apis) {
            String path = api.path();
            if (path.isEmpty()) {
                path = "/";
            }
            if (!path.equals("/") && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if (api.docResource() != null) {
                String docpath = path.equals("/") ? "/doc" : path + "/doc";
                handlers
                        .addPrefixPath(docpath, new CdmHttpUndertowHandler(new StaticResourceProcessor(api.docResource(), "text/html")));
            }
            handlers
                    .addPrefixPath(path, new CdmHttpUndertowHandler(new FastFailingProcessor(api.processor(), this, this.jsonFactory)));
        }

        // Wrap handlers with GracefulShutdownHandler for graceful shutdown support
        this.gracefulShutdownHandler = new GracefulShutdownHandler(handlers);

        Undertow.Builder builder = Undertow.builder()
                .addHttpListener(this.port, host)
                .setHandler(this.gracefulShutdownHandler);

        Optional<Env.Var> ioThreadCount = Env.optional(UNDERTOW_IO_THREAD_COUNT);
        if (ioThreadCount.isPresent()) {
            builder.setIoThreads(ioThreadCount.get().asInteger());
        }
        Optional<Env.Var> workerThreadCount = Env.optional(UNDERTOW_WORKER_THREAD_COUNT);
        if (workerThreadCount.isPresent()) {
            builder.setWorkerThreads(workerThreadCount.get().asInteger());
        }
        Optional<Env.Var> useDirectBuffer = Env.optional(UNDERTOW_USE_DIRECT_BUFFER);
        if (useDirectBuffer.isPresent()) {
            builder.setDirectBuffers(useDirectBuffer.get().asBoolean());
        }

        this.undertow = builder.build();
        this.undertow.start();

        try {
            Integer ioThread = undertow.getWorker().getIoThreadCount();
            Integer coreThreadCount = undertow.getWorker().getOption(WORKER_TASK_CORE_THREADS);
            Integer maxThreadCount = undertow.getWorker().getOption(WORKER_TASK_MAX_THREADS);
            Boolean useDirectBufferOpt = undertow.getWorker().getOption(USE_DIRECT_BUFFERS);
            this.log.info("Undertow server started with" +
                    " IoThreadCount=" + ioThread +
                    " useDirectBuffer=" + useDirectBufferOpt +
                    " and coreThreadCount=" + coreThreadCount +
                    " and maxThreadCount=" + maxThreadCount);
        } catch (IOException e) {
            this.log.error("Undertow server started with undefined options ", e);
        }
    }

    @Override
    protected void shutdownServer() throws ServerShutdownException {
        // Get graceful shutdown timeout from environment variable (default: 30 seconds)
        int timeoutSeconds = Env.optional(UNDERTOW_GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS)
                .map(Env.Var::asInteger)
                .orElse(30);

        this.log.info("Initiating graceful shutdown with timeout of " + timeoutSeconds + " seconds");

        try {
            this.gracefulShutdownHandler.shutdown();
            this.log.info("Server shut down: stopped accepting new requests");

            boolean completed = this.gracefulShutdownHandler.awaitShutdown(TimeUnit.SECONDS.toMillis(timeoutSeconds));
            if (completed) {
                this.log.info("All ongoing requests completed successfully");
            } else {
                this.log.warn("Graceful shutdown timeout reached, forcing shutdown");
            }
        } catch (InterruptedException e) {
            this.log.warn("Graceful shutdown interrupted, forcing shutdown");
            Thread.currentThread().interrupt();
        }

        // Stop the Undertow server
        this.undertow.stop();
        this.log.info("Undertow server stopped");
    }
}

package org.codingmatters.poom.containers.runtime.netty;

import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.ServerShutdownException;
import org.codingmatters.poom.containers.ServerStartupException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.processors.MatchingPathProcessor;
import org.codingmatters.rest.api.processors.StaticResourceProcessor;
import org.codingmatters.rest.netty.utils.HttpRequestHandler;
import org.codingmatters.rest.netty.utils.HttpServer;
import org.codingmatters.rest.server.netty.ProcessorRequestHandler;

import java.util.HashSet;
import java.util.LinkedList;

public class NettyApiContainerRuntime extends ApiContainerRuntime {
    public static final String NETTY_API_CONTAINER_BOSS_COUNT = "NETTY_API_CONTAINER_BOSS_COUNT";
    public static final String NETTY_API_CONTAINER_WORKER_COUNT = "NETTY_API_CONTAINER_WORKER_COUNT";

    private HttpServer nettyServer;
    private Processor mainProcessor;

    public NettyApiContainerRuntime(String host, int port, CategorizedLogger log) {
        super(log);
        this.nettyServer = HttpServer.server(
                host, port,
                this::handler,
                Env.optional(NETTY_API_CONTAINER_BOSS_COUNT).orElse(new Env.Var("0")).asInteger(),
                Env.optional(NETTY_API_CONTAINER_WORKER_COUNT).orElse(new Env.Var("0")).asInteger()
        );
    }

    @Override
    protected void startupServer(Api[] apis) throws ServerStartupException {
        this.mainProcessor = this.builderMainProcessor(apis);

        try {
            this.nettyServer.start();
            this.log.info("netty server started");
        } catch (Exception e) {
            throw new ServerStartupException("error starting netty server", e);
        }
    }

    private Processor builderMainProcessor(Api[] apis) {
        MatchingPathProcessor.Builder processorBuilder = new MatchingPathProcessor.Builder();
        if(apis != null && apis.length > 0) {
            LinkedList<Api> orderedApis = new LinkedList<>();
            for (Api api : apis) {
                orderedApis.push(api);
            }
            HashSet<String> registerd = new HashSet<>();
            for (Api api : orderedApis) {
                if(! registerd.contains(api.path())) {
                    if (api.docResource() != null) {
                        processorBuilder.whenMatching(api.path() + "/doc($|/.*)", new StaticResourceProcessor(api.docResource(), "text/html"));
                    }
                    processorBuilder.whenMatching(api.path() + "($|/.*)", api.processor());
                    registerd.add(api.path());
                }
            }
            this.log.info("registered paths : " + registerd);
        }
        return processorBuilder.whenNoMatch((req, resp) -> resp.status(404));
    }

    private HttpRequestHandler handler(String host, int port) {
        return new ProcessorRequestHandler(this.mainProcessor, host, port);
    }

    @Override
    protected void shutdownServer() throws ServerShutdownException {
        this.nettyServer.shutdown();
        try {
            this.nettyServer.awaitTermination();
            this.log.info("netty server stopped");
        } catch (InterruptedException e) {
            throw new ServerShutdownException("interrupted while waiting for shutdown", e);
        }
    }
}

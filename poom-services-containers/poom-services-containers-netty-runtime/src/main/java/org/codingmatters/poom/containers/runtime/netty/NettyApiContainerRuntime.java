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
import org.codingmatters.rest.netty.utils.config.NettyHttpConfig;
import org.codingmatters.rest.server.netty.ProcessorRequestHandler;

import java.util.HashSet;
import java.util.LinkedList;

public class NettyApiContainerRuntime extends ApiContainerRuntime {
    public static final String NETTY_API_CONTAINER_BOSS_COUNT = "NETTY_API_CONTAINER_BOSS_COUNT";
    public static final String NETTY_API_CONTAINER_WORKER_COUNT = "NETTY_API_CONTAINER_WORKER_COUNT";
    private static final String NETTY_API_CONTAINER_MAX_HEADER_SIZE = "NETTY_API_CONTAINER_MAX_HEADER_SIZE";
    private static final String NETTY_API_CONTAINER_MAX_INITIAL_LINE_LENGTH = "NETTY_API_CONTAINER_MAX_INITIAL_LINE_LENGTH";
    private static final String NETTY_API_CONTAINER_MAX_CHUNK_SIZE = "NETTY_API_CONTAINER_MAX_CHUNK_SIZE";
    private static final String NETTY_API_CONTAINER_INITIAL_BUFFER_SIZE = "NETTY_API_CONTAINER_INITIAL_BUFFER_SIZE";
    private static final String NETTY_API_CONTAINER_VALIDATE_HEADERS = "NETTY_API_CONTAINER_VALIDATE_HEADERS";
    private static final String NETTY_API_CONTAINER_ALLOW_DUPLICATE_CONTENT_LENGTH = "NETTY_API_CONTAINER_ALLOW_DUPLICATE_CONTENT_LENGTH";
    private static final String NETTY_API_CONTAINER_ALLOW_PARTIAL_CHUNCK = "NETTY_API_CONTAINER_ALLOW_PARTIAL_CHUNCK";

    private HttpServer nettyServer;
    private Processor mainProcessor;

    public NettyApiContainerRuntime(String host, int port, CategorizedLogger log) {
        super(log);
        this.nettyServer = HttpServer.server(this.configFromEnv(host, port), this::handler);
    }

    private NettyHttpConfig configFromEnv(String host, int port) {
        NettyHttpConfig.Builder config = NettyHttpConfig.builder()
                .host(host).port(port);
        if(Env.optional(NETTY_API_CONTAINER_BOSS_COUNT).isPresent()) {
            config.bossCount(Env.mandatory(NETTY_API_CONTAINER_BOSS_COUNT).asInteger());
        }
        if(Env.optional(NETTY_API_CONTAINER_WORKER_COUNT).isPresent()) {
            config.workerCount(Env.mandatory(NETTY_API_CONTAINER_WORKER_COUNT).asInteger());
        }
        if(Env.optional(NETTY_API_CONTAINER_MAX_HEADER_SIZE).isPresent()) {
            config.maxHeaderSize(Env.mandatory(NETTY_API_CONTAINER_MAX_HEADER_SIZE).asInteger());
        }
        if(Env.optional(NETTY_API_CONTAINER_MAX_INITIAL_LINE_LENGTH).isPresent()) {
            config.maxInitialLineLength(Env.mandatory(NETTY_API_CONTAINER_MAX_INITIAL_LINE_LENGTH).asInteger());
        }
        if(Env.optional(NETTY_API_CONTAINER_MAX_CHUNK_SIZE).isPresent()) {
            config.maxChunkSize(Env.mandatory(NETTY_API_CONTAINER_MAX_CHUNK_SIZE).asInteger());
        }
        if(Env.optional(NETTY_API_CONTAINER_INITIAL_BUFFER_SIZE).isPresent()) {
            config.initialBufferSize(Env.mandatory(NETTY_API_CONTAINER_INITIAL_BUFFER_SIZE).asInteger());
        }
        if(Env.optional(NETTY_API_CONTAINER_VALIDATE_HEADERS).isPresent()) {
            config.validateHeaders(Env.mandatory(NETTY_API_CONTAINER_VALIDATE_HEADERS).asString().equals("true"));
        }
        if(Env.optional(NETTY_API_CONTAINER_ALLOW_DUPLICATE_CONTENT_LENGTH).isPresent()) {
            config.allowDuplicateContentLengths(Env.mandatory(NETTY_API_CONTAINER_ALLOW_DUPLICATE_CONTENT_LENGTH).asString().equals("true"));
        }
        if(Env.optional(NETTY_API_CONTAINER_ALLOW_PARTIAL_CHUNCK).isPresent()) {
            config.allowPartialChunks(Env.mandatory(NETTY_API_CONTAINER_ALLOW_PARTIAL_CHUNCK).asString().equals("true"));
        }

        return config.build();
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
                String path = api.path();
                if(path.equals("/")) {
                    path = "";
                }
                if(path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if(! registerd.contains(path)) {
                    if (api.docResource() != null) {
                        String docpath = path + "/doc";
                        processorBuilder.whenMatching(docpath + "($|/.*)", new StaticResourceProcessor(api.docResource(), "text/html"));
                    }
                    processorBuilder.whenMatching(path + "($|/.*)", api.processor());
                    registerd.add(path);
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

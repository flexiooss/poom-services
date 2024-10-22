package org.codingmatters.poom.containers.runtime.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.ServerShutdownException;
import org.codingmatters.poom.containers.ServerStartupException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.processors.StaticResourceProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class UndertowApiContainerRuntime extends ApiContainerRuntime {

    private final String host;
    private final int port;

    private Undertow undertow;

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
            if(path.isEmpty()) {
                path = "/";
            }
            if(! path.equals("/") && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if (api.docResource() != null) {
                String docpath = path.equals("/") ? "/doc" : path + "/doc";
                handlers
                        .addPrefixPath(docpath, new CdmHttpUndertowHandler(new StaticResourceProcessor(api.docResource(), "text/html")));
            }
            handlers
                    .addPrefixPath(path, new CdmHttpUndertowHandler(api.processor()));
        }

        this.undertow = Undertow.builder()
                .addHttpListener(this.port, host)
                .setHandler(handlers)
                .build();
        this.undertow.start();
        this.log.info("undertow server started");
    }

    @Override
    protected void shutdownServer() throws ServerShutdownException {
        this.undertow.stop();
        this.log.info("undertow server stopped");
    }

    @Override
    public void requireStop() {

    }
}

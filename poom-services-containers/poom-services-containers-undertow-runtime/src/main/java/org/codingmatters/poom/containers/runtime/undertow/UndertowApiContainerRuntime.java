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

    protected UndertowApiContainerRuntime(String host, int port, CategorizedLogger log) {
        super(log);
        this.host = host;
        this.port = port;
    }

    @Override
    protected void startupServer(Api[] apis) throws ServerStartupException {
        PathHandler handlers = Handlers.path();

        for (Api api : this.apis) {
            if (api.docResource() != null) {
                handlers
                        .addPrefixPath(api.path() + "/doc", new CdmHttpUndertowHandler(new StaticResourceProcessor(api.docResource(), "text/html")));
            }
            handlers
                    .addPrefixPath(api.path(), new CdmHttpUndertowHandler(api.processor()));
        }
//            if (rawHandlers != null)
//                for (RawHandler handler : rawHandlers) {
//                    handlers.addPrefixPath(api.path() + handler.getPath(), handler.getHandler());
//                }
//        }
//
//        this.setupConfigSubscriptions(handlers);

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
}

package org.codingmatters.poom.containers;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Api;

public abstract class ApiContainerRuntime {
    protected final CategorizedLogger log;
    protected Api[] apis;

    protected abstract void startupServer(Api[] apis) throws ServerStartupException;
    protected abstract void shutdownServer() throws ServerShutdownException;

    protected ApiContainerRuntime(CategorizedLogger log) {
        this.log = log;
    }

    public ApiContainerRuntime apis(Api ... apis) {
        this.apis = apis;
        return this;
    }

    public void main() {
        try {
            this.go();
        } catch( Exception e ) {
            log.error("error starting service", e);
            System.exit( 2 );
        } finally {
            try {
                this.stop();
            } catch (Exception e) {
                log.error("error starting service", e);
                System.exit( 3 );
            }
        }
        System.exit( 0 );
    }

    private void go() throws ServerStartupException {
        this.startup();
        while (true) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    protected void startup() throws ServerStartupException {
        // TODO enable pre startup injection
        this.startupServer(this.apis);
    }

    protected void stop() throws ServerShutdownException {
        this.shutdownServer();
        // TODO enable post shutdown injection
    }

}

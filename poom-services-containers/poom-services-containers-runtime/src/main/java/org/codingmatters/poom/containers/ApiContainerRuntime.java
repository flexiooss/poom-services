package org.codingmatters.poom.containers;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Api;

public abstract class ApiContainerRuntime {
    protected final CategorizedLogger log;
    private final Handle handle;
    protected Api[] apis;

    private Runnable onStartup;
    private Runnable onShutdown;


    protected abstract void startupServer(Api[] apis) throws ServerStartupException;
    protected abstract void shutdownServer() throws ServerShutdownException;

    protected ApiContainerRuntime(CategorizedLogger log) {
        this.log = log;
        this.onStartup = () -> {};
        this.onShutdown = () -> {};
        this.handle = new Handle(this);
    }

    public ApiContainerRuntime apis(Api ... apis) {
        this.apis = apis;
        return this;
    }

    public void onStartup(Runnable onStartup) {
        this.onStartup = onStartup != null ? onStartup : () -> {};
    }

    public void onShutdown(Runnable onShutdown) {
        this.onShutdown = onShutdown != null ? onShutdown : () -> {};
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
        try {
            this.onStartup.run();
        } catch (Throwable e) {
            throw new ServerStartupException("error running pre startup runnable", e);
        }
        this.startupServer(this.apis);
    }

    protected void stop() throws ServerShutdownException {
        this.shutdownServer();
        try {
            this.onShutdown.run();
        } catch (Throwable t) {
            throw new ServerShutdownException("error running post shutdown runnable", t);
        }
    }

    public Handle handle() {
        return this.handle;
    }

    public class Handle {
        private final ApiContainerRuntime runtime;

        public Handle(ApiContainerRuntime runtime) {
            this.runtime = runtime;
        }

        public void start() throws ServerStartupException {
            this.runtime.startup();
        }
        public void stop() throws ServerShutdownException {
            this.runtime.stop();
        }
    }
}

package org.codingmatters.poom.services.runtime;

import io.undertow.Undertow;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.runtime.handlers.RequestLoggingHandler;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class Service {

    static public  Service fromEnv(Processor processor, String serviceName) {
        return new Service(
                processor,
                serviceName,
                Env.mandatory(Env.SERVICE_HOST).asString(),
                Env.mandatory(Env.SERVICE_PORT).asInteger()
        );
    }

    private Undertow server;
    private final Processor processor;
    private final String serviceName;
    private final int port;
    private final String host;

    public Service(Processor processor, String serviceName, String host, int port) {
        this.processor = processor;
        this.serviceName = serviceName;
        this.port = port;
        this.host = host;
    }

    public void main(CategorizedLogger log) {
        try {
            log.info("starting {} service at {}:{}", this.serviceName, this.host, this.port);
            this.start();
            while (true) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    break;
                }
            }
            log.info("stopping service {} as requested", this.serviceName);
            this.stop();
            System.exit( 0 );
        } catch( Exception e ) {
            log.error("error while executing service " + this.serviceName, e);
            this.stop();
            System.exit( 1 );
        }
    }

    public void start() {
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new RequestLoggingHandler(new CdmHttpUndertowHandler(this.processor)))
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }
}

package org.codingmatters.poom.services.report.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.report.api.service.handlers.ReportStore;
import org.codingmatters.poom.services.report.api.service.handlers.ServiceReportApiHandlersFactory;
import org.codingmatters.poom.services.report.api.service.handlers.report.store.FileBasedReportStore;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class ServiceReportService {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(ServiceReportService.class);

    static public final String STORAGE_FOLDER = "STORAGE_FOLDER";

    static public void main(String[] args) {
        ServiceReportService service = fromEnv();
        log.info("starting service report service");
        service.start();
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        log.info("stopping service report service");
        service.stop();
    }

    static public ServiceReportService fromEnv() {
        JsonFactory jsonFactory = new JsonFactory();
        ReportStore store = new FileBasedReportStore(
                Env.mandatory(STORAGE_FOLDER).asFile(),
                jsonFactory
        );

        return new ServiceReportService(
                Env.mandatory(Env.SERVICE_HOST).asString(),
                Env.mandatory(Env.SERVICE_PORT).asInteger(),
                new PoomServicesReportAPIProcessor(
                        "/service-report",
                        jsonFactory,
                        new ServiceReportApiHandlersFactory(store).build()
                )
        );
    }



    private Undertow server;
    private final int port;
    private final String host;
    private final Processor processor;

    public ServiceReportService(String host, int port, Processor processor) {
        this.port = port;
        this.host = host;
        this.processor = processor;
    }

    public void start() {
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(this.processor))
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }
}

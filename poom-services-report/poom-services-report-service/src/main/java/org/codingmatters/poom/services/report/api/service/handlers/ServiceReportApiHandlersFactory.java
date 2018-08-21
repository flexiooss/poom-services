package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.report.api.PoomServicesReportAPIHandlers;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceReportApiHandlersFactory {

    private final ReportStore store;
    private Optional<String> callbackUrl = Optional.empty();
    private ExecutorService callbackPool;

    public ServiceReportApiHandlersFactory(ReportStore store) {
        this.store = store;
    }

    public ServiceReportApiHandlersFactory callbackUrl(Optional<String> callbackUrl) {
        this.callbackUrl = callbackUrl;
        return this;
    }

    public ServiceReportApiHandlersFactory callbackPool(ExecutorService callbackPool) {
        this.callbackPool = callbackPool;
        return this;
    }

    public PoomServicesReportAPIHandlers build() {
        return new PoomServicesReportAPIHandlers.Builder()
                .reportsPostHandler(new ReportCreation(
                        store,
                        this.callbackUrl,
                        this.callbackPool == null && this.callbackUrl.isPresent() ?
                                Executors.newFixedThreadPool(1) :
                                this.callbackPool
                ))
                .reportsGetHandler(new ReportBrowsing(store))
                .build();
    }

}

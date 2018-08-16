package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.report.api.PoomServicesReportAPIHandlers;

public class ServiceReportApiHandlersFactory {

    private final ReportStore store;

    public ServiceReportApiHandlersFactory(ReportStore store) {
        this.store = store;
    }

    public PoomServicesReportAPIHandlers build() {
        return new PoomServicesReportAPIHandlers.Builder()
                .reportsPostHandler(new ReportCreation(store))
                .reportsGetHandler(new ReportBrowsing(store))
                .build();
    }

}

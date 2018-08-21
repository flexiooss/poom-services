package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.report.api.ReportsPostRequest;
import org.codingmatters.poom.services.support.date.UTC;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public abstract class AbstractReportCreationTest {
    static final String CONTAINER_ID = "cid";
    static final String SERVICE_NAME = "service";
    static final String SERVICE_VERSION = "version";
    static final String EXIT_STATUS = "42";
    static final String MAIN_CLASS = "service.MainClass";
    static final LocalDateTime START = UTC.now().minus(2,ChronoUnit.HOURS);
    static final LocalDateTime END = UTC.now().minus(1, ChronoUnit.HOURS);

    protected ReportsPostRequest.Builder requestWithHeadersSet() {
        return ReportsPostRequest.builder()
                .xContainerId(CONTAINER_ID)
                .xName(SERVICE_NAME)
                .xMainClass(MAIN_CLASS)
                .xVersion(SERVICE_VERSION)
                .xExitStatus(EXIT_STATUS)
                .xStart(START)
                .xEnd(END);
    }
}

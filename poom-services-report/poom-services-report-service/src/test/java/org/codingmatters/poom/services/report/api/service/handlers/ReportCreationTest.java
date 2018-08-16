package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.report.api.ReportsPostRequest;
import org.codingmatters.poom.services.report.api.reportspostresponse.Status201;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.types.File;
import org.codingmatters.rest.api.types.optional.OptionalFile;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ReportCreationTest {

    private static final String CONTAINER_ID = "cid";
    private static final String SERVICE_NAME = "service";
    private static final String SERVICE_VERSION = "version";
    private static final String EXIT_STATUS = "42";
    private static final String MAIN_CLASS = "service.MainClass";

    private ReportCreation handler;
    private LocalDateTime start;
    private LocalDateTime end;

    private final AtomicBoolean failNextStorage = new AtomicBoolean(false);
    private final AtomicReference<Object[]> lastStorageParams = new AtomicReference<>(null);
    private ReportStore store = new ReportStore() {
        @Override
        public PagedEntityList<Report> all(long startIndex, long endIndex) throws RepositoryException {
            return null;
        }

        @Override
        public PagedEntityList<Report> search(ReportQuery query, long startIndex, long endIndex) throws RepositoryException {
            return null;
        }

        @Override
        public Report store(Report report, OptionalFile dump) throws ReportStoreException {
            if(failNextStorage.get()) {
                throw new ReportStoreException("test generated error");
            }
            lastStorageParams.set(new Object[] {report, dump});
            return report.withId("setted-from-store");
        }

        @Override
        public Optional<Report> report(String id) throws ReportStoreException {
            return Optional.empty();
        }

        @Override
        public Optional<File> dump(String id) {
            return Optional.empty();
        }
    };

    @Before
    public void setUp() throws Exception {
        this.handler = new ReportCreation(this.store);
        this.start = UTC.now().minus(2, ChronoUnit.HOURS);
        this.end = UTC.now().minus(1, ChronoUnit.HOURS);
    }

    @Test
    public void givenHeadersAreSet__whenNoPayload__thenReportIsCreatedWithoutDump() {
        Status201 response = this.handler.apply(this.requestWithHeadersSet().build())
                .opt().status201().orElseThrow(() -> new AssertionError("entity should be created"));

        assertThat(
                response.payload().withReportedAt(null),
                is(Report.builder()
                        .id("setted-from-store")
                        .containerId(CONTAINER_ID)
                        .name(SERVICE_NAME)
                        .version(SERVICE_VERSION)
                        .mainClass(MAIN_CLASS)
                        .exitStatus(EXIT_STATUS)
                        .start(start)
                        .end(end)
                        .hasDump(false)
                        .build())
        );

        assertThat(response.payload().reportedAt(), is(notNullValue()));

        assertThat(this.lastStorageParams.get()[0], is(response.payload().withId(null)));
        assertThat(((OptionalFile)this.lastStorageParams.get()[1]).isPresent(), is(false));
    }

    @Test
    public void givenHeadersAreSet__whenPayloadProvided__thenReportIsCreatedWithDump() {
        File dump = File.builder()
                .contentType("application/octet-stream")
                .content("do you feel like we do ?".getBytes())
                .build();
        Status201 response = this.handler.apply(this.requestWithHeadersSet()
                .payload(dump)
                .build())
                .opt().status201().orElseThrow(() -> new AssertionError("entity should be created"));

        assertThat(
                response.payload().withReportedAt(null),
                is(Report.builder()
                        .id("setted-from-store")
                        .containerId(CONTAINER_ID)
                        .name(SERVICE_NAME)
                        .version(SERVICE_VERSION)
                        .mainClass(MAIN_CLASS)
                        .exitStatus(EXIT_STATUS)
                        .start(start)
                        .end(end)
                        .hasDump(true)
                        .build())
        );

        assertThat(response.payload().reportedAt(), is(notNullValue()));

        assertThat(this.lastStorageParams.get()[0], is(response.payload().withId(null)));
        assertThat(((OptionalFile)this.lastStorageParams.get()[1]).get(), is(dump));
    }

    @Test
    public void givenAllHeadersAreNotSet__whenNoName__thenRequestIsInvalid() {
        this.handler.apply(this.requestWithHeadersSet()
                .xName(null)
                .build())
                .opt().status400().orElseThrow(() -> new AssertionError("request shouldn't be valid"));
        assertThat(this.lastStorageParams.get(), is(nullValue()));
    }

    @Test
    public void givenAllHeadersAreNotSet__whenNoVersion__thenRequestIsInvalid() {
        this.handler.apply(this.requestWithHeadersSet()
                .xVersion(null)
                .build())
                .opt().status400().orElseThrow(() -> new AssertionError("request shouldn't be valid"));
        assertThat(this.lastStorageParams.get(), is(nullValue()));
    }

    @Test
    public void givenAllHeadersAreNotSet__whenNoStart__thenRequestIsInvalid() {
        this.handler.apply(this.requestWithHeadersSet()
                .xStart(null)
                .build())
                .opt().status400().orElseThrow(() -> new AssertionError("request shouldn't be valid"));
        assertThat(this.lastStorageParams.get(), is(nullValue()));
    }

    @Test
    public void givenAllHeadersAreNotSet__whenNoEnd__thenRequestIsInvalid() {
        this.handler.apply(this.requestWithHeadersSet()
                .xEnd(null)
                .build())
                .opt().status400().orElseThrow(() -> new AssertionError("request shouldn't be valid"));
        assertThat(this.lastStorageParams.get(), is(nullValue()));
    }

    @Test
    public void givenAllHeadersAreSet__whenStorageExceptionOccurs__thenRequestFails() {
        this.handler.apply(this.requestWithHeadersSet()
                .xEnd(null)
                .build())
                .opt().status400().orElseThrow(() -> new AssertionError("request shouldn't be valid"));
        assertThat(this.lastStorageParams.get(), is(nullValue()));
    }

    @Test
    public void givenAllHeadersAreNotSet__whenNoExitStatus__thenRequestIsInvalid() {
        this.failNextStorage.set(true);

        this.handler.apply(this.requestWithHeadersSet().build())
                .opt().status500().orElseThrow(() -> new AssertionError("request should fail"));
        assertThat(this.lastStorageParams.get(), is(nullValue()));
    }


    private ReportsPostRequest.Builder requestWithHeadersSet() {
        return ReportsPostRequest.builder()
                .xContainerId(CONTAINER_ID)
                .xName(SERVICE_NAME)
                .xMainClass(MAIN_CLASS)
                .xVersion(SERVICE_VERSION)
                .xExitStatus(EXIT_STATUS)
                .xStart(start)
                .xEnd(end);
    }

}
package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.types.File;
import org.codingmatters.rest.api.types.optional.OptionalFile;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ReportCreationCallbackTest extends AbstractReportCreationTest {

    private ReportStore store = new ReportStore(){
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
            return report.withId("12").withReportedAt(LocalDateTime.parse("2012-12-12T12:12:12Z", Processor.Formatters.DATETIMEONLY.formatter));
        }

        @Override
        public Optional<Report> report(String id) throws ReportStoreException {
            return Optional.empty();
        }

        @Override
        public Optional<File> dump(String id) throws ReportStoreException {
            return Optional.empty();
        }
    };

    private final AtomicReference<Map<String, List<String>>> lastQueryParameters = new AtomicReference<>();

    @Rule
    public UndertowResource callbackService = new UndertowResource(exchange -> {
        HashMap<String, List<String>> params = new HashMap<>();
        exchange.getQueryParameters().forEach((name, values) -> params.put(name, new ArrayList<>(values)));
        lastQueryParameters.set(params);
    });

    @Test
    public void givenNoCallbackUrl__whenReportCreated__thenCallbackIsNotCalled() throws Exception {
        ExecutorService callbackPool = Executors.newFixedThreadPool(1);
        new ReportCreation(
                this.store,
                Optional.empty(),
                callbackPool
        ).apply(this.requestWithHeadersSet().build())
                .opt().status201().orElseThrow(() -> new AssertionError("entity should be created"));

        callbackPool.shutdown();
        callbackPool.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(this.lastQueryParameters.get(), is(nullValue()));
    }

    @Test
    public void givenCallbackUrl__whenReportCreated__thenCallbackIsGettesWithReportProperties() throws Exception {
        ExecutorService callbackPool = Executors.newFixedThreadPool(1);
        new ReportCreation(
                this.store,
                Optional.of(this.callbackService.baseUrl()),
                callbackPool
        ).apply(this.requestWithHeadersSet()
                .xExitStatus("12")
                .build())
                .opt().status201().orElseThrow(() -> new AssertionError("entity should be created"));


        callbackPool.shutdown();
        callbackPool.awaitTermination(1, TimeUnit.MINUTES);

        /*
         name: string
         version: string
         main-class: string
         container-id: string
         start: string
         end: string
         exit-status: string
         has-dump: boolean
         reported-at: string
         */
        assertThat(this.lastQueryParameters.get().get("name"), contains(SERVICE_NAME));
        assertThat(this.lastQueryParameters.get().get("version"), contains(SERVICE_VERSION));
        assertThat(this.lastQueryParameters.get().get("main-class"), contains(MAIN_CLASS));
        assertThat(this.lastQueryParameters.get().get("container-id"), contains(CONTAINER_ID));
        assertThat(this.lastQueryParameters.get().get("start"), contains(START.format(Processor.Formatters.DATETIMEONLY.formatter)));
        assertThat(this.lastQueryParameters.get().get("end"), contains(END.format(Processor.Formatters.DATETIMEONLY.formatter)));
        assertThat(this.lastQueryParameters.get().get("exit-status"), contains("12"));
        assertThat(this.lastQueryParameters.get().get("has-dump"), contains("false"));
        assertThat(this.lastQueryParameters.get().get("reported-at"), contains("2012-12-12T12:12:12.000Z"));
    }

    @Ignore
    @Test
    public void realCallback() throws Exception {
        ExecutorService callbackPool = Executors.newFixedThreadPool(1);
        new ReportCreation(
                this.store,
                Optional.of("https://my.flexio.io/channelApi/flexHttpInOut/59d3a0105d443519843d0496/5b7cf26d35537330db5bc2f3"),
                callbackPool
        ).apply(this.requestWithHeadersSet()
                .xExitStatus("12")
                .build())
                .opt().status201().orElseThrow(() -> new AssertionError("entity should be created"));


        callbackPool.shutdown();
        callbackPool.awaitTermination(1, TimeUnit.MINUTES);
    }
}

package org.codingmatters.poom.services.report.api.service.handlers;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.report.api.ReportsGetRequest;
import org.codingmatters.poom.services.report.api.ValueList;
import org.codingmatters.poom.services.report.api.service.handlers.report.store.FileBasedReportStore;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.rest.api.types.optional.OptionalFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReportBrowsingTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    private JsonFactory jsonFactory = new JsonFactory();

    private ReportStore store;
    private ReportBrowsing handler;

    @Before
    public void setUp() throws Exception {
        this.store = new FileBasedReportStore(this.dir.getRoot(), this.jsonFactory);
        this.handler = new ReportBrowsing(this.store);
    }

    @Test
    public void completeList() throws Exception {
        this.createSomeReports(20);

        ValueList<Report> list = this.handler.apply(ReportsGetRequest.builder().build()).opt().status200().payload()
                .orElseThrow(() -> new AssertionError("should return complete list"));

        assertThat(list.size(), is(20));
    }

    @Test
    public void partialList() throws Exception {
        this.createSomeReports(200);

        ValueList<Report> list = this.handler.apply(ReportsGetRequest.builder().build()).opt().status206().payload()
                .orElseThrow(() -> new AssertionError("should return partial list"));

        assertThat(list.size(), is(100));
    }

    @Test
    public void queriedList() throws Exception {
        this.createSomeReports(200);

        ValueList<Report> list = this.handler.apply(ReportsGetRequest.builder()
                .reportedAt(UTC.now().minus(3, ChronoUnit.HOURS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH")))
                .build()).opt().status200().payload()
                .orElseThrow(() -> new AssertionError("should return partial list"));

        assertThat(list.size(), is(2));
    }

    private void createSomeReports(int count) throws ReportStore.ReportStoreException {
        LocalDateTime reportedAt = UTC.now();
        for (int i = 0; i < count; i++) {
            this.store.store(Report.builder()
                    .reportedAt(reportedAt)
                    .build(), OptionalFile.of(null));
            if(i % 2 == 0) reportedAt = reportedAt.minus(3, ChronoUnit.HOURS);
        }
    }


}
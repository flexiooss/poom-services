package org.codingmatters.poom.services.report.api.service.handlers.report.store;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.report.api.service.handlers.ReportStore;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.types.optional.OptionalFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FileBasedReportStoreListingTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    private FileBasedReportStore store;
    private JsonFactory jsonFactory = new JsonFactory();

    @Before
    public void setUp() throws Exception {
        this.store = new FileBasedReportStore(new File(this.dir.getRoot(), "report-store"), this.jsonFactory);
    }


    @Test
    public void givenNoReportStored__whenListingAllReports__thenEmptyListIsReturned() throws Exception {
        PagedEntityList<Report> page = this.store.all(0, 100);

        assertThat(page.size(), is(0));
        assertThat(page.total(), is(0L));
    }

    @Test
    public void givenSomeReportStored__whenListingAllReportsWithFullPage__thenAllReportsAreReturned() throws Exception {
        this.createSomeRports(20);

        PagedEntityList<Report> page = this.store.all(0, 100);

        assertThat("page size", page.size(), is(20));
        assertThat("start index", page.startIndex(), is(0L));
        assertThat("end index", page.endIndex(), is(19L));
        assertThat("total size", page.total(), is(20L));
    }

    @Test
    public void givenSomeReportStored__whenListingAllReportsWithStartPage__thenAllReportsAreReturned() throws Exception {
        this.createSomeRports(20);

        PagedEntityList<Report> page = this.store.all(0, 9);

        assertThat("page size", page.size(), is(10));
        assertThat("start index", page.startIndex(), is(0L));
        assertThat("end index", page.endIndex(), is(9L));
        assertThat("total size", page.total(), is(20L));
    }

    @Test
    public void givenSomeReportStored__whenListingAllReportsWithMiddlePage__thenAllReportsAreReturned() throws Exception {
        this.createSomeRports(20);

        PagedEntityList<Report> page = this.store.all(5, 14);

        assertThat("page size", page.size(), is(10));
        assertThat("start index", page.startIndex(), is(5L));
        assertThat("end index", page.endIndex(), is(14L));
        assertThat("total size", page.total(), is(20L));
    }

    @Test
    public void givenSomeReportStored__whenListingAllReportsWithEndPage__thenAllReportsAreReturned() throws Exception {
        this.createSomeRports(20);

        PagedEntityList<Report> page = this.store.all(15, 24);

        assertThat("page size", page.size(), is(5));
        assertThat("start index", page.startIndex(), is(15L));
        assertThat("end index", page.endIndex(), is(19L));
        assertThat("total size", page.total(), is(20L));
    }

    @Test
    public void givenNoReportStored__whenSearchingReports__thenEmptyListIsReturned() throws Exception {
        PagedEntityList<Report> page = this.store.search(ReportQuery.builder().build(),0, 100);

        assertThat(page.size(), is(0));
        assertThat(page.total(), is(0L));
    }

    @Test
    public void givenSomeReportStored__whenSearchingWithReportedAtQuery__thenReportsAreFiltered() throws Exception {
        this.createSomeRports(20);

        PagedEntityList<Report> page = this.store.search(ReportQuery.builder()
                .reportedAt(DateTimeFormatter.ofPattern("yyyy-MM-dd HH").format(UTC.now().minus(1, ChronoUnit.HOURS)))
                .build(),0, 100);

        assertThat("page size", page.size(), is(1));
        assertThat("total size", page.total(), is(1L));
    }

    @Test
    public void givenSomeReportStored__whenSearchingWithNameQuery__thenReportsAreFiltered() throws Exception {
        this.createSomeRports(20);

        PagedEntityList<Report> page = this.store.search(ReportQuery.builder()
                .name("12")
                .build(),0, 100);

        assertThat("page size", page.size(), is(1));
        assertThat("total size", page.total(), is(1L));
    }

    private void createSomeRports(int count) throws ReportStore.ReportStoreException {
        LocalDateTime reportedAt = UTC.now();
        for (int i = 0; i < count; i++) {
            this.store.store(Report.builder()
                    .reportedAt(reportedAt)
                    .name("" + i)
                    .build(), OptionalFile.of(null));
            reportedAt = reportedAt.minus(1, ChronoUnit.HOURS);
        }
    }
}

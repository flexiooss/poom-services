package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.report.api.ReportDumpGetRequest;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.types.File;
import org.codingmatters.rest.api.types.optional.OptionalFile;
import org.codingmatters.rest.io.Content;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReportDumpReadTest {

    private ReportDumpRead handler;
    private ReportStore store = new ReportStore() {
        @Override
        public Report store(Report report, OptionalFile dump) throws ReportStoreException {
            return null;
        }

        @Override
        public Optional<Report> report(String id) throws ReportStoreException {
            return Optional.of(Report.builder()
                    .id(id)
                    .hasDump(true)
                    .build());
        }

        @Override
        public Optional<File> dump(String id) throws ReportStoreException {
            return Optional.of(
                    File.builder()
                            .contentType("application/octet-stream")
                            .content(Content.from("do you feel like we do ?"))
                            .build()
            );
        }

        @Override
        public PagedEntityList<Report> all(long startIndex, long endIndex) throws RepositoryException {
            return null;
        }

        @Override
        public PagedEntityList<Report> search(ReportQuery query, long startIndex, long endIndex) throws RepositoryException {
            return null;
        }
    };

    @Before
    public void setUp() throws Exception {
        this.handler = new ReportDumpRead(this.store);
    }

    @Test
    public void readDump() throws Exception {
        File file = this.handler.apply(ReportDumpGetRequest.builder().reportId("12").build()).opt()
                .status200().orElseThrow(() -> new AssertionError("should have got a 200"))
                .payload();

        assertThat(file.contentType(), is("application/octet-stream"));
        assertThat(new String(file.content().asBytes()), is("do you feel like we do ?"));
    }
}
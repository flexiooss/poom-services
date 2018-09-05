package org.codingmatters.poom.services.report.api.service.handlers.report.store;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.services.report.api.service.handlers.ReportStore;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.json.ReportReader;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.rest.api.types.optional.OptionalFile;
import org.codingmatters.rest.io.Content;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileBasedReportStoreTest {

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
    public void givenReportIsInvalid__whenReportedAtIsMissing__thenReportStoreExceptionIsRaised() throws Exception {
        this.expected.expect(ReportStore.ReportStoreException.class);
        this.expected.expectMessage("missing reportedAt date");

        this.store.store(this.validReport().reportedAt(null).build(), OptionalFile.of(null));
    }

    @Test
    public void givenNoOperationOnStore__whenStoreIsCreated__thenStoragDirIsCreated() {
        assertTrue(new File(this.dir.getRoot(), "report-store").exists());
    }

    @Test
    public void givenReportIsValid__whenReportIsStored__thenIdIsCalculatedFromDateAndUUID() throws Exception {
        Report report = this.validReport().build();
        report = this.store.store(report, OptionalFile.of(null));

        assertThat(report.id(), startsWith(report.reportedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + "-"));
    }

    @Test
    public void givenReportIsValidAndNoDumpProvided__whenReportStored__thenReportDirIsCreated() throws Exception {
        Report report = this.validReport().build();
        report = this.store.store(report, OptionalFile.of(null));

        File reportDir = new File(this.dir.getRoot(), this.reportDirPath(report));

        assertTrue(this.reportDirPath(report) + " should exist", reportDir.exists());
    }

    @Test
    public void givenReportIsValidAndNoDumpProvided__whenReportStored__thenReportFileCreated() throws Exception {
        Report report = this.validReport().build();
        report = this.store.store(report, OptionalFile.of(null));

        String reportFilepath = this.descriptorPath(report);
        File reportFile = new File(this.dir.getRoot(), reportFilepath);

        assertTrue(this.descriptorPath(report) + " should exist", reportFile.exists());

        try(JsonParser parser = this.jsonFactory.createParser(reportFile)) {
            Report read = new ReportReader().read(parser);
            assertThat(read, is(report));
        }
    }

    @Test
    public void givenReportIsValidAndDumpProvided__whenReportStored__thenReportFileCreated_abdDumpIsStored() throws Exception {
        Report report = this.validReport().build();
        report = this.store.store(report, this.createFileWithContent("do you feel like we do ?"));

        File reportFile = new File(this.dir.getRoot(), this.descriptorPath(report));
        File dumpFile = new File(this.dir.getRoot(), this.dumpPath(report));

        assertTrue(this.descriptorPath(report) + " should exist", reportFile.exists());
        assertTrue(this.dumpPath(report) + " should exist", dumpFile.exists());

        assertThat(this.readBytes(dumpFile), is("do you feel like we do ?".getBytes()));
    }

    @Test
    public void whenReportWasStored__whenGettingReport__thenDescriptorIsRead() throws Exception {
        Report report = this.store.store(this.validReport()
                .name("service")
                .version("v1")
                .containerId("123456789")
                .build(),
                this.createFileWithContent("do you feel like we do ?"));

        assertThat(this.store.report(report.id()).get(), is(report));
    }

    @Test
    public void whenIdIsMalformedBecauseNoDashFound__whenGettingReport__thenReportStoreExceptionIsRaised() throws Exception {
        this.expected.expect(ReportStore.ReportStoreException.class);
        this.expected.expectMessage("report id is malformed");

        this.store.report("123456789");
    }

    @Test
    public void whenReportWasntStored__whenGettingReport__thenGettingEmptyResult() throws Exception {
        assertThat(this.store.report("123456789-132456789-123456-456789-78").isPresent(), is(false));
    }

    @Test
    public void whenDumpWasStored__whenGettingReport__thenDescriptorIsRead() throws Exception {
        Report report = this.store.store(this.validReport()
                .name("service")
                .version("v1")
                .containerId("123456789")
                .build(),
                this.createFileWithContent("do you feel like we do ?"));

        assertThat(this.store.dump(report.id()).get().content().asBytes(), is("do you feel like we do ?".getBytes()));
        assertThat(this.store.dump(report.id()).get().contentType(), is("application/octet-stream"));
    }

    @Test
    public void whenIdIsMalformedBecauseNoDashFound__whenGettingDump__thenReportStoreExceptionIsRaised() throws Exception {
        this.expected.expect(ReportStore.ReportStoreException.class);
        this.expected.expectMessage("report id is malformed");

        this.store.dump("123456789");
    }

    @Test
    public void whenReportWasntStored__whenGettingDump__thenGettingEmptyResult() throws Exception {
        assertThat(this.store.dump("123456789-132456789-123456-456789-78").isPresent(), is(false));
    }

    private Report.Builder validReport() {
        return Report.builder()
                .reportedAt(UTC.now());
    }

    private String reportDirPath(Report report) {
        return "/report-store/" + report.reportedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String descriptorPath(Report report) {
        return String.format(
                    "%s/%s.json",
                    reportDirPath(report),
                    report.id()
            );
    }

    private String dumpPath(Report report) {
        return String.format(
                    "%s/%s.dump",
                    reportDirPath(report),
                    report.id()
            );
    }

    private byte [] readBytes(File file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            for(int read = in.read(buffer); read != -1 ; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
        }

        return out.toByteArray();
    }

    private OptionalFile createFileWithContent(String content) {
        return OptionalFile.of(org.codingmatters.rest.api.types.File.builder().content(
                Content.from(content.getBytes())
        ).build());
    }
}
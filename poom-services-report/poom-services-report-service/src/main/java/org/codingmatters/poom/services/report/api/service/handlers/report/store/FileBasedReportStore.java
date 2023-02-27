package org.codingmatters.poom.services.report.api.service.handlers.report.store;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.report.api.service.handlers.ReportStore;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.poom.services.report.api.types.json.ReportReader;
import org.codingmatters.poom.services.report.api.types.json.ReportWriter;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.rest.api.types.optional.OptionalFile;
import org.codingmatters.rest.io.Content;

import java.io.*;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class FileBasedReportStore implements ReportStore {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(FileBasedReportStore.class);

    private static final DateTimeFormatter ID_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private final File storage;
    private final JsonFactory jsonFactory;

    public FileBasedReportStore(File storage, JsonFactory jsonFactory) {
        this.storage = storage;
        this.jsonFactory = jsonFactory;
        this.storage.mkdirs();
    }

    @Override
    public Report store(Report report, OptionalFile dump) throws ReportStoreException {
        this.validate(report);

        String datePart = report.reportedAt().format(ID_DATE_FORMATTER);
        report = report.withId(String.format("%s-%s", datePart, UUID.randomUUID().toString()));

        File dir = this.prepareDirectory(datePart);
        this.storeDescriptor(report, dir);
        if(dump.content().isPresent()) {
            this.storeDump(report, dump.get(), dir);
        }

        return report;
    }

    @Override
    public Optional<Report> report(String id) throws ReportStoreException {
        File descriptorFile = this.reportAsset(id, ".json");
        if(! descriptorFile.exists()) return Optional.empty();

        return Optional.of(this.readReport(descriptorFile));
    }

    @Override
    public Optional<org.codingmatters.rest.api.types.File> dump(String id) throws ReportStoreException {
        File dumpFile = reportAsset(id, ".dump");
        if(! dumpFile.exists()) return Optional.empty();
        return Optional.of(org.codingmatters.rest.api.types.File.builder()
                            .content(Content.from(dumpFile))
                            .contentType("application/octet-stream")
                            .build());

//        try(FileInputStream in = new FileInputStream(descriptorFile); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//            byte [] buffer = new byte[1024];
//            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
//                out.write(buffer, 0, read);
//            }
//            out.flush();
//            out.close();
//            return Optional.of(org.codingmatters.rest.api.types.File.builder()
//                    .content(out.toByteArray())
//                    .contentType("application/octet-stream")
//                    .build());
//        } catch (IOException e) {
//            throw new ReportStoreException("failed reading dump file", e);
//        }
    }


    @Override
    public PagedEntityList<Report> all(long startIndex, long endIndex) throws RepositoryException {
        try {
            return this.filterReports(startIndex, endIndex, dateDir -> true, report -> true);
        } catch (ReportStoreException e) {
            throw new RepositoryException("error while filtering reports", e);
        }
    }

    @Override
    public PagedEntityList<Report> search(ReportQuery query, long startIndex, long endIndex) throws RepositoryException {
        try {
            return this.filterReports(
                    startIndex, endIndex,
                    new ReportedAtMatcher(query),
                    new ReportDescriptorMatcher(query));
        } catch (ReportStoreException e) {
            throw new RepositoryException("error while filtering reports", e);
        }
    }

    private File reportAsset(String id, String extension) throws ReportStoreException {
        int index = this.validateId(id);
        File dir = new File(this.storage, id.substring(0, index));
        return new File(dir, id + extension);
    }

    private int validateId(String id) throws ReportStoreException {
        int index = id.indexOf('-');
        if(index == -1) throw new ReportStoreException("report id is malformed");
        return index;
    }

    private File prepareDirectory(String datePart) throws ReportStoreException {
        File reportDir = new File(this.storage, datePart);
        reportDir.mkdirs();
        return reportDir;
    }

    private void storeDescriptor(Report report, File dir) throws ReportStoreException {
        File descriptor = new File(dir, report.id() + ".json");
        try {
            try (JsonGenerator generator = this.jsonFactory.createGenerator(new FileOutputStream(descriptor))) {
                new ReportWriter().write(generator, report);
                generator.flush();
            }
        } catch (IOException e) {
            throw new ReportStoreException("failed storing report descriptor", e);
        }
    }

    private void storeDump(Report report, org.codingmatters.rest.api.types.File file, File dir) throws ReportStoreException {
        try {
            try(InputStream in = file.inputStream() ; FileOutputStream out = new FileOutputStream(new File(dir, report.id() + ".dump"))) {
                byte [] buffer = new byte[1024];
                for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
        } catch (IOException e) {
            throw new ReportStoreException("failed storing dump", e);
        }
    }

    private void validate(Report report) throws ReportStoreException {
        if(! report.opt().reportedAt().isPresent()) {
            throw new ReportStoreException("missing reportedAt date");
        }
    }

    private Report readReport(File descriptorFile) throws ReportStoreException {
        try(FileInputStream in = new FileInputStream(descriptorFile); JsonParser parser = this.jsonFactory.createParser(in)) {
            return new ReportReader().read(parser);
        } catch (IOException e) {
            throw new ReportStoreException("failed to read report's descriptor", e);
        }
    }

    private PagedEntityList<Report> filterReports(long startIndex, long endIndex, Predicate<String> dateMatcher, Predicate<Report> reportPredicate) throws ReportStoreException {
        long total = 0;
        LinkedList<Entity<Report>> results = new LinkedList<>();
        for (File byDateFolder : this.storage.listFiles()) {
            if(dateMatcher.test(byDateFolder.getName())) {
                for (File descriptor : byDateFolder.listFiles(file -> file.getName().endsWith(".json"))) {
                    Report report = this.readReport(descriptor);
                    if (reportPredicate.test(report)) {
                        if (this.inPageRange(total, startIndex, endIndex)) {
                            results.add(new ImmutableEntity<>(report.id(), BigInteger.ONE, report));
                        }
                        total++;
                    }
                }
            }
        }

        if(results.isEmpty()) {
            return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, results);
        } else {
            return new PagedEntityList.DefaultPagedEntityList<Report>(
                    startIndex,
                    startIndex + results.size() - 1,
                    total,
                    results
            );
        }
    }

    private boolean inPageRange(long index, long startIndex, long endIndex) {
        return index >= startIndex && index <= endIndex;
    }


}

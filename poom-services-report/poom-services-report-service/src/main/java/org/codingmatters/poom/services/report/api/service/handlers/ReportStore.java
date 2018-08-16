package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.rest.api.types.File;
import org.codingmatters.rest.api.types.optional.OptionalFile;

import java.util.Optional;

public interface ReportStore extends EntityLister<Report, ReportQuery> {
    Report store(Report report, OptionalFile dump) throws ReportStoreException ;
    Optional<Report> report(String id) throws ReportStoreException ;
    Optional<File> dump(String id) throws ReportStoreException;

    class ReportStoreException extends Exception {
        public ReportStoreException(String s) {
            super(s);
        }

        public ReportStoreException(String s, Throwable throwable) {
            super(s, throwable);
        }
    }
}

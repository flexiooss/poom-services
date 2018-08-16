package org.codingmatters.poom.services.report.api.service.handlers.report.store;

import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Predicate;

public class ReportDescriptorMatcher implements Predicate<Report> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final ReportQuery query;

    public ReportDescriptorMatcher(ReportQuery query) {
        this.query = query;
    }

    @Override
    public boolean test(Report report) {
        return this.matchesString(report.opt().name(), this.query.opt().name()) &&
                this.matchesString(report.opt().containerId(), this.query.opt().containerId()) &&
                this.matchesString(report.opt().version(), this.query.opt().version()) &&
                this.matchesString(report.opt().mainClass(), this.query.opt().mainClass()) &&
                this.matchesString(report.opt().exitStatus(), this.query.opt().exitStatus()) &&
                this.matchesBoolean(report.opt().hasDump(), this.query.opt().hasDump()) &&
                this.matchesString(this.dateAsString(report.opt().start()), this.query.opt().start()) &&
                this.matchesString(this.dateAsString(report.opt().end()), this.query.opt().end());
    }

    private Optional<String> dateAsString(Optional<LocalDateTime> date) {
        if(date.isPresent()) {
            return Optional.of(date.get().format(DATE_FORMATTER));
        } else {
            return Optional.empty();
        }
    }

    private boolean matchesString(Optional<String> value, Optional<String> pattern) {
        if(! pattern.isPresent()) return true;
        if(! value.isPresent()) return ! pattern.isPresent();

        return value.get().startsWith(pattern.get());
    }

    private boolean matchesBoolean(Optional<Boolean> value, Optional<Boolean> pattern) {
        if(! pattern.isPresent()) return true;
        if(! value.isPresent()) return ! pattern.isPresent();

        return value.get().equals(pattern.get());
    }


}

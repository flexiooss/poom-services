package org.codingmatters.poom.services.report.api.service.handlers.report.store;

import org.codingmatters.poom.services.report.api.types.ReportQuery;

import java.util.function.Predicate;

public class ReportedAtMatcher implements Predicate<String> {

    private final ReportQuery query;

    public ReportedAtMatcher(ReportQuery query) {
        this.query = query;
    }

    @Override
    public boolean test(String date) {
        if(date.isEmpty()) return ! query.opt().reportedAt().isPresent();
        if(query.opt().reportedAt().isPresent()) {
            return date.startsWith(query.reportedAt()
                    .replaceAll("-", "")
                    .replaceAll(":", "")
                    .replaceAll("\\.", "")
                    .replaceAll(" ", "")
            );
        } else {
            return true;
        }
    }
}

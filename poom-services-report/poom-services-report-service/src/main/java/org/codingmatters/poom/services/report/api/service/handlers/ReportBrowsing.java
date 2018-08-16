package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.report.api.ReportsGetRequest;
import org.codingmatters.poom.services.report.api.ReportsGetResponse;
import org.codingmatters.poom.services.report.api.types.Error;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ReportBrowsing implements Function<ReportsGetRequest, ReportsGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReportBrowsing.class);

    private final ReportStore store;

    public ReportBrowsing(ReportStore store) {
        this.store = store;
    }

    @Override
    public ReportsGetResponse apply(ReportsGetRequest request) {
        try {
            Optional<ReportQuery> query = new QueryBuilder().from(request);
            Rfc7233Pager.Page<Report> page = Rfc7233Pager
                    .forRequestedRange(request.range())
                    .unit("Report")
                    .maxPageSize(100)
                    .pager(this.store)
                    .page(query);
            if(page.isValid()) {
                if(page.isPartial()) {
                    return ReportsGetResponse.builder()
                            .status206(status -> status
                                    .acceptRange(page.acceptRange())
                                    .contentRange(page.contentRange())
                                    .payload(page.list().valueList())
                            )
                            .build();
                } else {
                    return ReportsGetResponse.builder()
                            .status200(status -> status
                                    .acceptRange(page.acceptRange())
                                    .contentRange(page.contentRange())
                                    .payload(page.list().valueList())
                            )
                            .build();
                }
            } else {
                return ReportsGetResponse.builder()
                        .status500(status -> status.payload(error -> error
                                .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                .token(log.tokenized().info("invalid range for request {} : {}", request, page.validationMessage()))
                                .description(page.validationMessage())
                        ))
                        .build();
            }
        } catch (RepositoryException e) {
            return ReportsGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error querying report store while handling request " + request, e))
                    ))
                    .build();
        }
    }

    static public class QueryBuilder {
        Optional<ReportQuery> from(ReportsGetRequest request) {
            ReportQuery.Builder builder = new ReportQuery.Builder();
            AtomicBoolean hasParameters = new AtomicBoolean(false);

            request.opt().reportedAt().ifPresent(s -> {builder.reportedAt(s); hasParameters.set(true);});
            request.opt().name().ifPresent(s -> {builder.name(s); hasParameters.set(true);});
            request.opt().version().ifPresent(s -> {builder.version(s); hasParameters.set(true);});
            request.opt().mainClass().ifPresent(s -> {builder.mainClass(s); hasParameters.set(true);});
            request.opt().containerId().ifPresent(s -> {builder.containerId(s); hasParameters.set(true);});
            request.opt().start().ifPresent(s -> {builder.start(s); hasParameters.set(true);});
            request.opt().end().ifPresent(s -> {builder.end(s); hasParameters.set(true);});
            request.opt().exitStatus().ifPresent(s -> {builder.exitStatus(s); hasParameters.set(true);});
            request.opt().hasDump().ifPresent(b -> {builder.hasDump(b); hasParameters.set(true);});

            return hasParameters.get() ? Optional.of(builder.build()) : Optional.empty();
        }
    }
}

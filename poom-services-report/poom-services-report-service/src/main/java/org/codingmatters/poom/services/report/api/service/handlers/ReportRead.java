package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.report.api.ReportGetRequest;
import org.codingmatters.poom.services.report.api.ReportGetResponse;
import org.codingmatters.poom.services.report.api.types.Error;
import org.codingmatters.poom.services.report.api.types.Report;

import java.util.Optional;
import java.util.function.Function;

public class ReportRead implements Function<ReportGetRequest, ReportGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReportRead.class);

    private final ReportStore store;

    public ReportRead(ReportStore store) {
        this.store = store;
    }

    @Override
    public ReportGetResponse apply(ReportGetRequest request) {
        try {
            Optional<Report> report = this.store.report(request.reportId());
            if(report.isPresent()) {
                log.audit().info("report read : {}", report);
                return ReportGetResponse.builder()
                        .status200(status -> status.xEntityId(request.reportId()).payload(report.get()))
                        .build();
            } else {
                return ReportGetResponse.builder()
                    .status404(status -> status.payload(error -> error
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .token(log.tokenized().error("no report found for report get request : {}", request))
                            .description("no such record")
                    ))
                    .build();
            }
        } catch (ReportStore.ReportStoreException e) {
            return ReportGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error handling report get request : " + request, e))
                    ))
                    .build();
        }
    }
}

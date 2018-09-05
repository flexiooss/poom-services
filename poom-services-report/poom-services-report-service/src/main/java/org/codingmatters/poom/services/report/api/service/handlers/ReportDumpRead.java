package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.report.api.ReportDumpGetRequest;
import org.codingmatters.poom.services.report.api.ReportDumpGetResponse;
import org.codingmatters.poom.services.report.api.types.Error;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.rest.api.types.File;

import java.util.Optional;
import java.util.function.Function;

public class ReportDumpRead implements Function<ReportDumpGetRequest, ReportDumpGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReportDumpRead.class);

    private final ReportStore store;

    public ReportDumpRead(ReportStore store) {
        this.store = store;
    }

    @Override
    public ReportDumpGetResponse apply(ReportDumpGetRequest request) {
        try {
            Optional<Report> report = this.store.report(request.reportId());
            if(report.isPresent()) {
                if(report.get().hasDump()) {
                    Optional<File> dump = this.store.dump(report.get().id());
                    if(dump.isPresent()) {
                        return ReportDumpGetResponse.builder()
                                .status200(status -> status
                                        .xEntityId(report.get().id())
                                        .payload(dump.get()))
                                .build();
                    } else {
                        return ReportDumpGetResponse.builder()
                                .status404(status -> status.payload(error -> error
                                        .code(Error.Code.RESOURCE_NOT_FOUND)
                                        .token(log.tokenized().error("report should have a dump, but, dump file is not found : {}", request))
                                        .description("report dump not found")
                                ))
                                .build();
                    }
                } else {
                    return ReportDumpGetResponse.builder()
                            .status404(status -> status.payload(error -> error
                                    .code(Error.Code.RESOURCE_NOT_FOUND)
                                    .token(log.tokenized().error("report doesn't have a dump : {}", request))
                                    .description("report doesn't have a dump")
                            ))
                            .build();
                }
            } else {
                return ReportDumpGetResponse.builder()
                        .status404(status -> status.payload(error -> error
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .token(log.tokenized().error("no report found for report get request : {}", request))
                                .description("no such report")
                        ))
                        .build();
            }
        } catch (ReportStore.ReportStoreException e) {
            return ReportDumpGetResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error handling report get request : " + request, e))
                    ))
                    .build();
        }
    }
}

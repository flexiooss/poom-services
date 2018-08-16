package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.report.api.ReportsPostRequest;
import org.codingmatters.poom.services.report.api.ReportsPostResponse;
import org.codingmatters.poom.services.report.api.optional.OptionalReportsPostResponse;
import org.codingmatters.poom.services.report.api.reportspostresponse.Status201;
import org.codingmatters.poom.services.report.api.types.Error;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.support.date.UTC;

import java.util.Optional;
import java.util.function.Function;

public class ReportCreation implements Function<ReportsPostRequest, ReportsPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReportCreation.class);

    private final ReportStore store;

    public ReportCreation(ReportStore store) {
        this.store = store;
    }

    @Override
    public ReportsPostResponse apply(ReportsPostRequest request) {
        Report report = Report.builder()
                .name(request.xName())
                .version(request.xVersion())
                .mainClass(request.xMainClass())
                .containerId(request.xContainerId())
                .start(request.xStart())
                .end(request.xEnd())
                .exitStatus(request.xExitStatus())
                .hasDump(request.opt().payload().content().isPresent())
                .reportedAt(UTC.now())
                .build();

        OptionalReportsPostResponse invalid = this.validate(request);
        if(invalid.isPresent()) {
            return invalid.get();
        }

        log.info("report creation requested {} dump, storing {}", report.hasDump() ? "with" : "without", report);
        try {
            report = this.store.store(report, request.opt().payload());
        } catch (ReportStore.ReportStoreException e) {
            return ReportsPostResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error while storing request " + request, e))
                            .description("unexpected error, see logs.")
                    ))
                    .build();
        }

        return ReportsPostResponse.builder().status201(Status201.builder()
                .payload(report)
                .build()).build();
    }

    private OptionalReportsPostResponse validate(ReportsPostRequest request) {
        return this
                .mandatoryHeader(request, "x-start", request.opt().xStart())
                .orElse(this.mandatoryHeader(request, "x-end", request.opt().xEnd())
                .orElse(this.mandatoryHeader(request, "x-version", request.opt().xVersion())
                .orElse(this.mandatoryHeader(request, "x-name", request.opt().xName())
                .orElse(this.mandatoryHeader(request, "x-exit-status", request.opt().xExitStatus())
                .orElse(OptionalReportsPostResponse.of(null))))));
    }

    private Optional<OptionalReportsPostResponse> mandatoryHeader(ReportsPostRequest request, String name, Optional value) {
        if(! value.isPresent()) {
            return Optional.of(this.invalid(name + " header must be setted", log.tokenized().info("invalid request, {} must be setted, was {}", name, request)));
        }
        return Optional.ofNullable(null);
    }

    private OptionalReportsPostResponse invalid(String description, String token) {
        return OptionalReportsPostResponse.of(ReportsPostResponse.builder()
                .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_REQUEST).description(description).token(token)))
                .build());
    }
}

package org.codingmatters.poom.services.report.api.service.handlers;

import okhttp3.Request;
import okhttp3.Response;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.report.api.ReportsPostRequest;
import org.codingmatters.poom.services.report.api.ReportsPostResponse;
import org.codingmatters.poom.services.report.api.optional.OptionalReportsPostResponse;
import org.codingmatters.poom.services.report.api.reportspostresponse.Status201;
import org.codingmatters.poom.services.report.api.types.Error;
import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class ReportCreation implements Function<ReportsPostRequest, ReportsPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReportCreation.class);

    private final ReportStore store;
    private final HttpClientWrapper client = OkHttpClientWrapper.build();

    private final ExecutorService callbackPool;
    private final Optional<String> callbackUrl;

    public ReportCreation(ReportStore store, Optional<String> callbackUrl, ExecutorService callbackPool) {
        this.store = store;
        this.callbackUrl = callbackUrl;
        this.callbackPool = callbackPool;
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
                .hasDump(request.opt().payload().content().isPresent() && request.payload().content().length() > 0)
                .reportedAt(UTC.now())
                .build();

        OptionalReportsPostResponse invalid = this.validate(request);
        if(invalid.isPresent()) {
            return invalid.get();
        }

        try {
            Report result = this.store.store(report, request.opt().payload());
            log.info("report stored {} dump, storing {}", report.hasDump() ? "with" : "without", report);
            if(this.callbackPool != null) {
                this.callbackPool.submit(() -> this.notify(result));
            }
            return ReportsPostResponse.builder().status201(Status201.builder()
                    .payload(result)
                    .build()).build();
        } catch (ReportStore.ReportStoreException e) {
            return ReportsPostResponse.builder()
                    .status500(status -> status.payload(error -> error
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error while storing request " + request, e))
                            .description("unexpected error, see logs.")
                    ))
                    .build();
        }


    }

    private void notify(Report report) {
        if(this.callbackUrl.isPresent()) {
            try {
                String url = this.callbackUrl.get();
                url += "?" + this.queryParameter("name", report.name());
                url += "&" + this.queryParameter("version", report.version());
                url += "&" + this.queryParameter("main-class", report.mainClass());
                url += "&" + this.queryParameter("container-id", report.containerId());
                url += "&" + this.queryParameter("start", this.formatted(report.start()));
                url += "&" + this.queryParameter("end", this.formatted(report.end()));
                url += "&" + this.queryParameter("exit-status", report.exitStatus());
                url += "&" + this.queryParameter("has-dump", report.hasDump().toString());
                url += "&" + this.queryParameter("reported-at", this.formatted(report.reportedAt()));


                Response response = this.client.execute(new Request.Builder()
                        .url(url)
                        .build());
                if(response.code() == 200) {
                    log.info("notified report to {}", url);
                } else {
                    log.error("error notifying to {}, response was {}", url, response);
                }
            } catch (IOException e) {
                log.error("error while notifying report to " + this.callbackUrl.get(), e);
            }
        } else {
            log.debug("not notifying report as no callback url defined");
        }
    }

    private String formatted(LocalDateTime date) {
        return date != null ? date.format(Processor.Formatters.DATETIMEONLY.formatter) : null;
    }

    private String queryParameter(String name, String value) throws UnsupportedEncodingException {
        return String.format("%s=%s", name, URLEncoder.encode(value, "UTF-8"));
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

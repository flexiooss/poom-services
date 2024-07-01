package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by nelt on 7/13/17.
 */
public interface CollectionGetProtocol<V, Q, Req, Resp> extends RepositoryRequestProtocol<V, Q, Req, Resp>, Function<Req, Resp> {
    default int maxPageSize() {return 50;}

    String rfc7233Unit();
    String rfc7233Range(Req request);

    Q parseQuery(Req request);

    Resp partialList(Rfc7233Pager.Page<V> page, Req request);
    Resp completeList(Rfc7233Pager.Page<V> page, Req request);
    Resp invalidRangeQuery(Rfc7233Pager.Page<V> page, String errorToken, Req request);
    Resp unexpectedError(RepositoryException e, String errorToken);

    default String rfc7233Unit(Req request) {
        return this.rfc7233Unit();
    }

    default Resp apply(Req request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            Optional<Resp> invalidResponse = this.validate(request);
            if(invalidResponse.isPresent()) {
                return invalidResponse.get();
            }

            try {
                Rfc7233Pager<V, Q> pager = Rfc7233Pager.forRequestedRange(this.rfc7233Range(request))
                        .unit(this.rfc7233Unit())
                        .maxPageSize(this.maxPageSize())
                        .pager(this.repository(request));

                Q query = this.parseQuery(request);

                Rfc7233Pager.Page<V> page;
                if(query != null) {
                    this.log().debug("{} list requested with filter : {}", this.rfc7233Unit(), query);
                    page = pager.page(query);
                } else {
                    page = pager.page();
                }

                if(page.isValid()) {
                    if (page.isPartial()) {
                        this.log().debug("returning partial {} list ({})", this.rfc7233Unit(), page.contentRange());
                        return this.partialList(page, request);
                    } else {
                        this.log().debug("returning complete {} list ({})", this.rfc7233Unit(), page.contentRange());
                        return this.completeList(page, request);
                    }
                } else {
                    String errorToken = UUID.randomUUID().toString();
                    MDC.put("error-token", errorToken);
                    this.log().info(page.validationMessage() + " (requested range: {})", page.requestedRange());
                    return this.invalidRangeQuery(page, errorToken, request);
                }
            } catch (RepositoryException e) {
                String errorToken = UUID.randomUUID().toString();
                MDC.put("error-token", errorToken);
                this.log().error("unexpected error while handling " + this.rfc7233Unit() + " list query", e);
                return this.unexpectedError(e, errorToken);
            }
        }
    }
}
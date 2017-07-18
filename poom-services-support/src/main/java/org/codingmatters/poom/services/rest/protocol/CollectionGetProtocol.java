package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Function;

/**
 * Created by nelt on 7/13/17.
 */
public interface CollectionGetProtocol<V, Q, Req, Resp> extends Function<Req, Resp> {

    Repository<V, Q> repository();

    int maxPageSize();

    String rfc7233Unit();
    String rfc7233Range(Req request);

    Q parseQuery(Req request);

    Resp partialList(Rfc7233Pager.Page<V> page);
    Resp completeList(Rfc7233Pager.Page<V> page);
    Resp invalidRangeQuery(Rfc7233Pager.Page<V> page, String errorToken);
    Resp unexpectedError(RepositoryException e, String errorToken);

    default Resp apply(Req request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            try {
                Rfc7233Pager<V, Q> pager = Rfc7233Pager.forRequestedRange(this.rfc7233Range(request))
                        .unit(this.rfc7233Unit())
                        .maxPageSize(this.maxPageSize())
                        .pager(this.repository());

                Q query = this.parseQuery(request);

                Rfc7233Pager.Page<V> page;
                if(query != null) {
                    page = pager.page(query);
                } else {
                    page = pager.page();
                }

                if(page.isValid()) {
                    if (page.isPartial()) {
                        return this.partialList(page);
                    } else {
                        return this.completeList(page);
                    }
                } else {
                    String errorToken = UUID.randomUUID().toString();
                    MDC.put("error-token", errorToken);
                    return this.invalidRangeQuery(page, errorToken);
                }
            } catch (RepositoryException e) {
                String errorToken = UUID.randomUUID().toString();
                MDC.put("error-token", errorToken);
                return this.unexpectedError(e, errorToken);
            }
        }
    }
}

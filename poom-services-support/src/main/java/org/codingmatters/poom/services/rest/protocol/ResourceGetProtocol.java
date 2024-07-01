package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by nelt on 7/18/17.
 */
public interface ResourceGetProtocol<V, Q, Req, Resp> extends RepositoryRequestProtocol<V, Q, Req, Resp>, Function<Req, Resp> {
    String entityId(Req request);

    Resp entityFound(Req request, Entity<V> entity);
    Resp entityNotFound(String errorToken);
    Resp unexpectedError(String errorToken);

    default Entity<V> resolveEntity(String entityId, Req request) throws RepositoryException {
        if(entityId == null) {
            throw new RepositoryException("cannot find an entity given a null id");
        }
        return this.repository(request).retrieve(entityId);
    }

    @Override
    default Resp apply(Req request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());

            Optional<Resp> invalidResponse = this.validate(request);
            if(invalidResponse.isPresent()) {
                return invalidResponse.get();
            }
            String entityId = this.entityId(request);
            try {
                Entity<V> entity = this.resolveEntity(entityId, request);
                if (entity != null) {
                    log().debug("request for entity {} returns version {}", entity.id(), entity.version());
                    return this.entityFound(request, entity);
                } else {
                    String errorToken = UUID.randomUUID().toString();
                    MDC.put("error-token", errorToken);
                    log().debug("no entity found with id: {}", entityId);

                    return this.entityNotFound(errorToken);
                }
            } catch (RepositoryException e) {String errorToken = UUID.randomUUID().toString();
                MDC.put("error-token", errorToken);

                log().error("unexpected error while looking up entity : " + entityId, e);
                return this.unexpectedError(errorToken);
            }
        }
    }
}

package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by nelt on 7/18/17.
 */
public interface ResourceGetProtocol<V, Q, Req, Resp> extends Function<Req, Resp> {

    default Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    Repository<V, Q> repository(Req request);
    String entityId(Req request);

    Resp entityFound(Entity<V> entity);
    Resp entityNotFound(String errorToken);
    Resp unexpectedError(String errorToken);

    default Optional<Resp> validate(Req request) { return Optional.ofNullable(null); }

    @Override
    default Resp apply(Req request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());

            Optional<Resp> invalidResponse = this.validate(request);
            if(invalidResponse.isPresent()) {
                return invalidResponse.get();
            }

            try {
                Entity<V> entity = this.repository(request).retrieve(this.entityId(request));
                if (entity != null) {
                    log().info("request for entity {} returns version {}", entity.id(), entity.version());
                    return this.entityFound(entity);
                } else {
                    String errorToken = UUID.randomUUID().toString();
                    MDC.put("error-token", errorToken);
                    log().info("no entity found with id: {}", this.entityId(request));

                    return this.entityNotFound(errorToken);
                }
            } catch (RepositoryException e) {String errorToken = UUID.randomUUID().toString();
                MDC.put("error-token", errorToken);

                log().error("unexpected error while looking up entity : {}", this.entityId(request));
                log().debug("unexpected exception", e);
                return this.unexpectedError(errorToken);
            }
        }
    }
}

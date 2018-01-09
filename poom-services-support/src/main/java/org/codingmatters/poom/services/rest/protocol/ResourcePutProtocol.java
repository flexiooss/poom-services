package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.change.Change;
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

public interface ResourcePutProtocol<V, Q, Req, Resp> extends Function<Req, Resp> {

    default Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    Repository<V, Q> repository(Req request);

    String entityId(Req request);
    Change<V> valueUpdate(Req request, Entity<V> entity);

    Resp entityUpdated(Entity<V> entity);
    Resp invalidUpdate(Change<V> change, String errorToken);
    Resp entityNotFound(String errorToken);
    Resp unexpectedError(RepositoryException e, String errorToken);

    default Optional<Resp> validate(Req request) { return Optional.ofNullable(null); }

    default Resp apply(Req request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());

            Optional<Resp> invalidResponse = this.validate(request);
            if(invalidResponse.isPresent()) {
                return invalidResponse.get();
            }

            try {
                Repository<V, Q> repository = this.repository(request);
                Entity<V> entity = repository.retrieve(this.entityId(request));
                if(entity != null) {
                    MDC.put("entity-id", entity.id());

                    Change<V> change = this.valueUpdate(request, entity);

                    if(change.validation().isValid()) {
                        V newValue = change.applied();

                        entity = repository.update(entity, newValue);

                        this.log().info("entity updated");
                        return this.entityUpdated(entity);
                    } else {
                        String errorToken = UUID.randomUUID().toString();
                        MDC.put("error-token", errorToken);
                        this.log().info("illegal entity change: {}", change.validation().message());
                        return this.invalidUpdate(change, errorToken);
                    }
                } else {
                    String errorToken = UUID.randomUUID().toString();
                    MDC.put("error-token", errorToken);
                    this.log().info("no entity found with id: {}", this.entityId(request));

                    return this.entityNotFound(errorToken);
                }
            } catch (RepositoryException e) {
                String errorToken = UUID.randomUUID().toString();
                MDC.put("error-token", errorToken);

                this.log().error("unexpected error while looking up entity: {}", this.entityId(request));
                this.log().debug("unexpected exception", e);
                return this.unexpectedError(e, errorToken);
            }
        }
    }
}

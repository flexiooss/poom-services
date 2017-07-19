package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Function;

public interface ResourcePutProtocol<V, Q, Req, Resp> extends Function<Req, Resp> {
    Logger log();
    Repository<V, Q> repository();

    String entityId(Req request);
    Change<V> valueUpdate(Req request, Entity<V> entity);

    Resp entityUpdated(Entity<V> entity);
    Resp invalidUpdate(Change<V> change, String errorToken);
    Resp entityNotFound(String errorToken);
    Resp unexpectedError(RepositoryException e, String errorToken);

    default Resp apply(Req request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            try {
                Entity<V> entity = this.repository().retrieve(this.entityId(request));
                if(entity != null) {
                    MDC.put("entity-id", entity.id());

                    Change<V> change = this.valueUpdate(request, entity);

                    if(change.validation().isValid()) {
                        V newValue = change.applied();

                        entity = this.repository().update(entity, newValue);

                        this.log().info("entity updated");
                        return this.entityUpdated(entity);
                    } else {
                        String errorToken = UUID.randomUUID().toString();
                        MDC.put("error-token", errorToken);
                        this.log().info("illegal job change: {}", change.validation().message());
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

                this.log().error("unexpected error while looking up job : {}", this.entityId(request));
                this.log().debug("unexpected exception", e);
                return this.unexpectedError(e, errorToken);
            }
        }
    }
}

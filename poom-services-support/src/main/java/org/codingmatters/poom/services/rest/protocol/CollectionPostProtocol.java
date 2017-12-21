package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface CollectionPostProtocol<V, Q, Req, Resp> extends Function<Req, Resp> {

    Logger log();
    Repository<V, Q> repository();

    Change<V> valueCreation(Req request);
    Resp entityCreated(Change<V> creation, Entity<V> entity);
    Resp invalidCreation(Change<V> creation, String errorToken);
    Resp unexpectedError(Change<V> creation, RepositoryException e, String errorToken);

    default Optional<Resp> validate(Req request) { return Optional.ofNullable(null); }

    default Resp apply(Req request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            Optional<Resp> invalidResponse = this.validate(request);
            if(invalidResponse.isPresent()) {
                return invalidResponse.get();
            }

            Change<V> creation = valueCreation(request);
            if (creation.validation().isValid()) {
                try {
                    Entity<V> entity = this.repository().create(creation.applied());
                    MDC.put("entity-id", entity.id());
                    this.log().info("created entity {}", entity.id());

                    return this.entityCreated(creation, entity);
                } catch (RepositoryException e) {
                    String errorToken = UUID.randomUUID().toString();
                    MDC.put("error-token", errorToken);
                    this.log().error("unexpected error while creating job", e);

                    return this.unexpectedError(creation, e, errorToken);
                }

            } else {
                String errorToken = UUID.randomUUID().toString();
                MDC.put("error-token", errorToken);
                this.log().info("job creation request with invalid job spec: {}", creation.validation().message());

                return this.invalidCreation(creation, errorToken);
            }
        }
    }

}

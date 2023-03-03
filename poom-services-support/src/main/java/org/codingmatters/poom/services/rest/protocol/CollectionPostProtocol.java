package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface CollectionPostProtocol<V, Q, Req, Resp> extends RepositoryRequestProtocol<V, Q, Req, Resp>, Function<Req, Resp> {

    Change<V> valueCreation(Req request);
    Resp entityCreated(Req request, Change<V> creation, Entity<V> entity);
    Resp invalidCreation(Change<V> creation, String errorToken);
    Resp unexpectedError(Change<V> creation, RepositoryException e, String errorToken);

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
                    Entity<V> entity = this.repository(request).create(creation.applied());
                    MDC.put("entity-id", entity.id());
                    this.log().info("created entity {}", entity.id());

                    return this.entityCreated(request, creation, entity);
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

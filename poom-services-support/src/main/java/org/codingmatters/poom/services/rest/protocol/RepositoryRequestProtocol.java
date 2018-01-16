package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.repositories.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public interface RepositoryRequestProtocol<V, Q, Req, Resp> {
    default Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    Repository<V, Q> repository(Req request);

    default Optional<Resp> validate(Req request) { return Optional.ofNullable(null); }
}

package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;

public interface EntityRetriever<EntityType> extends EntityRepositoryConfig {
    Optional<Entity<EntityType>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
}

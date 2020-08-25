package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.servives.domain.entities.Entity;

public interface EntityUpdater<EntityType, UpdateType> extends EntityRepositoryConfig {
    Entity<EntityType> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
}

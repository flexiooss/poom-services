package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.servives.domain.entities.Entity;

public interface EntityCreator<EntityType, CreationType> extends EntityRepositoryConfig {
    Entity<EntityType> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException;
}

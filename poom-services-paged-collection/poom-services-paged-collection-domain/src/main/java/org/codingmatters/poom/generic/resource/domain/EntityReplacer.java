package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.servives.domain.entities.Entity;

public interface EntityReplacer<EntityType, ReplaceType> extends EntityRepositoryConfig {
    Entity<EntityType> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
}

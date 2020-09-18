package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;

public interface EntityDeleter extends EntityRepositoryConfig {
    void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException;
}

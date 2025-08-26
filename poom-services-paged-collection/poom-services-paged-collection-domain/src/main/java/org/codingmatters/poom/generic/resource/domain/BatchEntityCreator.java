package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;

public interface BatchEntityCreator<CreationType> extends EntityRepositoryConfig {
    BatchCreateResponse createEntitiesFrom(CreationType ... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException;
}

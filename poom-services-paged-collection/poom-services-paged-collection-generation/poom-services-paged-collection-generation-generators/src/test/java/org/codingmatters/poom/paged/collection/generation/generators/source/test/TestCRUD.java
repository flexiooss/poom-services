package org.codingmatters.poom.paged.collection.generation.generators.source.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.generated.api.types.Create;
import org.generated.api.types.Replace;
import org.generated.api.types.Update;

import java.util.Optional;

public class TestCRUD implements PagedCollectionAdapter.CRUD<org.generated.api.types.Entity, Create, Replace, Update> {

    @Override
    public String entityType() {
        return "TestType";
    }

    @Override
    public String entityRepositoryUrl() {
        return "flexio-api://api/collection";
    }

    @Override
    public Optional<Entity<org.generated.api.types.Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }
}

package org.codingmatters.poom.paged.collection.generation.generators.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.generated.api.types.Create;
import org.generated.api.types.Replace;
import org.generated.api.types.Update;

import java.util.Optional;
import java.util.Set;

public class TestCRUD implements PagedCollectionAdapter.CRUD<org.generated.api.types.Entity, Create, Replace, Update> {

    @Override
    public Set<Action> supportedActions() {
        return Action.all;
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
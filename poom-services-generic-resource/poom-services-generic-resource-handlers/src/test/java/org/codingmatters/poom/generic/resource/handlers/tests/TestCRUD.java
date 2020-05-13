package org.codingmatters.poom.generic.resource.handlers.tests;

import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Optional;
import java.util.Set;

public class TestCRUD implements GenericResourceAdapter.CRUD<ObjectValue,ObjectValue, ObjectValue, ObjectValue> {

    @Override
    public Set<Action> supportedActions() {
        return Action.all;
    }

    @Override
    public String entityRepositoryUrl() {
        return "flexio-api://api/collection";
    }

    @Override
    public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public Entity<ObjectValue> replaceEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }

    @Override
    public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new AssertionError("NYIML");
    }
}

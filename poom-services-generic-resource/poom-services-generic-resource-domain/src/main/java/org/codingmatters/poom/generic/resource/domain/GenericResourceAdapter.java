package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;
import java.util.Set;

public interface GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> {
    @FunctionalInterface
    interface Provider<EntityTpe, CreationType, ReplaceType, UpdateType> {
        GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> adapter() throws Exception;
    }

    CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> crud();
    Pager<EntityTpe> pager();

    interface Pager<EntityTpe> {
        String unit();
        int maxPageSize();
        EntityLister<EntityTpe, PropertyQuery> lister();
    }

    interface CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> {
        String entityRepositoryUrl();
        Set<Action> supportedActions();

        Optional<Entity<EntityTpe>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityTpe> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityTpe> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityTpe> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
    }
}

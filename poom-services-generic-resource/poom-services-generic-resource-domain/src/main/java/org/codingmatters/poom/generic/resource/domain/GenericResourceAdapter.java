package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;
import java.util.Set;

public interface GenericResourceAdapter<EntityTpe> {
    @FunctionalInterface
    interface Provider<EntityTpe> {
        GenericResourceAdapter<EntityTpe> adapter() throws Exception;
    }

    CRUD<EntityTpe> crud();
    Pager<EntityTpe> pager();

    interface Pager<EntityTpe> {
        String unit();
        int maxPageSize();
        EntityLister<EntityTpe, PropertyQuery> lister();
    }

    interface CRUD<EntityTpe> {
        String entityRepositoryUrl();
        Set<Action> supportedActions();

        Optional<Entity<EntityTpe>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityTpe> createEntityFrom(EntityTpe value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityTpe> replaceEntityWith(String id, EntityTpe value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityTpe> updateEntityWith(String id, EntityTpe value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;

    }
}

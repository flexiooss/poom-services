package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.impl.BadRequestAdapter;
import org.codingmatters.poom.generic.resource.domain.impl.DefaultAdapterBuilder;
import org.codingmatters.poom.generic.resource.domain.impl.NotFoundAdapter;
import org.codingmatters.poom.generic.resource.domain.impl.UnexpectedExceptionAdapter;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;
import java.util.Set;

public interface PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> {
    @FunctionalInterface
    interface Provider<EntityTpe, CreationType, ReplaceType, UpdateType> {
        PagedCollectionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> adapter() throws Exception;
    }

    CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud();
    Pager<EntityType> pager();

    interface Pager<EntityType> {
        String unit();
        int maxPageSize();
        EntityLister<EntityType, PropertyQuery> lister();
    }

    interface CRUD<EntityType, CreationType, ReplaceType, UpdateType> {
        String entityRepositoryUrl();
        Set<Action> supportedActions();

        Optional<Entity<EntityType>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityType> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityType> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        Entity<EntityType> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
        void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException;
    }

    static <EntityType, CreationType, ReplaceType, UpdateType> DefaultAdapterBuilder<EntityType, CreationType, ReplaceType, UpdateType> builder() {
        return new DefaultAdapterBuilder<>();
    }

    static <EntityType, CreationType, ReplaceType, UpdateType> PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> badRequestAdapter() {
        return new BadRequestAdapter<>();
    }

    static <EntityType, CreationType, ReplaceType, UpdateType> PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> notFoundAdapter() {
        return new NotFoundAdapter<>();
    }

    static <EntityType, CreationType, ReplaceType, UpdateType> PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> unexpectedExceptionAdapter() {
        return new UnexpectedExceptionAdapter<>();
    }

}

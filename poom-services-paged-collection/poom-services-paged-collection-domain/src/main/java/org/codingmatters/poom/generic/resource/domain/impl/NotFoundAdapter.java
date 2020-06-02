package org.codingmatters.poom.generic.resource.domain.impl;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class NotFoundAdapter<EntityType, CreationType, ReplaceType, UpdateType> implements PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> {

    private CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud = new CRUD<EntityType, CreationType, ReplaceType, UpdateType>() {
        @Override
        public String entityRepositoryUrl() {
            return null;
        }

        @Override
        public Set<Action> supportedActions() {
            return Action.all;
        }

        @Override
        public Optional<Entity<EntityType>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public Entity<EntityType> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public Entity<EntityType> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public Entity<EntityType> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }
    };
    private Pager<EntityType> pager = new Pager<EntityType>() {
        @Override
        public String unit() {
            return null;
        }

        @Override
        public int maxPageSize() {
            return 0;
        }

        @Override
        public EntityLister<EntityType, PropertyQuery> lister() {
            return new EntityLister<EntityType, PropertyQuery>() {
                @Override
                public PagedEntityList<EntityType> all(long startIndex, long endIndex) throws RepositoryException {
                    return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, Collections.emptyList());
                }

                @Override
                public PagedEntityList<EntityType> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
                    return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, Collections.emptyList());
                }
            };
        }
    };

    @Override
    public CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud() {
        return this.crud;
    }

    @Override
    public Pager<EntityType> pager() {
        return this.pager;
    }
}

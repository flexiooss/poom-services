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

public class UnexpectedExceptionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> implements PagedCollectionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> {

    private CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> crud = new CRUD<EntityTpe, CreationType, ReplaceType, UpdateType>() {
        @Override
        public String entityRepositoryUrl() {
            return null;
        }

        @Override
        public Set<Action> supportedActions() {
            return Action.all;
        }

        @Override
        public Optional<Entity<EntityTpe>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new UnexpectedException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public Entity<EntityTpe> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new UnexpectedException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public Entity<EntityTpe> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new UnexpectedException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public Entity<EntityTpe> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new UnexpectedException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }

        @Override
        public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            throw new UnexpectedException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
        }
    };
    private Pager<EntityTpe> pager = new Pager<EntityTpe>() {
        @Override
        public String unit() {
            return null;
        }

        @Override
        public int maxPageSize() {
            return 0;
        }

        @Override
        public EntityLister<EntityTpe, PropertyQuery> lister() {
            return new EntityLister<EntityTpe, PropertyQuery>() {
                @Override
                public PagedEntityList<EntityTpe> all(long startIndex, long endIndex) throws RepositoryException {
                    return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, Collections.emptyList());
                }

                @Override
                public PagedEntityList<EntityTpe> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
                    return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, Collections.emptyList());
                }
            };
        }
    };

    @Override
    public CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> crud() {
        return this.crud;
    }

    @Override
    public Pager<EntityTpe> pager() {
        return this.pager;
    }
}

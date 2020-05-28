package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
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

    class DefaultAdapter<EntityType, CreationType, ReplaceType, UpdateType> implements PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> {

        private final CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud;
        private final Pager<EntityType> lister;

        public DefaultAdapter(CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud, Pager<EntityType> lister) {
            this.crud = crud;
            this.lister = lister;
        }

        @Override
        public CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud() {
            return this.crud;
        }

        @Override
        public Pager<EntityType> pager() {
            return this.lister;
        }
    }

    class DefaultPager<EntityType> implements Pager<EntityType> {

        private final String unit;
        private final int maxPageSize;
        private final EntityLister<EntityType, PropertyQuery> lister;

        public DefaultPager(String unit, int maxPageSize, EntityLister<EntityType, PropertyQuery> lister) {
            this.unit = unit;
            this.maxPageSize = maxPageSize;
            this.lister = lister;
        }

        @Override
        public String unit() {
            return this.unit;
        }

        @Override
        public int maxPageSize() {
            return this.maxPageSize;
        }

        @Override
        public EntityLister<EntityType, PropertyQuery> lister() {
            return this.lister;
        }
    }

    class BadRequestAdapter<EntityType, CreationType, ReplaceType, UpdateType> implements PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> {

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
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityType> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityType> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityType> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
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

    class NotFoundAdapter<EntityType, CreationType, ReplaceType, UpdateType> implements PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> {

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

    class UnexpectedExceptionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> implements PagedCollectionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> {

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

}

package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.api.generic.resource.api.types.Error;
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

    class DefaultAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> implements GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> {

        private final CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> crud;
        private final Pager<EntityTpe> lister;

        public DefaultAdapter(CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> crud, Pager<EntityTpe> lister) {
            this.crud = crud;
            this.lister = lister;
        }

        @Override
        public CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> crud() {
            return this.crud;
        }

        @Override
        public Pager<EntityTpe> pager() {
            return this.lister;
        }
    }

    class DefaultPager<EntityTpe> implements Pager<EntityTpe> {

        private final String unit;
        private final int maxPageSize;
        private final EntityLister<EntityTpe, PropertyQuery> lister;

        public DefaultPager(String unit, int maxPageSize, EntityLister<EntityTpe, PropertyQuery> lister) {
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
        public EntityLister<EntityTpe, PropertyQuery> lister() {
            return this.lister;
        }
    }

    class BadRequestAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> implements GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> {

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
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityTpe> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityTpe> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityTpe> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
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

    class NotFoundAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> implements GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> {

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
                throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityTpe> createEntityFrom(CreationType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityTpe> replaceEntityWith(String id, ReplaceType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public Entity<EntityTpe> updateEntityWith(String id, UpdateType value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
            }

            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).build(), "");
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

    class UnexpectedExceptionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> implements GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> {

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

package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.MovieCreationData;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;
import java.util.Set;

public class MovieCRUD implements PagedCollectionAdapter.CRUD<Movie, MovieCreationData, Movie, Void> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(MovieCRUD.class);

    private Set<Action> actions;
    private final String store;
    private final Repository<Movie, PropertyQuery> repository;
    private Optional<Movie.Category> category;

    public MovieCRUD(Set<Action> actions, String store, Repository<Movie, PropertyQuery> repository) {
        this(actions, store, repository, null);
    }
    public MovieCRUD(Set<Action> actions, String store, Repository<Movie, PropertyQuery> repository, Movie.Category category) {
        this.actions = actions;
        this.store = store;
        this.repository = repository;
        this.category = Optional.ofNullable(category);
    }

    @Override
    public String entityRepositoryUrl() {
        return String.format("/%s/movies", this.store);
    }

    @Override
    public Set<Action> supportedActions() {
        return this.actions;
    }

    @Override
    public Optional<Entity<Movie>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            return Optional.ofNullable(this.repository.retrieve(id));
        } catch (RepositoryException e) {
            throw this.repositoryError(e);
        }
    }

    @Override
    public Entity<Movie> createEntityFrom(MovieCreationData value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            if(this.category.isPresent()) {
                Entity<Movie> entity = this.repository.create(Movie.builder()
                        .category(this.category.get())
                        .title(value.title())
                        .filmMaker(value.filmMaker())
                        .build());
                entity = this.repository.update(entity, entity.value().withId(entity.id()));
                log.info("movie created : {}", entity);
                return entity;
            } else {
                throw new BadRequestException(
                        Error.builder()
                                .code(Error.Code.BAD_REQUEST)
                                .token(log.tokenized().info("create movie called without a category scope"))
                                .description("create movie must be invoked in a category context")
                                .build(),
                        "no category provided"
                );
            }
        } catch (RepositoryException e) {
            throw this.repositoryError(e);
        }
    }

    @Override
    public Entity<Movie> replaceEntityWith(String id, Movie value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            Entity<Movie> entity = this.repository.retrieve(id);
            if(entity == null) {
                throw new NotFoundException(
                        Error.builder()
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .token(log.tokenized().info("replace movie called for an unexistent movie {}", id))
                                .description("no such movie")
                                .build(),
                        "no movie with id " + id
                );
            } else {
                if(value.opt().id().isPresent()) {
                    if(! value.id().equals(entity.id())) {
                        throw new BadRequestException(
                                Error.builder()
                                    .code(Error.Code.BAD_REQUEST)
                                    .token(log.tokenized().info("replace movie tries to change id to {}, movie was {}", id, entity))
                                    .description("movie id is immutable")
                                    .build(),
                                "");
                    }
                }
                entity = this.repository.update(entity, value.withId(entity.id()));
                log.info("movie replaced : {}", entity);
                return entity;
            }
        } catch (RepositoryException e) {
            throw this.repositoryError(e);
        }
    }

    @Override
    public Entity<Movie> updateEntityWith(String id, Void value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new UnexpectedException(Error.builder().code(Error.Code.UNEXPECTED_ERROR).build(), "not implemented");
    }

    @Override
    public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            Entity<Movie> entity = this.repository.retrieve(id);
            if (entity != null) {
                this.repository.delete(entity);
                log.info("movie deleted : {}", entity);
            } else {
                throw new NotFoundException(
                        Error.builder()
                                .code(Error.Code.RESOURCE_NOT_FOUND)
                                .token(log.tokenized().info("delete movie called for an unexistent movie {}", id))
                                .description("no such movie")
                                .build(),
                        "no movie with id " + id
                );
            }
        } catch (RepositoryException e) {
            throw this.repositoryError(e);
        }
    }

    private UnexpectedException repositoryError(RepositoryException e) {
        return new UnexpectedException(
                Error.builder()
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token(log.tokenized().error("error accessing movie repository for store " + store, e))
                        .build(),
                "error accessing movie repository",
                e
        );
    }
}

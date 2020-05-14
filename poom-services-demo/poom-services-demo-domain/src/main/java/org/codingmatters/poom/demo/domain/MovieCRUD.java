package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.MovieCreationData;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;
import java.util.Set;

public class MovieCRUD implements GenericResourceAdapter.CRUD<Movie, MovieCreationData, Movie, Void> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(MovieCRUD.class);

    private Set<Action> actions;
    private final String store;
    private final Repository<Movie, PropertyQuery> repository;
    private Movie.Category category;

    public MovieCRUD(Set<Action> actions, String store, Repository<Movie, PropertyQuery> repository) {
        this.actions = actions;
        this.store = store;
        this.repository = repository;
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
            Entity<Movie> entity = this.repository.create(Movie.builder()
                    .category(this.category)
                    .title(value.title())
                    .filmMaker(value.filmMaker())
                    .build());
            return this.repository.update(entity, entity.value().withId(entity.id()));
        } catch (RepositoryException e) {
            throw this.repositoryError(e);
        }
    }

    @Override
    public Entity<Movie> replaceEntityWith(String id, Movie value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            Entity<Movie> entity = this.repository.retrieve(id);
            return this.repository.update(entity, value.withId(entity.id()));
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

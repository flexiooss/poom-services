package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.MovieCreationData;
import org.codingmatters.poom.generic.resource.domain.exceptions.BadRequestException;
import org.codingmatters.poom.generic.resource.domain.exceptions.NotFoundException;
import org.codingmatters.poom.generic.resource.domain.exceptions.UnexpectedException;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;


public class MovieCRUDTest {

    static public Movie A_MOVIE = Movie.builder()
            .id("42")
            .title("Vertigo")
            .filmMaker("Alfred Hitchcock")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Repository<Movie, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Movie.class);

    @Test
    public void givenRepositoryEmpty__whenRetrievingMovie__thenEmptyReturned() throws Exception {
        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        assertThat(crud.retrieveEntity("no such movie").isPresent(), is(false));
    }

    @Test
    public void givenRepositoryNotEmpty__whenRetrievingMovie_andIdDoesntExists__thenEmptyReturned() throws Exception {
        this.repository.createWithId(A_MOVIE.id(), A_MOVIE);

        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        assertThat(crud.retrieveEntity("no such movie").isPresent(), is(false));
    }

    @Test
    public void givenRepositoryNotEmpty__whenRetrievingMovie_andIdExists__thenMovieEntityReturned() throws Exception {
        this.repository.createWithId(A_MOVIE.id(), A_MOVIE);

        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        assertThat(crud.retrieveEntity(A_MOVIE.id()).get(), is(new ImmutableEntity<>(A_MOVIE.id(), BigInteger.ONE, A_MOVIE)));
    }

    @Test
    public void givenNotInACategoryContext__whenCreatingAMovie__thenBadRequestExceptionThrown() throws Exception {
        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("no category provided");

        crud.createEntityFrom(MovieCreationData.builder()
                .title(A_MOVIE.title())
                .filmMaker(A_MOVIE.filmMaker())
                .build());
    }

    @Test
    public void givenInACategoryContext__whenCreatingAMovie__thenEntityCreated_andCategoryIsFilledFromContext_andIdIsSetted_andMovieCreatedInRepository() throws Exception {
        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository, Movie.Category.REGULAR);

        Entity<Movie> entity = crud.createEntityFrom(MovieCreationData.builder()
                .title(A_MOVIE.title())
                .filmMaker(A_MOVIE.filmMaker())
                .build());

        assertThat(entity.value(), is(A_MOVIE.withId(entity.id()).withCategory(Movie.Category.REGULAR)));
        assertThat(this.repository.retrieve(entity.id()).value(), is(entity.value()));
    }

    @Test
    public void whenUpdateEntity__thenUnexpectedExceptionThrown() throws Exception {
        this.repository.createWithId(A_MOVIE.id(), A_MOVIE);

        thrown.expect(UnexpectedException.class);
        thrown.expectMessage("not implemented");

        new MovieCRUD(Action.all, "a-store", this.repository).updateEntityWith(A_MOVIE.id(), null);
    }

    @Test
    public void givenEntityExists__whenReplacingEntity_andEntityIdFilledWithTheSame__thenReplacedEntityReturned_andEntityReplacedInRepository() throws Exception {
        this.repository.createWithId(A_MOVIE.id(), A_MOVIE);

        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        Entity<Movie> entity = crud.replaceEntityWith(A_MOVIE.id(), A_MOVIE.withTitle("changed title"));

        assertThat(entity.value(), is(A_MOVIE.withTitle("changed title")));
        assertThat(this.repository.retrieve(entity.id()).value(), is(entity.value()));
    }

    @Test
    public void givenEntityExists__whenReplacingEntity_andEntityIdIsNull__thenReplacedEntityReturned_andEntityReplacedInRepository() throws Exception {
        this.repository.createWithId(A_MOVIE.id(), A_MOVIE);

        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        Entity<Movie> entity = crud.replaceEntityWith(A_MOVIE.id(), A_MOVIE.withId(null).withTitle("changed title"));

        assertThat(entity.value(), is(A_MOVIE.withTitle("changed title")));
        assertThat(this.repository.retrieve(entity.id()).value(), is(entity.value()));
    }

    @Test
    public void givenEntityExists__whenReplacingEntity_andEntityIdFilledWithADifferentId__thenBadRequestExceptionIsThrown_andMovieNotUpdatedInRepository() throws Exception {
        this.repository.createWithId(A_MOVIE.id(), A_MOVIE);

        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        thrown.expect(BadRequestException.class);
        crud.replaceEntityWith(A_MOVIE.id(), A_MOVIE.withId("changed-id").withTitle("changed title"));

        assertThat(this.repository.retrieve(A_MOVIE.id()).value(), is(A_MOVIE));
    }

    @Test
    public void givenEntityDoesntExist__whenReplacingEntity_andEntityIdFilledWithADifferentId__thenNotFoundExceptionIsThrown_andMovieNotUpdatedInRepository() throws Exception {
        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        thrown.expect(NotFoundException.class);
        crud.replaceEntityWith(A_MOVIE.id(), A_MOVIE.withId("changed-id").withTitle("changed title"));

        assertThat(this.repository.retrieve(A_MOVIE.id()), is(nullValue()));
    }

    @Test
    public void givenMovieExists__whenDeletingMovie__thenMovieDeletedFromRepository() throws Exception {
        this.repository.createWithId(A_MOVIE.id(), A_MOVIE);

        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        crud.deleteEntity(A_MOVIE.id());

        assertThat(this.repository.retrieve(A_MOVIE.id()), is(nullValue()));
    }

    @Test
    public void givenMovieDoesntExist__whenDeletingMovie__thenNotFoundExceptionIsThrown() throws Exception {
        MovieCRUD crud = new MovieCRUD(Action.all, "a-store", this.repository);

        thrown.expect(NotFoundException.class);
        crud.deleteEntity(A_MOVIE.id());
    }
}
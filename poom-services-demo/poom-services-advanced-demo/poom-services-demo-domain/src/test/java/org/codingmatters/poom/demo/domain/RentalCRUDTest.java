package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.RentalAction;
import org.codingmatters.poom.apis.demo.api.types.RentalRequest;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;
import org.codingmatters.poom.generic.resource.domain.exceptions.BadRequestException;
import org.codingmatters.poom.generic.resource.domain.exceptions.UnexpectedException;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class RentalCRUDTest {

    static public Movie MOVIE = Movie.builder()
            .id("42")
            .title("Psycho")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.HORROR)
            .build();

    public static final Billing BILLING = Billing.builder().frequentRenterPoints(12L).price(42.0).build();

    private Repository<Rental, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Rental.class);
    private final RentalCRUD crud = new RentalCRUD(Action.all, "a-store", this.repository, MOVIE, (rental, at) -> BILLING);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void givenRentalDoesntExist__whenRetrievingRental__thenNotFoundException() throws Exception {
        assertThat(this.crud.retrieveEntity("no-such-rental").isPresent(), is(false));
    }

    @Test
    public void givenRentalExists__whenRetrievingRental__thenRentalEntityRetrived() throws Exception {
        Entity<Rental> existing = this.repository.createWithId("a-rental", Rental.builder()
                .id("a-rental")
                .customer("custo")
                .movieId(MOVIE.id())
                .start(UTC.now())
                .build()
        );

        Optional<Entity<Rental>> actual = this.crud.retrieveEntity("a-rental");

        assertThat(actual.get(), is(existing));
    }

    @Test
    public void givenMovieIsFreeForRent__whenIssuingRentalRequest__thenRentalCreated() throws Exception {
        Entity<Rental> actual = this.crud.createEntityFrom(RentalRequest.builder()
                .customer("a-customer")
                .build());

        assertThat(actual.value().customer(), is("a-customer"));
        assertThat(actual.value().movieId(), is(MOVIE.id()));
        assertThat(actual.value().status(), is(Rental.Status.OUT));
        assertThat(actual.value().start(), is(around(UTC.now())));
        assertThat(actual.value().dueDate(), is(around(UTC.now().plusDays(3L))));

        assertThat(actual.value().returnedDate(), is(nullValue()));
        assertThat(actual.value().billing(), is(nullValue()));
    }

    @Test
    public void givenMovieIsNotFreeForRent__whenIssuingRentalRequest__thenBadRequestException() throws Exception {
        this.repository.create(Rental.builder().movieId(MOVIE.id()).status(Rental.Status.OUT).build());

        thrown.expect(BadRequestException.class);
        this.crud.createEntityFrom(RentalRequest.builder()
                .customer("a-customer")
                .build());
    }

    @Test
    public void whenIssuingRentalRequest_andNoCustomerProvided__thenBadRequestException() throws Exception {
        thrown.expect(BadRequestException.class);
        this.crud.createEntityFrom(RentalRequest.builder()
                .customer(null)
                .build());
    }

    @Test
    public void givenRentalExists__whenReplacingRental__thenNotImplementedUnexpectedException() throws Exception {
        this.repository.createWithId("a-rental", Rental.builder().movieId(MOVIE.id()).status(Rental.Status.OUT).build());

        thrown.expect(UnexpectedException.class);
        thrown.expectMessage("not implemented");

        this.crud.replaceEntityWith("a-rental", null);
    }

    @Test
    public void givenRentalExists__whenDeletingRental__thenNotImplementedUnexpectedException() throws Exception {
        this.repository.createWithId("a-rental", Rental.builder().movieId(MOVIE.id()).status(Rental.Status.OUT).build());

        thrown.expect(UnexpectedException.class);
        thrown.expectMessage("not implemented");

        this.crud.deleteEntity("a-rental");
    }

    @Test
    public void givenMovieIsOut__whenReturningMovie__thenMovieIsMarkedReturned_andBillingCalculated_andRepositoryValueUpdated() throws Exception {
        this.repository.createWithId("a-rental", Rental.builder().movieId(MOVIE.id()).status(Rental.Status.OUT)
                .dueDate(UTC.now().plusDays(2L))
                .build());

        Entity<Rental> returned = this.crud.updateEntityWith("a-rental", RentalAction.builder().type(RentalAction.Type.RETURN).build());

        assertThat(returned.value().returnedDate(), is(around(UTC.now())));
        assertThat(returned.value().status(), is(Rental.Status.RETURNED));
        assertThat(returned.value().billing(), is(notNullValue()));

        assertThat(returned.value(), is(this.repository.retrieve("a-rental").value()));
    }

    @Test
    public void givenMovieIsLate__whenReturningMovie__thenMovieIsMarkedReturned_andBillingCalculated_andRepositoryValueUpdated() throws Exception {
        this.repository.createWithId("a-rental", Rental.builder().movieId(MOVIE.id()).status(Rental.Status.LATE)
                .dueDate(UTC.now().minusDays(2L))
                .build());

        Entity<Rental> returned = this.crud.updateEntityWith("a-rental", RentalAction.builder().type(RentalAction.Type.RETURN).build());

        assertThat(returned.value().returnedDate(), is(around(UTC.now())));
        assertThat(returned.value().status(), is(Rental.Status.RETURNED));
        assertThat(returned.value().billing(), is(BILLING));

        assertThat(returned.value(), is(this.repository.retrieve("a-rental").value()));
    }

    @Test
    public void givenMovieIsOut__whenExtendingMovie__thenMovieIsMarkedOut_andDueDateIsExtended_andRepositoryValueUpdated() throws Exception {
        this.repository.createWithId("a-rental", Rental.builder().movieId(MOVIE.id()).status(Rental.Status.OUT)
                .dueDate(UTC.now().plusDays(2L))
                .build());

        Entity<Rental> returned = this.crud.updateEntityWith("a-rental", RentalAction.builder().type(RentalAction.Type.EXTENSION).build());

        assertThat(returned.value().status(), is(Rental.Status.OUT));
        assertThat(returned.value().dueDate(), is(around(UTC.now().plusDays(3L))));
        assertThat(returned.value().returnedDate(), is(nullValue()));
        assertThat(returned.value().billing(), is(nullValue()));

        assertThat(returned.value(), is(this.repository.retrieve("a-rental").value()));
    }

    @Test
    public void givenMovieIsLate__whenExtendingMovie__thenMovieIsMarkedOut_andDueDateIsExtended_andRepositoryValueUpdated() throws Exception {
        this.repository.createWithId("a-rental", Rental.builder().movieId(MOVIE.id()).status(Rental.Status.LATE)
                .dueDate(UTC.now().minusDays(2L))
                .build());

        Entity<Rental> returned = this.crud.updateEntityWith("a-rental", RentalAction.builder().type(RentalAction.Type.EXTENSION).build());

        assertThat(returned.value().status(), is(Rental.Status.OUT));
        assertThat(returned.value().dueDate(), is(around(UTC.now().plusDays(3L))));
        assertThat(returned.value().returnedDate(), is(nullValue()));
        assertThat(returned.value().billing(), is(nullValue()));

        assertThat(returned.value(), is(this.repository.retrieve("a-rental").value()));
    }
}
package org.codingmatters.poom.demo.domain.rentals;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LateRentalTaskTest {

    private final Repository<Rental, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Rental.class);

    @Test
    public void givenRepositoryEmpty__whenProcessing__thenNothingChanged() throws Exception {
        new LateRentalTask(this.repository).process(UTC.now());

        assertThat(this.repository.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenOneRentalInRepository_andDueDateNotPassed__whenProcessing__thenNothingChanged() throws Exception {
        Entity<Rental> rental = this.repository.createWithId("rental", Rental.builder().id("rental")
                .status(Rental.Status.OUT)
                .dueDate(UTC.now().plusDays(1L))
                .build());

        new LateRentalTask(this.repository).process(UTC.now());

        assertThat(this.repository.retrieve("rental").value(), is(rental.value()));
    }

    @Test
    public void givenOneRentalInRepository_andRentalIsOut_andDueDatePassed__whenProcessing__thenRentalIsMarkedLate() throws Exception {
        Entity<Rental> rental = this.repository.createWithId("rental", Rental.builder().id("rental")
                .status(Rental.Status.OUT)
                .dueDate(UTC.now().minusDays(1L))
                .build());

        new LateRentalTask(this.repository).process(UTC.now());

        assertThat(this.repository.retrieve("rental").value(), is(rental.value().withStatus(Rental.Status.LATE)));
    }

    @Test
    public void givenOneRentalInRepository_andRentalIsReturned_andDueDatePassed__whenProcessing__thenNothingChanged() throws Exception {
        Entity<Rental> rental = this.repository.createWithId("rental", Rental.builder().id("rental")
                .status(Rental.Status.RETURNED)
                .dueDate(UTC.now().minusDays(1L))
                .build());

        new LateRentalTask(this.repository).process(UTC.now());

        assertThat(this.repository.retrieve("rental").value(), is(rental.value()));
    }
}
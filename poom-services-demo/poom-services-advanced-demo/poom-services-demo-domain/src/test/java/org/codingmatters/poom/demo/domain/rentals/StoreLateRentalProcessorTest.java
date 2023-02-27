package org.codingmatters.poom.demo.domain.rentals;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.laterentaltask.Report;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StoreLateRentalProcessorTest {

    private final Repository<Rental, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Rental.class);

    @Test
    public void givenRepositoryEmpty__whenProcessing__thenNothingChanged() throws Exception {
        assertThat(
                new StoreLateRentalProcess(this.repository).process(UTC.now()),
                is(Report.builder().accountProcessed(1L).lateRentalCount(0L).build())
        );

        assertThat(this.repository.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenOneRentalInRepository_andDueDateNotPassed__whenProcessing__thenNothingChanged() throws Exception {
        Entity<Rental> rental = this.repository.createWithId("rental", Rental.builder().id("rental")
                .status(Rental.Status.OUT)
                .dueDate(UTC.now().plusDays(1L))
                .build());

        assertThat(
                new StoreLateRentalProcess(this.repository).process(UTC.now()),
                is(Report.builder().accountProcessed(1L).lateRentalCount(0L).build())
        );

        assertThat(this.repository.retrieve("rental").value(), is(rental.value()));
    }

    @Test
    public void givenOneRentalInRepository_andRentalIsOut_andDueDatePassed__whenProcessing__thenRentalIsMarkedLate() throws Exception {
        Entity<Rental> rental = this.repository.createWithId("rental", Rental.builder().id("rental")
                .status(Rental.Status.OUT)
                .dueDate(UTC.now().minusDays(1L))
                .build());

        assertThat(
                new StoreLateRentalProcess(this.repository).process(UTC.now()),
                is(Report.builder().accountProcessed(1L).lateRentalCount(1L).build())
        );

        assertThat(this.repository.retrieve("rental").value(), is(rental.value().withStatus(Rental.Status.LATE)));
    }

    @Test
    public void givenOneRentalInRepository_andRentalIsReturned_andDueDatePassed__whenProcessing__thenNothingChanged() throws Exception {
        Entity<Rental> rental = this.repository.createWithId("rental", Rental.builder().id("rental")
                .status(Rental.Status.RETURNED)
                .dueDate(UTC.now().minusDays(1L))
                .build());

        assertThat(
                new StoreLateRentalProcess(this.repository).process(UTC.now()),
                is(Report.builder().accountProcessed(1L).lateRentalCount(0L).build())
        );

        assertThat(this.repository.retrieve("rental").value(), is(rental.value()));
    }
}
package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.LateRentalTask;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.laterentaltask.Report;
import org.codingmatters.poom.demo.domain.rentals.LateRentalProcessor;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class LateRentalTaskCRUDTest {
    static private Eventually eventually = Eventually.defaults();

    private Repository<LateRentalTask, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(LateRentalTask.class);
    private Repository<Store, PropertyQuery> storeRepository = InMemoryRepositoryWithPropertyQuery.validating(Store.class);
    private Repository<Rental, PropertyQuery> rentalRepository = InMemoryRepositoryWithPropertyQuery.validating(Rental.class);

    private LateRentalTaskCRUD crud = new LateRentalTaskCRUD(
            new LateRentalProcessor(this.repository,this.storeRepository, store -> Optional.of(this.rentalRepository)),
            Executors.newSingleThreadExecutor()
    );

    @Test
    public void givenNoStore__whenCreatingTask__thenTaskEventuallyFinishes() throws Exception {
        Entity<LateRentalTask> entity = this.crud.createEntityFrom(ObjectValue.builder().build());

        eventually.assertThat(() -> this.repository.retrieve(entity.id()).value().status(), is(LateRentalTask.Status.DONE));
    }

    @Test
    public void givenAStoreExists_andARentalIsPassedDueDate__whenCreatingTask__thenTaskEventuallyFinishes_andRentalIsMarkedAsLate() throws Exception {
        this.storeRepository.create(Store.builder().name("netflix").build());
        Entity<Rental> rentalEntity = this.rentalRepository.create(Rental.builder().status(Rental.Status.OUT).dueDate(UTC.now().minusDays(1)).build());

        Entity<LateRentalTask> entity = this.crud.createEntityFrom(ObjectValue.builder().build());

        eventually.assertThat(() -> this.repository.retrieve(entity.id()).value().status(), is(LateRentalTask.Status.DONE));

        LateRentalTask task = this.repository.retrieve(entity.id()).value();
        assertThat(task.report(), is(Report.builder().accountProcessed(1L).lateRentalCount(1L).build()));

        Rental rental = this.rentalRepository.retrieve(rentalEntity.id()).value();
        assertThat(rental.status(), is(Rental.Status.LATE));
    }
}
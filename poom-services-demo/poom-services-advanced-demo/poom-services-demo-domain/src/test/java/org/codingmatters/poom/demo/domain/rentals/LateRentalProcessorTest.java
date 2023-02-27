package org.codingmatters.poom.demo.domain.rentals;

import org.codingmatters.poom.apis.demo.api.types.LateRentalTask;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.laterentaltask.Report;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LateRentalProcessorTest {

    private Repository<LateRentalTask, PropertyQuery> taskRepository = InMemoryRepositoryWithPropertyQuery.validating(LateRentalTask.class);
    private Repository<Store, PropertyQuery> storeRepository = InMemoryRepositoryWithPropertyQuery.validating(Store.class);

    private final Map<String, Repository<Rental, PropertyQuery>> rentalRepositories = new HashMap<>();
    private final Function<String, Optional<Repository<Rental, PropertyQuery>>> rentalRepositorySupplier = store -> Optional.of(this.rentalRepositories.computeIfAbsent(store, s -> InMemoryRepositoryWithPropertyQuery.validating(Rental.class)));

    private final LateRentalProcessor process = new LateRentalProcessor(
            this.taskRepository,
            this.storeRepository,
            rentalRepositorySupplier
    );

    @Test
    public void givenNoStores__whenProcessingTask__thenNoAccountAndRentalProcessed_andTaskEnded() throws Exception {
        Entity<LateRentalTask> task = this.taskRepository.create(LateRentalTask.builder().start(UTC.now()).build());
        assertThat(this.process.process(task), is(Report.builder().accountProcessed(0L).lateRentalCount(0L).build()));

        task = this.taskRepository.retrieve(task.id());
        assertThat(task.value().status(), is(LateRentalTask.Status.DONE));
        assertThat(task.value().end(), is(around(UTC.now())));
        assertThat(task.value().report(), is(Report.builder().accountProcessed(0L).lateRentalCount(0L).build()));
    }

    @Test
    public void givenOneStores_andNoRentalInStore__whenProcessingTask__thenOneAccountProcessed_andNoRentalProcessed_andTaskEnded() throws Exception {
        this.storeRepository.create(Store.builder().name("store").build());

        Entity<LateRentalTask> task = this.taskRepository.create(LateRentalTask.builder().start(UTC.now()).build());
        assertThat(this.process.process(task), is(Report.builder().accountProcessed(1L).lateRentalCount(0L).build()));

        task = this.taskRepository.retrieve(task.id());
        assertThat(task.value().status(), is(LateRentalTask.Status.DONE));
        assertThat(task.value().end(), is(around(UTC.now())));
        assertThat(task.value().report(), is(Report.builder().accountProcessed(1L).lateRentalCount(0L).build()));
    }

    @Test
    public void givenOneStores_andOneLateRentalInStore__whenProcessingTask__thenOneAccountProcessed_andOneRentalProcessed_andTaskEnded() throws Exception {
        this.storeRepository.create(Store.builder().name("store").build());
        this.rentalRepositorySupplier.apply("store").get().create(Rental.builder().status(Rental.Status.OUT).dueDate(UTC.now().minusDays(1L)).build());

        Entity<LateRentalTask> task = this.taskRepository.create(LateRentalTask.builder().start(UTC.now()).build());
        assertThat(this.process.process(task), is(Report.builder().accountProcessed(1L).lateRentalCount(1L).build()));

        task = this.taskRepository.retrieve(task.id());
        assertThat(task.value().status(), is(LateRentalTask.Status.DONE));
        assertThat(task.value().end(), is(around(UTC.now())));
        assertThat(task.value().report(), is(Report.builder().accountProcessed(1L).lateRentalCount(1L).build()));
    }

    @Test
    public void givenTwoStores_andOneLateRentalInStore__whenProcessingTask__thenOneAccountProcessed_andOneRentalProcessed_andTaskEnded() throws Exception {
        this.storeRepository.create(Store.builder().name("store1").build());
        this.storeRepository.create(Store.builder().name("store2").build());
        this.rentalRepositorySupplier.apply("store1").get().create(Rental.builder().status(Rental.Status.OUT).dueDate(UTC.now().minusDays(1L)).build());
        this.rentalRepositorySupplier.apply("store2").get().create(Rental.builder().status(Rental.Status.OUT).dueDate(UTC.now().minusDays(1L)).build());

        Entity<LateRentalTask> task = this.taskRepository.create(LateRentalTask.builder().start(UTC.now()).build());
        assertThat(this.process.process(task), is(Report.builder().accountProcessed(2L).lateRentalCount(2L).build()));

        task = this.taskRepository.retrieve(task.id());
        assertThat(task.value().status(), is(LateRentalTask.Status.DONE));
        assertThat(task.value().end(), is(around(UTC.now())));
        assertThat(task.value().report(), is(Report.builder().accountProcessed(2L).lateRentalCount(2L).build()));
    }
}
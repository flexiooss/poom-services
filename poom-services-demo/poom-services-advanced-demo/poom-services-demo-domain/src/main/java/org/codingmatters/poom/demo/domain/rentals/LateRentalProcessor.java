package org.codingmatters.poom.demo.domain.rentals;

import org.codingmatters.poom.apis.demo.api.types.LateRentalTask;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.laterentaltask.Report;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.impl.DefaultRepositoryIterator;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.domain.entities.Entity;

import java.util.Optional;
import java.util.function.Function;

public class LateRentalProcessor {
    static private final CategorizedLogger log =CategorizedLogger.getLogger(LateRentalProcessor.class);

    private final Repository<Store, PropertyQuery> storeRepository;
    private final Repository<LateRentalTask, PropertyQuery> taskRepository;
    private final Function<String, Optional<Repository<Rental, PropertyQuery>>> rentalRepositorySupplier;

    public LateRentalProcessor(
            Repository<LateRentalTask, PropertyQuery> taskRepository,
            Repository<Store, PropertyQuery> storeRepository,
            Function<String, Optional<Repository<Rental, PropertyQuery>>> rentalRepositorySupplier) {
        this.taskRepository = taskRepository;
        this.storeRepository = storeRepository;
        this.rentalRepositorySupplier = rentalRepositorySupplier;
    }

    public Repository<LateRentalTask, PropertyQuery> taskRepository() {
        return taskRepository;
    }

    public Report process(Entity<LateRentalTask> taskEntity) {
        try {
            taskEntity = this.taskRepository.update(taskEntity, taskEntity.value().withReport(
                    Report.builder()
                            .accountProcessed(0L)
                            .lateRentalCount(0L)
                            .build()
            ));
            DefaultRepositoryIterator.SearchRepositoryIterator<Store, PropertyQuery> stores = new DefaultRepositoryIterator.SearchRepositoryIterator<>(this.storeRepository, PropertyQuery.builder().sort("name").build(), 1000);
            while(stores.hasNext()) {
                Entity<Store> store = stores.next();
                Optional<Repository<Rental, PropertyQuery>> rentalRepository = this.rentalRepositorySupplier.apply(store.value().name());
                if (rentalRepository.isPresent()) {
                    Report report = new StoreLateRentalProcess(rentalRepository.get()).process(taskEntity.value().start());
                    taskEntity = this.taskRepository.update(taskEntity, taskEntity.value().withReport(
                            Report.builder()
                                    .accountProcessed(taskEntity.value().report().accountProcessed() + 1)
                                    .lateRentalCount(taskEntity.value().report().lateRentalCount() + report.lateRentalCount())
                                    .build()
                    ));
                }
            }
            taskEntity = this.taskRepository.update(taskEntity, taskEntity.value().withStatus(LateRentalTask.Status.DONE).withEnd(UTC.now()));
        } catch (RepositoryException e) {
            log.error("error running late rental process", e);
            try {
                this.taskRepository.update(taskEntity, taskEntity.value().withStatus(LateRentalTask.Status.FAILED).withEnd(UTC.now()));
            } catch (RepositoryException repositoryException) {
                log.error("[GRAVE] failed updating late rental task to set failure", e);
            }
        }
        return taskEntity.value().report();
    }
}

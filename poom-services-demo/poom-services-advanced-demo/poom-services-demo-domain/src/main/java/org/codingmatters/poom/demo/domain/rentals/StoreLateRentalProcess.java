package org.codingmatters.poom.demo.domain.rentals;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.laterentaltask.Report;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StoreLateRentalProcess {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StoreLateRentalProcess.class);

    private final Repository<Rental, PropertyQuery> repository;

    public StoreLateRentalProcess(Repository<Rental, PropertyQuery> repository) {
        this.repository = repository;
    }

    public Report process(LocalDateTime until) {
        Long count = 0L;
        try {
            PagedEntityList<Rental> lateRentals;
            do {
                lateRentals = this.repository.search(this.lateRentalsQuery(until), 0, 1000);
                for (Entity<Rental> lateRental : lateRentals) {
                    this.repository.update(lateRental, lateRental.value().withStatus(Rental.Status.LATE));
                    count++;
                }
            } while(lateRentals.total() > 0);
        } catch (RepositoryException e) {
            log.error("error running late rental tasks, stopping task", e);
        }
        return Report.builder().accountProcessed(1L).lateRentalCount(count).build();
    }

    private PropertyQuery lateRentalsQuery(LocalDateTime until) {
        return PropertyQuery.builder()
                .filter(String.format(
                        "status == '%s' && dueDate <= %s",
                        Rental.Status.OUT.name(),
                        until.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .sort("start asc")
                .build();
    }
}

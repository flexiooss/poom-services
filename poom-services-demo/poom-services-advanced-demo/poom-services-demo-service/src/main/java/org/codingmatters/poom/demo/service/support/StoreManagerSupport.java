package org.codingmatters.poom.demo.service.support;

import org.codingmatters.poom.apis.demo.api.types.LateRentalTask;
import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.domain.rentals.LateRentalProcessor;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class StoreManagerSupport {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StoreManagerSupport.class);

    private final Repository<Store, PropertyQuery> stores = InMemoryRepositoryWithPropertyQuery.validating(Store.class);
    private final Repository<LateRentalTask, PropertyQuery> lateRentals = InMemoryRepositoryWithPropertyQuery.validating(LateRentalTask.class);

    private final Map<String, Repository<Movie, PropertyQuery>> movieRespositories = new HashMap<>();
    private final Map<String, Repository<Rental, PropertyQuery>> rentalRespositories = new HashMap<>();

    private LateRentalProcessor lateRentalProcessor = new LateRentalProcessor(
            this.lateRentals,
            this.stores,
            this::rentalRepositoryForStore
    );

    public Repository<Store, PropertyQuery> storeRepository() {
        return this.stores;
    }

    public Repository<LateRentalTask, PropertyQuery> lateRentalRepository() {
        return lateRentals;
    }

    public Optional<Repository<Movie, PropertyQuery>> movieRepositoryForStore(String store) {
        Entity<Store> storeEntity = null;
        try {
            storeEntity = this.stores.retrieve(store);
        } catch (RepositoryException e) {
            log.error("failed accessing store repository", e);
            return Optional.empty();
        }
        if(storeEntity != null) {
            return Optional.of(this.movieRespositories.computeIfAbsent(store, s -> InMemoryRepositoryWithPropertyQuery.validating(Movie.class)));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Repository<Rental, PropertyQuery>> rentalRepositoryForStore(String store) {
        Entity<Store> storeEntity = null;
        try {
            storeEntity = this.stores.retrieve(store);
        } catch (RepositoryException e) {
            log.error("failed accessing store repository", e);
            return Optional.empty();
        }
        if(storeEntity != null) {
            return Optional.of(this.rentalRespositories.computeIfAbsent(store, s -> InMemoryRepositoryWithPropertyQuery.validating(Rental.class)));
        } else {
            return Optional.empty();
        }
    }

    public StoreManager createStoreManager(ExecutorService pool) {
        return new StoreManager(this.stores, this::movieRepositoryForStore, this::rentalRepositoryForStore, this.lateRentalProcessor, pool);
    }
}

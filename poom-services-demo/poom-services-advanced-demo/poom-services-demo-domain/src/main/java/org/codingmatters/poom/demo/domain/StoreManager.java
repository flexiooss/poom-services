package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.*;
import org.codingmatters.poom.demo.domain.billing.CategoryBillingProcessor;
import org.codingmatters.poom.demo.domain.rentals.LateRentalProcessor;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.impl.DefaultPager;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class StoreManager {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StoreManager.class);

    private final Repository<Store, PropertyQuery> storeRepository;
    private final Function<String, Optional<Repository<Movie, PropertyQuery>>> movieRepositoryForStoreProvider;
    private final Function<String, Optional<Repository<Rental, PropertyQuery>>> rentalRepositoryForStoreProvider;

    private final LateRentalProcessor lateRentalProcessor;
    private final ExecutorService pool;

    public StoreManager(
            Repository<Store, PropertyQuery> storeRepository,
            Function<String, Optional<Repository<Movie, PropertyQuery>>> movieRepositoryForStoreProvider,
            Function<String, Optional<Repository<Rental, PropertyQuery>>> rentalRepositoryForStoreProvider,
            LateRentalProcessor lateRentalProcessor,
            ExecutorService pool
    ) {
        this.storeRepository = storeRepository;
        this.movieRepositoryForStoreProvider = movieRepositoryForStoreProvider;
        this.rentalRepositoryForStoreProvider = rentalRepositoryForStoreProvider;
        this.lateRentalProcessor = lateRentalProcessor;
        this.pool = pool;
    }

    public boolean storeExists(String storeName) throws RepositoryException {
        PagedEntityList<Store> entities = this.storeRepository.search(
                PropertyQuery.builder().filter(String.format("name == '%s'", storeName)).build(),
                0, 0);
        return entities.total() > 0;
    }

    public EntityLister<Store, PropertyQuery> storeLister() {
        return this.storeRepository;
    }

    public PagedCollectionAdapter<Movie, MovieCreationData, Movie, Void> categoryMoviesAdpter(String store, String categoryName) {
        try {
            Movie.Category category = Movie.Category.valueOf(categoryName);
            return this.movieAdapter(store, Action.actions(Action.CREATE), category);
        } catch (IllegalArgumentException e) {
            log.info("category movie adapter called for a non existing category : {}", categoryName);
            return PagedCollectionAdapter.notFoundAdapter();
        }
    }

    public PagedCollectionAdapter<Movie, MovieCreationData, Movie, Void> storeMoviesAdpter(String store) {
        return this.movieAdapter(store, Action.actions(Action.REPLACE), null);
    }

    private PagedCollectionAdapter<Movie, MovieCreationData, Movie, Void> movieAdapter(String store, Set<Action> actions, Movie.Category category) {
        try {
            if(! this.storeExists(store)) {
                log.warn("request on an unexisting store {}", store);
                return PagedCollectionAdapter.notFoundAdapter();
            }
        } catch (RepositoryException e) {
            log.error("error accessing store repository", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();
        }
        Optional<Repository<Movie, PropertyQuery>> movieRepository = this.movieRepositoryForStoreProvider.apply(store);
        if(! movieRepository.isPresent()) {
            log.error("error accessing movie repository for store {}", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }

        return PagedCollectionAdapter.<Movie, MovieCreationData, Movie, Void>builder()
                .crud(new MovieCRUD(actions, store, movieRepository.get(), category))
                .pager(new MoviePager(movieRepository.get(), category))
                .build();
    }

    public PagedCollectionAdapter<Rental, RentalRequest, Void, RentalAction> movieRentalsAdapter(String store, String movieId) {
        try {
            if(! this.storeExists(store)) {
                log.warn("request on an unexisting store {}", store);
                return PagedCollectionAdapter.notFoundAdapter();
            }
        } catch (RepositoryException e) {
            log.error("error accessing store repository", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }

        Optional<Repository<Movie, PropertyQuery>> movieRepository = this.movieRepositoryForStoreProvider.apply(store);
        if(! movieRepository.isPresent()) {
            log.error("error accessing movie repository for store {}", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }
        Entity<Movie> movie = null;
        try {
            movie = movieRepository.get().retrieve(movieId);
            if(movie == null) {
                log.info("no such movie {} in store {}", movieId, store);
                return PagedCollectionAdapter.notFoundAdapter();
            }
        } catch (RepositoryException e) {
            log.error("error accessing movies in repository for store {}", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }

        System.out.println(movie);
        Optional<Repository<Rental, PropertyQuery>> repository = this.rentalRepositoryForStoreProvider.apply(store);
        if(! repository.isPresent()) {
            log.error("error accessing rental repository for store {}", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }

        return PagedCollectionAdapter.<Rental, RentalRequest, Void, RentalAction>builder()
                .crud(new RentalCRUD(
                        Action.actions(Action.CREATE, Action.UPDATE),
                        store,
                        repository.get(),
                        movie.value(),
                        new CategoryBillingProcessor(movie.value())
                ))
                .pager(new RentalPager(repository.get(), movie.value()))
                .build();
    }

    public PagedCollectionAdapter<Rental, RentalRequest, Void, RentalAction> customerRentalsAdapter(String store, String customer) {
        try {
            if(! this.storeExists(store)) {
                log.warn("request on an unexisting store {}", store);
                return PagedCollectionAdapter.notFoundAdapter();
            }
        } catch (RepositoryException e) {
            log.error("error accessing store repository", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }
        if(customer == null || customer.isEmpty()) {
            log.warn("request on an unexisting store {}", store);
            return PagedCollectionAdapter.notFoundAdapter();
        }

        Optional<Repository<Rental, PropertyQuery>> repository = this.rentalRepositoryForStoreProvider.apply(store);
        if(! repository.isPresent()) {
            log.error("error accessing rental repository for store {}", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }
        return PagedCollectionAdapter.<Rental, RentalRequest, Void, RentalAction>builder()
                .crud(null)
                .pager(new CustomerRentalPager(repository.get(), customer))
                .build();
    }

    public PagedCollectionAdapter<Rental, RentalRequest, Void, RentalAction> rentalsAdapter(String store) {
        try {
            if(! this.storeExists(store)) {
                log.warn("request on an unexisting store {}", store);
                return PagedCollectionAdapter.notFoundAdapter();
            }
        } catch (RepositoryException e) {
            log.error("error accessing store repository", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();
        }

        Optional<Repository<Rental, PropertyQuery>> repository = this.rentalRepositoryForStoreProvider.apply(store);
        if(! repository.isPresent()) {
            log.error("error accessing rental repository for store {}", store);
            return PagedCollectionAdapter.unexpectedExceptionAdapter();        }
        return PagedCollectionAdapter.<Rental, RentalRequest, Void, RentalAction>builder()
                .crud(new RentalCRUD(Action.actions(Action.RETRIEVE), store, repository.get(), null, null))
                .pager(new DefaultPager("Rental", 10, repository.get()))
                .build();
    }

    public PagedCollectionAdapter<LateRentalTask, ObjectValue, Void, Void> lateRentalTaskAdapter() {
        return PagedCollectionAdapter.<LateRentalTask, ObjectValue, Void, Void>builder()
                .crud(new LateRentalTaskCRUD(this.lateRentalProcessor, this.pool))
                .lister("LateRentalTask", 1000, this.lateRentalProcessor.taskRepository())
                .build();
    }
}

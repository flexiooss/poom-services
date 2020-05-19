package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.*;
import org.codingmatters.poom.demo.domain.billing.CategoryBillingProcessor;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class StoreManager {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StoreManager.class);

    private final Repository<Store, PropertyQuery> storeRepository;
    private final Function<String, Optional<Repository<Movie, PropertyQuery>>> movieRepositoryForStoreProvider;
    private final Function<String, Optional<Repository<Rental, PropertyQuery>>> rentalRepositoryForStoreProvider;

    public StoreManager(Repository<Store, PropertyQuery> storeRepository, Function<String, Optional<Repository<Movie, PropertyQuery>>> movieRepositoryForStoreProvider, Function<String, Optional<Repository<Rental, PropertyQuery>>> rentalRepositoryForStoreProvider) {
        this.storeRepository = storeRepository;
        this.movieRepositoryForStoreProvider = movieRepositoryForStoreProvider;
        this.rentalRepositoryForStoreProvider = rentalRepositoryForStoreProvider;
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

    public GenericResourceAdapter<Movie, MovieCreationData, Movie, Void> categoryMoviesAdpter(String store, String categoryName) {
        try {
            Movie.Category category = Movie.Category.valueOf(categoryName);
            return this.movieAdapter(store, Action.create, category);
        } catch (IllegalArgumentException e) {
            log.info("category movie adapter called for a non existing category : {}", categoryName);
            return new GenericResourceAdapter.NotFoundAdapter<>();
        }
    }

    public GenericResourceAdapter<Movie, MovieCreationData, Movie, Void> storeMoviesAdpter(String store) {
        return this.movieAdapter(store, Action.replace, null);
    }

    private GenericResourceAdapter<Movie, MovieCreationData, Movie, Void> movieAdapter(String store, Set<Action> actions, Movie.Category category) {
        try {
            if(! this.storeExists(store)) {
                log.warn("request on an unexisting store {}", store);
                return new GenericResourceAdapter.NotFoundAdapter<>();
            }
        } catch (RepositoryException e) {
            log.error("error accessing store repository", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }
        Optional<Repository<Movie, PropertyQuery>> movieRepository = this.movieRepositoryForStoreProvider.apply(store);
        if(! movieRepository.isPresent()) {
            log.error("error accessing movie repository for store {}", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }

        return new GenericResourceAdapter.DefaultAdapter<Movie, MovieCreationData, Movie, Void>(
                new MovieCRUD(actions, store, movieRepository.get(), category),
                new MoviePager(movieRepository.get(), category)
        );
    }

    public GenericResourceAdapter<Rental, RentalRequest, Void, RentalAction> movieRentalsAdapter(String store, String movieId) {
        try {
            if(! this.storeExists(store)) {
                log.warn("request on an unexisting store {}", store);
                return new GenericResourceAdapter.NotFoundAdapter<>();
            }
        } catch (RepositoryException e) {
            log.error("error accessing store repository", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }

        Optional<Repository<Movie, PropertyQuery>> movieRepository = this.movieRepositoryForStoreProvider.apply(store);
        if(! movieRepository.isPresent()) {
            log.error("error accessing movie repository for store {}", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }
        Entity<Movie> movie = null;
        try {
            movie = movieRepository.get().retrieve(movieId);
            if(movie == null) {
                log.info("no such movie {} in store {}", movieId, store);
                return new GenericResourceAdapter.NotFoundAdapter<>();
            }
        } catch (RepositoryException e) {
            log.error("error accessing movies in repository for store {}", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }

        System.out.println(movie);
        Optional<Repository<Rental, PropertyQuery>> repository = this.rentalRepositoryForStoreProvider.apply(store);
        if(! repository.isPresent()) {
            log.error("error accessing rental repository for store {}", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }

        return new GenericResourceAdapter.DefaultAdapter<Rental, RentalRequest, Void, RentalAction>(
                new RentalCRUD(Action.createUpdate, store, repository.get(), movie.value(), new CategoryBillingProcessor()),
                new RentalPager(repository.get(), movie.value())
        );
    }

    public GenericResourceAdapter<Rental, RentalRequest, Void, RentalAction> customerRentalsAdapter(String store, String customer) {
        try {
            if(! this.storeExists(store)) {
                log.warn("request on an unexisting store {}", store);
                return new GenericResourceAdapter.NotFoundAdapter<>();
            }
        } catch (RepositoryException e) {
            log.error("error accessing store repository", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }
        if(customer == null || customer.isEmpty()) {
            log.warn("request on an unexisting store {}", store);
            return new GenericResourceAdapter.NotFoundAdapter<>();
        }

        Optional<Repository<Rental, PropertyQuery>> repository = this.rentalRepositoryForStoreProvider.apply(store);
        if(! repository.isPresent()) {
            log.error("error accessing rental repository for store {}", store);
            return new GenericResourceAdapter.UnexpectedExceptionAdapter<>();
        }
        return new GenericResourceAdapter.DefaultAdapter<Rental, RentalRequest, Void, RentalAction>(
                null,
                new CustomerRentalPager(repository.get(), customer)
        );
    }
}

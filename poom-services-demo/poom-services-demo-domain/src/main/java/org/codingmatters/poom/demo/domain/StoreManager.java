package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.MovieCreationData;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class StoreManager {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StoreManager.class);

    private final Repository<Store, PropertyQuery> storeRepository;
    private final Function<String, Optional<Repository<Movie, PropertyQuery>>> movieRepositoryForStoreProvider;

    public StoreManager(Repository<Store, PropertyQuery> storeRepository, Function<String, Optional<Repository<Movie, PropertyQuery>>> movieRepositoryForStoreProvider) {
        this.storeRepository = storeRepository;
        this.movieRepositoryForStoreProvider = movieRepositoryForStoreProvider;
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
}

package org.codingmatters.poom.demo.processor.tests;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.rules.ExternalResource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class StoreManagerResource extends ExternalResource {

    private Repository<Store, PropertyQuery> storeRepository;
    private StoreManager storeManager;
    private AtomicReference<Repository<Movie, PropertyQuery>> nextMovieRepository = new AtomicReference<>();

    @Override
    protected void before() throws Throwable {
        super.before();
        this.storeRepository = InMemoryRepositoryWithPropertyQuery.validating(Store.class);
        this.storeManager = new StoreManager(this.storeRepository, this::movieRepository, null, null, null);
    }

    private Optional<Repository<Movie, PropertyQuery>> movieRepository(String store) {
        return Optional.ofNullable(this.nextMovieRepository.get());
    }

    public StoreManagerResource nextMovieRepository(Repository<Movie, PropertyQuery> repo) {
        this.nextMovieRepository.set(repo);
        return this;
    }

    @Override
    protected void after() {
        super.after();
    }

    public Repository<Store, PropertyQuery> storeRepository() {
        return storeRepository;
    }

    public StoreManager storeManager() {
        return storeManager;
    }

}

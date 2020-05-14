package org.codingmatters.poom.demo.processor.tests;

import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.rules.ExternalResource;

public class StoreManagerResource extends ExternalResource {

    private Repository<Store, PropertyQuery> storeRepository;
    private StoreManager storeManager;

    @Override
    protected void before() throws Throwable {
        super.before();
        this.storeRepository = InMemoryRepositoryWithPropertyQuery.validating(Store.class);
        this.storeManager = new StoreManager(this.storeRepository);
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

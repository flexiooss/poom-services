package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

public class StoreManager {
    private final Repository<Store, PropertyQuery> storeRepository;

    public StoreManager(Repository<Store, PropertyQuery> storeRepository) {
        this.storeRepository = storeRepository;
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

}

package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.demo.domain.spec.store.Address;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StoreManagerTest {

    public static final Store STORE = Store.builder()
            .name("NETFLIX")
            .address(Address.builder()
                    .street("100 Winchester Circle")
                    .postalCode("95032")
                    .town("Los Gatos, CA")
                    .country("USA")
                    .build()).build();

    private final Repository<Store, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Store.class);
    private final StoreManager manager = new StoreManager(repository);

    @Test
    public void givenRepositoryIsEmpty__whenLookingUpExistenceOfAStore__thenFalse() throws Exception {
        assertThat(manager.storeExists("whatever"), is(false));
    }

    @Test
    public void givenRepositoryHasValues__whenLookingUpExistenceOfAStore_andAStoreWithThatNameExists__thenTrue() throws Exception {
        this.repository.create(STORE);
        assertThat(manager.storeExists(STORE.name()), is(true));
    }

    @Test
    public void givenRepositoryHasValues__whenLookingUpExistenceOfAStore_andNoStoreWithThatNameExists__thenFalse() throws Exception {
        this.repository.create(STORE);
        assertThat(manager.storeExists("no sich store"), is(false));
    }
}
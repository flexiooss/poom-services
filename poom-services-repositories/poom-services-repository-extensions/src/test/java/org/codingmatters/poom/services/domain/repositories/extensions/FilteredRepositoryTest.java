package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.tests.Dummy3;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FilteredRepositoryTest {

    private Repository<Dummy3, PropertyQuery> delegateRepository;
    private Repository<Dummy3, PropertyQuery> repository;

    private Repository<Dummy3, PropertyQuery> createDelegateRepository() {
        return InMemoryRepositoryWithPropertyQuery.validating(Dummy3.class);
    }

    @Before
    public void setUp() throws Exception {
        this.delegateRepository = this.createDelegateRepository();
        this.repository = new FilteredRepository<>(Dummy3.class, delegateRepository, "boolProp == true", value -> value.withBoolProp(true));

        for (int i = 0; i < 100; i++) {
            Boolean bool;
            switch (i % 3) {
                case 0:
                    bool = true;
                    break;
                case 1:
                    bool = false;
                    break;
                default:
                    bool = null;
            }
            Dummy3 value = Dummy3.builder()
                    .stringProp("%03d", i)
                    .integerProp(i)
                    .boolProp(bool)
                    .build();
            Entity<Dummy3> e = this.delegateRepository.create(value);
        }
    }

    /*ALL*/
    @Test
    public void whenAll_thenAllFilteredValuesReturned() throws RepositoryException {
        PagedEntityList<Dummy3> all = this.repository.all(0, 1000);
        assertThat(all.total(), is(34L));
        for (Entity<Dummy3> entity : all) {
            assertThat(entity.value().boolProp(), is(true));
        }
        assertThat(all.get(0).value().integerProp(), is(0));
        assertThat(all.get(1).value().integerProp(), is(3));
        assertThat(all.get(33).value().integerProp(), is(99));
    }

    /*SEARCH*/
    @Test
    public void givenNoAdditionalFilter_whenSearch_thenAllFilteredValuesReturned() throws RepositoryException {
        PagedEntityList<Dummy3> search = this.repository.search(PropertyQuery.builder().build(), 0, 1000);
        assertThat(search.total(), is(34L));
        for (Entity<Dummy3> Dummy3Entity : search) {
            assertThat(Dummy3Entity.value().boolProp(), is(true));
        }
    }

    @Test
    public void givenAdditionalFilter_whenSearch_thenFilteredValuesReturned() throws RepositoryException {
        PagedEntityList<Dummy3> search = this.repository.search(PropertyQuery.builder().filter("(integerProp >= 28 && integerProp <= 30) || (integerProp >= 42 && integerProp <= 44)").build(), 0, 1000);
        assertThat(search.total(), is(2L));
        for (Entity<Dummy3> Dummy3Entity : search) {
            assertThat(Dummy3Entity.value().boolProp(), is(true));
            int integerValue = Dummy3Entity.value().integerProp();
            assertThat((integerValue >= 28 && integerValue <= 30) || (integerValue >= 42 && integerValue <= 44), is(true));
        }
        assertThat(search.get(0).value().integerProp(), is(30));
        assertThat(search.get(1).value().integerProp(), is(42));
    }

    @Test
    public void givenOrderNoFilter_whenSearch_thenOrderedValuesReturned() throws RepositoryException {
        PagedEntityList<Dummy3> search = this.repository.search(PropertyQuery.builder().sort("integerProp desc").build(), 0, 1000);
        assertThat(search.total(), is(34L));
        assertThat(search.get(0).value().integerProp(), is(99));
        assertThat(search.get(32).value().integerProp(), is(3));
        assertThat(search.get(33).value().integerProp(), is(0));
    }

    @Test
    public void givenAdditionalFilterAndSort_whenSearch_thenFilteredOrderedValuesReturned() throws RepositoryException {
        PagedEntityList<Dummy3> search = this.repository.search(PropertyQuery.builder().filter("(integerProp >= 28 && integerProp <= 30) || (integerProp >= 42 && integerProp <= 44)").sort("integerProp desc").build(), 0, 1000);
        assertThat(search.total(), is(2L));
        for (Entity<Dummy3> entity : search) {
            assertThat(entity.value().boolProp(), is(true));
            int integerValue = entity.value().integerProp();
            assertThat((integerValue >= 28 && integerValue <= 30) || (integerValue >= 42 && integerValue <= 44), is(true));
        }
        assertThat(search.get(0).value().integerProp(), is(42));
        assertThat(search.get(1).value().integerProp(), is(30));
    }

    @Test
    public void givenBadFilter_whenSearch_thenError() throws RepositoryException {
        Assert.assertThrows("unparseable query : (propBool == true) && (cpt)", RepositoryException.class, ()-> this.repository.search(PropertyQuery.builder().filter("cpt").sort("integerProp escalier").build(), 0, 1000));
    }

    /*RETRIEVE*/

    @Test
    public void givenEntityExistsAndMatches_whenRetrieve_thenEntity() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.delegateRepository.create(Dummy3.builder()
                .boolProp(true)
                .build());
        Entity<Dummy3> retrievedEntity = this.repository.retrieve(createdEntity.id());
        assertThat(retrievedEntity, is(createdEntity));
    }

    @Test
    public void givenEntityDoesNotExist_whenRetrieve_thenNull() throws RepositoryException {
        Entity<Dummy3> retrievedEntity = this.repository.retrieve("not_found");
        assertThat(retrievedEntity, nullValue());
    }

    @Test
    public void givenEntityExistsAndNotMatches_whenRetrieve_thenNull() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.delegateRepository.create(Dummy3.builder()
                .boolProp(false)
                .build());
        Entity<Dummy3> retrievedEntity = this.repository.retrieve(createdEntity.id());
        assertThat(retrievedEntity, nullValue());
    }

    /*CREATE*/
    @Test
    public void givenEntityInitializer_whenCreate_thenUseInitializedValues() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.repository.create(Dummy3.builder()
                .boolProp(false)
                .integerProp(42)
                .build());
        assertThat(createdEntity.value(), is(Dummy3.builder()
                .boolProp(true)
                .integerProp(42)
                .build()));
    }

    @Test
    public void givenEntityInitializer_whenCreateWithId_thenUseInitializedValues() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.repository.createWithId("some-id", Dummy3.builder()
                .boolProp(false)
                .integerProp(42)
                .build());
        assertThat(createdEntity.id(), is("some-id"));
        assertThat(createdEntity.value(), is(Dummy3.builder()
                .boolProp(true)
                .integerProp(42)
                .build()));
    }

    @Test
    public void givenEntityInitializer_whenCreateWithIdAndVersion_thenUseInitializedValues() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.repository.createWithIdAndVersion("some-id", BigInteger.TEN, Dummy3.builder()
                .boolProp(false)
                .integerProp(42)
                .build());
        assertThat(createdEntity.id(), is("some-id"));
        assertThat(createdEntity.version(), is(BigInteger.TEN));
        assertThat(createdEntity.value(), is(Dummy3.builder()
                .boolProp(true)
                .integerProp(42)
                .build()));
    }

    /*UPDATE*/
    @Test
    public void givenExistingEntityMatchingFilter_whenUpdate_thenUpdate() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.delegateRepository.create(Dummy3.builder()
                .boolProp(true)
                .integerProp(12)
                .build());
        Dummy3 updateData = Dummy3.builder().integerProp(42).boolProp(true).build();
        Entity<Dummy3> updatedEntity = this.repository.update(createdEntity, updateData);
        assertThat(updatedEntity.value(), is(Dummy3.builder()
                .boolProp(true)
                .integerProp(42)
                .build()
        ));
    }

    @Test
    public void givenExistingEntityMatchingFilterAndUpdateDataNotMatches_whenUpdate_thenUpdateWithInitializedEntity() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.delegateRepository.create(Dummy3.builder()
                .boolProp(true)
                .integerProp(12)
                .build());
        Dummy3 updateData = Dummy3.builder().integerProp(42).boolProp(false).build();
        Entity<Dummy3> updatedEntity = this.repository.update(createdEntity, updateData);
        assertThat(updatedEntity.value(), is(Dummy3.builder()
                .boolProp(true)
                .integerProp(42)
                .build()
        ));
    }

    @Test
    public void givenExistingEntityNotMatchingFilter_whenUpdate_thenError() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.delegateRepository.create(Dummy3.builder()
                .boolProp(false)
                .integerProp(12)
                .build());
        Dummy3 updateData = Dummy3.builder().integerProp(42).build();
        Assert.assertThrows("retrieved entity does not match filter", RepositoryException.class, () -> this.repository.update(createdEntity, updateData));
    }

    /*DELETE*/
    @Test
    public void givenEntityMatching_whenDelete_thenDelete() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.delegateRepository.create(Dummy3.builder()
                .boolProp(true)
                .integerProp(12)
                .build());
        assertThat(delegateRepository.all(0, 1000).total(), is(101L));
        this.repository.delete(createdEntity);
        assertThat(delegateRepository.all(0, 1000).total(), is(100L));
    }

    @Test
    public void givenEntityNotMatching_whenDelete_thenError() throws RepositoryException {
        Entity<Dummy3> createdEntity = this.delegateRepository.create(Dummy3.builder()
                .boolProp(false)
                .integerProp(12)
                .build());
        assertThat(delegateRepository.all(0, 1000).total(), is(101L));

        Assert.assertThrows("retrieved entity does not match filter", RepositoryException.class, () -> this.repository.delete(createdEntity));
        assertThat(delegateRepository.all(0, 1000).total(), is(101L));
    }

    @Test
    public void givenSomeMatchingEntities_whenDeleteFromQuery_thenDelete() throws RepositoryException {
        assertThat(delegateRepository.all(0, 1000).total(), is(100L));
        this.repository.deleteFrom(PropertyQuery.builder().build());
        PagedEntityList<Dummy3> all = delegateRepository.all(0, 1000);
        assertThat(all.total(), is(66L));
        for (Entity<Dummy3> entity : all) {
            assertThat(entity.value().boolProp(), not(is(true)));
        }
    }
}

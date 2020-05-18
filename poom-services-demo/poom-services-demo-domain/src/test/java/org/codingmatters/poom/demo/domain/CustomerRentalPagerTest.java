package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class CustomerRentalPagerTest {

    Repository<Rental, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Rental.class);

    @Before
    public void setUp() throws Exception {
        this.repository.create(Rental.builder().id("1.1").customer("c1").build());
        this.repository.create(Rental.builder().id("1.2").customer("c1").build());
        this.repository.create(Rental.builder().id("2.1").customer("c2").build());
        this.repository.create(Rental.builder().id("2.2").customer("c2").build());
    }

    @Test
    public void givenRepositoryHasRentalsForCustomer1and2__whenGettingAllCustomer1Rentals__thenOnlyCustomer1RentalsAreReturned() throws Exception {
        assertThat(
                new CustomerRentalPager(this.repository, "c1").all(0, 1000).valueList().stream().map(rental -> rental.id()).toArray(),
                is(arrayContaining("1.1", "1.2"))
        );
    }

    @Test
    public void givenRepositoryHasRentalsForCustomer1and2__whenGettingSearchingCustomer1Rentals_andNullFilter__thenOnlyCustomer1RentalsAreReturned() throws Exception {
        assertThat(
                new CustomerRentalPager(this.repository, "c1").search(PropertyQuery.builder().build(), 0, 1000).valueList().stream().map(rental -> rental.id()).toArray(),
                is(arrayContaining("1.1", "1.2"))
        );
    }

    @Test
    public void givenRepositoryHasRentalsForCustomer1and2__whenGettingSearchingCustomer1Rentals_andEmptyFilter__thenOnlyCustomer1RentalsAreReturned() throws Exception {
        assertThat(
                new CustomerRentalPager(this.repository, "c1").search(PropertyQuery.builder().filter("").build(), 0, 1000).valueList().stream().map(rental -> rental.id()).toArray(),
                is(arrayContaining("1.1", "1.2"))
        );
    }

    @Test
    public void givenRepositoryHasRentalsForCustomer1and2__whenGettingSearchingCustomer1Rentals_andFilter__thenOnlyCustomer1RentalsAreReturned() throws Exception {
        assertThat(
                new CustomerRentalPager(this.repository, "c1").search(PropertyQuery.builder().filter("id == '1.1'").build(), 0, 1000).valueList().stream().map(rental -> rental.id()).toArray(),
                is(arrayContaining("1.1"))
        );
    }
}
package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;


public class RentalPagerTest {

    static public Movie MOVIE1 = Movie.builder()
            .id("12")
            .title("Vertigo")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.REGULAR)
            .build();

    static public Rental[] MOVIE1_RENTALS = new Rental[] {
            Rental.builder().id("M1.1").movie(MOVIE1).customer("cust1").build(),
            Rental.builder().id("M1.2").movie(MOVIE1).customer("cust1").build(),
            Rental.builder().id("M1.3").movie(MOVIE1).customer("cust2").build()
    };

    static public Movie MOVIE2 = Movie.builder()
            .id("42")
            .title("Psycho")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.HORROR)
            .build();

    static public Rental[] MOVIE2_RENTALS = new Rental[] {
            Rental.builder().id("M2.1").movie(MOVIE2).customer("cust1").build()
    };

    private final Repository<Rental, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Rental.class);

    @Before
    public void setUp() throws Exception {
        for (Rental rental : MOVIE1_RENTALS) {
            this.repository.createWithId(rental.id(), rental);
        }
        for (Rental rental : MOVIE2_RENTALS) {
            this.repository.createWithId(rental.id(), rental);
        }
    }

    @Test
    public void givenRentalPagerOnMovie1Films__whenListingAll__thenOnlyMovie1RentalsReturned() throws Exception {
        assertThat(new RentalPager(this.repository, MOVIE1).lister().all(0, 1000).valueList(), contains(MOVIE1_RENTALS));
    }

    @Test
    public void givenRentalPagerOnMovie1Films__whenListingSearch_andNoFilter__thenOnlyMovie1RentalsReturned() throws Exception {
        assertThat(
                new RentalPager(this.repository, MOVIE1).lister().search(
                        PropertyQuery.builder().build(),
                        0, 1000).valueList(),
                contains(MOVIE1_RENTALS)
        );
    }

    @Test
    public void givenRentalPagerOnMovie1Films__whenListingSearch_andFilterEmpty__thenOnlyMovie1RentalsReturned() throws Exception {
        assertThat(
                new RentalPager(this.repository, MOVIE1).lister().search(
                        PropertyQuery.builder().filter("").build(),
                        0, 1000).valueList(),
                contains(MOVIE1_RENTALS)
        );
    }

    @Test
    public void givenRentalPagerOnMovie1Films__whenListingSearch_andFilterSetted__thenFilteredMovie1RentalsReturned() throws Exception {
        assertThat(
                new RentalPager(this.repository, MOVIE1).lister().search(
                        PropertyQuery.builder().filter("customer == 'cust1'").build(),
                        0, 1000).valueList(),
                contains(Arrays.stream(MOVIE1_RENTALS).filter(rental -> rental.customer().equals("cust1")).toArray())
        );
    }

    @Test
    public void givenRentalPagerOnMovie2Films__whenListingAll__thenOnlyMovie2RentalsReturned() throws Exception {
        assertThat(new RentalPager(this.repository, MOVIE2).lister().all(0, 1000).valueList(), contains(MOVIE2_RENTALS));
    }


}
package org.codingmatters.poom.demo.domain.billing;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;
import org.codingmatters.poom.demo.domain.billing.CategoryBillingProcessor;
import org.codingmatters.poom.services.support.date.UTC;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CategoryBillingProcessorTest {

    private final CategoryBillingProcessor billingProcessor = new CategoryBillingProcessor();

    @Test
    public void givenRegularMovie__whenRentedLessThan2Days__thenPriceIs3_renterPointsIs20() throws Exception {
        assertThat(
                this.billingProcessor.process(Rental.builder()
                        .movie(Movie.builder().category(Movie.Category.REGULAR).build())
                        .start(UTC.now().minusDays(1L)).build(), UTC.now()),

                is(Billing.builder()
                        .price(3.0)
                        .frequentRenterPoints(20L)
                        .build()));
    }

    @Test
    public void givenRegularMovie__whenRentedMoreThan2Days__thenPriceIs3Plus5PerAdditionalDays_renterPointsIs20() throws Exception {
        assertThat(
                this.billingProcessor.process(Rental.builder()
                        .movie(Movie.builder().category(Movie.Category.REGULAR).build())
                        .start(UTC.now().minusDays(4L)).build(), UTC.now()),

                is(Billing.builder()
                        .price(3.0 + 2 * 5.0)
                        .frequentRenterPoints(20L)
                        .build()));
    }

    @Test
    public void givenHorrorMovie__whenRentedLessThan3Days__thenPriceIs2_renterPointsIs10() throws Exception {
        assertThat(
                this.billingProcessor.process(Rental.builder()
                        .movie(Movie.builder().category(Movie.Category.HORROR).build())
                        .start(UTC.now().minusDays(1L)).build(), UTC.now()),

                is(Billing.builder()
                        .price(2.0)
                        .frequentRenterPoints(10L)
                        .build()));
    }

    @Test
    public void givenHorrorMovie__whenRentedMoreThan3Days__thenPriceIs2Plus1PerAdditionalDay_renterPointsIs10() throws Exception {
        assertThat(
                this.billingProcessor.process(Rental.builder()
                        .movie(Movie.builder().category(Movie.Category.HORROR).build())
                        .start(UTC.now().minusDays(5L)).build(), UTC.now()),
                is(Billing.builder()
                        .price(2.0 + 2 * 1)
                        .frequentRenterPoints(10L)
                        .build()));
    }

    @Test
    public void givenChildrenMovie__thenPriceIs1PerDay_renterPointsIs5PerDay() throws Exception {
        assertThat(
                this.billingProcessor.process(Rental.builder()
                        .movie(Movie.builder().category(Movie.Category.CHILDREN).build())
                        .start(UTC.now().minusDays(10L)).build(), UTC.now()),
                is(Billing.builder()
                        .price(10 * 1.0)
                        .frequentRenterPoints(10 * 5L)
                        .build()));
    }
}
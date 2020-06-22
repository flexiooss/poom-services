package org.codingmatters.poom.demo.domain.billing;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;

import java.time.LocalDateTime;

public class CategoryBillingProcessor implements BillingProcessor {
    private final Movie movie;

    public CategoryBillingProcessor(Movie movie) {
        this.movie = movie;
    }

    @Override
    public Billing process(Rental rental, LocalDateTime at) {
        switch (this.movie.category()) {
            case REGULAR:
                return new RegularBillingProcessor().process(rental, at);
            case HORROR:
                return new HorrorBillingProcessor().process(rental, at);
            case CHILDREN:
                return new ChildrenBillingProcessor().process(rental, at);
        }
        return null;
    }
}

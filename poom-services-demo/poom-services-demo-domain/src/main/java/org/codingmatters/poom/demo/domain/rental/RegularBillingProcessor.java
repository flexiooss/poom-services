package org.codingmatters.poom.demo.domain.rental;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;

import java.time.Duration;
import java.time.LocalDateTime;

public class RegularBillingProcessor implements BillingProcessor {
    @Override
    public Billing process(Rental rental, LocalDateTime at) {
        long daysRented = Duration.between(rental.start(), at).toDays();
        long additional = daysRented > 2 ? daysRented - 2 : 0;
        return Billing.builder()
                .price(3.0 + additional * 5.0)
                .frequentRenterPoints(20L)
                .build();
    }
}

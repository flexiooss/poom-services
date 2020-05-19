package org.codingmatters.poom.demo.domain.billing;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;

import java.time.Duration;
import java.time.LocalDateTime;

public class HorrorBillingProcessor implements BillingProcessor {
    @Override
    public Billing process(Rental rental, LocalDateTime at) {
        long daysRented = Duration.between(rental.start(), at).toDays();
        long additional = daysRented > 3 ? daysRented - 3 : 0;
        return Billing.builder()
                .price(2.0 + additional * 1.0)
                .frequentRenterPoints(10L)
                .build();
    }
}

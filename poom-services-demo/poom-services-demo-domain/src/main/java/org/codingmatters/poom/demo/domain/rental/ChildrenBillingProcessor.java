package org.codingmatters.poom.demo.domain.rental;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;

import java.time.Duration;
import java.time.LocalDateTime;

public class ChildrenBillingProcessor implements BillingProcessor {
    @Override
    public Billing process(Rental rental, LocalDateTime at) {
        long daysRented = Duration.between(rental.start(), at).toDays();
        return Billing.builder()
                .price(daysRented * 1.0)
                .frequentRenterPoints(daysRented * 5)
                .build();
    }

}

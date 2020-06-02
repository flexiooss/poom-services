package org.codingmatters.poom.demo.domain.billing;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;

import java.time.LocalDateTime;

@FunctionalInterface
public interface BillingProcessor {
    Billing process(Rental rental, LocalDateTime at);
}

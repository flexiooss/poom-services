package org.codingmatters.poom.services.domain.entities;

import java.math.BigInteger;

/**
 * Created by nelt on 6/2/17.
 */
public interface Entity<V> {
    String id();
    BigInteger version();
    V value();
}

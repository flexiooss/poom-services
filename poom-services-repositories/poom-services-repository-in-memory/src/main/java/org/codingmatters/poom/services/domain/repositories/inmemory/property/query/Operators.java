package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

public interface Operators {

    static boolean eq(Object left, Object right) {
        left = normalized(left);
        right = normalized(right);

        if(left == null) {
            return right == null;
        }
        if(right == null) return false;

        return Objects.equals(normalized(left), normalized(right));
    }

    static boolean gt(Object left, Object right, boolean strict) {
        left = normalized(left);
        right = normalized(right);

        if(left == null) {
            return false;
        }

        if(right instanceof Comparable) {
            if(left instanceof Comparable) {
                if(strict) {
                    return ((Comparable)left).compareTo(right) > 0;
                } else {
                    return ((Comparable)left).compareTo(right) >= 0;
                }
            } else {
                return false;
            }
        }

        return false;
    }

    static boolean lt(Object left, Object right, boolean strict) {
        left = normalized(left);
        right = normalized(right);

        if(left == null) {
            return false;
        }

        if(right instanceof Comparable) {
            if(left instanceof Comparable) {
                if(strict) {
                    return ((Comparable)left).compareTo(right) < 0;
                } else {
                    return ((Comparable)left).compareTo(right) <= 0;
                }
            } else {
                return false;
            }
        }

        return false;
    }

    static boolean startsWith(Object left, Object right) {
        left = normalized(left);
        right = normalized(right);

        if(left == null) return right == null;
        return left.toString().startsWith(right.toString());
    }

    static boolean endsWith(Object left, Object right) {
        left = normalized(left);
        right = normalized(right);

        if(left == null) return right == null;
        return left.toString().endsWith(right.toString());
    }

    static boolean contains(Object left, Object right) {
        left = normalized(left);
        right = normalized(right);

        if(left == null) return right == null;
        return left.toString().contains(right.toString());
    }


    static Object normalized(Object o) {
        if(o == null) return null;
        if(o instanceof Number) {
            return new BigDecimal("" + o.toString());
        }
        return o;
    }
}

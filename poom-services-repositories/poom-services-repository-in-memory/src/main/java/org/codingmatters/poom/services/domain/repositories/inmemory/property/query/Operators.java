package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

public interface Operators {

    static boolean eq(Object left, Object right) {
        return Objects.equals(normalized(left), normalized(right));
    }

    static Object normalized(Object o) {
        if(o == null) return o;

        if(o instanceof LocalDateTime) return DATETIMEONLY_FORMATTER.format((TemporalAccessor) o);
        if(o instanceof LocalDate) return DATEONLY_FORMATTER.format((TemporalAccessor) o);
        if(o instanceof LocalTime) return TIMEONLY_FORMATTER.format((TemporalAccessor) o);

        return o;
    }

    static boolean gt(Object left, Object right, boolean strict) {
        left = normalized(left);
        right = normalized(right);

        if(left == null) {
            return false;
        }
        if(right instanceof Number) {
            if(left instanceof Number) {
                if(strict) {
                    return ((Number)left).doubleValue() > ((Number)right).doubleValue();
                } else {
                    return ((Number)left).doubleValue() >= ((Number)right).doubleValue();
                }
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
        if(right instanceof Number) {
            if(left instanceof Number) {
                if(strict) {
                    return ((Number)left).doubleValue() < ((Number)right).doubleValue();
                } else {
                    return ((Number)left).doubleValue() <= ((Number)right).doubleValue();
                }
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

    DateTimeFormatter DATEONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter TIMEONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss[.SSS]['Z']");
    DateTimeFormatter DATETIMEONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]['Z']");
}

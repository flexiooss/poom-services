package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public interface Operators {

    static boolean eq(Object left, Object right) {
        left = normalized(left);
        right = normalized(right);

        if (left == null) {
            return right == null;
        }
        if (right == null) return false;

        return Objects.equals(normalized(left), normalized(right));
    }

    static boolean gt(Object left, Object right, boolean strict) {
        left = normalized(left);
        right = normalized(right);

        if (left == null) {
            return false;
        }

        if (right instanceof Comparable) {
            if (left instanceof Comparable) {
                if (strict) {
                    return ((Comparable) left).compareTo(right) > 0;
                } else {
                    return ((Comparable) left).compareTo(right) >= 0;
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

        if (left == null) {
            return false;
        }

        if (right instanceof Comparable) {
            if (left instanceof Comparable) {
                if (strict) {
                    return ((Comparable) left).compareTo(right) < 0;
                } else {
                    return ((Comparable) left).compareTo(right) <= 0;
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

        if (left == null) return right == null;
        return left.toString().startsWith(right.toString());
    }

    static boolean endsWith(Object left, Object right) {
        left = normalized(left);
        right = normalized(right);

        if (left == null) return right == null;
        return left.toString().endsWith(right.toString());
    }

    static boolean startsWithOne(Object left, List<Object> right) {
        left = normalized(left);
        for (Object o : right) {
            Object value = normalized(o);
            if (left == null) {
                if (value == null) return true;
            } else {
                if (left.toString().startsWith(value.toString())) return true;
            }
        }
        return false;
    }

    static boolean endsWithOne(Object left, List<Object> right) {
        left = normalized(left);
        for (Object o : right) {
            Object value = normalized(o);
            if (left == null) {
                if (value == null) return true;
            } else {
                if (left.toString().endsWith(value.toString())) return true;
            }
        }
        return false;
    }

    static boolean containsOne(Object left, Collection<Object> right) {
        left = normalized(left);
        if(left instanceof Collection) {
            for (Object leftValue : ((Collection) left)) {
                if(containsOne(leftValue, right)) {
                    return true;
                }
            }
        } else if (left instanceof Object[]) {
            for (Object leftValue : ((Object[]) left)) {
                if (containsOne(leftValue, right)) {
                    return true;
                }
            }
        } else {
            for (Object o : right) {
                Object value = normalized(o);
                if (left == null) {
                    if (value == null) {
                        return true;
                    }
                } else {
                    if(left instanceof String) {
                        if (left.toString().contains(value.toString())) {
                            return true;
                        }
                    } else {
                        if(left.equals(value)) return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean containsAll(Object left, Collection<Object> right) {
        left = normalized(left);
        if(left instanceof Collection) {
            for (Object o : right) {
                o = normalized(o);
                if(! containsOne(o, ((Collection<Object>) left))) return false;
            }
        } else {
            for (Object o : right) {
                Object value = normalized(o);
                if (left == null) {
                    if (value != null) return false;
                } else {
                    if (left instanceof String) {
                        if (!left.toString().contains(value.toString())) return false;
                    } else {
                        if (!left.equals(value)) return false;
                    }
                }
            }
        }
        return true;
    }

    static boolean in(Object left, List<Object> right) {
        return right == null ? false : right.contains(left);
    }

    static Object normalized(Object o) {
        if (o == null) return null;
        if (o instanceof Number) {
            return new BigDecimal("" + o.toString());
        }
        if (o instanceof LocalDate) {
            return ((LocalDate) o).atStartOfDay();
        }
        if (o instanceof LocalTime) {
            return ((LocalTime) o).atDate(LocalDate.EPOCH);
        }
        return o;
    }
}

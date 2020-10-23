package org.codingmatters.poom.services.tests;

import org.codingmatters.poom.services.tests.impl.DateMatcher;
import org.hamcrest.Matcher;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

public class DateMatchers {

    public static final int DEFAULT_AROUND_PRECISION = 1000;

    static private DateMatcher INSTANCE = new DateMatcher(DEFAULT_AROUND_PRECISION);

    static public DateMatcher defaults() {
        return INSTANCE;
    }

    static public DateMatcher withPrecision(int precision) {
        return new DateMatcher(precision);
    }


    static public LocalDateTime atMilliPrecision(LocalDateTime from) {
        return INSTANCE.atMilliPrecision(from);
    }

    static public LocalTime atMilliPrecision(LocalTime from) {
        return INSTANCE.atMilliPrecision(from);
    }

    static public Date plus(Date now, long value, int CALENDAR_CONSTANT) {
        return INSTANCE.plus(now, value, CALENDAR_CONSTANT);
    }

    static public Matcher<Date> around(Date date) {
        return INSTANCE.around(date);
    }

    static public Matcher<LocalDateTime> around(LocalDateTime date) {
        return INSTANCE.around(date);
    }

    static public Matcher<ZonedDateTime> around(ZonedDateTime date) {
        return INSTANCE.around(date);
    }

    static public Matcher<Long> around(long expected) {
        return INSTANCE.around(expected);
    }

    static public Matcher<Date> after(Date date) {
        return INSTANCE.after(date);
    }

    static public Matcher<LocalDateTime> after(LocalDateTime date) {
        return INSTANCE.after(date);
    }

    static public Matcher<ZonedDateTime> after(ZonedDateTime date) {
        return INSTANCE.after(date);
    }

    static public Matcher<Date> before(Date date) {
        return INSTANCE.before(date);
    }

    static public Matcher<LocalDateTime> before(LocalDateTime date) {
        return INSTANCE.before(date);
    }

    static public Matcher<ZonedDateTime> before(ZonedDateTime date) {
        return INSTANCE.before(date);
    }

    public static Matcher<Number> between(Number min, Number max) {
        return INSTANCE.between(min, max);
    }

    public static Matcher<ZonedDateTime> between(ZonedDateTime min, ZonedDateTime max) {
        return INSTANCE.between(min, max);
    }

    public static Matcher<LocalDateTime> between(LocalDateTime min, LocalDateTime max) {
        return INSTANCE.between(min, max);
    }


    static public Matcher<Optional> isPresent() {
        return INSTANCE.isPresent();
    }
}

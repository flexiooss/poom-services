package org.codingmatters.poom.services.tests.impl;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

public class DateMatcher {
    private final int aroundPrecision;

    public DateMatcher(int aroundPrecision) {
        this.aroundPrecision = aroundPrecision;
    }


    public LocalDateTime atMilliPrecision(LocalDateTime from) {
        return from.with(ChronoField.MICRO_OF_SECOND, 0).with(ChronoField.MILLI_OF_SECOND, from.get(ChronoField.MILLI_OF_SECOND));
    }
    public LocalTime atMilliPrecision(LocalTime from) {
        return from.with(ChronoField.MICRO_OF_SECOND, 0).with(ChronoField.MILLI_OF_SECOND, from.get(ChronoField.MILLI_OF_SECOND));
    }
    public Date plus(Date now, long value, int CALENDAR_CONSTANT) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(CALENDAR_CONSTANT, (int) value);
        return cal.getTime();
    }
    public Matcher<Date> around(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MILLISECOND, this.aroundPrecision);
        Date before = cal.getTime();
        cal.add(Calendar.MILLISECOND, - 2 * this.aroundPrecision);
        Date after = cal.getTime();

        return new TypeSafeMatcher<Date>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a date around ").appendValue(date);
            }

            @Override
            protected boolean matchesSafely(Date item) {
                return item != null && item.after(after) && item.before(before);
            }
        };
    }
    public Matcher<LocalDateTime> around(LocalDateTime date) {
        return new TypeSafeMatcher<LocalDateTime>() {
            @Override
            protected boolean matchesSafely(LocalDateTime item) {
                return item.isAfter(date.minus(aroundPrecision, ChronoUnit.MILLIS)) &&
                        item.isBefore(date.plus(aroundPrecision, ChronoUnit.MILLIS));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a local date time around ").appendValue(date);
            }
        };
    }
    public Matcher<ZonedDateTime> around(ZonedDateTime date) {
        return new TypeSafeMatcher<ZonedDateTime>() {
            @Override
            protected boolean matchesSafely(ZonedDateTime item) {
                return item.isAfter(date.minus(aroundPrecision, ChronoUnit.MILLIS)) &&
                        item.isBefore(date.plus(aroundPrecision, ChronoUnit.MILLIS));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a zoned date time around ").appendValue(date);
            }
        };
    }
    public Matcher<Long> around(long expected) {
        return new TypeSafeMatcher<Long>() {
            @Override
            protected boolean matchesSafely(Long item) {
                return item != null && item > expected - aroundPrecision && item < expected + aroundPrecision;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a timestamp around ").appendValue(expected);
            }
        };
    }
    public Matcher<Date> after(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        Date after = cal.getTime();

        return new TypeSafeMatcher<Date>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a date after ").appendValue(date);
            }

            @Override
            protected boolean matchesSafely(Date item) {
                return item != null && item.after(after);
            }
        };
    }
    public Matcher<LocalDateTime> after(LocalDateTime date) {
        return new TypeSafeMatcher<LocalDateTime>() {
            @Override
            protected boolean matchesSafely(LocalDateTime item) {
                return item.isAfter(date);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a local date time after ").appendValue(date);
            }
        };
    }
    public Matcher<ZonedDateTime> after(ZonedDateTime date) {
        return new TypeSafeMatcher<ZonedDateTime>() {
            @Override
            protected boolean matchesSafely(ZonedDateTime item) {
                return item.isAfter(date);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a zoned date time after ").appendValue(date);
            }
        };
    }


    public Matcher<Date> before(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Date before = cal.getTime();

        return new TypeSafeMatcher<Date>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a date before ").appendValue(date);
            }

            @Override
            protected boolean matchesSafely(Date item) {
                return item != null && item.before(before);
            }
        };
    }

    public Matcher<LocalDateTime> before(LocalDateTime date) {
        return new TypeSafeMatcher<LocalDateTime>() {
            @Override
            protected boolean matchesSafely(LocalDateTime item) {
                return item.isBefore(date);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a local date time before ").appendValue(date);
            }
        };
    }

    public Matcher<ZonedDateTime> before(ZonedDateTime date) {
        return new TypeSafeMatcher<ZonedDateTime>() {
            @Override
            protected boolean matchesSafely(ZonedDateTime item) {
                return item.isBefore(date);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a zoned date time before ").appendValue(date);
            }
        };
    }

    public Matcher<Number> between(Number min, Number max) {
        return new TypeSafeMatcher<Number>() {
            @Override
            protected boolean matchesSafely(Number item) {
                return item.doubleValue() > min.doubleValue() && item.doubleValue() < max.doubleValue();
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public Matcher<ZonedDateTime> between(ZonedDateTime min, ZonedDateTime max) {
        return Matchers.anyOf(
                after(min), before(max)
        );
    }

    public Matcher<LocalDateTime> between(LocalDateTime min, LocalDateTime max) {
        return Matchers.anyOf(
                after(min), before(max)
        );
    }


    public Matcher<Optional> isPresent() {
        return new TypeSafeMatcher<Optional>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("an optional that is present");
            }

            @Override
            protected boolean matchesSafely(Optional optional) {
                return optional.isPresent();
            }
        };
    }
}

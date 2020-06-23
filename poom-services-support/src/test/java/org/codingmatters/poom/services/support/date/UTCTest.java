package org.codingmatters.poom.services.support.date;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class UTCTest {

    @Test
    public void givenConvertingToUTC__whenTZAheadOfUTC__thenResultIsNowMinusTheTZOffset() throws Exception {
        LocalDateTime now = UTC.now();

        LocalDateTime utc = UTC.from(now, TimeZone.getTimeZone("GMT+00:18"));

        assertThat(utc, is(now.minus(18, ChronoUnit.MINUTES)));
    }
    @Test
    public void givenConvertingToUTC__whenTZBackwardOfUTC__thenResultIsNowPlusTheTZOffset() throws Exception {
        LocalDateTime now = UTC.now();

        LocalDateTime utc = UTC.from(now, TimeZone.getTimeZone("GMT-00:18"));

        assertThat(utc, is(now.plus(18, ChronoUnit.MINUTES)));
    }

    @Test
    public void givenConvertingToUTC__whenTZHasSummerWinterOffset_andDateIsInTheWinter__thenUTCIsNowMinusTheWinterOffset() throws Exception {
        LocalDateTime now = LocalDateTime.of(2020, 2, 9, 8, 35);

        LocalDateTime utc = UTC.from(now, TimeZone.getTimeZone("Europe/Paris"));

        assertThat(utc, is(LocalDateTime.of(2020, 2, 9, 7, 35)));
    }

    @Test
    public void givenConvertingToUTC__whenTZHasSummerWinterOffset_andDateIsInTheSummer__thenUTCIsNowMinusTheSummerOffset() throws Exception {
        LocalDateTime now = LocalDateTime.of(2020, 8, 15, 8, 35);

        LocalDateTime utc = UTC.from(now, TimeZone.getTimeZone("Europe/Paris"));

        assertThat(utc, is(LocalDateTime.of(2020, 8, 15, 6, 35)));
    }

    @Test
    public void givenConvertingToUTCFromZonedDateTime__whenTZHasSummerWinterOffset_andDateIsInTheSummer__thenUTCIsNowMinusTheSummerOffset() throws Exception {
        ZonedDateTime zoned = ZonedDateTime.of(LocalDateTime.of(2020, 8, 15, 8, 35), TimeZone.getTimeZone("Europe/Paris").toZoneId());

        assertThat(UTC.from(zoned), is(LocalDateTime.of(2020, 8, 15, 6, 35)));
    }

    @Test
    public void givenConvertingToUTCFromZonedDateTime__whenWinterOffset_andDateIsInTheSummer__thenUTCIsNowMinusTheSummerOffset() throws Exception {
        ZonedDateTime zoned = ZonedDateTime.of(LocalDateTime.of(2020, 2, 9, 8, 35), TimeZone.getTimeZone("Europe/Paris").toZoneId());

        assertThat(UTC.from(zoned), is(LocalDateTime.of(2020, 2, 9, 7, 35)));
    }

    @Test
    public void givenConvertingFromUTC__whenTZAhaedFromUTC__thenConvertedIsUTCPlusZoneOffset() throws Exception {
        LocalDateTime utc = UTC.now();

        LocalDateTime at = UTC.at(utc, TimeZone.getTimeZone("GMT+00:18"));

        assertThat(at, is(utc.plusMinutes(18)));
    }

    @Test
    public void givenConvertingFromUTC__whenTZBackwardFromUTC__thenConvertedIsUTCMinusZoneOffset() throws Exception {
        LocalDateTime utc = UTC.now();

        LocalDateTime at = UTC.at(utc, TimeZone.getTimeZone("GMT-00:18"));

        assertThat(at, is(utc.minusMinutes(18)));
    }
}
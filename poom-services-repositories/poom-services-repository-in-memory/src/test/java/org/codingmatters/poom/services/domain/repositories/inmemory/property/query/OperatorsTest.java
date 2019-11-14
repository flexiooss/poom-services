package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class OperatorsTest {

    @Test
    public void givenLocalDateTime__when__then() throws Exception {
        LocalDateTime aDate = this.now();

        assertTrue(Operators.eq(aDate, aDate));
        assertFalse(Operators.eq(aDate, aDate.plusDays(1L)));
        assertTrue(Operators.eq(aDate, this.format(aDate)));
        assertFalse(Operators.eq(aDate, this.format(aDate.plusDays(1L))));
    }

    @Test
    public void givenLocalDate__when__then() throws Exception {
        LocalDate aDate = this.now().toLocalDate();

        assertTrue(Operators.eq(aDate, aDate));
        assertFalse(Operators.eq(aDate, aDate.plusDays(1L)));
        assertTrue(Operators.eq(aDate, this.format(aDate)));
        assertFalse(Operators.eq(aDate, this.format(aDate.plusDays(1L))));
    }

    @Test
    public void givenLocalTime__when__then() throws Exception {
        LocalTime aDate = this.now().toLocalTime();

        assertTrue(Operators.eq(aDate, aDate));
        assertFalse(Operators.eq(aDate, aDate.plusHours(1L)));
        assertTrue(Operators.eq(aDate, this.format(aDate)));
        assertFalse(Operators.eq(aDate, this.format(aDate.plusHours(1L))));
    }

    private String format(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss[.SSS]['Z']"));
    }

    /*
    DateTimeFormatter TIMEONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss[.SSS]['Z']");
     */
    private String format(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String format(LocalDateTime aDate) {
        return aDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]['Z']"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC.normalized());
    }
}
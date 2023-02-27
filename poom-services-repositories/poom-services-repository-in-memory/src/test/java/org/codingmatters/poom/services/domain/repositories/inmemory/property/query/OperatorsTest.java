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
    }

    @Test
    public void givenLocalDate__when__then() throws Exception {
        LocalDate aDate = this.now().toLocalDate();

        assertTrue(Operators.eq(aDate, aDate));
        assertFalse(Operators.eq(aDate, aDate.plusDays(1L)));
    }

    @Test
    public void givenLocalTime__when__then() throws Exception {
        LocalTime aDate = this.now().toLocalTime();

        assertTrue(Operators.eq(aDate, aDate));
        assertFalse(Operators.eq(aDate, aDate.plusHours(1L)));
    }


    @Test
    public void whenLeftIsLocalDateTime_andRightIsLocalDate__then() throws Exception {
        LocalDateTime left = LocalDateTime.now();
        LocalDate right = LocalDate.now().plusDays(1);

        assertTrue(Operators.lt(left, right, true));
    }

    @Test
    public void whenRightIsLocalDateTime_andLeftIsLocalDate__then() throws Exception {
        LocalDateTime right = LocalDateTime.now();
        LocalDate left = LocalDate.now().minusDays(1);

        assertTrue(Operators.lt(left, right, true));
    }

    @Test
    public void whenRightIsLocalDateTime_andLeftIsLocalDateTime__then() throws Exception {
        LocalDateTime right = LocalDateTime.now();
        LocalDateTime left = LocalDateTime.now().minusDays(1);

        assertTrue(Operators.lt(left, right, true));
    }

    @Test
    public void whenLeftIsLocalTime_andRightIsLocalDate__then() throws Exception {
        LocalTime left = LocalTime.now();
        LocalDate right = LocalDate.now().plusDays(1);

        assertTrue(Operators.lt(left, right, true));
    }

    @Test
    public void whenRightIsLocalTime_andLeftIsLocalDate__then() throws Exception {
        LocalTime right = LocalTime.now();
        LocalDate left = LocalDate.now().plusDays(1);

        assertFalse(Operators.lt(left, right, true));
    }

    @Test
    public void whenLeftIsLocalDateTime_andRightIsLocalTime__then() throws Exception {
        LocalDateTime left = LocalDateTime.now();
        LocalTime right = LocalTime.now();

        assertFalse(Operators.lt(left, right, true));
    }

    @Test
    public void whenRightIsLocalDateTime_andLeftIsLocalTime__then() throws Exception {
        LocalDateTime right = LocalDateTime.now();
        LocalTime left = LocalTime.now();

        assertTrue(Operators.lt(left, right, true));
    }

    @Test
    public void whenLeftIsLocalTime_andRightIsLocalTime__then() throws Exception {
        LocalTime left = LocalTime.now();
        LocalTime right = LocalTime.now().plusMinutes(12);

        assertTrue(Operators.lt(left, right, true));
    }

    public String format(LocalDateTime aDate) {
        return aDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]['Z']"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC.normalized());
    }
}
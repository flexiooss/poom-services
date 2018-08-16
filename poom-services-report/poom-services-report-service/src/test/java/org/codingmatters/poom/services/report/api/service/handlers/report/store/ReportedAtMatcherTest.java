package org.codingmatters.poom.services.report.api.service.handlers.report.store;

import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReportedAtMatcherTest {

    @Test
    public void givenReportedAtPatternIsNotFilled__thenEverythingMatches() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().build()).test("20420812134358123"));
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt(null).build()).test("20420812134358123"));
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("").build()).test("20420812134358123"));
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("  ").build()).test("20420812134358123"));
    }

    @Test
    public void givenReportedAtPatternIsFilled__thenMatchesYear() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042").build()).test("20420812134358123"));
        assertFalse(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2043").build()).test("20420812134358123"));
    }

    @Test
    public void givenReportedAtPatternIsFilled__thenMatchesYearMonth() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08").build()).test("20420812134358123"));
        assertFalse(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-07").build()).test("20420812134358123"));
    }

    @Test
    public void givenReportedAtPatternIsFilled__thenMatchesYearMonthDay() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12").build()).test("20420812134358123"));
        assertFalse(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-11").build()).test("20420812134358123"));
    }

    @Test
    public void givenReportedAtPatternIsFilled__thenMatchesYearMonthDayHour() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 13").build()).test("20420812134358123"));
        assertFalse(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 14").build()).test("20420812134358123"));
    }

    @Test
    public void givenReportedAtPatternIsFilled__thenMatchesYearMonthDayHourMinute() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 13:43").build()).test("20420812134358123"));
        assertFalse(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 14:42").build()).test("20420812134358123"));
    }

    @Test
    public void givenReportedAtPatternIsFilled__thenMatchesYearMonthDayHourMinuteSecond() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 13:43:58").build()).test("20420812134358123"));
        assertFalse(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 14:42:52").build()).test("20420812134358123"));
    }

    @Test
    public void givenReportedAtPatternIsFilled__thenMatchesYearMonthDayHourMinuteSecondMillis() {
        assertTrue(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 13:43:58.123").build()).test("20420812134358123"));
        assertFalse(new ReportedAtMatcher(ReportQuery.builder().reportedAt("2042-08-12 14:42:52.122").build()).test("20420812134358123"));
    }
}
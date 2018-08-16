package org.codingmatters.poom.services.report.api.service.handlers.report.store;

import org.codingmatters.poom.services.report.api.types.Report;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReportDescriptorMatcherTest {

    @Test
    public void givenReportNameIsProvided__whenQueryNameIsProvided__thenMatchesOnName() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().name("start").build()).test(Report.builder().name("starts with").build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().name("start").build()).test(Report.builder().name("doesnt starts with").build()));
    }

    @Test
    public void givenReportNameIsNotProvided__whenQueryNameIsProvided__thenMatchesOnName() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().build()).test(Report.builder().build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().name("doesnt starts with").build()).test(Report.builder().build()));
    }

    @Test
    public void givenReportContainerIdIsProvided__whenContainerIdQueryIdProvided__thenMatchesOnContainerId() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().containerId("start").build()).test(Report.builder().containerId("starts with").build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().containerId("start").build()).test(Report.builder().containerId("doesnt starts with").build()));
    }

    @Test
    public void givenReportMainClassIsProvided__whenMainClassQueryIdProvided__thenMatchesOnMainClass() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().mainClass("start").build()).test(Report.builder().mainClass("starts with").build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().mainClass("start").build()).test(Report.builder().mainClass("doesnt starts with").build()));
    }

    @Test
    public void givenReportExitStatusIsProvided__whenExitStatusQueryIdProvided__thenMatchesOnExitStatus() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().exitStatus("start").build()).test(Report.builder().exitStatus("starts with").build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().exitStatus("start").build()).test(Report.builder().exitStatus("doesnt starts with").build()));
    }

    @Test
    public void givenReportVersionIsProvided__whenVersionQueryIdProvided__thenMatchesOnVersion() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().version("start").build()).test(Report.builder().version("starts with").build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().version("start").build()).test(Report.builder().version("doesnt starts with").build()));
    }

    @Test
    public void givenReportStartIsProvided__whenStartQueryProvided__thenMatchesOnStart() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().start("2015-08").build()).test(Report.builder().start(UTC.now().withYear(2015).withMonth(8)).build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().start("2015-08").build()).test(Report.builder().start(UTC.now().withYear(2015).withMonth(9)).build()));
    }

    @Test
    public void givenReportEndIsProvided__whenEndQueryProvided__thenMatchesOnEnd() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().end("2015-08").build()).test(Report.builder().end(UTC.now().withYear(2015).withMonth(8)).build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().end("2015-08").build()).test(Report.builder().end(UTC.now().withYear(2015).withMonth(9)).build()));
    }

    @Test
    public void givenReportHasDumpIsProvided__whenHasDumpQueryIdProvided__thenMatchesOnHasDump() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().hasDump(true).build()).test(Report.builder().hasDump(true).build()));
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().hasDump(false).build()).test(Report.builder().hasDump(false).build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().hasDump(false).build()).test(Report.builder().hasDump(true).build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().hasDump(true).build()).test(Report.builder().hasDump(false).build()));
    }

    @Test
    public void givenReportIsFilledUp__whenTwoCriteriaQueryProvided__thenMatchesOnBoth() {
        assertTrue(new ReportDescriptorMatcher(ReportQuery.builder().name("start").containerId("start").build()).test(Report.builder().name("starts with").containerId("starts with").build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().name("start").containerId("start").build()).test(Report.builder().name("doesnt starts with").build()));
        assertFalse(new ReportDescriptorMatcher(ReportQuery.builder().name("start").containerId("start").build()).test(Report.builder().containerId("doesnt starts with").build()));
    }
}
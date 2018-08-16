package org.codingmatters.poom.services.report.api.service.handlers;

import org.codingmatters.poom.services.report.api.ReportsGetRequest;
import org.codingmatters.poom.services.report.api.types.ReportQuery;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class QueryBuilderTest {

    @Test
    public void givenQueryParametersPresent__whenBuildingQuery__thenQueryIsFilledUp() {
        assertThat(
                new ReportBrowsing.QueryBuilder().from(ReportsGetRequest.builder()
                        .reportedAt("reportedAt")
                        .name("name")
                        .version("version")
                        .mainClass("Main.class")
                        .containerId("container-id")
                        .start("start")
                        .end("end")
                        .exitStatus("exit-status")
                        .hasDump(true)
                        .build()).get(),
                is(ReportQuery.builder()
                        .reportedAt("reportedAt")
                        .name("name")
                        .version("version")
                        .mainClass("Main.class")
                        .containerId("container-id")
                        .start("start")
                        .end("end")
                        .exitStatus("exit-status")
                        .hasDump(true)
                        .build())
        );
    }

    @Test
    public void givenQueryParametersNotPresent__whenBuildingQuery__thenQueryIsNotPresent() {
        assertFalse(new ReportBrowsing.QueryBuilder().from(ReportsGetRequest.builder().build()).isPresent());
    }
}

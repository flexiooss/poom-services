package org.codingmatters.poom.services.support;

import org.junit.Test;
import org.slf4j.MDC;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/17/17.
 */
public class LoggingContextTest {

    @Test
    public void restoreNullContext() throws Exception {
        assertThat(MDC.getCopyOfContextMap(), is(nullValue()));
        try(LoggingContext context = LoggingContext.start()) {
            MDC.put("new", "value");
            assertThat(MDC.get("new"), is("value"));
        }
        assertThat(MDC.getCopyOfContextMap(), is(nullValue()));
    }

    @Test
    public void restorePreviousContext() throws Exception {
        MDC.put("previous", "value");

        try(LoggingContext context = LoggingContext.start()) {
            MDC.put("new", "value");

            assertThat(MDC.get("previous"), is("value"));
            assertThat(MDC.get("new"), is("value"));
        }

        assertThat(MDC.get("previous"), is("value"));
        assertThat(MDC.get("new"), is(nullValue()));
    }

    @Test
    public void restoreChangedValueFromPreviousContext() throws Exception {
        MDC.put("previous", "value");

        try(LoggingContext context = LoggingContext.start()) {
            MDC.put("new", "value");
            MDC.put("previous", "changed");

            assertThat(MDC.get("previous"), is("changed"));
            assertThat(MDC.get("new"), is("value"));
        }

        assertThat(MDC.get("previous"), is("value"));
        assertThat(MDC.get("new"), is(nullValue()));
    }

    @Test
    public void embeddedContexts() throws Exception {
        MDC.put("before", "value");
        try(LoggingContext context = LoggingContext.start()) {
            MDC.put("first", "value");
            try(LoggingContext subContext = LoggingContext.start()) {
                MDC.put("second", "value");

                assertThat(MDC.get("before"), is(notNullValue()));
                assertThat(MDC.get("first"), is(notNullValue()));
                assertThat(MDC.get("second"), is(notNullValue()));
            }
            assertThat(MDC.get("before"), is(notNullValue()));
            assertThat(MDC.get("first"), is(notNullValue()));
            assertThat(MDC.get("second"), is(nullValue()));
        }
        assertThat(MDC.get("before"), is(notNullValue()));
        assertThat(MDC.get("first"), is(nullValue()));
        assertThat(MDC.get("second"), is(nullValue()));
    }
}

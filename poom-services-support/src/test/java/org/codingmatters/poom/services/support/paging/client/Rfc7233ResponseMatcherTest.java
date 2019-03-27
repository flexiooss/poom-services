package org.codingmatters.poom.services.support.paging.client;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class Rfc7233ResponseMatcherTest {

    @Test
    public void givenNominalResponse__whenParsing__thenValuesAreParsed() throws Exception {
        Rfc7233ResponseMatcher matcher = new Rfc7233ResponseMatcher(new TestClientResponseDelegate()
                .withCode(206)
                .withHeader("content-range", "FlexioEvent 18-756/3014846")
                .withHeader("accept-range", "FlexioEvent 1000")
        );

        assertThat(matcher.first(), Matchers.is(Long.valueOf(18)));
        assertThat(matcher.last(), Matchers.is(Long.valueOf(756)));
        assertThat(matcher.total(), Matchers.is(Long.valueOf(3014846)));
        assertThat(matcher.pageSize(), Matchers.is(Long.valueOf(1000)));
    }
}
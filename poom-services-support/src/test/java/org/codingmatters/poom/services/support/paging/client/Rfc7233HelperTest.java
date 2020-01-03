package org.codingmatters.poom.services.support.paging.client;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

public class Rfc7233HelperTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void givenManyPages__whenFistRequested__thenNextRangeIsNextPage() throws Exception {
        Rfc7233Helper actual = new Rfc7233Helper("Something 0-9/100", "Something 10");
        assertThat(actual.first(), is(0L));
        assertThat(actual.last(), is(9L));
        assertThat(actual.total(), is(100L));
        assertThat(actual.pageSize(), is(10L));
        assertThat(actual.nextRange(), is("10-19"));
    }

    @Test
    public void givenOnePage__whenRequested__thenNextRangeIsStillCalculated() throws Exception {
        Rfc7233Helper actual = new Rfc7233Helper("Something 0-9/10", "Something 10");
        assertThat(actual.first(), is(0L));
        assertThat(actual.last(), is(9L));
        assertThat(actual.total(), is(10L));
        assertThat(actual.pageSize(), is(10L));
        assertThat(actual.nextRange(), is("10-19"));
    }

    @Test
    public void whenUnparseableContentRange__thenUnparseableRfc7233QueryThrown() throws Exception {
        thrown.expect(Rfc7233Helper.UnparseableRfc7233Query.class);
        thrown.expectMessage(startsWith("cannot parse content range from :"));

        Rfc7233Helper actual = new Rfc7233Helper("-9/100", "Something 10");
    }

    @Test
    public void whenUnparseableAcceptRange__thenUnparseableRfc7233QueryThrown() throws Exception {
        thrown.expect(Rfc7233Helper.UnparseableRfc7233Query.class);
        thrown.expectMessage(startsWith("cannot parse accept range from :"));

        Rfc7233Helper actual = new Rfc7233Helper("Something 0-9/100", "beh");
    }



}
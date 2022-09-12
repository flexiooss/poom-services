package org.codingmatters.poom.services.support.process;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class SimpleProcessInvokerTest {

    @Test
    public void whenStatus0__thenResponseStatusIs0_isErrorIsFalse_outIsComandOutput_errIsEmpty() throws Exception {
        SimpleProcessInvoker.ProcessResponse response = SimpleProcessInvoker.invoker(new ProcessInvoker(), new ProcessBuilder()).invoke("ls", "-al");
        System.out.println(response);
        assertThat(response.status(), is(0));
        assertThat(response.isError(), is(false));
        assertThat(response.out(), is(not(emptyOrNullString())));
        assertThat(response.err(), is(emptyOrNullString()));
    }
}
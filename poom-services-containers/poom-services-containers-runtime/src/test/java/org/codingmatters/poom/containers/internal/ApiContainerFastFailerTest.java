package org.codingmatters.poom.containers.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.containers.internal.api.types.Error;
import org.codingmatters.poom.containers.internal.api.types.json.ErrorReader;
import org.codingmatters.poom.fast.failing.exceptions.InsufficientPermissionFFException;
import org.codingmatters.poom.fast.failing.exceptions.NeedsAuthorizationFFException;
import org.codingmatters.poom.fast.failing.exceptions.RecoverableFFException;
import org.codingmatters.poom.fast.failing.exceptions.UnrecoverableFFException;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

class ApiContainerFastFailerTest {


    private AtomicBoolean stopRequested = new AtomicBoolean(false);
    private void requireStop() {
        this.stopRequested.set(true);
    }

    @Test
    void whenUnrecoverableFFException__thenStopRequired_and500Response() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();

        new ApiContainerFastFailer(this::requireStop, response, this.jsonFactory).failAndStop(new UnrecoverableFFException("cannot recover"));

        assertThat(this.stopRequested.get(), is(true));
        assertThat(response.contentType(), is("application/json"));
        assertThat(response.status(), is(500));
        assertThat(from(response.payload()).withToken(null), is(Error.builder()
                .code(Error.Code.UNEXPECTED_ERROR)
                .description("cannot recover")
                .build()));
        assertThat(from(response.payload()).token(), is(notNullValue()));
    }

    @Test
    void whenRecoverableFFException__thenStopNotRequired_and500Response() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();

        new ApiContainerFastFailer(this::requireStop, response, this.jsonFactory).failAndContinue(new RecoverableFFException("will continue") {});

        assertThat(this.stopRequested.get(), is(false));
        assertThat(response.contentType(), is("application/json"));
        assertThat(response.status(), is(500));
        assertThat(from(response.payload()).withToken(null), is(Error.builder()
                .code(Error.Code.UNEXPECTED_ERROR)
                .description("will continue")
                .build()));
        assertThat(from(response.payload()).token(), is(notNullValue()));
    }

    @Test
    void whenInsufficientPermissionFFException__thenStopNotRequired_and403Response() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();

        new ApiContainerFastFailer(this::requireStop, response, this.jsonFactory).failAndContinue(new InsufficientPermissionFFException("forbidden"));

        assertThat(this.stopRequested.get(), is(false));
        assertThat(response.contentType(), is("application/json"));
        assertThat(response.status(), is(403));
        assertThat(from(response.payload()).withToken(null), is(Error.builder()
                .code(Error.Code.BAD_REQUEST)
                .description("forbidden")
                .build()));
        assertThat(from(response.payload()).token(), is(notNullValue()));
    }

    @Test
    void whenNeedsAuthorizationFFException__thenStopNotRequired_and401Response() throws Exception {
        TestResponseDeleguate response = new TestResponseDeleguate();

        new ApiContainerFastFailer(this::requireStop, response, this.jsonFactory).failAndContinue(new NeedsAuthorizationFFException("unauthorized"));

        assertThat(this.stopRequested.get(), is(false));
        assertThat(response.contentType(), is("application/json"));
        assertThat(response.status(), is(401));
        assertThat(from(response.payload()).withToken(null), is(Error.builder()
                .code(Error.Code.UNAUTHORIZED)
                .description("unauthorized")
                .build()));
        assertThat(from(response.payload()).token(), is(notNullValue()));
    }

    private final JsonFactory jsonFactory = new JsonFactory();

    private Error from(byte[] json) throws Exception {
        try(JsonParser parser = this.jsonFactory.createParser(json)) {
            return new ErrorReader().read(parser);
        }
    }
}
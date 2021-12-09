package org.codingmatters.poom.services.runtime.handlers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RequestLoggingStateProviderTest {

    private AtomicReference<RequestLoggingStateProvider> state = new AtomicReference<>(RequestLoggingStateProvider.NEVER);

    @Rule
    public UndertowResource server = new UndertowResource(new CdmHttpUndertowHandler(
            new RequestLoggingProcessor(
                    (requestDelegate, responseDelegate) -> responseDelegate.status(200),
                    requestDelegate -> this.state.get().shouldLog(requestDelegate),
                    new JsonFactory()
            )

    ));

    private final OkHttpClient client = new OkHttpClient();
    private ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @Before
    public void setUp() throws Exception {
        this.logAppender.start();
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RequestLoggingProcessor.class);
        logger.addAppender(this.logAppender);
    }

    @Test
    public void givenShouldNotLog__whenGet__thenRequestNotLogged() throws Exception {
        this.state.set(RequestLoggingStateProvider.NEVER);

        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl()).get().build()).execute();

        assertThat(response.code(), is(200));
        assertThat(this.logAppender.list, is(empty()));
    }

    @Test
    public void givenShouldLog__whenGet__thenRequestLogged() throws Exception {
        this.state.set(RequestLoggingStateProvider.ALWAYS);

        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl()).get().build()).execute();

        assertThat(response.code(), is(200));
        assertThat(this.logAppender.list, hasSize(1));
    }
}
package org.codingmatters.poom.services.runtime.handlers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RequestLoggingProcessorTest {

    public static final Processor SUCCESS_200 = (requestDelegate, responseDelegate) -> {
        responseDelegate.status(200);
        responseDelegate.addHeader("one-val", "value");
        responseDelegate.addHeader("many-vals", "v1", "v2");

    };
    public static final Processor FAILURE = (requestDelegate, responseDelegate) -> {
        throw new RuntimeException("unchecked");
    };
    private static final Processor BODY_READ = (requestDelegate, responseDelegate) -> {
        responseDelegate.status(200);
        StringBuilder body = new StringBuilder();
        try(InputStream in = requestDelegate.payload(); Reader reader = new InputStreamReader(in)) {
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer); read != -1; read = reader.read(buffer)) {
                body.append(buffer, 0, read);
            }
        }
        System.out.println("read body :: " + body.toString());
    };
    public static final String SMALL_JSON = "{\"response\":\"body\"}";
    public static final Processor SUCCESS_200_WITH_SMALL_JSON_BODY_AND_HEADER_CONTENT_TYPE = (requestDelegate, responseDelegate) -> {
        responseDelegate.status(200);
        responseDelegate.addHeader("content-type", "application/json");
        responseDelegate.payload(SMALL_JSON, "utf-8");
    };
    public static final Processor SUCCESS_200_WITH_SMALL_JSON_BODY_AND_EXPLICIT_CONTENT_TYPE = (requestDelegate, responseDelegate) -> {
        responseDelegate.status(200);
        responseDelegate.contenType("application/json");
        responseDelegate.payload(SMALL_JSON, "utf-8");
    };

    public static final Processor SUCCESS_200_WITH_INPUTSTREAM_JSON_BODY = (requestDelegate, responseDelegate) -> {
        responseDelegate.status(200);
        responseDelegate.contenType("application/json");
        responseDelegate.payload(Thread.currentThread().getContextClassLoader().getResourceAsStream("payload.json"));
    };


    private final AtomicReference<Processor> processor = new AtomicReference<>(SUCCESS_200);

    @Rule
    public UndertowResource server = new UndertowResource(new CdmHttpUndertowHandler(
            new RequestLoggingProcessor(
                    (requestDelegate, responseDelegate) -> processor.get().process(requestDelegate,responseDelegate),
                    RequestLoggingStateProvider.ALWAYS,
                    new JsonFactory()
            )

    ));
    private final OkHttpClient client = new OkHttpClient();

    private ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        this.logAppender.start();
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(RequestLoggingProcessor.class);
        logger.addAppender(this.logAppender);
    }

    @Test
    public void givenSuccess__whenGet__thenQueryHeadersAndParametersAreLogged() throws Exception {
        this.processor.set(SUCCESS_200);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl() + "?q1=p1&q2=p2&q1=p3")
                .header("one-val", "value")
                .addHeader("many-vals", "v1")
                .addHeader("many-vals", "v2")
                .get().build()).execute();
        assertThat(response.code(), is(200));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");
        Map<String, Object> loggedResponse = (Map<String, Object>) loggedData.get("response");

        assertThat(loggedRequest.get("path"), is("/"));
        assertThat(((List)((Map)loggedRequest.get("headers")).get("one-val")).toArray(new String[0]), arrayContaining("value"));
        assertThat(((List)((Map)loggedRequest.get("headers")).get("many-vals")).toArray(new String[0]), arrayContaining("v1", "v2"));
        assertThat(((List)((Map)loggedRequest.get("query-parameters")).get("q1")).toArray(new String[0]), arrayContaining("p1", "p3"));
        assertThat(((List)((Map)loggedRequest.get("query-parameters")).get("q2")).toArray(new String[0]), arrayContaining("p2"));
        assertThat(loggedRequest.get("payload"), is(nullValue()));

        assertThat(loggedResponse.get("status"), is(200));
        assertThat(loggedResponse.get("payload"), is(nullValue()));
    }

    @Test
    public void givenSuccess__whenGet_andAuthorizationSet__thenAuthorizationNotLogged() throws Exception {
        this.processor.set(SUCCESS_200);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .header("authorization", "Bearer 12")
                .get().build()).execute();
        assertThat(response.code(), is(200));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");

        boolean found = false;
        for (String header : ((Map<String, Object>) loggedRequest.get("headers")).keySet()) {
            if(header.equalsIgnoreCase("authorization")) {
                found = true;
                assertThat(((List)((Map)loggedRequest.get("headers")).get(header)).toArray(new String[0]), arrayContaining("undisclosed"));
            }
        }
        assertThat("authorization header should be logged with undisclosed tag", found, is(true));
    }

    @Test
    public void givenSuccess__whenGet_andAuthorizationWithLeadingCapitalSet__thenAuthorizationNotLogged() throws Exception {
        this.processor.set(SUCCESS_200);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .header("Authorization", "Bearer 12")
                .get().build()).execute();
        assertThat(response.code(), is(200));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");

        boolean found = false;
        for (String header : ((Map<String, Object>) loggedRequest.get("headers")).keySet()) {
            if(header.equalsIgnoreCase("authorization")) {
                found = true;
                assertThat(((List)((Map)loggedRequest.get("headers")).get(header)).toArray(new String[0]), arrayContaining("undisclosed"));
            }
        }
        assertThat("authorization header should be logged with undisclosed tag", found, is(true));
    }

    @Test
    public void givenFailure__whenGet__theNoLog() throws Exception {
        this.processor.set(FAILURE);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl()).get().build()).execute();
        assertThat(response.code(), is(500));

        assertThat(this.logAppender.list, is(empty()));
    }

    @Test
    public void givenSuccess__whenGet__thenResponseHeadersAreLogged() throws Exception {
        this.processor.set(SUCCESS_200);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .get().build()).execute();
        assertThat(response.code(), is(200));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");
        Map<String, Object> loggedResponse = (Map<String, Object>) loggedData.get("response");

        assertThat(loggedRequest.get("path"), is("/"));
        assertThat(loggedRequest.get("payload"), is(nullValue()));

        assertThat(loggedResponse.get("status"), is(200));
        assertThat(((List)((Map)loggedResponse.get("headers")).get("one-val")).toArray(new String[0]), arrayContaining("value"));
        assertThat(((List)((Map)loggedResponse.get("headers")).get("many-vals")).toArray(new String[0]), arrayContaining("v1", "v2"));
        assertThat(loggedResponse.get("payload"), is(nullValue()));
    }

    @Test
    public void givenSuccess__whenPost_andJsonBody__thenRequestBodyIsLogged() throws Exception {
        this.processor.set(BODY_READ);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .post(RequestBody.create("{\"my\":\"object\"}", MediaType.get("application/json")))
                .build()).execute();

        Thread.sleep(1000);
        assertThat(response.code(), is(200));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        System.out.println(log.getFormattedMessage());
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");
        Map<String, Object> loggedResponse = (Map<String, Object>) loggedData.get("response");

        assertThat(loggedRequest.get("path"), is("/"));
        assertThat(loggedRequest.get("payload"), is("{\"my\":\"object\"}"));

        assertThat(loggedResponse.get("status"), is(200));
        assertThat(loggedResponse.get("payload"), is(nullValue()));
    }

    @Test
    public void givenSuccess__whenPost_andJsonBody_andBodyNotRead__thenRequestBodyIsLogged() throws Exception {
        this.processor.set(SUCCESS_200);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .post(RequestBody.create("{\"my\":\"object\"}", MediaType.get("application/json")))
                .build()).execute();

        Thread.sleep(1000);
        assertThat(response.code(), is(200));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        System.out.println(log.getFormattedMessage());
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");
        Map<String, Object> loggedResponse = (Map<String, Object>) loggedData.get("response");

        assertThat(loggedRequest.get("path"), is("/"));
        assertThat(loggedRequest.get("payload"), is("{\"my\":\"object\"}"));

        assertThat(loggedResponse.get("status"), is(200));
        assertThat(loggedResponse.get("payload"), is(nullValue()));
    }

    @Test
    public void givenSuccess_andResponseHasBody_andContentTypeExplicitelySet__whenGet__thenResponseBodyIsLogged() throws Exception {
        this.processor.set(SUCCESS_200_WITH_SMALL_JSON_BODY_AND_EXPLICIT_CONTENT_TYPE);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .get().build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is(SMALL_JSON));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        System.out.println(log.getFormattedMessage());
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");
        Map<String, Object> loggedResponse = (Map<String, Object>) loggedData.get("response");

        assertThat(loggedRequest.get("path"), is("/"));
        assertThat(loggedRequest.get("payload"), is(nullValue()));

        assertThat(loggedResponse.get("status"), is(200));
        assertThat(loggedResponse.get("payload"), is(SMALL_JSON));
    }

    @Test
    public void givenSuccess_andResponseHasBody_andContentTypeSetThroughHeader__whenGet() throws Exception {
        this.processor.set(SUCCESS_200_WITH_SMALL_JSON_BODY_AND_EXPLICIT_CONTENT_TYPE);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .get().build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is(SMALL_JSON));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        System.out.println(log.getFormattedMessage());
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");
        Map<String, Object> loggedResponse = (Map<String, Object>) loggedData.get("response");

        assertThat(loggedRequest.get("path"), is("/"));
        assertThat(loggedRequest.get("payload"), is(nullValue()));

        assertThat(loggedResponse.get("status"), is(200));
        assertThat(loggedResponse.get("payload"), is(SMALL_JSON));
    }

    @Test
    public void givenSuccess_andResponseHasBody_andPayloadSetAsInputStream__whenGet() throws Exception {
        this.processor.set(SUCCESS_200_WITH_INPUTSTREAM_JSON_BODY);
        Response response = this.client.newCall(new Request.Builder().url(this.server.baseUrl())
                .get().build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is(resourceContent("payload.json")));

        ILoggingEvent log = this.logAppender.list.get(0);
        assertThat(log.getMarker().getName(), is("REQUEST"));
        assertThat(log.getLevel().toString(), is("INFO"));
        System.out.println(log.getFormattedMessage());
        Map<String, Object> loggedData = this.parse(log.getFormattedMessage());
        Map<String, Object> loggedRequest = (Map<String, Object>) loggedData.get("request");
        Map<String, Object> loggedResponse = (Map<String, Object>) loggedData.get("response");

        assertThat(loggedRequest.get("path"), is("/"));
        assertThat(loggedRequest.get("payload"), is(nullValue()));

        assertThat(loggedResponse.get("status"), is(200));
        assertThat(loggedResponse.get("payload"), is(resourceContent("payload.json")));
    }

    private String resourceContent(String resource) throws IOException {
        try(
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                Reader reader = new InputStreamReader(in)) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }

    private Map<String, Object> parse(String json) throws JsonProcessingException {
        return this.mapper.readValue(json, Map.class);
    }
}
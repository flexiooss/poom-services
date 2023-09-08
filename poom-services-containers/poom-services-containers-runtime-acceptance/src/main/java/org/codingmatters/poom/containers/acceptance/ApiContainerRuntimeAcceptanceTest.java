package org.codingmatters.poom.containers.acceptance;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.ApiContainerRuntimeBuilder;
import org.codingmatters.poom.containers.RuntimeTestHandle;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.processors.ProcessorChain;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class ApiContainerRuntimeAcceptanceTest {
    abstract protected ApiContainerRuntime createContainer(String host, int port, CategorizedLogger logger);

    private int freePort;
    private RuntimeTestHandle runtime;

    private OkHttpClient client = new OkHttpClient();

    @Before
    public void setUp() throws Exception {
        try {
            ServerSocket freePortSocket = new ServerSocket(0);
            this.freePort = freePortSocket.getLocalPort();
            freePortSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.runtime = new RuntimeTestHandle(this.createContainer("localhost", this.freePort, CategorizedLogger.getLogger("test-runtime")));
    }

    @After
    public void tearDown() throws Exception {
        this.runtime.doStop();
    }

    @Test
    public void whenNoApiRegistered__thenStartsOk() throws Exception {
        new ApiContainerRuntimeBuilder().build(this.runtime.runtime());
        this.runtime.doStart();
    }

    @Test
    public void givenOneApiRegistered__whenCallingApiPath__thenExpectedResponse() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm up", "utf-8")
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to").build()).execute();

        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm up"));
    }

    @Test
    public void givenOneApiRegistered__whenWrapper__thenExpectedResponse() throws Exception {
        new ApiContainerRuntimeBuilder()
                .apiProcessorWrapper(processor -> ProcessorChain.chain(
                        (requestDelegate, responseDelegate) -> responseDelegate.addHeader("wrapped", "true")
                        ).then(processor)
                )
                .withApi(new TestApi("/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm up", "utf-8")
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to").build()).execute();

        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm up"));
        assertThat(response.header("wrapped"), is("true"));
    }

    @Test
    public void givenOneApiRegistered__whenHasDoc__thenDocReturned() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm up", "utf-8"),
                        true
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/doc").build()).execute();

        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is(this.content("doc-index.html")));
    }

    @Test
    public void givenOneApiRegistered__whenNotHasDoc__thenDocPassedToProcessor() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm up", "utf-8"),
                        false
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/doc").build()).execute();

        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm up"));
    }

    private String content(String resource) throws IOException {
        try(Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer); read != -1 ; read = reader.read(buffer)) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }

    @Test
    public void givenOneApiRegistered__whenCallingNonApiPath__thenEmpty404() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm up", "utf-8")
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/another/path").build()).execute();

        assertThat(response.code(), is(404));
        assertThat(response.body().string(), is(""));
    }

    @Test
    public void givenTwoApisRegisterd__whenDistinctPath__thenDispachedOk() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/first/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm first and I'm up", "utf-8")
                ))
                .withApi(new TestApi("/second/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm second and I'm up", "utf-8")
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/first/path/to").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm first and I'm up"));

        response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/second/path/to").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm second and I'm up"));
    }

    @Test
    public void givenTwoApisRegisterd__whenSamePathStart__thenDispachedOk() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/path/to/first", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm first and I'm up", "utf-8")
                ))
                .withApi(new TestApi("/path/to/second", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm second and I'm up", "utf-8")
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/first").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm first and I'm up"));

        response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/second").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm second and I'm up"));
    }

    @Test
    public void givenTwoApisRegisterd__whenSamePath__thenDispachedToLast() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm first and I'm up", "utf-8")
                ))
                .withApi(new TestApi("/path/to", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm second and I'm up", "utf-8")
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/second").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm second and I'm up"));
    }

    @Test
    public void givenTwoApisRegisterd__whenOverlappingPath__thenDispachedOk() throws Exception {
        new ApiContainerRuntimeBuilder()
                .withApi(new TestApi("/path/to/first", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm first and I'm up", "utf-8")
                ))
                .withApi(new TestApi("/path/to/first/second", (requestDelegate, responseDelegate) ->
                        responseDelegate.contenType("text/plain").status(200).payload("I'm second and I'm up", "utf-8")
                ))
                .build(this.runtime.runtime());
        this.runtime.doStart();

        Response response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/first").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm first and I'm up"));

        response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/first/third").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm first and I'm up"));

        response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/first/second").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm second and I'm up"));

        response = this.client.newCall(new Request.Builder().url("http://localhost:" + this.freePort + "/path/to/first/second/third").build()).execute();
        assertThat(response.code(), is(200));
        assertThat(response.body().string(), is("I'm second and I'm up"));
    }
}

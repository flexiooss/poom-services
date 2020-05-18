package org.codingmatters.poom.generic.resource.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.generic.resource.processor.tests.TestAdapter;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class GenericResourceProcessorBuilderTest {

    static private final JsonFactory JSON_FACTORY = new JsonFactory();
    static public final String BASE_URL = "https://some.where";

    @Test
    public void givenAdapterSettedOnBaseUrl__whenGettingBaseUrl__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL, processor, adapter);
    }

    @Test
    public void givenAdapterSettedOnEmptyPath__whenGettingBaseUrl__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("", new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL, processor, adapter);
    }

    @Test
    public void givenAdapterSettedOnNullPath__whenGettingBaseUrl__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder(null, new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL, processor, adapter);
    }

    @Test
    public void givenAdapterSettedOnSubpath__whenGettingBaseUrl__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/down/the/path", new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/down/the/path", processor, adapter);
    }

    @Test
    public void givenAdapterSettedOnSubpathWithTrailingSlash__whenGettingBaseUrl__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/down/the/path", new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/down/the/path/", processor, adapter);
    }

    @Test
    public void givenAdapterSettedOnBaseUrl__whenGettingWithTrailingSlash__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/", processor, adapter);
    }

    @Test
    public void givenAdapterSettedOnSlash__whenGettingWithTrailingSlash__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/", processor, adapter);
    }

    @Test
    public void givenAdapterSettedOnSlash__whenGettingBaseUrl__thenGenericCollectionAtBaseURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/", processor, adapter);
    }

    @Test
    public void givenOneAdapterSetted__whenAdapterSettedOnChild__thenGenericCollectionAtChildURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/else/where", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/else/where", processor, adapter);
    }

    @Test
    public void givenOneAdapterSetted__whenAdapterSettedOnPathWithUriParam__thenGenericCollectionAtChildURL() throws Exception {
        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/some/{param}/where", () -> adapter)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/some/value/where", processor, adapter);
    }

    @Test
    public void givenTwoAdaptersSetted__whenAdapterSettedOnSiblings__thenGenericCollectionAtBothURL() throws Exception {
        TestAdapter adapter1 = new TestAdapter();
        TestAdapter adapter2 = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/here", () -> adapter1)
                .resourceAt("/there", () -> adapter2)
                .build();

        this.asertGenericCollectionAt(BASE_URL + "/here", processor, adapter1);
        this.asertGenericCollectionAt(BASE_URL + "/there", processor, adapter2);
    }

    @Test
    public void givenFallbackProcessorDefined__whenNoResourceDefined__thenFallbackHit() throws Exception {
        AtomicInteger fallbackHit = new AtomicInteger(0);

        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .build((requestDelegate, responseDelegate) -> fallbackHit.incrementAndGet());

        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, BASE_URL + "/").build(), response);
            assertThat(fallbackHit.get(), is(1));
        }
    }

    @Test
    public void givenFallbackProcessorDefined__whenResourceDefinedAtRoot_andCallingRoot__thenGenericCollectionHit() throws Exception {
        AtomicInteger fallbackHit = new AtomicInteger(0);

        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build((requestDelegate, responseDelegate) -> fallbackHit.incrementAndGet());

        this.asertGenericCollectionAt(BASE_URL + "/", processor, adapter);
        assertThat(fallbackHit.get(), is(0));
    }

    @Test
    public void givenFallbackProcessorDefined__whenResourceDefinedAtRoot_andCallingBelowResource__thenFallbackHit() throws Exception {
        AtomicInteger fallbackHit = new AtomicInteger(0);

        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("", () -> adapter)
                .build((requestDelegate, responseDelegate) -> fallbackHit.incrementAndGet());

        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, BASE_URL + "/some/where").build(), response);
            assertThat(fallbackHit.get(), is(1));
        }
    }

    @Test
    public void givenFallbackProcessorDefined__whenResourceDefinedAtChild_andCallingRoot__thenFallbackHit() throws Exception {
        AtomicInteger fallbackHit = new AtomicInteger(0);

        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/here", () -> adapter)
                .build((requestDelegate, responseDelegate) -> fallbackHit.incrementAndGet());

        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, BASE_URL + "/").build(), response);
            assertThat(fallbackHit.get(), is(1));
        }
    }

    @Test
    public void givenFallbackProcessorDefined__whenResourceDefinedAtChild_andCallingSibling__thenFallbackHit() throws Exception {
        AtomicInteger fallbackHit = new AtomicInteger(0);

        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/here", () -> adapter)
                .build((requestDelegate, responseDelegate) -> fallbackHit.incrementAndGet());

        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, BASE_URL + "/else").build(), response);
            assertThat(fallbackHit.get(), is(1));
        }
    }

    @Test
    public void givenFallbackProcessorDefined__whenResourceDefinedAtChild_andCallingChild__thenGenericCollectionHit() throws Exception {
        AtomicInteger fallbackHit = new AtomicInteger(0);

        TestAdapter adapter = new TestAdapter();
        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("/here", () -> adapter)
                .build((requestDelegate, responseDelegate) -> fallbackHit.incrementAndGet());

        this.asertGenericCollectionAt(BASE_URL + "/here", processor, adapter);
        assertThat(fallbackHit.get(), is(0));
    }

    @Test
    public void givenAdapterSetted__whenPreProcessorDefined__thenPreprocessorIsCalled() throws Exception {
        AtomicInteger preprocessed = new AtomicInteger();

        GenericResourceProcessorBuilder builder = new GenericResourceProcessorBuilder("/", new JsonFactory());
        Processor processor = builder
                .resourceAt("", (req, res) -> preprocessed.incrementAndGet(), () -> new TestAdapter())
                .build();

        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, BASE_URL + "/").build(), response);
            assertThat(preprocessed.get(), is(1));
        }
    }

    private void asertGenericCollectionAt(String url, Processor processor, TestAdapter adapter) throws Exception {
        while(url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, url + "/").build(), response);
            assertThat("list", adapter.listCounter.get(), is(1));
        }
        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, url + "/").addQueryParam("filter", "a == 12").build(), response);
            assertThat("search", adapter.searchCounter.get(), is(1));
        }
        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.POST, url + "/").payload(streamFrom("{\"a\":12}")).build(), response);
            assertThat("create", adapter.createCounter.get(), is(1));
        }
        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, url + "/42").build(), response);
            assertThat("retrieve", adapter.retrieveCounter.get(), is(1));
        }
        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.PUT, url + "/42").payload(streamFrom("{\"a\":12}")).build(), response);
            assertThat("replace", adapter.replaceCounter.get(), is(1));
        }
        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.PATCH, url + "/42").payload(streamFrom("{\"a\":12}")).build(), response);
            assertThat("update", adapter.updateCounter.get(), is(1));
        }
        try(TestResponseDeleguate response = new TestResponseDeleguate();) {
            processor.process(TestRequestDeleguate.request(RequestDelegate.Method.DELETE, url + "/42").build(), response);
            assertThat("delete", adapter.deleteCounter.get(), is(1));
        }
    }

    static private ByteArrayInputStream streamFrom(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}
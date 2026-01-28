package org.codingmatters.poom.json.rpc.processor;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.server.handlers.PathHandler;
import org.codingmatters.poom.json.rpc.RpcMethodHandler;
import org.codingmatters.poom.json.rpc.descriptors.RpcEntryPointDescriptor;
import org.codingmatters.poom.json.rpc.descriptors.RpcMethodDescriptor;
import org.codingmatters.poom.json.rpc.processor.test.binding.MethodParams;
import org.codingmatters.poom.json.rpc.processor.test.binding.MethodResult;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JsonRpcProcessorTest {

    private static final String URL = "http://plip/plpop";
    private JsonFactory jsonFactory = new JsonFactory();

    @ParameterizedTest
    @EnumSource(value = RequestDelegate.Method.class, names = {"GET", "PUT", "PATCH", "DELETE", "HEAD", "UNIMPLEMENTED"})
    void whileProcess__whenNotPost__then405(RequestDelegate.Method method) throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        new JsonRpcProcessor(RpcEntryPointDescriptor.builder().build(), this.jsonFactory).process(
                TestRequestDeleguate.request(method, URL).build(),
                response
        );

        assertThat(response.status(), Matchers.is(405));
        assertThat(response.contentType(), Matchers.is("text/plain"));
        assertThat(new String(response.payload()), Matchers.is("Method Not Allowed"));
    }

    @Test
    void whileProcess__givenPost__whenContentTypeNotJson__then415() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder();

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/not-json")
                        .payload(this.asPayload("[{}]"))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(415));
        assertThat(response.contentType(), Matchers.is("text/plain"));
        assertThat(new String(response.payload()), Matchers.is("Unsupported Media Type"));
    }

    @Test
    void whileProcess__givenPost_andContentTypeIsJson__whenNoJsonrpc__thenInvalidRequest() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder();

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(this.asPayload("""
                                [{}]
                                """))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(200));
        assertThat(response.contentType(), Matchers.is("application/json"));
        assertThat(new String(response.payload()), Matchers.is(
                                """
                                {"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"}}"""
        ));
    }

    @Test
    void whileProcess__givenPost_andContentTypeIsJson__whenMalformedJson__thenParseError() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder();

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(this.asPayload("""
                                plif plaf plouf
                                """))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(200));
        assertThat(response.contentType(), Matchers.is("application/json"));
        assertThat(new String(response.payload()), Matchers.is(
                                """
                                {"jsonrpc":"2.0","error":{"code": -32700,"message": "Parse error"},"id": null}"""
        ));
    }

    @Test
    void whileProcess__givenPost_andContentTypeIsJson__whenNoMethod__thenInvalidRequest() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder();

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(this.asPayload("""
                                [{"jsonrpc":"2.0"}]
                                """))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(200));
        assertThat(response.contentType(), Matchers.is("application/json"));
        assertThat(new String(response.payload()), Matchers.is(
                """
                {"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"}}"""
        ));
    }

    @Test
    void whileProcess__givenPost_andContentTypeIsJson__whenJsonRpcDoesntMatch__thenInvalidRequest() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder();

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(this.asPayload("""
                                [{"jsonrpc":"4.0"}]
                                """))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(200));
        assertThat(response.contentType(), Matchers.is("application/json"));
        assertThat(new String(response.payload()), Matchers.is(
                """
                {"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"}}"""
        ));
    }



    @Test
    void whileProcess__givenPost_andContentTypeIsJson__whenMethodHasNoDescriptor__thenMethodNotFound() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder();

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(this.asPayload("""
                                [{"jsonrpc":"2.0","method":"noDescriptor"}]
                                """))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(200));
        assertThat(response.contentType(), Matchers.is("application/json"));
        assertThat(new String(response.payload()), Matchers.is(
                """
                {"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"}}"""
        ));
    }

    @Test
    void whileProcess__givenPost__whenMethodDescriptor__thenHandlerApplied() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder()
                .methodsAdd(RpcMethodDescriptor.builder()
                        .method("calledMethod")
                        .paramsValue(MethodParams.class)
                        .resultValue(MethodResult.class)
                        .handler((RpcMethodHandler<MethodParams, MethodResult>) methodParams -> MethodResult.builder().params(methodParams).build())
                        .build());

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(this.asPayload("""
                                [{"jsonrpc":"2.0","method":"calledMethod","params":{"prop":"test value 1"}},{"jsonrpc":"2.0","method":"calledMethod","params":{"prop":"test value 2"}}]
                                """))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(200));
        assertThat(response.contentType(), Matchers.is("application/json"));

        assertThat(new String(response.payload()), Matchers.is(
                """
                [{"jsonrpc":"2.0","result":{"params":{"prop":"test value 1"}}},{"jsonrpc":"2.0","result":{"params":{"prop":"test value 2"}}}]"""
        ));
    }

    @Test
    void whileProcess__whenSingleRequest__thenSingleResponse() throws Exception {
        TestResponseDeleguate response =  new TestResponseDeleguate();

        RpcEntryPointDescriptor.Builder descriptor = RpcEntryPointDescriptor.builder()
                .methodsAdd(RpcMethodDescriptor.builder()
                        .method("calledMethod")
                        .paramsValue(MethodParams.class)
                        .resultValue(MethodResult.class)
                        .handler((RpcMethodHandler<MethodParams, MethodResult>) methodParams -> MethodResult.builder().params(methodParams).build())
                        .build());

        new JsonRpcProcessor(descriptor.build(), this.jsonFactory).process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(this.asPayload("""
                                {"jsonrpc":"2.0","method":"calledMethod","params":{"prop":"test value 1"}}
                                """))
                        .build(),
                response
        );

        assertThat(response.status(), Matchers.is(200));
        assertThat(response.contentType(), Matchers.is("application/json"));

        assertThat(new String(response.payload()), Matchers.is(
                """
                {"jsonrpc":"2.0","result":{"params":{"prop":"test value 1"}}}"""
        ));
    }

    private ByteArrayInputStream asPayload(String str) {
        return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
    }
}
package org.codingmatters.poom.api.test.generation;

import org.codingmatters.poom.handler.CumulatingHandlerResource;
import org.codingmatters.poom.handler.CumulatingTestHandler;
import org.codingmatters.rest.api.generator.*;
import org.codingmatters.rest.api.generator.handlers.HandlersHelper;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.helpers.ObjectHelper;
import org.codingmatters.tests.reflect.ReflectMatchers;
import org.codingmatters.tests.reflect.matchers.ConstructorMatcher;
import org.codingmatters.value.objects.generation.Naming;
import org.codingmatters.value.objects.generation.SpecCodeGenerator;
import org.codingmatters.value.objects.spec.Spec;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.rules.TemporaryFolder;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.internal.impl.commons.model.Api;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class TestApiGeneratorTest {

    public static final String ROOT_PACK = "org.generated";
    public static final String TYPES_PACK = ROOT_PACK + ".types";
    public static final String API_PACK = ROOT_PACK + ".api";
    public static final String CLIENT_PACK = ROOT_PACK + ".client";

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Rule
    public FileHelper fileHelper = new FileHelper();

    private CompiledCode compiled;

    private RamlModelResult raml;

    @Before
    public void setUp() throws Exception {
        this.raml = new RamlModelBuilder().buildApi(this.fileHelper.fileResource("handlers/handlers.raml"));
        Spec typesSpec = new ApiTypesGenerator().generate(raml);
        new SpecCodeGenerator(typesSpec, TYPES_PACK, this.dir.getRoot()).generate();

        Spec apiSpec = new ApiGenerator(TYPES_PACK).generate(raml);
        new SpecCodeGenerator(apiSpec, API_PACK, this.dir.getRoot()).generate();

        new HandlersGenerator(API_PACK, TYPES_PACK, API_PACK, this.dir.getRoot()).generate(raml);

        new ClientInterfaceGenerator(CLIENT_PACK, API_PACK, this.dir.getRoot()).generate(raml);
        new ClientHandlerImplementation(CLIENT_PACK, API_PACK, TYPES_PACK, this.dir.getRoot()).generate(raml);

        new TestApiGenerator(API_PACK, CLIENT_PACK, new Naming(), this.dir.getRoot())
                .generate(this.raml);

//        this.fileHelper.printJavaContent("", this.dir.getRoot());
        this.fileHelper.printFile(this.dir.getRoot(), "TestAPITestApi.java");

        this.compiled = CompiledCode.builder().source(this.dir.getRoot()).compile();
    }

    @Test
    public void thenTestApiClassGenerated() throws Exception {
        assertThat(
                this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").get(),
                is(aClass().implementing(BeforeEachCallback.class).implementing(AfterEachCallback.class))
        );
    }

    @Test
    public void thenTestApiClassHasPublicNoParametersConstructor() throws Exception {
        this.fileHelper.printJavaContent("", this.dir.getRoot());
        this.fileHelper.printFile(this.dir.getRoot(), "TestAPITestApi.java");

        assertThat(
                this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").get(),
                is(aClass().with(aConstructor().withParameters(new Class[0])))
        );
    }

    @Test
    public void thenTestApiClassHasClientMethod() throws Exception {
        assertThat(
                this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").get(),
                is(aClass()
                        .with(aPublic().method().named("client").withoutParameters().returning(this.compiled.classLoader().get(CLIENT_PACK + ".TestAPIClient").get())))
        );
    }

    @Test
    public void thenTestApiClassHasClientField() throws Exception {
        assertThat(
                this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").get(),
                is(aClass()
                        .with(aPrivate().field().final_().named("client").withType(this.compiled.classLoader().get(CLIENT_PACK + ".TestAPIClient").get())))
        );
    }

    @Test
    public void whenClientCalled__thenNotNull() throws Exception {
        ObjectHelper testApi = this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").newInstance();
        ObjectHelper actualClient = testApi.call("client");

        assertThat(actualClient.get(), is(notNullValue()));
    }

    @Test
    public void thenTestApiClassHasACumulatingHandlerResourceMethodForEachResourceMethod() throws Exception {
        assertThat(
                this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").get(),
                is(aClass()
                        .with(aPublic().method().named("rootGet").returning(CumulatingTestHandler.class))
                        .with(aPublic().method().named("subGet").returning(CumulatingTestHandler.class))
                )
        );
    }

    @Test
    public void thenTestApiClassHasACumulatingTestHandlerFieldForEachResourceMethod() throws Exception {
        assertThat(
                this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").get(),
                is(aClass()
                        .with(aPrivate().field().final_().named("rootGet").withType(CumulatingTestHandler.class))
                        .with(aPrivate().field().final_().named("rootPost").withType(CumulatingTestHandler.class))
                        .with(aPrivate().field().final_().named("subGet").withType(CumulatingTestHandler.class))
                )
        );
    }

    @Test
    public void whenHandlerResourceGetterInvoked__thenNotNull() throws Exception {
        ObjectHelper testApi = this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").newInstance();

        assertThat(testApi.call("rootGet").get(), notNullValue());
        assertThat(testApi.call("rootPost").get(), notNullValue());
        assertThat(testApi.call("subGet").get(), notNullValue());
    }

    @Test
    public void givenCallingClientMethod__whenNextResponseNotSet__thenNullPointerException() throws Exception {
        ObjectHelper testApi = this.compiled.classLoader().get(API_PACK + ".test.TestAPITestApi").newInstance();
        ObjectHelper client = testApi.call("client");

        ObjectHelper actual = client.call("root")
                .as(CLIENT_PACK + ".TestAPIClient$Root")
                .call("get", this.compiled.classLoader().get(API_PACK + ".RootGetRequest").get())
                .with(this.compiled.classLoader().get(API_PACK + ".RootGetRequest").call("builder").call("build").get());

        assertThat(actual.get(), is(nullValue()));

//        AssertionError error = assertThrows(AssertionError.class, () -> client.call("root")
//                .as(CLIENT_PACK + ".TestAPIClient$Root")
//                .call("get", this.compiled.classLoader().get(API_PACK + ".RootGetRequest").get())
//                .with(this.compiled.classLoader().get(API_PACK + ".RootGetRequest").call("builder").call("build").get())
//        );
//        assertThat(
//                error.getCause().getCause(),
//                isA(NullPointerException.class)
//        );
//        assertThat(
//                error.getCause().getCause().getMessage(),
//                is("Cannot invoke \"java.util.function.Function.apply(Object)\" because the return value of \"org.generated.api.TestAPIHandlers.rootGetHandler()\" is null")
//
//        );
    }
}
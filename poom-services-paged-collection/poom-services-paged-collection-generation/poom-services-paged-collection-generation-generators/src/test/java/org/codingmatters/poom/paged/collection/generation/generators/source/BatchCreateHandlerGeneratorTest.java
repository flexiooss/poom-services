package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.api.paged.collection.api.types.batchcreateresponse.Errors;
import org.codingmatters.poom.generic.resource.domain.BatchEntityCreator;
import org.codingmatters.poom.generic.resource.domain.CheckedEntityActionProvider;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestCRUD;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestData;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.codingmatters.value.objects.values.ObjectValue;
import org.generated.api.NoParamsBatchPostRequest;
import org.generated.api.NoParamsBatchPostResponse;
import org.generated.api.types.Error;
import org.generated.api.types.*;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BatchCreateHandlerGeneratorTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Rule
    public FileHelper fileHelper = new FileHelper();

    private ClassLoaderHelper classes;

    @Before
    public void setUp() throws Exception {
        GenerationUtils.writeJavaFile(
                this.dir.getRoot(),
                "org.generated.handlers",
                new BatchCreateHandlerGenerator(TestData.FULL_COLLECTION).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }


    private Function<NoParamsBatchPostRequest, NoParamsBatchPostResponse> handler(PagedCollectionAdapter.FromRequestProvider<NoParamsBatchPostRequest, org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoParamsBatchPostRequest, NoParamsBatchPostResponse>) classes.get("org.generated.handlers.NoParamsBatchCreate")
                .newInstance(CheckedEntityActionProvider.class)
                .with(
                        (CheckedEntityActionProvider<NoParamsBatchPostRequest, BatchEntityCreator<Create>>) request -> provider.adapter(request).crud()
                )
                .get();
    }


    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoParamsBatchCreate.java");

        assertThat(
                classes.get("org.generated.handlers.NoParamsBatchCreate").get(),
                Matchers.is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(CheckedEntityActionProvider.class)
                                        .withParameters(
                                                classTypeParameter(NoParamsBatchPostRequest.class),
                                                typeParameter().aType(genericType()
                                                        .baseClass(BatchEntityCreator.class)
                                                        .withParameters(
                                                                classTypeParameter(Create.class)
                                                        )
                                                )
                                        )
                                )
                        )
                        .with(aPublic().method().named("apply")
                                .withParameters(NoParamsBatchPostRequest.class)
                                .returning(NoParamsBatchPostResponse.class)
                        )
                )
        );
    }


    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsBatchPostResponse response = this.handler((request) -> {
            throw new Exception("");
        }).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }


    @Test
    public void givenAdapterGetted__whenBatchCreateResponseIsNull__then500() throws Exception {
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return null;
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenNoValuePosted__thenEmptyArrayIsPassedToAdapter() throws Exception {
        AtomicReference<Create[]> requestedPayload = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                requestedPayload.set(values);
                return org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse.builder().build();
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        assertThat(requestedPayload.get(), is(emptyArray()));
    }

    @Test
    public void givenAdapterOK__whenOneValuePosted__thenPostedOneValueArrayIsPassedToAdapter() throws Exception {
        Create aValue = Create.builder().p("v").build();
        AtomicReference<Create[]> requestedPayload = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                requestedPayload.set(values);
                return org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse.builder().build();
            }
        })).apply(NoParamsBatchPostRequest.builder().payload(aValue).build());

        assertThat(requestedPayload.get(), is(arrayContaining(aValue)));
    }

    @Test
    public void givenAdapterOK__whenOnlySuccess__then201() throws Exception {
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse.builder()
                        .success("42")
                        .build();
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status201().orElseThrow(() -> new AssertionError("expected 201, got " + response));
        assertThat(response.status201().xEntityType(), is("TestType"));
        assertThat(response.status201().payload(), is(BatchCreateResponse.builder()
                .success("42")
                .build()));
    }

    @Test
    public void givenAdapterOK__whenOnlyErrors__then207() throws Exception {
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse.builder()
                        .errorsAdd(Errors.builder().error(org.codingmatters.poom.api.paged.collection.api.types.Error.builder().description("boum badaboum").build()).entity(ObjectValue.builder().property("p", v -> v.stringValue("v")).build()).build())
                        .build();
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status207().orElseThrow(() -> new AssertionError("expected 207, got " + response));
        assertThat(response.status207().xEntityType(), is("TestType"));
        assertThat(response.status207().payload(), is(BatchCreateResponse.builder()
                .errorsAdd(org.generated.api.types.batchcreateresponse.Errors.builder().error(Error.builder().description("boum badaboum").build()).entity(ObjectValue.builder().property("p", v -> v.stringValue("v")).build()).build())
                .build()));
    }

    @Test
    public void givenAdapterOK__whenMixedResults__then207() throws Exception {
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse.builder()
                        .successAdd("42")
                        .errorsAdd(Errors.builder().error(org.codingmatters.poom.api.paged.collection.api.types.Error.builder().description("boum badaboum").build()).entity(ObjectValue.builder().property("p", v -> v.stringValue("v")).build()).build())
                        .build();
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status207().orElseThrow(() -> new AssertionError("expected 207, got " + response));
        assertThat(response.status207().xEntityType(), is("TestType"));
        assertThat(response.status207().payload(), is(BatchCreateResponse.builder()
                .successAdd("42")
                .errorsAdd(org.generated.api.types.batchcreateresponse.Errors.builder().error(Error.builder().description("boum badaboum").build()).entity(ObjectValue.builder().property("p", v -> v.stringValue("v")).build()).build())
                .build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                throw new BadRequestException(error, msg);
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                throw new ForbiddenException(error, msg);
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                throw new NotFoundException(error, msg);
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                throw new UnauthorizedException(error, msg);
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsBatchPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse createEntitiesFrom(Create... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                throw new UnexpectedException(error, msg);
            }
        })).apply(NoParamsBatchPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(Error.fromMap(error.toMap()).build()));
    }
}
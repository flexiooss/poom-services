package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.generic.resource.domain.CheckedEntityActionProvider;
import org.codingmatters.poom.generic.resource.domain.EntityUpdater;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestCRUD;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestData;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.generated.api.NoParamsElementPatchRequest;
import org.generated.api.NoParamsElementPatchResponse;
import org.generated.api.types.Create;
import org.generated.api.types.Error;
import org.generated.api.types.Replace;
import org.generated.api.types.Update;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UpdateHandlerGeneratorTest {

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
                new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION, ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoParamsElementPatchRequest, NoParamsElementPatchResponse> handler(PagedCollectionAdapter.FromRequestProvider<NoParamsElementPatchRequest, org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoParamsElementPatchRequest, NoParamsElementPatchResponse>) classes.get("org.generated.handlers.NoParamsUpdate")
                .newInstance(CheckedEntityActionProvider.class)
                .with(
                        (CheckedEntityActionProvider<NoParamsElementPatchRequest, EntityUpdater<org.generated.api.types.Entity, Update>>) request -> provider.adapter(request).crud()
                )
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoParamsUpdate.java");

        assertThat(
                classes.get("org.generated.handlers.NoParamsUpdate").get(),
                is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(CheckedEntityActionProvider.class)
                                        .withParameters(
                                                classTypeParameter(NoParamsElementPatchRequest.class),
                                                typeParameter().aType(genericType()
                                                        .baseClass(EntityUpdater.class)
                                                        .withParameters(
                                                                classTypeParameter(org.generated.api.types.Entity.class),
                                                                classTypeParameter(Update.class)
                                                        )
                                                )
                                        )
                                )
                        )
                        .with(aPublic().method().named("apply")
                                .withParameters(NoParamsElementPatchRequest.class)
                                .returning(NoParamsElementPatchResponse.class)
                        )
                )
        );
    }


    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementPatchResponse response = this.handler((request) -> {
            throw new Exception("");
        }).apply(NoParamsElementPatchRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterGetted_andEntityIdProvided__whenReplacedEntityIsNull__then500() throws Exception {
        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter( new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return null;
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenNoEntityId__then400() throws Exception {
        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter(new TestCRUD()))
                .apply(NoParamsElementPatchRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        Error error = response.status400().payload();
        assertThat(error.code(), is(Error.Code.BAD_REQUEST));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenEntityIdProvided__thenEntityIdIsPassedToAdapter() throws Exception {
        AtomicReference<String> requestedId = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedId.set(id);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().build());
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        assertThat(requestedId.get(), is("12"));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenNoValuePosted__thenEmptyObjectIsPassedToAdapter() throws Exception {
        AtomicReference<Update> requestedPayload = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().build());
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        assertThat(requestedPayload.get(), is(Update.builder().build()));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValuePosted__thenPostedValueIsPassedToAdapter() throws Exception {
        Update aValue = Update.builder().p("v").build();
        AtomicReference<Update> requestedPayload = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().build());
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").payload(aValue).build());

        assertThat(requestedPayload.get(), is(aValue));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValueReplaced__then201_andXEntityIdSetted_andLocationSetted_andValueReturned() throws Exception {
        org.generated.api.types.Entity aValue = org.generated.api.types.Entity.builder().p("v").build();

        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.TWO, aValue);
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().xEntityId(), is("12"));
        assertThat(response.status200().xEntityType(), is("TestType"));
        assertThat(response.status200().location(), is(new TestCRUD().entityRepositoryUrl() + "/12"));
        assertThat(response.status200().payload(), is(aValue));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPatchResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> updateEntityWith(String id, Update value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPatchRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }
}

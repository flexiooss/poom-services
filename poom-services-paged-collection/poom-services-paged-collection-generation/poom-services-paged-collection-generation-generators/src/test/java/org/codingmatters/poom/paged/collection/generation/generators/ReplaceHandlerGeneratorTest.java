package org.codingmatters.poom.paged.collection.generation.generators;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestCRUD;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestData;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.generated.api.NoParamsElementPutRequest;
import org.generated.api.NoParamsElementPutResponse;
import org.generated.api.types.Create;
import org.generated.api.types.Error;
import org.generated.api.types.Replace;
import org.generated.api.types.Update;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ReplaceHandlerGeneratorTest {

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
                new ReplaceHandlerGenerator(TestData.FULL_COLLECTION).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoParamsElementPutRequest, NoParamsElementPutResponse> handler(PagedCollectionAdapter.Provider<org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoParamsElementPutRequest, NoParamsElementPutResponse>) classes.get("org.generated.handlers.NoParamsReplace")
                .newInstance(PagedCollectionAdapter.Provider.class)
                .with(provider)
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoParamsReplace.java");

        assertThat(
                classes.get("org.generated.handlers.NoParamsReplace").get(),
                is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(PagedCollectionAdapter.Provider.class)
                                        .withParameters(
                                                classTypeParameter(org.generated.api.types.Entity.class),
                                                classTypeParameter(Create.class),
                                                classTypeParameter(Replace.class),
                                                classTypeParameter(Update.class)
                                        )
                                )
                                .withParameters(PagedCollectionAdapter.Provider.class)
                        )
                        .with(aPublic().method().named("apply")
                                .withParameters(NoParamsElementPutRequest.class)
                                .returning(NoParamsElementPutResponse.class)
                        )
                )
        );
    }


    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementPutResponse response = this.handler(() -> {
            throw new Exception("");
        }).apply(NoParamsElementPutRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenCRUDIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter()).apply(NoParamsElementPutRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }


    @Test
    public void givenAdapterGetted_andEntityIdProvided__whenReplacedEntityIsNull__then500() throws Exception {
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return null;
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenREPLACEActionNotSupportedValueCreated__then405() throws Exception {
        org.generated.api.types.Entity aValue = org.generated.api.types.Entity.builder().p("v").build();

        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Set<Action> supportedActions() {
                return Action.actions(Action.CREATE, Action.UPDATE);
            }

            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.ONE, aValue);
            }
        })).apply(NoParamsElementPutRequest.builder().build());

        response.opt().status405().orElseThrow(() -> new AssertionError("expected 405, got " + response));

        Error error = response.status405().payload();
        assertThat(error.code(), is(Error.Code.ENTITY_REPLACEMENT_NOT_ALLOWED));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenNoEntityId__then400() throws Exception {
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD()))
                .apply(NoParamsElementPutRequest.builder().build());

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

        this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedId.set(id);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().build());
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        assertThat(requestedId.get(), is("12"));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenNoValuePosted__thenEmptyObjectIsPassedToAdapter() throws Exception {
        AtomicReference<Replace> requestedPayload = new AtomicReference<>();

        this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().build());
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        assertThat(requestedPayload.get(), is(Replace.builder().build()));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValuePosted__thenPostedValueIsPassedToAdapter() throws Exception {
        Replace aValue = Replace.builder().p("v").build();
        AtomicReference<Replace> requestedPayload = new AtomicReference<>();

        this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().build());
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").payload(aValue).build());

        assertThat(requestedPayload.get(), is(aValue));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValueReplaced__then201_andXEntityIdSetted_andLocationSetted_andValueReturned() throws Exception {
        org.generated.api.types.Entity aValue = org.generated.api.types.Entity.builder().p("v").build();

        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.TWO, aValue);
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().xEntityId(), is("12"));
        assertThat(response.status200().location(), is(new TestCRUD().entityRepositoryUrl() + "/12"));
        assertThat(response.status200().payload(), is(aValue));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementPutResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementPutRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }

}
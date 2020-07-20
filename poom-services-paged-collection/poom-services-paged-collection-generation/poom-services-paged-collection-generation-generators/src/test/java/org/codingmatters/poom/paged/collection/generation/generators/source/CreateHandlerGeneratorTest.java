package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.generic.resource.domain.CheckedEntityActionProvider;
import org.codingmatters.poom.generic.resource.domain.EntityCreator;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestCRUD;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestData;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.generated.api.NoParamsPostRequest;
import org.generated.api.NoParamsPostResponse;
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

public class CreateHandlerGeneratorTest {

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
                new CreateHandlerGenerator(TestData.FULL_COLLECTION).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoParamsPostRequest, NoParamsPostResponse> handler(PagedCollectionAdapter.FromRequestProvider<NoParamsPostRequest, org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoParamsPostRequest, NoParamsPostResponse>) classes.get("org.generated.handlers.NoParamsCreate")
                .newInstance(CheckedEntityActionProvider.class)
                .with(
                        (CheckedEntityActionProvider<NoParamsPostRequest, EntityCreator<org.generated.api.types.Entity, Create>>) request -> provider.adapter(request).crud()
                )
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoParamsCreate.java");

        assertThat(
                classes.get("org.generated.handlers.NoParamsCreate").get(),
                is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(CheckedEntityActionProvider.class)
                                        .withParameters(
                                                classTypeParameter(NoParamsPostRequest.class),
                                                typeParameter().aType(genericType()
                                                        .baseClass(EntityCreator.class)
                                                        .withParameters(
                                                                classTypeParameter(org.generated.api.types.Entity.class),
                                                                classTypeParameter(Create.class)
                                                        )
                                                )
                                        )
                                )
                        )
                        .with(aPublic().method().named("apply")
                                .withParameters(NoParamsPostRequest.class)
                                .returning(NoParamsPostResponse.class)
                        )
                )
        );
    }



    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsPostResponse response = this.handler((request) -> {
            throw new Exception("");
        }).apply(NoParamsPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }


    @Test
    public void givenAdapterGetted__whenCreatedEntityIsNull__then500() throws Exception {
        NoParamsPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return null;
            }
        })).apply(NoParamsPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenNoValuePosted__thenEmptyObjectIsPassedToAdapter() throws Exception {
        AtomicReference<Create> requestedPayload = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().p("v").build());
            }
        })).apply(NoParamsPostRequest.builder().build());

        assertThat(requestedPayload.get(), is(org.generated.api.types.Create.builder().build()));
    }

    @Test
    public void givenAdapterOK__whenValuePosted__thenPostedValueIsPassedToAdapter() throws Exception {
        Create aValue = Create.builder().p("v").build();
        AtomicReference<Create> requestedPayload = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().p("v").build());
            }
        })).apply(NoParamsPostRequest.builder().payload(aValue).build());

        assertThat(requestedPayload.get(), is(aValue));
    }

    @Test
    public void givenAdapterOK__whenValueCreated__then201_andXEntityIdSetted_andLocationSetted_andValueReturned() throws Exception {
        org.generated.api.types.Entity aValue = org.generated.api.types.Entity.builder().p("v").build();

        NoParamsPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.ONE, aValue);
            }
        })).apply(NoParamsPostRequest.builder().build());

        response.opt().status201().orElseThrow(() -> new AssertionError("expected 201, got " + response));

        assertThat(response.status201().xEntityId(), is("12"));
        assertThat(response.status201().location(), is(new TestCRUD().entityRepositoryUrl() + "/12"));
        assertThat(response.status201().payload(), is(aValue));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(error, msg);
            }
        })).apply(NoParamsPostRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(error, msg);
            }
        })).apply(NoParamsPostRequest.builder().build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(error, msg);
            }
        })).apply(NoParamsPostRequest.builder().build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(error, msg);
            }
        })).apply(NoParamsPostRequest.builder().build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(Error.fromMap(error.toMap()).build()));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        org.codingmatters.poom.api.paged.collection.api.types.Error error = org.codingmatters.poom.api.paged.collection.api.types.Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsPostResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> createEntityFrom(Create value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(error, msg);
            }
        })).apply(NoParamsPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(Error.fromMap(error.toMap()).build()));
    }
}
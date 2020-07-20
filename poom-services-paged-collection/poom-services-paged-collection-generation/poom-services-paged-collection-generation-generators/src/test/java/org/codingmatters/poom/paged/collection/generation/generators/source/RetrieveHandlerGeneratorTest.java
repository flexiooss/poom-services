package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.generic.resource.domain.CheckedEntityActionProvider;
import org.codingmatters.poom.generic.resource.domain.EntityCreator;
import org.codingmatters.poom.generic.resource.domain.EntityRetriever;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestCRUD;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestData;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.generated.api.NoParamsElementGetRequest;
import org.generated.api.NoParamsElementGetResponse;
import org.generated.api.NoParamsPostRequest;
import org.generated.api.types.*;
import org.generated.api.types.Error;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RetrieveHandlerGeneratorTest {

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
                new RetrieveHandlerGenerator(TestData.FULL_COLLECTION).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoParamsElementGetRequest, NoParamsElementGetResponse> handler(PagedCollectionAdapter.FromRequestProvider<NoParamsElementGetRequest, org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoParamsElementGetRequest, NoParamsElementGetResponse>) classes.get("org.generated.handlers.NoParamsRetrieve")
                .newInstance(CheckedEntityActionProvider.class)
                .with(
                        (CheckedEntityActionProvider<NoParamsElementGetRequest, EntityRetriever<Entity>>) request -> provider.adapter(request).crud()
                )
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoParamsRetrieve.java");

        assertThat(
                classes.get("org.generated.handlers.NoParamsRetrieve").get(),
                is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(CheckedEntityActionProvider.class)
                                        .withParameters(
                                                classTypeParameter(NoParamsElementGetRequest.class),
                                                typeParameter().aType(genericType()
                                                        .baseClass(EntityRetriever.class)
                                                        .withParameters(
                                                                classTypeParameter(org.generated.api.types.Entity.class)
                                                        )
                                                )
                                        )
                                )
                        )
                        .with(aPublic().method().named("apply")
                                .withParameters(NoParamsElementGetRequest.class)
                                .returning(NoParamsElementGetResponse.class)
                        )
                )
        );
    }
    
    
    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementGetResponse response = this.handler((request) -> {
            throw new Exception("");
        }).apply(NoParamsElementGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenNoEntityIdProvided__then400_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD())).apply(NoParamsElementGetRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        Error error = response.status400().payload();
        assertThat(error.code(), is(Error.Code.BAD_REQUEST));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenEntityIdProvided__thenEntityIdPassedToAdapter() throws Exception {
        AtomicReference<String> requestedEntityId = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedEntityId.set(id);
                return Optional.empty();
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        assertThat(requestedEntityId.get(), is("12"));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenEntityNotFound__then404_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return Optional.empty();
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        Error error = response.status404().payload();
        assertThat(error.code(), is(Error.Code.RESOURCE_NOT_FOUND));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.ENTITY_NOT_FOUND));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining("12")));
        assertThat(error.messages().get(1).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(1).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenEntityFound__then200_andEntityReturned() throws Exception {
        Entity aValue = Entity.builder().p("v").build();
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return Optional.of(new ImmutableEntity<>("12", BigInteger.ONE, aValue));
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().xEntityId(), is("12"));
        assertThat(response.status200().location(), is(new TestCRUD().entityRepositoryUrl() + "/12"));
        assertThat(response.status200().payload(), is(aValue));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementGetResponse response = this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<org.codingmatters.poom.servives.domain.entities.Entity<Entity>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementGetRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }
}
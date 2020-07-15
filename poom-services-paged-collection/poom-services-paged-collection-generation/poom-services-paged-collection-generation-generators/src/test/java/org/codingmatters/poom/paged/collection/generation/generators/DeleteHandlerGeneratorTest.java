package org.codingmatters.poom.paged.collection.generation.generators;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestCRUD;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestData;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.generated.api.NoParamsElementDeleteRequest;
import org.generated.api.NoParamsElementDeleteResponse;
import org.generated.api.types.Create;
import org.generated.api.types.Error;
import org.generated.api.types.Replace;
import org.generated.api.types.Update;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DeleteHandlerGeneratorTest {

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
                new DeleteHandlerGenerator(TestData.FULL_COLLECTION).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoParamsElementDeleteRequest, NoParamsElementDeleteResponse> handler(PagedCollectionAdapter.Provider<org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoParamsElementDeleteRequest, NoParamsElementDeleteResponse>) classes.get("org.generated.handlers.NoParamsDelete")
                .newInstance(PagedCollectionAdapter.Provider.class)
                .with(provider)
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoParamsDelete.java");

        assertThat(
                classes.get("org.generated.handlers.NoParamsDelete").get(),
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
                                .withParameters(NoParamsElementDeleteRequest.class)
                                .returning(NoParamsElementDeleteResponse.class)
                        )
                )
        );
    }

    
    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementDeleteResponse response = this.handler(() -> {
            throw new Exception("");
        }).apply(NoParamsElementDeleteRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenCRUDIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter()).apply(NoParamsElementDeleteRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenNoEntityIdProvided__then400() throws Exception {
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter(new TestCRUD())).apply(NoParamsElementDeleteRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        Error error = response.status400().payload();
        assertThat(error.code(), is(Error.Code.BAD_REQUEST));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenEntityIdProvided__thenEntityIdPassedToAdapter() throws Exception {
        AtomicReference<String> requestedId = new AtomicReference<>();

        this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedId.set(id);
            }
        })).apply(NoParamsElementDeleteRequest.builder().entityId("12").build());

        assertThat(requestedId.get(), is("12"));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenEntityDeleted__then204() throws Exception {
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            }
        })).apply(NoParamsElementDeleteRequest.builder().entityId("12").build());

        response.opt().status204().orElseThrow(() -> new AssertionError("expected 204, got " + response));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementDeleteRequest.builder().entityId("12").build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementDeleteRequest.builder().entityId("12").build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementDeleteRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementDeleteRequest.builder().entityId("12").build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        NoParamsElementDeleteResponse response = this.handler(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(org.codingmatters.poom.api.paged.collection.api.types.Error.fromMap(error.toMap()).build(), msg);
            }
        })).apply(NoParamsElementDeleteRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }
}
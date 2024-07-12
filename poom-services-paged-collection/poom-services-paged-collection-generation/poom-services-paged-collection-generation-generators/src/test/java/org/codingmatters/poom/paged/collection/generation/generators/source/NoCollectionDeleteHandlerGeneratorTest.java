package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.generic.resource.domain.CheckedEntityActionProvider;
import org.codingmatters.poom.generic.resource.domain.EntityDeleter;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.impl.DefaultAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestData;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.generated.api.NoCollectionDeleteRequest;
import org.generated.api.NoCollectionDeleteResponse;
import org.generated.api.types.Entity;
import org.generated.api.types.Replace;
import org.generated.api.types.Update;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NoCollectionDeleteHandlerGeneratorTest {

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
                new DeleteHandlerGenerator(TestData.NO_COLLECTION_COLLECTION).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoCollectionDeleteRequest, NoCollectionDeleteResponse> handler(PagedCollectionAdapter.FromRequestProvider<NoCollectionDeleteRequest, org.generated.api.types.Entity, Void, Replace, Update> provider) {
        return (Function<NoCollectionDeleteRequest, NoCollectionDeleteResponse>) classes.get("org.generated.handlers.NoCollectionDelete")
                .newInstance(CheckedEntityActionProvider.class)
                .with(
                        (CheckedEntityActionProvider<NoCollectionDeleteRequest, EntityDeleter>) request -> provider.adapter(request).crud()
                )
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoCollectionDelete.java");

        assertThat(
                classes.get("org.generated.handlers.NoCollectionDelete").get(),
                is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(CheckedEntityActionProvider.class)
                                        .withParameters(
                                                classTypeParameter(NoCollectionDeleteRequest.class),
                                                classTypeParameter(EntityDeleter.class)
                                        )
                                )
                        )
                        .with(aPublic().method().named("apply")
                                .withParameters(NoCollectionDeleteRequest.class)
                                .returning(NoCollectionDeleteResponse.class)
                        )
                )
        );
    }


    @Test
    public void whenDeleting__thenEntityIdIsNull() throws Exception {
        AtomicReference<String> requestedId = new AtomicReference<>();

        this.handler((request) -> new DefaultAdapter<>(new PagedCollectionAdapter.CRUD<Entity, Void, Replace, Update>() {
            @Override
            public org.codingmatters.poom.services.domain.entities.Entity<Entity> createEntityFrom(Void unused) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return null;
            }

            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                requestedId.set(id);
            }

            @Override
            public org.codingmatters.poom.services.domain.entities.Entity<Entity> replaceEntityWith(String s, Replace replace) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return null;
            }

            @Override
            public Optional<org.codingmatters.poom.services.domain.entities.Entity<Entity>> retrieveEntity(String s) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return Optional.empty();
            }

            @Override
            public org.codingmatters.poom.services.domain.entities.Entity<Entity> updateEntityWith(String s, Update update) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return null;
            }

            @Override
            public String entityType() {
                return "";
            }

            @Override
            public String entityRepositoryUrl() {
                return "";
            }
        }, null)).apply(NoCollectionDeleteRequest.builder().build());

        assertThat(requestedId.get(), is(nullValue()));
    }
}
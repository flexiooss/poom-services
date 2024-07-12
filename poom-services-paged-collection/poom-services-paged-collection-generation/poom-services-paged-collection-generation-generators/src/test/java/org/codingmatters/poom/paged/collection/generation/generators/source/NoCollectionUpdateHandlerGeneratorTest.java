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
import org.generated.api.NoCollectionPatchRequest;
import org.generated.api.NoCollectionPatchResponse;
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

public class NoCollectionUpdateHandlerGeneratorTest {

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
                new ReplaceOrUpdateHandlerGenerator(TestData.NO_COLLECTION_COLLECTION, ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoCollectionPatchRequest, NoCollectionPatchResponse> handler(PagedCollectionAdapter.FromRequestProvider<NoCollectionPatchRequest, org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoCollectionPatchRequest, NoCollectionPatchResponse>) classes.get("org.generated.handlers.NoCollectionUpdate")
                .newInstance(CheckedEntityActionProvider.class)
                .with(
                        (CheckedEntityActionProvider<NoCollectionPatchRequest, EntityUpdater<org.generated.api.types.Entity, Update>>) request -> provider.adapter(request).crud()
                )
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoCollectionUpdate.java");

        assertThat(
                classes.get("org.generated.handlers.NoCollectionUpdate").get(),
                is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(CheckedEntityActionProvider.class)
                                        .withParameters(
                                                classTypeParameter(NoCollectionPatchRequest.class),
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
                                .withParameters(NoCollectionPatchRequest.class)
                                .returning(NoCollectionPatchResponse.class)
                        )
                )
        );
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
        })).apply(NoCollectionPatchRequest.builder().build());

        assertThat(requestedId.get(), is(nullValue()));
    }

}

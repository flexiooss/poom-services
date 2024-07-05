package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.generic.resource.domain.CheckedEntityActionProvider;
import org.codingmatters.poom.generic.resource.domain.EntityReplacer;
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
import org.generated.api.NoCollectionPutRequest;
import org.generated.api.NoCollectionPutResponse;
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

public class NoCollectionReplaceHandlerGeneratorTest {

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
                new ReplaceOrUpdateHandlerGenerator(TestData.NO_COLLECTION_COLLECTION, ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoCollectionPutRequest, NoCollectionPutResponse> handler(PagedCollectionAdapter.FromRequestProvider<NoCollectionPutRequest, org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoCollectionPutRequest, NoCollectionPutResponse>) classes.get("org.generated.handlers.NoCollectionReplace")
                .newInstance(CheckedEntityActionProvider.class)
                .with(
                        (CheckedEntityActionProvider<NoCollectionPutRequest, EntityReplacer<org.generated.api.types.Entity, Replace>>) request -> provider.adapter(request).crud()
                )
                .get();
    }

    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoCollectionReplace.java");

        assertThat(
                classes.get("org.generated.handlers.NoCollectionReplace").get(),
                is(aPublic().class_()
                        .implementing(genericType().baseClass(Function.class))
                        .with(aPublic().constructor()
                                .withParameters(genericType()
                                        .baseClass(CheckedEntityActionProvider.class)
                                        .withParameters(
                                                classTypeParameter(NoCollectionPutRequest.class),
                                                typeParameter().aType(genericType()
                                                        .baseClass(EntityReplacer.class)
                                                        .withParameters(
                                                                classTypeParameter(org.generated.api.types.Entity.class),
                                                                classTypeParameter(Replace.class)
                                                        )
                                                )
                                        )
                                )
                        )
                        .with(aPublic().method().named("apply")
                                .withParameters(NoCollectionPutRequest.class)
                                .returning(NoCollectionPutResponse.class)
                        )
                )
        );
    }


    @Test
    public void givenAdapterOK__whenEntityIdProvided__thenEntityIdIsPassedToAdapter() throws Exception {
        AtomicReference<String> requestedId = new AtomicReference<>();

        this.handler((request) -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<org.generated.api.types.Entity> replaceEntityWith(String id, Replace value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedId.set(id);
                return new ImmutableEntity<>("12", BigInteger.ONE, org.generated.api.types.Entity.builder().build());
            }
        })).apply(NoCollectionPutRequest.builder().build());

        assertThat(requestedId.get(), is(nullValue()));
    }


}
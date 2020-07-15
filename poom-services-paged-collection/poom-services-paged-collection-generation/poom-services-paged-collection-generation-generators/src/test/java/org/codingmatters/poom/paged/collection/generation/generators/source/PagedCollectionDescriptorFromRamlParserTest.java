package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestData;
import org.codingmatters.poom.paged.collection.generation.spec.Action;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.paged.collection.generation.spec.pagedcollectiondescriptor.Types;
import org.codingmatters.rest.api.generator.exception.RamlSpecException;
import org.codingmatters.rest.maven.plugin.raml.RamlFileCollector;
import org.generated.api.*;
import org.generated.api.types.*;
import org.generated.api.types.Error;
import org.junit.Assert;
import org.junit.Test;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PagedCollectionDescriptorFromRamlParserTest {

    static RamlModelResult TEST_API_RAML = raml("test-api-spec.raml");

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenCollectionNameSetFromCollectionDisplayName() throws Exception {
        PagedCollectionDescriptor[] parsed = new PagedCollectionDescriptorFromRamlParser(TEST_API_RAML, "org.generated.api", "org.generated.api.types").parse();

        Assert.assertTrue(Arrays.stream(parsed).anyMatch(descriptor -> descriptor.name().equals(TestData.FULL_COLLECTION.name())));
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenTypesAreGetFromApiSpec() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.types(),
                is(TestData.FULL_COLLECTION.types())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenEntityIdParamIsGetFromPagedCollectionEntityUriParameter() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.entityIdParam(),
                is("entity-id")
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenBrowseActionIsGetFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.browse(),
                is(TestData.FULL_COLLECTION.browse())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenRetrieveActionIsGetFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.retrieve(),
                is(TestData.FULL_COLLECTION.retrieve())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenCreateActionIsGetFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.create(),
                is(TestData.FULL_COLLECTION.create())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenDeleteActionIsGetFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.delete(),
                is(TestData.FULL_COLLECTION.delete())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenReplaceActionIsGetFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.replace(),
                is(TestData.FULL_COLLECTION.replace())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenUpdateActionIsGetFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor.update(),
                is(TestData.FULL_COLLECTION.update())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingFullCollection__thenDescriptorIsCreatedFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseFullCollection();

        assertThat(
                collectionDescriptor,
                is(TestData.FULL_COLLECTION)
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingCompleteCollectionWithParams__thenDescriptorIsCreatedFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("WithParam");

        assertThat(
                collectionDescriptor,
                is(PagedCollectionDescriptor.builder()
                            .name("WithParam").entityIdParam("entity-id")
                            .types(Types.builder()
                                    .entity(org.generated.api.types.Entity.class.getName())
                                    .create(Create.class.getName())
                                    .replace(Replace.class.getName())
                                    .update(Update.class.getName())
                                    .error(Error.class.getName())
                                    .message(Message.class.getName())
                                    .build())
                            .browse(Action.builder()
                                    .requestValueObject(WithParamGetRequest.class.getName())
                                    .responseValueObject(WithParamGetResponse.class.getName())
                                    .build())
                            .create(Action.builder()
                                    .requestValueObject(WithParamPostRequest.class.getName())
                                    .responseValueObject(WithParamPostResponse.class.getName())
                                    .build())
                            .retrieve(Action.builder()
                                    .requestValueObject(WithParamsElementGetRequest.class.getName())
                                    .responseValueObject(WithParamsElementGetResponse.class.getName())
                                    .build())
                            .delete(Action.builder()
                                    .requestValueObject(WithParamsElementDeleteRequest.class.getName())
                                    .responseValueObject(WithParamsElementDeleteResponse.class.getName())
                                    .build())
                            .replace(Action.builder()
                                    .requestValueObject(WithParamsElementPutRequest.class.getName())
                                    .responseValueObject(WithParamsElementPutResponse.class.getName())
                                    .build())
                            .update(Action.builder()
                                    .requestValueObject(WithParamsElementPatchRequest.class.getName())
                                    .responseValueObject(WithParamsElementPatchResponse.class.getName())
                                    .build())
                            .build())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingRootCollection__thenDescriptorIsCreatedFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("Root");

        assertThat(
                collectionDescriptor,
                is(PagedCollectionDescriptor.builder()
                        .name("Root").entityIdParam("entity-id")
                        .types(Types.builder()
                                .entity(org.generated.api.types.Entity.class.getName())
                                .create(Create.class.getName())
                                .replace(Replace.class.getName())
                                .update(Update.class.getName())
                                .error(Error.class.getName())
                                .message(Message.class.getName())
                                .build())
                        .browse(Action.builder()
                                .requestValueObject(RootGetRequest.class.getName())
                                .responseValueObject(RootGetResponse.class.getName())
                                .build())
                        .create(Action.builder()
                                .requestValueObject(RootPostRequest.class.getName())
                                .responseValueObject(RootPostResponse.class.getName())
                                .build())
                        .retrieve(Action.builder()
                                .requestValueObject(RootEntityGetRequest.class.getName())
                                .responseValueObject(RootEntityGetResponse.class.getName())
                                .build())
                        .delete(Action.builder()
                                .requestValueObject(RootEntityDeleteRequest.class.getName())
                                .responseValueObject(RootEntityDeleteResponse.class.getName())
                                .build())
                        .replace(Action.builder()
                                .requestValueObject(RootEntityPutRequest.class.getName())
                                .responseValueObject(RootEntityPutResponse.class.getName())
                                .build())
                        .update(Action.builder()
                                .requestValueObject(RootEntityPatchRequest.class.getName())
                                .responseValueObject(RootEntityPatchResponse.class.getName())
                                .build())
                        .build())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingNodeCollection__thenDescriptorIsCreatedFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("Node");

        assertThat(
                collectionDescriptor,
                is(PagedCollectionDescriptor.builder()
                        .name("Node").entityIdParam("entity-id")
                        .types(Types.builder()
                                .entity(org.generated.api.types.Entity.class.getName())
                                .create(Create.class.getName())
                                .replace(Replace.class.getName())
                                .update(Update.class.getName())
                                .error(Error.class.getName())
                                .message(Message.class.getName())
                                .build())
                        .browse(Action.builder()
                                .requestValueObject(NodeGetRequest.class.getName())
                                .responseValueObject(NodeGetResponse.class.getName())
                                .build())
                        .create(Action.builder()
                                .requestValueObject(NodePostRequest.class.getName())
                                .responseValueObject(NodePostResponse.class.getName())
                                .build())
                        .retrieve(Action.builder()
                                .requestValueObject(NodeEntityGetRequest.class.getName())
                                .responseValueObject(NodeEntityGetResponse.class.getName())
                                .build())
                        .delete(Action.builder()
                                .requestValueObject(NodeEntityDeleteRequest.class.getName())
                                .responseValueObject(NodeEntityDeleteResponse.class.getName())
                                .build())
                        .replace(Action.builder()
                                .requestValueObject(NodeEntityPutRequest.class.getName())
                                .responseValueObject(NodeEntityPutResponse.class.getName())
                                .build())
                        .update(Action.builder()
                                .requestValueObject(NodeEntityPatchRequest.class.getName())
                                .responseValueObject(NodeEntityPatchResponse.class.getName())
                                .build())
                        .build())
        );
    }

    @Test
    public void givenParsingTestApi__whenGettingLeafCollection__thenDescriptorIsCreatedFromApi() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("Leaf");

        assertThat(
                collectionDescriptor,
                is(PagedCollectionDescriptor.builder()
                        .name("Leaf").entityIdParam("entity-id")
                        .types(Types.builder()
                                .entity(org.generated.api.types.Entity.class.getName())
                                .create(Create.class.getName())
                                .replace(Replace.class.getName())
                                .update(Update.class.getName())
                                .error(Error.class.getName())
                                .message(Message.class.getName())
                                .build())
                        .browse(Action.builder()
                                .requestValueObject(LeafGetRequest.class.getName())
                                .responseValueObject(LeafGetResponse.class.getName())
                                .build())
                        .create(Action.builder()
                                .requestValueObject(LeafPostRequest.class.getName())
                                .responseValueObject(LeafPostResponse.class.getName())
                                .build())
                        .retrieve(Action.builder()
                                .requestValueObject(LeafEntityGetRequest.class.getName())
                                .responseValueObject(LeafEntityGetResponse.class.getName())
                                .build())
                        .delete(Action.builder()
                                .requestValueObject(LeafEntityDeleteRequest.class.getName())
                                .responseValueObject(LeafEntityDeleteResponse.class.getName())
                                .build())
                        .replace(Action.builder()
                                .requestValueObject(LeafEntityPutRequest.class.getName())
                                .responseValueObject(LeafEntityPutResponse.class.getName())
                                .build())
                        .update(Action.builder()
                                .requestValueObject(LeafEntityPatchRequest.class.getName())
                                .responseValueObject(LeafEntityPatchResponse.class.getName())
                                .build())
                        .build())
        );
    }


    @Test
    public void givenParsingTestApi__whenCollectionWithoutAnnotation__thenNoDescriptor() throws Exception {
        PagedCollectionDescriptor[] collections = new PagedCollectionDescriptorFromRamlParser(TEST_API_RAML, "org.generated.api", "org.generated.api.types").parse();

        for (PagedCollectionDescriptor collection : collections) {
            assertThat(collection.name(), is(not("NotACollection")));
        }
    }

    @Test
    public void givenParsingTestApi__whenGettingNoCreateCollection__thenDescriptorHasNoCreateAction() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("NoCreate");

        assertThat(collectionDescriptor.browse(), is(notNullValue()));
        assertThat(collectionDescriptor.create(), is(nullValue()));
        assertThat(collectionDescriptor.retrieve(), is(notNullValue()));
        assertThat(collectionDescriptor.replace(), is(notNullValue()));
        assertThat(collectionDescriptor.update(), is(notNullValue()));
        assertThat(collectionDescriptor.delete(), is(notNullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingNoRetrieveCollection__thenDescriptorHasNoRetrieveAction() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("NoRetrieve");

        assertThat(collectionDescriptor.browse(), is(notNullValue()));
        assertThat(collectionDescriptor.create(), is(notNullValue()));
        assertThat(collectionDescriptor.retrieve(), is(nullValue()));
        assertThat(collectionDescriptor.replace(), is(notNullValue()));
        assertThat(collectionDescriptor.update(), is(notNullValue()));
        assertThat(collectionDescriptor.delete(), is(notNullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingNoReplaceCollection__thenDescriptorHasNoRetrieveAction() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("NoReplace");

        assertThat(collectionDescriptor.browse(), is(notNullValue()));
        assertThat(collectionDescriptor.create(), is(notNullValue()));
        assertThat(collectionDescriptor.retrieve(), is(notNullValue()));
        assertThat(collectionDescriptor.replace(), is(nullValue()));
        assertThat(collectionDescriptor.update(), is(notNullValue()));
        assertThat(collectionDescriptor.delete(), is(notNullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingNoUpdateCollection__thenDescriptorHasNoUpdateAction() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("NoUpdate");

        assertThat(collectionDescriptor.browse(), is(notNullValue()));
        assertThat(collectionDescriptor.create(), is(notNullValue()));
        assertThat(collectionDescriptor.retrieve(), is(notNullValue()));
        assertThat(collectionDescriptor.replace(), is(notNullValue()));
        assertThat(collectionDescriptor.update(), is(nullValue()));
        assertThat(collectionDescriptor.delete(), is(notNullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingNoDeleteCollection__thenDescriptorHasNoDeleteAction() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("NoDelete");

        assertThat(collectionDescriptor.browse(), is(notNullValue()));
        assertThat(collectionDescriptor.create(), is(notNullValue()));
        assertThat(collectionDescriptor.retrieve(), is(notNullValue()));
        assertThat(collectionDescriptor.replace(), is(notNullValue()));
        assertThat(collectionDescriptor.update(), is(notNullValue()));
        assertThat(collectionDescriptor.delete(), is(nullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingNoBrowseCollection__thenDescriptorHasNoDeleteAction() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("NoBrowse");

        assertThat(collectionDescriptor.browse(), is(nullValue()));
        assertThat(collectionDescriptor.create(), is(notNullValue()));
        assertThat(collectionDescriptor.retrieve(), is(notNullValue()));
        assertThat(collectionDescriptor.replace(), is(notNullValue()));
        assertThat(collectionDescriptor.update(), is(notNullValue()));
        assertThat(collectionDescriptor.delete(), is(notNullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingOnlyCreate__thenEntityTypeIsSet_andEntityIdIsNull() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("OnlyCreate");

        assertThat(collectionDescriptor.types().entity(), is(Entity.class.getName()));
        assertThat(collectionDescriptor.entityIdParam(), is(nullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingOnlyBrowse__thenEntityTypeIsSet_andEntityIdIsNull() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("OnlyBrowse");

        assertThat(collectionDescriptor.types().entity(), is(Entity.class.getName()));
        assertThat(collectionDescriptor.entityIdParam(), is(nullValue()));
    }

    @Test
    public void givenParsingTestApi__whenGettingOnlyRetrieve__thenEntityTypeIsSet_andEntityIdIsSet() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("OnlyRetrieve");

        assertThat(collectionDescriptor.types().entity(), is(Entity.class.getName()));
        assertThat(collectionDescriptor.entityIdParam(), is("entity-id"));
    }

    @Test
    public void givenParsingTestApi__whenGettingOnlyReplace__thenEntityTypeIsSet_andEntityIdIsSet() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("OnlyReplace");

        assertThat(collectionDescriptor.types().entity(), is(Entity.class.getName()));
        assertThat(collectionDescriptor.entityIdParam(), is("entity-id"));
    }

    @Test
    public void givenParsingTestApi__whenGettingOnlyUpdate__thenEntityTypeIsSet_andEntityIdIsSet() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("OnlyUpdate");

        assertThat(collectionDescriptor.types().entity(), is(Entity.class.getName()));
        assertThat(collectionDescriptor.entityIdParam(), is("entity-id"));
    }

    @Test
    public void givenParsingTestApi__whenGettingOnlyDelete__thenEntityTypeIsNotSet_andEntityIdIsNotNull() throws Exception {
        PagedCollectionDescriptor collectionDescriptor = this.parseCollection("OnlyDelete");

        assertThat(collectionDescriptor.types().entity(), is(nullValue()));
        assertThat(collectionDescriptor.entityIdParam(), is("entity-id"));
    }


    private PagedCollectionDescriptor parseFullCollection() throws Exception {
        String collectionName = TestData.FULL_COLLECTION.name();
        return parseCollection(collectionName);
    }

    private PagedCollectionDescriptor parseCollection(String collectionName) throws RamlSpecException {
        return Arrays.stream(
                    new PagedCollectionDescriptorFromRamlParser(TEST_API_RAML, "org.generated.api", "org.generated.api.types").parse()
            ).filter(pagedCollectionDescriptor -> pagedCollectionDescriptor.name().equals(collectionName)).findFirst().get();
    }

    static private RamlModelResult raml(String resource) {
        RamlFileCollector.Builder builder = RamlFileCollector.spec(resource);
        RamlFileCollector collector;
        try {
            collector = builder.build();
        } catch (IOException e) {
            throw new RuntimeException("failed parsing " + resource, e);
        }
        return new RamlModelBuilder().buildApi(collector.specFile());
    }
}
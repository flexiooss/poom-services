package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.paged.collection.generation.generators.source.exception.IncoherentDescriptorException;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestData;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.tests.compile.FileHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DegradedHandlersGeneratorTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Rule
    public FileHelper fileHelper = new FileHelper();

    @Test
    public void givenEmptyCollection__whenGenerating__thenBrowseHandlerNotGenerated() throws Exception {
        assertThat(
                new BrowseHandlerGenerator(PagedCollectionDescriptor.builder().name("EmptyCollection").build()).handler(),
                is(nullValue())
        );
    }

    @Test
    public void givenEmptyCollection__whenGenerating__thenCreateHandlerNotGenerated() throws Exception {
        assertThat(
                new CreateHandlerGenerator(PagedCollectionDescriptor.builder().name("EmptyCollection").build()).handler(),
                is(nullValue())
        );
    }

    @Test
    public void givenEmptyCollection__whenGenerating__thenRetrieveHandlerNotGenerated() throws Exception {
        assertThat(
                new RetrieveHandlerGenerator(PagedCollectionDescriptor.builder().name("EmptyCollection").build()).handler(),
                is(nullValue())
        );
    }

    @Test
    public void givenEmptyCollection__whenGenerating__thenReplaceHandlerNotGenerated() throws Exception {
        assertThat(
                new ReplaceOrUpdateHandlerGenerator(
                        PagedCollectionDescriptor.builder().name("EmptyCollection").build(),
                        ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace
                ).handler(),
                is(nullValue())
        );
    }

    @Test
    public void givenEmptyCollection__whenGenerating__thenUpdateHandlerNotGenerated() throws Exception {
        assertThat(
                new ReplaceOrUpdateHandlerGenerator(
                        PagedCollectionDescriptor.builder().name("EmptyCollection").build(),
                        ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update
                ).handler(),
                is(nullValue())
        );
    }

    @Test
    public void givenEmptyCollection__whenGenerating__thenDeleteHandlerNotGenerated() throws Exception {
        assertThat(
                new DeleteHandlerGenerator(PagedCollectionDescriptor.builder().name("EmptyCollection").build()).handler(),
                is(nullValue())
        );
    }








    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingCreate__whenEntityIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new CreateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withEntity(null))
        ).handler();
    }

    @Test()
    public void givenGeneratingCreate__whenEntityIdParamIsNotSet__thenGenerationOk() throws Exception {
        assertThat(new CreateHandlerGenerator(TestData.FULL_COLLECTION
                .withEntityIdParam(null)
        ).handler(), is(notNullValue()));
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingCreate__whenRequestIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new CreateHandlerGenerator(TestData.FULL_COLLECTION
                .withCreate(TestData.FULL_COLLECTION.create().withRequestValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingCreate__whenResponseIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new CreateHandlerGenerator(TestData.FULL_COLLECTION
                .withCreate(TestData.FULL_COLLECTION.create().withResponseValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingCreate__whenCreateTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new CreateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withCreate(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingCreate__whenErrorTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new CreateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withError(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingCreate__whenMessageTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new CreateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withMessage(null))
        ).handler();
    }





    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingRetrieve__whenEntityIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new RetrieveHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withEntity(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingRetrieve__whenEntityIdParamIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new RetrieveHandlerGenerator(TestData.FULL_COLLECTION
                .withEntityIdParam(null)
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingRetrieve__whenRequestIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new RetrieveHandlerGenerator(TestData.FULL_COLLECTION
                .withRetrieve(TestData.FULL_COLLECTION.retrieve().withRequestValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingRetrieve__whenResponseIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new RetrieveHandlerGenerator(TestData.FULL_COLLECTION
                .withRetrieve(TestData.FULL_COLLECTION.retrieve().withResponseValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingRetrieve__whenErrorTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new RetrieveHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withError(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingRetrieve__whenMessageTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new RetrieveHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withMessage(null))
        ).handler();
    }



    @Test()
    public void givenGeneratingBrowse__whenEntityIsNotSet__thenGenerationOk() throws Exception {
        assertThat(new BrowseHandlerGenerator(TestData.FULL_COLLECTION
                .withEntityIdParam(null)
        ).handler(), is(notNullValue()));
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingBrowse__whenEntityIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new BrowseHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withEntity(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingBrowse__whenActionRequestIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new BrowseHandlerGenerator(TestData.FULL_COLLECTION
                .withBrowse(TestData.FULL_COLLECTION.browse().withRequestValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingBrowse__whenActionResponseIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new BrowseHandlerGenerator(TestData.FULL_COLLECTION
                .withBrowse(TestData.FULL_COLLECTION.browse().withResponseValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingBrowse__whenErrorTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new BrowseHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withError(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingBrowse__whenMessageTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new BrowseHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withMessage(null))
        ).handler();
    }





    @Test()
    public void givenGeneratingDelete__whenEntityIsNotSet__thenGenerationOk() throws Exception {
        assertThat(new DeleteHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withEntity(null))
        ).handler(), is(notNullValue()));
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingDelete__whenEntityIdParamIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new DeleteHandlerGenerator(TestData.FULL_COLLECTION
                .withEntityIdParam(null)
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingDelete__whenRequestIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new DeleteHandlerGenerator(TestData.FULL_COLLECTION
                .withDelete(TestData.FULL_COLLECTION.delete().withRequestValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingDelete__whenResponseIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new DeleteHandlerGenerator(TestData.FULL_COLLECTION
                .withDelete(TestData.FULL_COLLECTION.delete().withResponseValueObject(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingDelete__whenErrorTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new DeleteHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withError(null))
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingDelete__whenMessageTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new DeleteHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withMessage(null))
        ).handler();
    }






    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingReplace__whenEntityIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withEntity(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingReplace__whenEntityIdParamIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withEntityIdParam(null)
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingReplace__whenRequestIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withReplace(TestData.FULL_COLLECTION.replace().withRequestValueObject(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingReplace__whenResponseIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withReplace(TestData.FULL_COLLECTION.replace().withResponseValueObject(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingReplace__whenReplaceTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withReplace(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingReplace__whenErrorTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withError(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingReplace__whenMessageTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withMessage(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler();
    }





    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingUpdate__whenEntityIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withEntity(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update
        ).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingUpdate__whenEntityIdParamIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withEntityIdParam(null)
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingUpdate__whenRequestIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withUpdate(TestData.FULL_COLLECTION.update().withRequestValueObject(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingUpdate__whenResponseIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withUpdate(TestData.FULL_COLLECTION.update().withResponseValueObject(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingUpdate__whenUpdateTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withUpdate(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingUpdate__whenErrorTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withError(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler();
    }

    @Test(expected = IncoherentDescriptorException.class)
    public void givenGeneratingUpdate__whenMessageTypeIsNotSet__thenIncoherentDescriptorException() throws Exception {
        new ReplaceOrUpdateHandlerGenerator(TestData.FULL_COLLECTION
                .withTypes(TestData.FULL_COLLECTION.types().withMessage(null))
                , ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler();
    }

}
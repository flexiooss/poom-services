package org.codingmatters.poom.paged.collection.generation.generators;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestData;
import org.codingmatters.poom.paged.collection.generation.generators.test.TestPager;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.generated.api.NoParamsGetRequest;
import org.generated.api.NoParamsGetResponse;
import org.generated.api.types.*;
import org.generated.api.types.Error;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BrowseHandlerGeneratorTest {

    static private class ListerRequest {
        public final Long start;
        public final Long end;
        public final PropertyQuery query;

        public ListerRequest(Long start, Long end) {
            this(start, end, null);
        }
        public ListerRequest(Long start, Long end, PropertyQuery query) {
            this.start = start;
            this.end = end;
            this.query = query;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListerRequest that = (ListerRequest) o;
            return Objects.equals(start, that.start) &&
                    Objects.equals(end, that.end) &&
                    Objects.equals(query, that.query);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, query);
        }

        @Override
        public String toString() {
            return "ListerRequest{" +
                    "start=" + start +
                    ", end=" + end +
                    ", query=" + query +
                    '}';
        }
    }

    private static final AtomicReference<ListerRequest> lastRequest = new AtomicReference<>();

    private static final EntityLister<org.generated.api.types.Entity, PropertyQuery> ENTITY_LISTER = new EntityLister<>() {
        @Override
        public PagedEntityList<org.generated.api.types.Entity> all(long start, long end) throws RepositoryException {
            lastRequest.set(new ListerRequest(start, end));
            return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, new LinkedList<>());
        }

        @Override
        public PagedEntityList<org.generated.api.types.Entity> search(PropertyQuery propertyQuery, long start, long end) throws RepositoryException {
            lastRequest.set(new ListerRequest(start, end, propertyQuery));
            return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, new LinkedList<>());
        }
    };


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
                new BrowseHandlerGenerator(TestData.FULL_COLLECTION).handler()
        );
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }

    private Function<NoParamsGetRequest, NoParamsGetResponse> handler(PagedCollectionAdapter.Provider<org.generated.api.types.Entity, Create, Replace, Update> provider) {
        return (Function<NoParamsGetRequest, NoParamsGetResponse>) classes.get("org.generated.handlers.NoParamsBrowse")
                .newInstance(PagedCollectionAdapter.Provider.class)
                .with(provider)
                .get();
    }


    @Test
    public void givenGeneratingBrowseHandler__whenFullCollection__thenPublicInterface() throws Exception {
        this.fileHelper.printFile(this.dir.getRoot(), "NoParamsBrowse.java");

        assertThat(
                classes.get("org.generated.handlers.NoParamsBrowse").get(),
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
                                .withParameters(NoParamsGetRequest.class)
                                .returning(NoParamsGetResponse.class)
                        )
                )
        );
    }

    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsGetResponse response = this.handler(() -> {
            throw new Exception("");
        }).apply(NoParamsGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenPagerIsNull__then405_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(null, null)).apply(NoParamsGetRequest.builder().build());

        response.opt().status405().orElseThrow(() -> new AssertionError("expected 405, got " + response));

        Error error = response.status405().payload();
        assertThat(error.code(), is(Error.Code.COLLECTION_BROWSING_NOT_ALLOWED));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerIsNotNull__whenUnitIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager(null, ENTITY_LISTER, 100))).apply(NoParamsGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerIsNotNull__whenListerIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", null, 100))).apply(NoParamsGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerIsNotNull__whenMaxPageSizeIsNotGreaterThanZero__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", null, 0))).apply(NoParamsGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenNoParameters__thenRequestDelegatedToLister_andRangeIs0ToMaxPageSizeMinus1() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(NoParamsGetRequest.builder().build());

        assertThat(lastRequest.get(), is(new ListerRequest(0L, 99L, null)));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenRangeParameterBelowMaxSize__thenRequestDelegatedToLister_andRangeIsPassedUnchanged() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(NoParamsGetRequest.builder().range("12-42").build());

        assertThat(lastRequest.get(), is(new ListerRequest(12L, 42L, null)));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenRangeParameterOverMaxSize__thenRequestDelegatedToLister_andRangeIsPassedConstrainedToMaxPageSize() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(NoParamsGetRequest.builder().range("12-142").build());

        assertThat(lastRequest.get(), is(new ListerRequest(12L, 111L, null)));
    }



    @Test
    public void givenAdapterOk_andPagerOk__whenFilterAndSortByParameter__thenRequestDelegatedToLister_andQueryIsParsed() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(NoParamsGetRequest.builder().filter("a > 12").orderBy("b").build());

        assertThat(lastRequest.get(), is(new ListerRequest(0L, 99L, PropertyQuery.builder().filter("a > 12").sort("b").build())));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenCompleteList__then200() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", new EntityLister<>() {
            @Override
            public PagedEntityList<org.generated.api.types.Entity> all(long start, long end) throws RepositoryException {
                return new PagedEntityList.DefaultPagedEntityList<>(0, 44, 45, entities(45));
            }

            @Override
            public PagedEntityList<org.generated.api.types.Entity> search(PropertyQuery propertyQuery, long start, long end) throws RepositoryException {
                return null;
            }
        }, 100))).apply(NoParamsGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));
    }


    @Test
    public void givenAdapterOk_andPagerOk__whenPartialList__then206() throws Exception {
        NoParamsGetResponse response = this.handler(() -> new TestAdapter(new TestPager("Unit", new EntityLister<>() {
            @Override
            public PagedEntityList<org.generated.api.types.Entity> all(long start, long end) throws RepositoryException {
                return new PagedEntityList.DefaultPagedEntityList<>(0, 99, 200, entities(100));
            }

            @Override
            public PagedEntityList<org.generated.api.types.Entity> search(PropertyQuery propertyQuery, long start, long end) throws RepositoryException {
                return null;
            }
        }, 100))).apply(NoParamsGetRequest.builder().build());


        response.opt().status206().orElseThrow(() -> new AssertionError("expected 206, got " + response));
    }

    private List<Entity<org.generated.api.types.Entity>> entities(int count) {
        List<Entity<org.generated.api.types.Entity>> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new ImmutableEntity<org.generated.api.types.Entity>("" + i, BigInteger.ONE, org.generated.api.types.Entity.builder().p("" + i).build()));
        }
        return result;
    }
}
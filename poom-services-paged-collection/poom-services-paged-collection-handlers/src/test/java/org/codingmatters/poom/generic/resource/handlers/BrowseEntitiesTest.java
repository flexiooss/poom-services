package org.codingmatters.poom.generic.resource.handlers;


import org.codingmatters.poom.api.generic.resource.api.PagedCollectionGetRequest;
import org.codingmatters.poom.api.generic.resource.api.PagedCollectionGetResponse;
import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.generic.resource.handlers.tests.TestAdapter;
import org.codingmatters.poom.generic.resource.handlers.tests.TestPager;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

public class BrowseEntitiesTest {

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

    private static final EntityLister<ObjectValue, PropertyQuery> ENTITY_LISTER = new EntityLister<ObjectValue, PropertyQuery>() {
        @Override
        public PagedEntityList<ObjectValue> all(long start, long end) throws RepositoryException {
            lastRequest.set(new ListerRequest(start, end));
            return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, new LinkedList<>());
        }

        @Override
        public PagedEntityList<ObjectValue> search(PropertyQuery propertyQuery, long start, long end) throws RepositoryException {
            lastRequest.set(new ListerRequest(start, end, propertyQuery));
            return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, new LinkedList<>());
        }
    };

    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> {
            throw new Exception("");
        }).apply(PagedCollectionGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenPagerIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(null, null)).apply(PagedCollectionGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerIsNotNull__whenUnitIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager(null, ENTITY_LISTER, 100))).apply(PagedCollectionGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerIsNotNull__whenListerIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", null, 100))).apply(PagedCollectionGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerIsNotNull__whenMaxPageSizeIsNotGreaterThanZero__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", null, 0))).apply(PagedCollectionGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenNoParameters__thenRequestDelegatedToLister_andRangeIs0ToMaxPageSizeMinus1() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(PagedCollectionGetRequest.builder().build());

        assertThat(lastRequest.get(), is(new ListerRequest(0L, 99L, null)));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenRangeParameterBelowMaxSize__thenRequestDelegatedToLister_andRangeIsPassedUnchanged() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(PagedCollectionGetRequest.builder().range("12-42").build());

        assertThat(lastRequest.get(), is(new ListerRequest(12L, 42L, null)));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenRangeParameterOverMaxSize__thenRequestDelegatedToLister_andRangeIsPassedConstrainedToMaxPageSize() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(PagedCollectionGetRequest.builder().range("12-142").build());

        assertThat(lastRequest.get(), is(new ListerRequest(12L, 111L, null)));
    }



    @Test
    public void givenAdapterOk_andPagerOk__whenFilterAndSortByParameter__thenRequestDelegatedToLister_andQueryIsParsed() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
                .apply(PagedCollectionGetRequest.builder().filter("a > 12").orderBy("b").build());

        assertThat(lastRequest.get(), is(new ListerRequest(0L, 99L, PropertyQuery.builder().filter("a > 12").sort("b").build())));
    }

    @Test
    public void givenAdapterOk_andPagerOk__whenCompleteList__then200() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", new EntityLister<ObjectValue, PropertyQuery>() {
            @Override
            public PagedEntityList<ObjectValue> all(long start, long end) throws RepositoryException {
                return new PagedEntityList.DefaultPagedEntityList<ObjectValue>(0, 44, 45, entities(45));
            }

            @Override
            public PagedEntityList<ObjectValue> search(PropertyQuery propertyQuery, long start, long end) throws RepositoryException {
                return null;
            }
        }, 100))).apply(PagedCollectionGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));
    }


    @Test
    public void givenAdapterOk_andPagerOk__whenPartialList__then206() throws Exception {
        PagedCollectionGetResponse response = new BrowseEntities(() -> new TestAdapter(new TestPager("Unit", new EntityLister<ObjectValue, PropertyQuery>() {
            @Override
            public PagedEntityList<ObjectValue> all(long start, long end) throws RepositoryException {
                return new PagedEntityList.DefaultPagedEntityList<ObjectValue>(0, 99, 200, entities(100));
            }

            @Override
            public PagedEntityList<ObjectValue> search(PropertyQuery propertyQuery, long start, long end) throws RepositoryException {
                return null;
            }
        }, 100))).apply(PagedCollectionGetRequest.builder().build());

        response.opt().status206().orElseThrow(() -> new AssertionError("expected 206, got " + response));
    }

    private List<Entity<ObjectValue>> entities(int count) {
        List<Entity<ObjectValue>> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new ImmutableEntity<ObjectValue>("" + i, BigInteger.ONE, ObjectValue.builder().property("p", PropertyValue.builder().longValue(Long.valueOf(i)).build()).build()));
        }
        return result;
    }
}
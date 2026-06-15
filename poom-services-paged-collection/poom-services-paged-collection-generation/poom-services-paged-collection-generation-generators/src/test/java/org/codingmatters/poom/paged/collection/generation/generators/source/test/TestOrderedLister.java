package org.codingmatters.poom.paged.collection.generation.generators.source.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.generated.api.types.Entity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class TestOrderedLister implements PagedCollectionAdapter.OrderedLister<Entity> {

    public enum Method { INIT_LATEST, INIT_OLDEST, SINCE, BEFORE }

    public static class LastCall {
        public final Method method;
        public final String cursor;      // since value for Method.SINCE, before value for Method.BEFORE, null for INIT_*
        public final Optional<String> optBefore;  // only meaningful for Method.SINCE
        public final Optional<PropertyQuery> query;
        public final long start;
        public final long end;

        public LastCall(Method method, String cursor, Optional<String> optBefore,
                        Optional<PropertyQuery> query, long start, long end) {
            this.method = method;
            this.cursor = cursor;
            this.optBefore = optBefore;
            this.query = query;
            this.start = start;
            this.end = end;
        }
    }

    public final AtomicReference<LastCall> lastCall = new AtomicReference<>();
    private PagedCollectionAdapter.OrderedPage<Entity> result;

    public TestOrderedLister(PagedCollectionAdapter.OrderedPage<Entity> result) {
        this.result = result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> initLatest(Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.INIT_LATEST, null, Optional.empty(), query, start, end));
        return result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> initOldest(Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.INIT_OLDEST, null, Optional.empty(), query, start, end));
        return result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> since(String since, Optional<String> before,
                                                             Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.SINCE, since, before, query, start, end));
        return result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> before(String before,
                                                              Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.BEFORE, before, Optional.empty(), query, start, end));
        return result;
    }

    /** Convenience factory for a minimal OrderedPage returning the given list with given cursor values. */
    public static PagedCollectionAdapter.OrderedPage<Entity> orderedPage(
            PagedEntityList<Entity> list, String since, String before) {
        return new PagedCollectionAdapter.OrderedPage<Entity>() {
            @Override public PagedEntityList<Entity> list() { return list; }
            @Override public Optional<String> since() { return Optional.ofNullable(since); }
            @Override public Optional<String> before() { return Optional.ofNullable(before); }
        };
    }
}

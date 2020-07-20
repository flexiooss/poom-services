package org.codingmatters.poom.generic.resource.domain;

public interface PagerProvider<Request, EntityType> {
    PagedCollectionAdapter.Pager<EntityType> pager(Request request) throws Exception;
}

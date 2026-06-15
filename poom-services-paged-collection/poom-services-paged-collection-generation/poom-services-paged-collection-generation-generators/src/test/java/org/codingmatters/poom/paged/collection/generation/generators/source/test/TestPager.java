package org.codingmatters.poom.paged.collection.generation.generators.source.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.generated.api.types.Entity;

public class TestPager implements PagedCollectionAdapter.Pager<Entity> {
    private final String unit;
    private final EntityLister<Entity, PropertyQuery> lister;
    private final int maxPageSize;
    private final int defaultPageSize;
    private final PagedCollectionAdapter.OrderedLister<Entity> orderedLister;

    public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize) {
        this(unit, lister, maxPageSize, maxPageSize, null);
    }

    public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize, int defaultPageSize) {
        this(unit, lister, maxPageSize, defaultPageSize, null);
    }

    public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize,
                     int defaultPageSize, PagedCollectionAdapter.OrderedLister<Entity> orderedLister) {
        this.unit = unit;
        this.lister = lister;
        this.maxPageSize = maxPageSize;
        this.defaultPageSize = defaultPageSize;
        this.orderedLister = orderedLister;
    }

    @Override public String unit() { return this.unit; }
    @Override public int maxPageSize() { return this.maxPageSize; }
    @Override public int defaultPageSize() { return this.defaultPageSize; }
    @Override public EntityLister<Entity, PropertyQuery> lister() { return this.lister; }

    @Override
    public PagedCollectionAdapter.OrderedLister<Entity> orderedLister() {
        return this.orderedLister;
    }
}

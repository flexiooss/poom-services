package org.codingmatters.poom.paged.collection.generation.generators.source.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.generated.api.types.Entity;

public class TestPager implements PagedCollectionAdapter.Pager<Entity> {
    private final String unit;
    private final EntityLister<Entity, PropertyQuery> lister;
    private final int maxPageSize;

    public TestPager() {
        this(null, null, -1);
    }
    public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize) {
        this.unit = unit;
        this.lister = lister;
        this.maxPageSize = maxPageSize;
    }

    @Override
    public String unit() {
        return this.unit;
    }

    @Override
    public int maxPageSize() {
        return this.maxPageSize;
    }

    @Override
    public EntityLister<Entity, PropertyQuery> lister() {
        return this.lister;
    }
}

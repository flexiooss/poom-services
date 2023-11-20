package org.codingmatters.poom.generic.resource.domain.impl;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;

public class DefaultPager<EntityType> implements PagedCollectionAdapter.Pager<EntityType> {

    private final String unit;
    private final int maxPageSize;
    private final int defaultPageSize;
    private final EntityLister<EntityType, PropertyQuery> lister;

    public DefaultPager(String unit, int maxPageSize, EntityLister<EntityType, PropertyQuery> lister) {
        this(unit, maxPageSize, maxPageSize, lister);
    }
    public DefaultPager(String unit, int maxPageSize, int defaultPageSize, EntityLister<EntityType, PropertyQuery> lister) {
        this.unit = unit;
        this.maxPageSize = maxPageSize;
        this.defaultPageSize = defaultPageSize;
        this.lister = lister;
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
    public EntityLister<EntityType, PropertyQuery> lister() {
        return this.lister;
    }
}

package org.codingmatters.poom.generic.resource.handlers.tests;

import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.value.objects.values.ObjectValue;

public class TestPager implements GenericResourceAdapter.Pager<ObjectValue> {
    private final String unit;
    private final EntityLister<ObjectValue, PropertyQuery> lister;
    private final int maxPageSize;

    public TestPager() {
        this(null, null, -1);
    }
    public TestPager(String unit, EntityLister<ObjectValue, PropertyQuery> lister, int maxPageSize) {
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
    public EntityLister<ObjectValue, PropertyQuery> lister() {
        return this.lister;
    }
}

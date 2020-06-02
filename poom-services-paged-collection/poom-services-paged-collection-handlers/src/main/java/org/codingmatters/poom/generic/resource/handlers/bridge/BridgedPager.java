package org.codingmatters.poom.generic.resource.handlers.bridge;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

public class BridgedPager<EntityTpe> implements PagedCollectionAdapter.Pager<ObjectValue> {
    private final PagedCollectionAdapter.Pager<EntityTpe> delegate;
    private final Function fromEntityType;
    private EntityLister<ObjectValue, PropertyQuery> lister;

    public  BridgedPager(PagedCollectionAdapter.Pager<EntityTpe> delegate, Function fromEntityType) {
        this.delegate = delegate;
        this.fromEntityType = fromEntityType;
        this.lister = new BridgedLister(this.delegate.lister(), this.fromEntityType);
    }

    @Override
    public String unit() {
        return this.delegate.unit();
    }

    @Override
    public int maxPageSize() {
        return this.delegate.maxPageSize();
    }

    @Override
    public EntityLister<ObjectValue, PropertyQuery> lister() {
        return this.lister;
    }
}

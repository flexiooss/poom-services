package org.codingmatters.poom.generic.resource.domain.impl;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;

public class DefaultAdapterBuilder<EntityType, CreationType, ReplaceType, UpdateType> {
    private PagedCollectionAdapter.CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud;
    private PagedCollectionAdapter.Pager<EntityType> pager;

    public DefaultAdapterBuilder<EntityType, CreationType, ReplaceType, UpdateType> crud(PagedCollectionAdapter.CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud) {
        this.crud = crud;
        return this;
    }

    public DefaultAdapterBuilder<EntityType, CreationType, ReplaceType, UpdateType> pager(PagedCollectionAdapter.Pager<EntityType> pager) {
        this.pager = pager;
        return this;
    }

    public DefaultAdapterBuilder<EntityType, CreationType, ReplaceType, UpdateType> lister(
            String unit,
            int maxPageSize,
            EntityLister<EntityType, PropertyQuery> lister) {
        this.pager = new DefaultPager<>(unit, maxPageSize, lister);
        return this;
    }

    public PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> build() {
        return new DefaultAdapter<>(this.crud, this.pager);
    }
}

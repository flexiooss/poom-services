package org.codingmatters.poom.generic.resource.domain.impl;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;

public class DefaultAdapter<EntityType, CreationType, ReplaceType, UpdateType> implements PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> {

    private final CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud;
    private final Pager<EntityType> lister;

    public DefaultAdapter(CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud, Pager<EntityType> lister) {
        this.crud = crud;
        this.lister = lister;
    }

    @Override
    public CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud() {
        return this.crud;
    }

    @Override
    public Pager<EntityType> pager() {
        return this.lister;
    }
}

package org.codingmatters.poom.paged.collection.generation.generators.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.generated.api.types.Create;
import org.generated.api.types.Entity;
import org.generated.api.types.Replace;
import org.generated.api.types.Update;

public class TestAdapter implements PagedCollectionAdapter<Entity, Create, Replace, Update> {
    private CRUD<Entity, Create, Replace, Update> crud;
    private Pager<Entity> pager;

    public TestAdapter() {
        this(null, null);
    }
    public TestAdapter(CRUD<Entity, Create, Replace, Update> crud) {
        this(crud, null);
    }
    public TestAdapter(Pager<Entity> pager) {
        this(null, pager);
    }
    public TestAdapter(CRUD<Entity, Create, Replace, Update> crud, Pager<Entity> pager) {
        this.crud = crud;
        this.pager = pager;
    }

    @Override
    public CRUD<Entity, Create, Replace, Update> crud() {
        return this.crud;
    }

    @Override
    public Pager<Entity> pager() {
        return this.pager;
    }
}

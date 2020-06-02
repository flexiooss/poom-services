package org.codingmatters.poom.generic.resource.handlers.tests;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.value.objects.values.ObjectValue;

public class TestAdapter implements PagedCollectionAdapter {

    private final TestPager pager;
    private final TestCRUD crud;

    public TestAdapter() {
        this(null, null);
    }

    public TestAdapter(TestPager pager) {
        this(pager, null);
    }

    public TestAdapter(TestCRUD crud) {
        this(null, crud);
    }
    public TestAdapter(TestPager pager, TestCRUD crud) {
        this.pager = pager;
        this.crud = crud;
    }

    @Override
    public PagedCollectionAdapter.CRUD<ObjectValue,ObjectValue, ObjectValue, ObjectValue> crud() {
        return this.crud;
    }

    @Override
    public PagedCollectionAdapter.Pager<ObjectValue> pager() {
        return this.pager;
    }
}

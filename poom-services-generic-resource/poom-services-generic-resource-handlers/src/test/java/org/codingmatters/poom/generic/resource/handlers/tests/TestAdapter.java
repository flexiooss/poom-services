package org.codingmatters.poom.generic.resource.handlers.tests;

import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.value.objects.values.ObjectValue;

public class TestAdapter implements GenericResourceAdapter {

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
    public GenericResourceAdapter.CRUD<ObjectValue,ObjectValue, ObjectValue, ObjectValue> crud() {
        return this.crud;
    }

    @Override
    public GenericResourceAdapter.Pager<ObjectValue> pager() {
        return this.pager;
    }
}

package org.codingmatters.poom.generic.resource.handlers.bridge;

import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

public class BridgedAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> implements GenericResourceAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(BridgedAdapter.class);

    @FunctionalInterface
    interface Provider<EntityTpe, CreationType, ReplaceType, UpdateType> {
        GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> adapter() throws Exception;
    }

    private final GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> deleguate;
    private final CRUD<ObjectValue, ObjectValue, ObjectValue, ObjectValue> crud;
    private Pager<ObjectValue> pager;

    public BridgedAdapter(
            GenericResourceAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> deleguate,
            Function<EntityTpe, ObjectValue> fromEntityType,
            Function<ObjectValue, CreationType> toCreationType,
            Function<ObjectValue, ReplaceType> toReplaceType,
            Function<ObjectValue, UpdateType> toUpdateType
    ) {
        this.deleguate = deleguate;
        this.crud = new BridgedCRUD(this.deleguate.crud(), fromEntityType, toCreationType, toReplaceType, toUpdateType);
        this.pager = new BridgedPager(this.deleguate.pager(), fromEntityType);
    }

    @Override
    public CRUD<ObjectValue, ObjectValue, ObjectValue, ObjectValue> crud() {
        return this.crud;
    }

    @Override
    public Pager<ObjectValue> pager() {
        return this.pager;
    }
}

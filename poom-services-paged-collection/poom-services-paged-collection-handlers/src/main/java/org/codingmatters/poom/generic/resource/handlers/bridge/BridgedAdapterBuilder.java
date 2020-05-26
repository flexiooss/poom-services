package org.codingmatters.poom.generic.resource.handlers.bridge;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.value.objects.values.ObjectValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class BridgedAdapterBuilder {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(BridgedAdapterBuilder.class);

    private Class entityType = null;
    private Class creationType = null;
    private Class replaceType = null;
    private Class updateType = null;

    public BridgedAdapterBuilder() {
    }

    public BridgedAdapterBuilder entityType(Class entityType) {
        this.entityType = entityType;
        this.assertIsAValueObjectClass(entityType);
        return this;
    }

    public BridgedAdapterBuilder creationType(Class creationType) {
        this.creationType = creationType;
        this.assertIsAValueObjectClass(creationType);
        return this;
    }

    public BridgedAdapterBuilder replaceType(Class replaceType) {
        this.replaceType = replaceType;
        this.assertIsAValueObjectClass(replaceType);
        return this;
    }

    public BridgedAdapterBuilder updateType(Class updateType) {
        this.updateType = updateType;
        this.assertIsAValueObjectClass(updateType);
        return this;
    }

    public PagedCollectionAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> build(PagedCollectionAdapter adapter) {
        Converter converter = new Converter(this.entityType, this.creationType, this.replaceType, this.updateType);

        return new BridgedAdapter<>(
                adapter,
                converter::fromEntity,
                converter::toCreationType,
                converter::toReplaceType,
                converter::toUpdateType
        );
    }


    private void assertIsAValueObjectClass(Class c) {
        try {
            c.getMethod("toMap");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("class " + c.getName() + " is not a value object class (doesn't implements toMap)");
        }
        Method fromMap = null;
        try {
            fromMap = c.getMethod("fromMap", Map.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("class " + c.getName() + " is not a value object class (doesn't implements fromMap)");
        }
        Method build = null;
        try {
            build = fromMap.getReturnType().getMethod("build");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("class " + c.getName() + " is not a value object class (Builder doesn't implements build)");
        }
        if(! build.getReturnType().equals(c)) {
            throw new AssertionError("class " + c.getName() + " is not a value object class (Builder build doesn't return a value of " + c.getName() + ")");
        }
    }

    private class Converter {
        private final Class entityType;
        private final Class creationType;
        private final Class replaceType;
        private final Class updateType;

        public Converter(Class entityType, Class creationType, Class replaceType, Class updateType) {
            this.entityType = entityType;
            this.creationType = creationType;
            this.replaceType = replaceType;
            this.updateType = updateType;
        }

        private <EntityTpe> ObjectValue fromEntity(EntityTpe entity) {
            if(entity == null || this.entityType == null) return null;
            return ObjectValue.fromMap(this.invokeToMap(this.entityType, entity)).build();
        }

        private <CreationType> CreationType toCreationType(ObjectValue value) {
            if(value == null || this.creationType == null) return null;
            return this.invokeFromMapBuilder(this.creationType, value.toMap());
        }

        private <ReplaceType> ReplaceType toReplaceType(ObjectValue value) {
            if(value == null || this.replaceType == null) return null;
            return this.invokeFromMapBuilder(this.replaceType, value.toMap());
        }

        private <UpdateType> UpdateType toUpdateType(ObjectValue value) {
            if(value == null || this.updateType == null) return null;
            return this.invokeFromMapBuilder(this.updateType, value.toMap());
        }

        private Map invokeToMap(Class c, Object o) {
            try {
                return (Map) c.getMethod("toMap").invoke(o);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("[GRAVE] for bridged adater, class " + c + " doesn't implement method toMap", e);
                return null;
            }
        }

        private <T> T invokeFromMapBuilder(Class c, Map map) {
            Object builder;
            try {
                builder = c.getMethod("fromMap", Map.class).invoke(null, map);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("[GRAVE] for bridged adater, class " + c + " doesn't implement method fromMap(Map), is this a value object class ?", e);
                return null;
            }
            Object result;
            try {
                result = builder.getClass().getMethod("build").invoke(builder);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("[GRAVE] for bridged adater, class " + builder.getClass() + " doesn't implement method build(), is this a value object builder ?", e);
                return null;
            }
            try {
                return (T) result;
            } catch (ClassCastException e) {
                log.error("[GRAVE] for bridged adater, builded value " + result + " is not a " + c, e);
                return null;
            }
        }
    }
}

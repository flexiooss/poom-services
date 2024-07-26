package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class PropertyResolver {

    private final Class valueObjectClass;

    public PropertyResolver(Class valueObjectClass) {
        this.valueObjectClass = valueObjectClass;
    }

    public boolean hasProperty(String property) {
        if (this.isNestedProperty(property)) {
            return this.hasNestedProperty(this.head(property), this.tail(property));
        }
        try {
            if (this.valueObjectClass == ObjectValue.class) {
                return true;
            }
            return methodForProperty(property) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public String firstPart(String property) {
        return property.substring(0, property.indexOf('.'));
    }

    private boolean hasNestedProperty(String property, String subpath) {
        try {
            Method method = methodForProperty(property);
            return new PropertyResolver(method.getReturnType()).hasProperty(subpath);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private Method methodForProperty(String property) throws NoSuchMethodException {
        if (this.valueObjectClass == ObjectValue.class) {
            return this.valueObjectClass.getMethod("property", String.class);
        } else {
            return this.valueObjectClass.getMethod(property);
        }
    }

    static class TypedValue {

        private final Object value;
        private final Class<?> type;

        TypedValue(Object value, Class<?> type) {
            this.value = value;
            this.type = type;
        }

        public Object value() {
            return value;
        }

        public Class<?> type() {
            return type;
        }
    }

    private TypedValue invokePropertyMethod(String property, Method method, Object on) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (this.valueObjectClass == ObjectValue.class) {
            PropertyValue propertyValue = (PropertyValue) method.invoke(on, property);
            if (propertyValue == null) {
                return new TypedValue(null, Object.class);
            } else if (propertyValue.cardinality().equals(PropertyValue.Cardinality.SINGLE)) {
                Object value = propertyValue.single().rawValue();
                Class<?> aClass = value instanceof ObjectValue ? ObjectValue.class : value.getClass();
                return new TypedValue(value, aClass);
            } else {
                return new TypedValue(Arrays.stream(propertyValue.multiple()).map(PropertyValue.Value::rawValue).toList(), List.class);
            }
        } else {
            Object value;
            try {
                value = method.invoke(on);
            } catch (NullPointerException e) {
                throw e;
            }
            if (value != null && value.getClass().isEnum()) {
                return new TypedValue(value.getClass().getMethod("name").invoke(value), String.class);
            } else {
                return new TypedValue(value, method.getReturnType());
            }
        }
    }

    public TypedValue resolve(Object o, String property) {
        if (o == null) return new TypedValue(null, Object.class);

        if (this.isNestedProperty(property)) {
            return this.resolveNestedProperty(o, this.head(property), this.tail(property));
        }
        try {
            return this.invokePropertyMethod(property, this.methodForProperty(property), o);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return new TypedValue(null, Object.class);
        }
    }

    private TypedValue resolveNestedProperty(Object o, String property, String subpath) {
        if (o == null) return null;
        try {
            Method method = methodForProperty(property);
            TypedValue sub = this.invokePropertyMethod(property, method, o);
            return new PropertyResolver(sub.type).resolve(sub.value, subpath);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private boolean isNestedProperty(String property) {
        return property.indexOf('.') != -1;
    }

    private String head(String property) {
        return property.substring(0, property.indexOf('.'));
    }

    private String tail(String property) {
        return property.substring(property.indexOf('.') + 1);
    }
}

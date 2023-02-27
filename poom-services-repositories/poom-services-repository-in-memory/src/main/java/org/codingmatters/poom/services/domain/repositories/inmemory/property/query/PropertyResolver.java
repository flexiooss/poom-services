package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PropertyResolver {
    private final Class valueObjectClass;

    public PropertyResolver(Class valueObjectClass) {
        this.valueObjectClass = valueObjectClass;
    }

    public boolean hasProperty(String property) {
        if(this.isNestedProperty(property)) {
            return this.hasNestedProperty(this.head(property), this.tail(property));
        }
        try {
            if(this.valueObjectClass == ObjectValue.class) {
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
        if(this.valueObjectClass == ObjectValue.class) {
            return this.valueObjectClass.getMethod("property", String.class);
        } else {
            return this.valueObjectClass.getMethod(property);
        }
    }

    private Object invokePropertyMethod(String property, Method method, Object on) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if(this.valueObjectClass == ObjectValue.class) {
            PropertyValue propertyValue = (PropertyValue) method.invoke(on, property);
            if(propertyValue == null) {
                return null;
            } else if(propertyValue.cardinality().equals(PropertyValue.Cardinality.SINGLE)) {
                return propertyValue.single().rawValue();
            } else {
                return propertyValue.rawValue();
            }
        } else {
            Object value;
            try {
                value = method.invoke(on);
            } catch (NullPointerException e) {
                throw e;
            }
            if(value != null && value.getClass().isEnum()) {
                return value.getClass().getMethod("name").invoke(value);
            } else {
                return value;
            }
        }
    }

    public Object resolve(Object o, String property) {
        if(o == null) return null;

        if(this.isNestedProperty(property)) {
            return this.resolveNestedProperty(o, this.head(property), this.tail(property));
        }
        try {
            return this.invokePropertyMethod(property, this.methodForProperty(property), o);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    private Object resolveNestedProperty(Object o, String property, String subpath) {
        if(o == null) return null;
        try {
            Method method = methodForProperty(property);
            Object sub = this.invokePropertyMethod(property, method, o);
            return new PropertyResolver(method.getReturnType()).resolve(sub, subpath);
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

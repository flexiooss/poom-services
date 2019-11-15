package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class PropertyResolver {
    private final Class valueObjectCalss;

    public PropertyResolver(Class valueObjectCalss) {
        this.valueObjectCalss = valueObjectCalss;
    }

    public boolean hasProperty(String property) {
        if(this.isNestedProperty(property)) {
            return this.hasNestedProperty(this.head(property), this.tail(property));
        }
        try {
            return this.valueObjectCalss.getMethod(property) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public String firstPart(String property) {
        return property.substring(0, property.indexOf('.'));
    }

    private boolean hasNestedProperty(String property, String subpath) {
        try {
            Method method = this.valueObjectCalss.getMethod(property);
            return new PropertyResolver(method.getReturnType()).hasProperty(subpath);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public Object resolve(Object o, String property) {
        if(this.isNestedProperty(property)) {
            return this.resolveNestedProperty(o, this.head(property), this.tail(property));
        }
        try {
            return this.valueObjectCalss.getMethod(property).invoke(o);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    private Object resolveNestedProperty(Object o, String property, String subpath) {
        try {
            Method method = this.valueObjectCalss.getMethod(property);
            Object sub = method.invoke(o);
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

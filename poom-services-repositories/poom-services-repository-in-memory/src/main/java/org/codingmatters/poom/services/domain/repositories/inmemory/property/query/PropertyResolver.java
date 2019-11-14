package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import java.lang.reflect.InvocationTargetException;

public class PropertyResolver {
    private final Class valueObjectCalss;

    public PropertyResolver(Class valueObjectCalss) {
        this.valueObjectCalss = valueObjectCalss;
    }

    public boolean hasProperty(String property) {
        try {
            return this.valueObjectCalss.getMethod(property) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public Object resolve(Object o, String property) {
        try {
            return this.valueObjectCalss.getMethod(property).invoke(o);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}

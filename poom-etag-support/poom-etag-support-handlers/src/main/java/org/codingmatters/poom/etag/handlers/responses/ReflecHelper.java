package org.codingmatters.poom.etag.handlers.responses;

import org.codingmatters.poom.etag.handlers.exception.UnETaggable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ReflecHelper {
    public static <T> Class<? extends T> lookupValueObject(Class<?> aClass) throws UnETaggable {
        try {
            for (Class<?> anInterface : aClass.getInterfaces()) {
                if (anInterface.getDeclaredMethod("toMap") != null) {
                    return (Class<? extends T>) anInterface;
                }
            }
            return null;
        } catch (NoSuchMethodException e) {
            throw new UnETaggable("class " + aClass + " is not an ETaggable", e);
        }
    }

    public static <T> Map<String, Object> asMap(T response, Class clazz) throws UnETaggable {
        try {
            Method toMap = clazz.getMethod("toMap");
            return (Map<String, Object>) toMap.invoke(clazz.cast(response));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UnETaggable("class " + response.getClass() + " is not an ETaggable : " + response, e);
        }
    }
}

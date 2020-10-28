package org.codingmatters.poom.etag.handlers;

import org.codingmatters.poom.etag.handlers.exception.UnETaggable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ETaggedResponse<T> {

    public static <T> ETaggedResponse<T> from(T response) throws UnETaggable {
        Class<? extends T> responseValueObject = lookupValueObject(response.getClass());
        return new ETaggedResponse<>(responseValueObject, asMap(response, responseValueObject));
    }

    public static <T> ETaggedResponse<T> create304(Class<? extends T> responseType, String xEntityId, String eTag, String cacheControl) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> status304 = new HashMap<>();
        status304.put("xEntityId", xEntityId);
        status304.put("eTag", eTag);
        status304.put("cacheControl", cacheControl);

        responseMap.put("status304", status304);

        return new ETaggedResponse<>(responseType, responseMap);
    }

    public static <T> ETaggedResponse<T> create412(Class responseType, String errorToken) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> status412 = new HashMap<>();
        status412.put("errorToken", errorToken);
        responseMap.put("status412", status412);

        return new ETaggedResponse<>(responseType, responseMap);
    }

    public static <T> ETaggedResponse<T> create500(Class responseType, String errorToken) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> status500 = new HashMap<>();
        status500.put("errorToken", errorToken);
        responseMap.put("status500", status500);

        return new ETaggedResponse<>(responseType, responseMap);
    }



    private ETaggedResponse(Class<? extends T> responseClass, Map<String, Object> responseAsMap) {
        this.responseClass = responseClass;
        this.responseAsMap = responseAsMap;
    }

    private final Class<? extends T> responseClass;
    private final Map<String, Object> responseAsMap;



    public String xEntityId() {
        return (String) this.status().get("xEntityId");
    }

    public String eTag() {
        return (String) this.status().get("eTag");
    }

    public String cacheControl() {
        return (String) this.status().get("cacheControl");
    }


    public ETaggedResponse<T> xEntityId(String changed) {
        this.status().put("xEntityId", changed);
        return this;
    }

    public ETaggedResponse<T> eTag(String changed) {
        this.status().put("eTag", changed);
        return this;
    }

    public ETaggedResponse<T> cacheControl(String changed) {
        this.status().put("cacheControl", changed);
        return this;
    }

    private Map<Object, Object> status() {
        Optional<String> status = this.responseAsMap.keySet().stream().filter(key -> key.startsWith("status") && this.responseAsMap.get(key) != null).findFirst();
        if(status.isPresent()) {
            return (Map<Object, Object>) this.responseAsMap.get(status.get());
        } else {
            return null;
        }
    }



    private static <T> Class<? extends T> lookupValueObject(Class<?> aClass) throws UnETaggable {
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

    private static <T> Map<String, Object> asMap(T response, Class clazz) throws UnETaggable {
        try {
            Method toMap = clazz.getMethod("toMap");
            return (Map<String, Object>) toMap.invoke(clazz.cast(response));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UnETaggable("class " + response.getClass() + " is not an ETaggable : " + response, e);
        }
    }

    public T response() throws UnETaggable {
        try {
            Method builderMethod = this.responseClass.getMethod("fromMap", Map.class);
            Object builder = builderMethod.invoke(null, this.responseAsMap);
            Method buildMethod = builder.getClass().getMethod("build");

            return (T) buildMethod.invoke(builder);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new UnETaggable("failed building etagged response", e);
        }
    }
}

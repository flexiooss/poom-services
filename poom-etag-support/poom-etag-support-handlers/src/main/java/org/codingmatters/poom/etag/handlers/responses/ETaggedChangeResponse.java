package org.codingmatters.poom.etag.handlers.responses;

import org.codingmatters.poom.etag.handlers.exception.UnETaggable;

import java.util.HashMap;
import java.util.Map;

public class ETaggedChangeResponse<T> extends ETaggedResponse<T> {

    public static <T> ETaggedChangeResponse<T> from(T response) throws UnETaggable {
        Class<? extends T> responseValueObject = ReflecHelper.lookupValueObject(response.getClass());
        return new ETaggedChangeResponse<>(responseValueObject, ReflecHelper.asMap(response, responseValueObject));
    }

    public static <T> ETaggedChangeResponse<T> create412(Class responseType, String errorToken) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> status412 = new HashMap<>();
        status412.put("errorToken", errorToken);
        responseMap.put("status412", status412);

        return new ETaggedChangeResponse<>(responseType, responseMap);
    }

    public static <T> ETaggedChangeResponse<T> create500(Class responseType, String errorToken) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> status500 = new HashMap<>();
        status500.put("errorToken", errorToken);
        responseMap.put("status500", status500);

        return new ETaggedChangeResponse<>(responseType, responseMap);
    }


    private ETaggedChangeResponse(Class<? extends T> responseClass, Map<String, Object> responseAsMap) {
        super(responseClass, responseAsMap);
    }

    public ETaggedChangeResponse<T> xEntityId(String changed) {
        this.status().put("xEntityId", changed);
        return this;
    }

    public ETaggedChangeResponse<T> eTag(String changed) {
        this.status().put("eTag", changed);
        return this;
    }

    public ETaggedChangeResponse<T> cacheControl(String changed) {
        this.status().put("cacheControl", changed);
        return this;
    }
}

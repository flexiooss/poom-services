package org.codingmatters.poom.etag.api;

public interface ETaggedRequest {
    String ifMatch();
    String ifNoneMatch();
}

package org.codingmatters.poom.etag.handlers.exception;

public class UnETaggable extends Exception {
    public UnETaggable(String message) {
        super(message);
    }

    public UnETaggable(String message, Throwable cause) {
        super(message, cause);
    }
}

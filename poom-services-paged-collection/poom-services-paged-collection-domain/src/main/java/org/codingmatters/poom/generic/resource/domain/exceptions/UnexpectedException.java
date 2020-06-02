package org.codingmatters.poom.generic.resource.domain.exceptions;

import org.codingmatters.poom.api.paged.collection.api.types.Error;

public class UnexpectedException extends Exception {
    private final Error error;

    public UnexpectedException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public UnexpectedException(Error error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public Error error() {
        return error;
    }
}

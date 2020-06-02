package org.codingmatters.poom.generic.resource.domain.exceptions;

import org.codingmatters.poom.api.paged.collection.api.types.Error;

public class NotFoundException extends Exception {
    private final Error error;

    public NotFoundException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public NotFoundException(Error error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public Error error() {
        return error;
    }
}

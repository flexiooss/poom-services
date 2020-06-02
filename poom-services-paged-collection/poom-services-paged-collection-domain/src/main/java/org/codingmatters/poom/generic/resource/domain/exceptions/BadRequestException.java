package org.codingmatters.poom.generic.resource.domain.exceptions;


import org.codingmatters.poom.api.paged.collection.api.types.Error;

public class BadRequestException extends Exception {
    private final Error error;

    public BadRequestException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public BadRequestException(Error error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public Error error() {
        return error;
    }
}

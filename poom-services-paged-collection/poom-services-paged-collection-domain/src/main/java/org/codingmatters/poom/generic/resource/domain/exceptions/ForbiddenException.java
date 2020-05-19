package org.codingmatters.poom.generic.resource.domain.exceptions;

import org.codingmatters.poom.api.generic.resource.api.types.Error;

public class ForbiddenException extends Exception {
    private final Error error;

    public ForbiddenException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public ForbiddenException(Error error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public Error error() {
        return error;
    }
}

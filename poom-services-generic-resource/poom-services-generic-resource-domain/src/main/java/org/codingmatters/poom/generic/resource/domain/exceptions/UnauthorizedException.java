package org.codingmatters.poom.generic.resource.domain.exceptions;

import org.codingmatters.poom.api.generic.resource.api.types.Error;

public class UnauthorizedException extends Exception {
    private final Error error;

    public UnauthorizedException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public UnauthorizedException(Error error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public Error error() {
        return error;
    }
}

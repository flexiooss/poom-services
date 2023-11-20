package org.codingmatters.poom.generic.resource.domain.exceptions;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class UnauthorizedException extends Exception {
    private final Error error;

    public UnauthorizedException(CategorizedLogger log, String message) {
        this(Error.builder().code(Error.Code.UNAUTHORIZED).token(log.tokenized().error(message)).build(), message);
    }
    public UnauthorizedException(CategorizedLogger log, String message, Throwable cause) {
        this(Error.builder().code(Error.Code.UNAUTHORIZED).token(log.tokenized().error(message)).build(), message, cause);
    }

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

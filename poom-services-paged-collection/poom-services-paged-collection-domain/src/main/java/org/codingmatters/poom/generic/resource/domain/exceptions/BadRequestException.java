package org.codingmatters.poom.generic.resource.domain.exceptions;


import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class BadRequestException extends Exception {
    private final Error error;

    public BadRequestException(CategorizedLogger log, String message) {
        this(Error.builder().code(Error.Code.BAD_REQUEST).token(log.tokenized().error(message)).build(), message);
    }

    public BadRequestException(CategorizedLogger log, String message, Throwable cause) {
        this(Error.builder().code(Error.Code.BAD_REQUEST).token(log.tokenized().error(message, cause)).build(), message, cause);
    }

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

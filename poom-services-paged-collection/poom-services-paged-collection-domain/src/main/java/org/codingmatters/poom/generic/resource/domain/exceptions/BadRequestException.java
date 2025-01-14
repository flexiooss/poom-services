package org.codingmatters.poom.generic.resource.domain.exceptions;


import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class BadRequestException extends Exception {
    private final Error error;

    public BadRequestException(CategorizedLogger log, String message, String description) {
        this(Error.builder().code(Error.Code.BAD_REQUEST).token(log.tokenized().error(message)).description(description).build(), message);
    }

    public BadRequestException(CategorizedLogger log, String message, String description, Throwable cause) {
        this(Error.builder().code(Error.Code.BAD_REQUEST).token(log.tokenized().error(message, cause)).description(description).build(), message, cause);
    }

    public BadRequestException(CategorizedLogger log, String message) {
        this(log, message, (String) null);
    }

    public BadRequestException(CategorizedLogger log, String message, Throwable cause) {
        this(log, message, null, cause);
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

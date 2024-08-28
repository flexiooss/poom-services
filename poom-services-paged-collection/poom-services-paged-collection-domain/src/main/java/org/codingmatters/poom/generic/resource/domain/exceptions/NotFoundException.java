package org.codingmatters.poom.generic.resource.domain.exceptions;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class NotFoundException extends Exception {
    private final Error error;

    public NotFoundException(CategorizedLogger log, String message) {
        this(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).token(log.tokenized().error(message)).description(message).build(), message);
    }

    public NotFoundException(CategorizedLogger log, String message, Throwable cause) {
        this(Error.builder().code(Error.Code.RESOURCE_NOT_FOUND).token(log.tokenized().error(message, cause)).description(message).build(), message, cause);
    }

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

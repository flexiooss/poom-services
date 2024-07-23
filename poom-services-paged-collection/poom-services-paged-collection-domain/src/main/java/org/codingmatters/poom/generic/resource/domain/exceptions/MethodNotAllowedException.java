package org.codingmatters.poom.generic.resource.domain.exceptions;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class MethodNotAllowedException extends Exception {
    private final Error error;

    public MethodNotAllowedException(CategorizedLogger log, Error.Code code, String message) {
        this(Error.builder().code(code).token(log.tokenized().error(message)).build(), message);
    }

    public MethodNotAllowedException(CategorizedLogger log, Error.Code code, String message, Throwable cause) {
        this(Error.builder().code(code).token(log.tokenized().error(message, cause)).build(), message, cause);
    }

    public MethodNotAllowedException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public MethodNotAllowedException(Error error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public Error error() {
        return error;
    }
}

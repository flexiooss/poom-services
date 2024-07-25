package org.codingmatters.poom.services.domain.exceptions;

public class AlreadyExistsException extends RepositoryException {
    public AlreadyExistsException(String s) {
        super(s);
    }

    public AlreadyExistsException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

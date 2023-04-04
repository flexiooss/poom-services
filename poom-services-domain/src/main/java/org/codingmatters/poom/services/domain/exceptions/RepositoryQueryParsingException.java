package org.codingmatters.poom.services.domain.exceptions;

public class RepositoryQueryParsingException extends RepositoryException {
    public RepositoryQueryParsingException(String s) {
        super(s);
    }

    public RepositoryQueryParsingException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

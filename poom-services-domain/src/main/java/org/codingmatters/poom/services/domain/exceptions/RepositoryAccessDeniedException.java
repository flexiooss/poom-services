package org.codingmatters.poom.services.domain.exceptions;

public class RepositoryAccessDeniedException extends RepositoryException {

    public RepositoryAccessDeniedException(String s) {
        super(s);
    }

    public RepositoryAccessDeniedException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

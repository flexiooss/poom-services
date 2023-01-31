package org.codingmatters.poom.services.domain.exceptions;

public class OptimisticLockingException extends RepositoryException {

    public OptimisticLockingException(String s) {
        super(s);
    }

    public OptimisticLockingException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

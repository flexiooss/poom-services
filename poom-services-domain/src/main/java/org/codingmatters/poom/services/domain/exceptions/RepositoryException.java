package org.codingmatters.poom.services.domain.exceptions;

/**
 * Created by nelt on 6/5/17.
 */
public class RepositoryException extends Exception {
    public RepositoryException(String s) {
        super(s);
    }

    public RepositoryException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

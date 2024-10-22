package org.codingmatters.poom.fast.failing.exceptions;

public class InsufficientPermissionFFException extends RecoverableFFException {
    public InsufficientPermissionFFException(String message) {
        super(message);
    }

    public InsufficientPermissionFFException(String message, Throwable cause) {
        super(message, cause);
    }
}

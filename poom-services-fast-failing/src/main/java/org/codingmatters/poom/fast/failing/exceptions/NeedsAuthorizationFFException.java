package org.codingmatters.poom.fast.failing.exceptions;

public class NeedsAuthorizationFFException extends RecoverableFFException {
    public NeedsAuthorizationFFException(String message) {
        super(message);
    }

    public NeedsAuthorizationFFException(String message, Throwable cause) {
        super(message, cause);
    }
}

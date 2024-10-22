package org.codingmatters.poom.fast.failing.exceptions;

public abstract class RecoverableFFException extends FailFastException {
    public RecoverableFFException(String message) {
        super(message);
    }

    public RecoverableFFException(String message, Throwable cause) {
        super(message, cause);
    }
}

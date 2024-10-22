package org.codingmatters.poom.fast.failing.exceptions;

public abstract class FailFastException extends RuntimeException {
    public FailFastException(String message) {
        super(message);
    }

    public FailFastException(String message, Throwable cause) {
        super(message, cause);
    }
}

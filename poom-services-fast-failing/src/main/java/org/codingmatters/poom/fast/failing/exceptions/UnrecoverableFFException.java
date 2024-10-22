package org.codingmatters.poom.fast.failing.exceptions;

public class UnrecoverableFFException extends FailFastException {
    public UnrecoverableFFException(String message) {
        super(message);
    }

    public UnrecoverableFFException(String message, Throwable cause) {
        super(message, cause);
    }
}

package org.codingmatters.poom.api.registry.exception;

public class ApiRegistryException extends Exception {
    public ApiRegistryException(String message) {
        super(message);
    }

    public ApiRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}

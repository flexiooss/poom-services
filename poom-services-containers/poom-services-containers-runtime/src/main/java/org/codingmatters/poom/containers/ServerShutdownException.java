package org.codingmatters.poom.containers;

public class ServerShutdownException extends Exception {
    public ServerShutdownException(String message) {
        super(message);
    }

    public ServerShutdownException(String message, Throwable cause) {
        super(message, cause);
    }
}

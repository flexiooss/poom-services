package org.codingmatters.poom.services.domain.property.query.events;

public class SortEventException extends Exception {
    public SortEventException(Throwable throwable) {
        super(throwable);
    }

    public SortEventException(String message) {
        super(message);
    }

    public SortEventException(String message, Throwable cause) {
        super(message, cause);
    }
}

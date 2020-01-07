package org.codingmatters.poom.services.domain.property.query.events;

public class SortEventError extends Error {
    public SortEventError(String s) {
        super(s);
    }

    public SortEventError(String s, Throwable throwable) {
        super(s, throwable);
    }
}

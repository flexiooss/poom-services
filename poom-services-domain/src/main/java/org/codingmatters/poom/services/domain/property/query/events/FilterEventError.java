package org.codingmatters.poom.services.domain.property.query.events;

public class FilterEventError extends Error {
    public FilterEventError(String s) {
        super(s);
    }

    public FilterEventError(String s, Throwable throwable) {
        super(s, throwable);
    }
}

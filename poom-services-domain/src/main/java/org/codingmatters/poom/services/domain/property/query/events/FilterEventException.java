package org.codingmatters.poom.services.domain.property.query.events;

public class FilterEventException extends Exception {
    public FilterEventException(FilterEventError error) {
        super(error.getMessage(), error);
    }
}

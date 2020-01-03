package org.codingmatters.poom.services.domain.property.query;

import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;
import org.codingmatters.poom.services.domain.property.query.events.SortEventError;

public interface SortEvents<T> {
    enum Direction {ASC, DESC}

    default T sorted(String property, Direction direction) throws SortEventError {throw new SortEventError("feature not implemented");}

    static <T> SortEvents<T> noop() {
        return new SortEvents<T>() {
            @Override
            public T sorted(String property, Direction direction) throws SortEventError {
                return null;
            }
        };
    }
}

package org.codingmatters.poom.caches.invalidation;

import java.util.Optional;

public class Invalidation<V> {

    static public <V> Invalidation<V> valid() {
        return new Invalidation<>(false, Optional.empty());
    }

    static public <V> Invalidation<V> invalid() {
        return new Invalidation<>(true, Optional.empty());
    }

    static public <V> Invalidation<V> replaced(V value) {
        return new Invalidation<>(true, Optional.ofNullable(value));
    }

    private final boolean invalid;
    private final Optional<V> newValue;

    private Invalidation(boolean invalid, Optional<V> newValue) {
        this.invalid = invalid;
        this.newValue = newValue;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public Optional<V> newValue() {
        return newValue;
    }
}

package org.codingmatters.poom.services.domain.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Created by nelt on 6/20/17.
 */
public class ImmutableEntity<V> implements Entity<V> {

    static public <V> ImmutableEntity<V> from(Entity<V> entity) {
        return entity != null ? new ImmutableEntity<>(entity.id(), entity.version(), entity.value()) : null;
    }

    private final String id;
    private final BigInteger version;
    private final V value;

    public ImmutableEntity(String id, BigInteger version, V value) {
        this.id = id;
        this.version = version;
        this.value = value;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public synchronized BigInteger version() {
        return this.version;
    }

    @Override
    public synchronized V value() {
        return this.value;
    }

    @Override
    public String toString() {
        return "ImmutableEntity{" +
                "id='" + id + '\'' +
                ", version=" + version +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutableEntity<?> that = (ImmutableEntity<?>) o;
        return Objects.equals(this.id(), that.id()) &&
                Objects.equals(this.version(), that.version()) &&
                Objects.equals(this.value(), that.value());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id(), this.version(), this.value());
    }
}

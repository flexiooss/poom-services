package org.codingmatters.poom.services.domain.entities;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Created by nelt on 6/5/17.
 */
public class MutableEntity<V> implements Entity<V> {
    private final String id;
    private BigInteger version;
    private V value;

    public MutableEntity(String id, BigInteger version, V value) {
        this.id = id;
        this.version = version;
        this.value = value;
    }

    public MutableEntity(String id, V value) {
        this.id = id;
        this.version = BigInteger.ONE;
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

    public synchronized void changeValue(V newValue) {
        this.version = this.version.add(BigInteger.ONE);
        this.value = newValue;
    }

    @Override
    public String toString() {
        return "MutableEntity{" +
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

package org.codingmatters.poom.servives.domain.entities;

import java.math.BigInteger;

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
}

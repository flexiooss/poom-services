package org.codingmatters.poom.servives.domain.entities;

import java.math.BigInteger;

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
}

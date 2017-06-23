package org.codingmatters.poom.services.domain.change;

/**
 * Created by nelt on 6/23/17.
 */
public abstract class Change<T> {

    private final T currentValue;
    private final T newValue;
    private final Validation validation;

    public Change(T currentValue, T newValue) {
        this.currentValue = currentValue;
        this.newValue = newValue;
        this.validation = this.validate();
    }

    protected abstract Validation validate();
    public abstract T applied();

    public Validation validation() {
        return this.validation;
    }

    public T currentValue() {
        return currentValue;
    }

    public T newValue() {
        return newValue;
    }
}

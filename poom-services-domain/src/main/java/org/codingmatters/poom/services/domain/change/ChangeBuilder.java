package org.codingmatters.poom.services.domain.change;

/**
 * Created by nelt on 6/23/17.
 */
public class ChangeBuilder<V, C extends Change<V>> {

    private final V currentValue;
    private final ChangeCreator<V, C> creator;

    public ChangeBuilder(V currentValue, ChangeCreator<V, C> creator) {
        this.currentValue = currentValue;
        this.creator = creator;
    }

    public C to(V newValue) {
        return this.creator.create(this.currentValue, newValue);
    }

    @FunctionalInterface
    public interface ChangeCreator<V, C> {
        C create(V currentValue, V newValue);
    }
}

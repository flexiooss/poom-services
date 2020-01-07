package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.codingmatters.poom.services.domain.property.query.StackedSortEvents;
import org.codingmatters.poom.services.domain.property.query.events.SortEventError;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Comparator;
import java.util.Objects;

public class ReflectSortEvents extends StackedSortEvents<Comparator<Entity>> {
    private final PropertyResolver propertyResolver;

    public ReflectSortEvents(Class valueObjectCalss) {
        super((entity, t1) -> 0);
        this.propertyResolver = new PropertyResolver(valueObjectCalss);
    }

    @Override
    public Void sorted(String property, Direction direction) throws SortEventError {
        this.push((e1, e2) -> direction.equals(Direction.ASC) ? this.compare(e1, e2, property) : this.compare(e2, e1, property));
        return null;
    }

    private int compare(Entity e1, Entity e2, String property) {
        Object v1 = this.propertyResolver.resolve(e1.value(), property);
        Object v2 = this.propertyResolver.resolve(e2.value(), property);

        if(v1 instanceof Comparable && v2 instanceof Comparable) {
            return Objects.compare((Comparable) v1, (Comparable) v2, Comparator.naturalOrder());
        } else {
            return 0;
        }
    }
}

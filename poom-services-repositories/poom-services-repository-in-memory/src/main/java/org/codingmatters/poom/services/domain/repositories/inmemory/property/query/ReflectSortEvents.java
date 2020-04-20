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
        Comparator<Entity> comparator = (e1, e2) -> direction.equals(Direction.ASC) ? this.compare(e1, e2, property) : this.compare(e2, e1, property);

        if(this.isEmpty()) {
            this.push(comparator);
        } else {
            Comparator<Entity> previous = this.pop();
            this.push((e1, e2) -> compareThisAndThen(e1, e2, previous, comparator));
        }
        return null;
    }

    private int compare(Entity e1, Entity e2, String property) {
        Object v1 = this.propertyResolver.resolve(e1.value(), property);
        Object v2 = this.propertyResolver.resolve(e2.value(), property);

        if(v1 == null) {
            if(v2 == null) {
                return 0;
            } else {
                return 1;
            }
        }

        if(v2 == null) {
            return -1;
        }

        if(v1 instanceof Comparable && v2 instanceof Comparable) {
            return Objects.compare((Comparable) v1, (Comparable) v2, Comparator.naturalOrder());
        } else {
            return 0;
        }
    }

    private int compareThisAndThen(Entity e1, Entity e2, Comparator<Entity> c1, Comparator<Entity> c2) {
        int result1 = c1.compare(e1, e2);
        if(result1 == 0) {
            return c2.compare(e1, e2);
        } else {
            return result1;
        }
    }
}

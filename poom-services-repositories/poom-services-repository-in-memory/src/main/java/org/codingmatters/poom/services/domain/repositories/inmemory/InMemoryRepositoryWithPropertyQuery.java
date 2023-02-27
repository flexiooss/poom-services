package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.PropertyQueryParser;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventException;
import org.codingmatters.poom.services.domain.property.query.events.SortEventException;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.property.query.PropertyResolver;
import org.codingmatters.poom.services.domain.repositories.inmemory.property.query.ReflectFilterEvents;
import org.codingmatters.poom.services.domain.repositories.inmemory.property.query.ReflectSortEvents;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Comparator;
import java.util.function.Predicate;

public class InMemoryRepositoryWithPropertyQuery<V> extends InMemoryRepository<V, PropertyQuery> {

    static public <V> Repository<V, PropertyQuery> validating(Class<? extends V> valueClass) {
        return validating(valueClass, false);
    }
    static public <V> Repository<V, PropertyQuery> validating(Class<? extends V> valueClass, boolean withOptimisticLocking) {
        return new InMemoryRepositoryWithPropertyQuery<>(valueClass, true, withOptimisticLocking);
    }

    static public <V> Repository<V, PropertyQuery> notValidating(Class<? extends V> valueClass) {
        return notValidating(valueClass, false);
    }
    static public <V> Repository<V, PropertyQuery> notValidating(Class<? extends V> valueClass, boolean withOptimisticLocking) {
        return new InMemoryRepositoryWithPropertyQuery<>(valueClass, false, withOptimisticLocking);
    }

    private final Class<? extends V> valueClass;
    private final PropertyQueryParser.Builder parserBuilder;

    @Deprecated(forRemoval = true)
    public InMemoryRepositoryWithPropertyQuery(Class<? extends V> valueClass) {
        this(valueClass, true, false);
    }

    private InMemoryRepositoryWithPropertyQuery(Class<? extends V> valueClass, boolean validating, boolean withOptimisticLocking) {
        super(withOptimisticLocking);
        this.valueClass = valueClass;
        PropertyResolver resolver = new PropertyResolver(valueClass);
        if(validating) {
            this.parserBuilder = PropertyQueryParser
                    .builder()
                    .leftHandSidePropertyValidator(resolver::hasProperty)
                    .rightHandSidePropertyValidator(resolver::hasProperty)
            ;
        } else {
            this.parserBuilder = PropertyQueryParser
                    .builder()
            ;
        }
    }

    @Override
    public PagedEntityList<V> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        Predicate<V> queryPredicate = this.queryPredicate(query);
        Comparator<Entity<V>> queryComparator = this.queryComparator(query);
        return this.paged(this.stream().filter(vEntity -> queryPredicate.test(vEntity.value())).sorted(queryComparator), startIndex, endIndex);
    }

    @Override
    public void deleteFrom(PropertyQuery query) throws RepositoryException {
        Predicate<V> queryPredicate = this.queryPredicate(query);
        Entity<V>[] toDelete = this.stream().filter(vEntity -> queryPredicate.test(vEntity.value())).toArray(i -> new Entity[i]);
        for (Entity<V> entity : toDelete) {
            this.delete(entity);
        }
    }

    public Predicate<V> queryPredicate(PropertyQuery query) throws RepositoryException {
        ReflectFilterEvents<V> events = new ReflectFilterEvents<>(this.valueClass);
        try {
            this.parserBuilder.build(events).parse(query);
        } catch (InvalidPropertyException | FilterEventException | SortEventException e) {
            throw new RepositoryException("unparseable query : " + query, e);
        }
        return events.result();
    }


    private Comparator<Entity<V>> queryComparator(PropertyQuery query) throws RepositoryException {
        ReflectSortEvents<V> events = new ReflectSortEvents<>(this.valueClass);
        try {
            this.parserBuilder.build(events).parse(query);
        } catch (InvalidPropertyException | FilterEventException | SortEventException e) {
            throw new RepositoryException("unparseable query : " + query, e);
        }
        return events.result();
    }
}

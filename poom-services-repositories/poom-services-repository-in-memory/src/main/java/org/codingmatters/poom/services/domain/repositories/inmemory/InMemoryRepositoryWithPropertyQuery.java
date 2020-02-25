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
        return new InMemoryRepositoryWithPropertyQuery<>(valueClass, true);
    }

    static public <V> Repository<V, PropertyQuery> notValidating(Class<? extends V> valueClass) {
        return new InMemoryRepositoryWithPropertyQuery<>(valueClass, false);
    }

    private final Class<? extends V> valueClass;
    private PropertyQueryParser.Builder parserBuilder;

    @Deprecated(forRemoval = true)
    public InMemoryRepositoryWithPropertyQuery(Class<? extends V> valueClass) {
        this(valueClass, true);
    }

    private InMemoryRepositoryWithPropertyQuery(Class<? extends V> valueClass, boolean validating) {
        this.valueClass = valueClass;
        PropertyResolver resolver = new PropertyResolver(valueClass);
        this.parserBuilder = PropertyQueryParser
                .builder()
                .leftHandSidePropertyValidator(s -> resolver.hasProperty(s))
                .rightHandSidePropertyValidator(s -> resolver.hasProperty(s))
        ;
        if(validating) {
            this.parserBuilder = PropertyQueryParser
                    .builder()
                    .leftHandSidePropertyValidator(s -> resolver.hasProperty(s))
                    .rightHandSidePropertyValidator(s -> resolver.hasProperty(s))
            ;
        } else {
            this.parserBuilder = PropertyQueryParser
                    .builder()
            ;
        }
    }

    @Override
    public PagedEntityList<V> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        Predicate queryPredicate = this.queryPredicate(query);
        Comparator queryComparator = this.queryComparator(query);
        return this.paged(this.stream().filter(vEntity -> queryPredicate.test(vEntity.value())).sorted(queryComparator), startIndex, endIndex);
    }

    @Override
    public void deleteFrom(PropertyQuery query) throws RepositoryException {
        Predicate queryPredicate = this.queryPredicate(query);
        Entity[] toDelete = this.stream().filter(vEntity -> queryPredicate.test(vEntity.value())).toArray(i -> new Entity[i]);
        for (Entity entity : toDelete) {
            this.delete(entity);
        }
    }

    public Predicate queryPredicate(PropertyQuery query) throws RepositoryException {
        ReflectFilterEvents events = new ReflectFilterEvents(this.valueClass);
        try {
            this.parserBuilder.build(events).parse(query);
        } catch (InvalidPropertyException | FilterEventException | SortEventException e) {
            throw new RepositoryException("unparseable query : " + query, e);
        }
        return events.result();
    }


    private Comparator<Entity> queryComparator(PropertyQuery query) throws RepositoryException {
        ReflectSortEvents events = new ReflectSortEvents(this.valueClass);
        try {
            this.parserBuilder.build(events).parse(query);
        } catch (InvalidPropertyException | FilterEventException | SortEventException e) {
            throw new RepositoryException("unparseable query : " + query, e);
        }
        return events.result();
    }
}

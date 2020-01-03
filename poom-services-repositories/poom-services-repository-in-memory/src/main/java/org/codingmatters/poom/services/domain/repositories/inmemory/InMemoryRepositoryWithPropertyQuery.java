package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.PropertyQueryParser;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventException;
import org.codingmatters.poom.services.domain.property.query.events.SortEventException;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;
import org.codingmatters.poom.services.domain.repositories.inmemory.property.query.PropertyResolver;
import org.codingmatters.poom.services.domain.repositories.inmemory.property.query.ReflectFilterEvents;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.function.Predicate;

public class InMemoryRepositoryWithPropertyQuery<V> extends InMemoryRepository<V, PropertyQuery> {

    private final Class<? extends V> valueClass;
    private PropertyQueryParser.Builder parserBuilder;

    public InMemoryRepositoryWithPropertyQuery(Class<? extends V> valueClass) {
        this.valueClass = valueClass;
        PropertyResolver resolver = new PropertyResolver(valueClass);
        this.parserBuilder = PropertyQueryParser
                .builder()
                .leftHandSidePropertyValidator(s -> resolver.hasProperty(s))
                .rightHandSidePropertyValidator(s -> resolver.hasProperty(s))
                ;
    }

    @Override
    public PagedEntityList<V> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        Predicate queryPredicate = this.queryPredicate(query);
        return this.paged(this.stream().filter(vEntity -> queryPredicate.test(vEntity.value())), startIndex, endIndex);
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
}

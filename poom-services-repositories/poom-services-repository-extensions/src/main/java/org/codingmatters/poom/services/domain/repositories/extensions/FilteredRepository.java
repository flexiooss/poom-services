package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.PropertyQueryParser;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventException;
import org.codingmatters.poom.services.domain.property.query.events.SortEventException;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.property.query.PropertyResolver;
import org.codingmatters.poom.services.domain.repositories.inmemory.property.query.ReflectFilterEvents;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class FilteredRepository<V> implements Repository<V, PropertyQuery> {
    private final Class<? extends V> valueClass;
    private final Repository<V, PropertyQuery> delegate;
    private final String filter;
    private final Function<V, V> entityInitializer;
    private final PropertyQueryParser.Builder parserBuilder;

    public FilteredRepository(Class<? extends V> valueClass, Repository<V, PropertyQuery> delegate, String filter, Function<V, V> entityInitializer) {
        this.valueClass = valueClass;
        this.delegate = delegate;
        this.filter = filter;
        this.entityInitializer = entityInitializer;
        PropertyResolver resolver = new PropertyResolver(valueClass);
        this.parserBuilder = PropertyQueryParser
                .builder()
                .leftHandSidePropertyValidator(resolver::hasProperty)
                .rightHandSidePropertyValidator(resolver::hasProperty)
        ;
    }

    @Override
    public PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException {
        return this.search(PropertyQuery.builder().build(), startIndex, endIndex);
    }

    @Override
    public PagedEntityList<V> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        return this.delegate.search(mergeQuery(query), startIndex, endIndex);
    }

    @Override
    public Entity<V> create(V withValue) throws RepositoryException {
        return this.delegate.create(this.entityInitializer.apply(withValue));
    }

    @Override
    public Entity<V> createWithId(String id, V withValue) throws RepositoryException {
        return this.delegate.createWithId(id, this.entityInitializer.apply(withValue));
    }

    @Override
    public Entity<V> createWithIdAndVersion(String id, BigInteger version, V withValue) throws RepositoryException {
        return this.delegate.createWithIdAndVersion(id, version, this.entityInitializer.apply(withValue));
    }

    @Override
    public Entity<V> retrieve(String id) throws RepositoryException {
        Entity<V> retrieved = this.delegate.retrieve(id);
        if (retrieved == null || !this.queryPredicate(PropertyQuery.builder().filter(this.filter).build()).test(retrieved.value())) {
            return null;
        }
        return retrieved;
    }

    @Override
    public Entity<V> update(Entity<V> entity, V withValue) throws RepositoryException {
        Entity<V> retrieved = this.delegate.retrieve(entity.id());
        if (retrieved != null && !this.queryPredicate(PropertyQuery.builder().filter(this.filter).build()).test(retrieved.value())) {
            throw new RepositoryException("retrieved entity does not match filter");
        }
        return this.delegate.update(entity, this.entityInitializer.apply(withValue));
    }

    @Override
    public void delete(Entity<V> entity) throws RepositoryException {
        Entity<V> retrievedEntity = this.delegate.retrieve(entity.id());
        if (retrievedEntity != null && !this.queryPredicate(PropertyQuery.builder().filter(this.filter).build()).test(retrievedEntity.value())) {
            throw new RepositoryException("retrieved entity does not match filter");
        }
        this.delegate.delete(entity);
    }

    @Override
    public void deleteFrom(PropertyQuery query) throws RepositoryException {
        this.delegate.deleteFrom(mergeQuery(query));
    }

    private PropertyQuery mergeQuery(PropertyQuery query) {
        return PropertyQueryFilterMerger.mergeFilters(this.filter, query);
    }

    public Predicate<V> queryPredicate(PropertyQuery query) throws RepositoryException {
        ReflectFilterEvents<V> events = new ReflectFilterEvents<V>(this.valueClass);
        try {
            this.parserBuilder.build(events).parse(query);
        } catch (InvalidPropertyException | FilterEventException | SortEventException e) {
            throw new RepositoryException("unparseable query : " + query, e);
        }
        return events.result();
    }
}

package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.LinkedList;
import java.util.List;

public class EnrichedLister<U,V> implements EntityLister<V, PropertyQuery> {
    @FunctionalInterface
    public interface Enricher<U, V> {
        Entity<V> enrich(Entity<U> entity) throws EnrichingException;

        class EnrichingException extends Exception {
            public EnrichingException(String message) {
                super(message);
            }

            public EnrichingException(String message, Throwable cause) {
                super(message, cause);
            }
        }
    }

    @FunctionalInterface
    public interface ValueEnricher<U, V> extends Enricher<U, V> {

        V enrichedValue(U value) throws EnrichingException;

        default Entity<V> enrich(Entity<U> entity) throws Enricher.EnrichingException {
            return new ImmutableEntity<>(entity.id(), entity.version(), this.enrichedValue(entity.value()));
        }

    }

    private final EntityLister<U, PropertyQuery> deleguate;
    private final Enricher<U, V> enricher;

    public EnrichedLister(EntityLister<U, PropertyQuery> deleguate, Enricher<U, V> enricher) {
        this.deleguate = deleguate;
        this.enricher = enricher;
    }

    @Override
    public PagedEntityList<V> all(long start, long end) throws RepositoryException {
        return this.enriched(this.deleguate.all(start, end));
    }

    @Override
    public PagedEntityList<V> search(PropertyQuery query, long start, long end) throws RepositoryException {
        return this.enriched(this.deleguate.search(query, start, end));
    }

    private PagedEntityList<V> enriched(PagedEntityList<U> entities) throws RepositoryException {
        List<Entity<V>> vEntities = new LinkedList<>();
        for (Entity<U> uEntity : entities) {
            try {
                vEntities.add(this.enricher.enrich(uEntity));
            } catch (Enricher.EnrichingException e) {
                throw new RepositoryException("failed enriching entity " + uEntity, e);
            }
        }
        return new PagedEntityList.DefaultPagedEntityList<>(entities.startIndex(), entities.endIndex(), entities.total(), vEntities);
    }
}
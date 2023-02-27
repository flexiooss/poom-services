package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

public class FilteredEntityLister <V> implements EntityLister<V, PropertyQuery> {
    @FunctionalInterface
    public interface FilterSupplier {
        String filter() throws RepositoryException;
    }

    private final EntityLister<V, PropertyQuery> deleguate;
    private final FilterSupplier filterSupplier;

    public FilteredEntityLister(EntityLister<V, PropertyQuery> deleguate, FilterSupplier filterSupplier) {
        this.deleguate = deleguate;
        this.filterSupplier = filterSupplier;
    }

    @Override
    public PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException {
        if(this.filterSupplier == null) return this.deleguate.all(startIndex, endIndex);
        String filter = this.filterSupplier.filter();
        if(filter == null || filter.trim().isEmpty()) return this.deleguate.all(startIndex, endIndex);

        return this.deleguate.search(PropertyQuery.builder().filter(filter).build(), startIndex, endIndex);
    }

    @Override
    public PagedEntityList<V> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        if(this.filterSupplier == null) return this.deleguate.search(query, startIndex, endIndex);
        String filter = this.filterSupplier.filter();
        if(filter == null || filter.trim().isEmpty()) return this.deleguate.search(query, startIndex, endIndex);

        if(query == null) {
            query = PropertyQuery.builder().build();
        }
        if(query.opt().filter().isPresent() && ! query.filter().trim().isEmpty()) {
            query = query.withFilter(String.format("(%s) && (%s)", filter, query.filter()));
        } else {
            query = query.withFilter(filter);
        }
        return this.deleguate.search(query, startIndex, endIndex);
    }
}
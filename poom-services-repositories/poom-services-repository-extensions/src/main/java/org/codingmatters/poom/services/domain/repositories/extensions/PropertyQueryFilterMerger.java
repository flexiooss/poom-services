package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;

public class PropertyQueryFilterMerger {

    public static PropertyQuery mergeFilters(String filter, PropertyQuery query) {
        if (query == null) {
            query = PropertyQuery.builder().build();
        }
        String mergedFilter = null;
        if (filter != null && !filter.trim().isEmpty()) {
            if (query.opt().filter().isPresent() && !query.filter().trim().isEmpty()) {
                mergedFilter = String.format("(%s) && (%s)", filter, query.filter());
            } else {
                mergedFilter = filter;
            }
        } else {
            if (query.opt().filter().isPresent() && !query.filter().trim().isEmpty()) {
                mergedFilter = query.filter();
            }
        }
        return query.withFilter(mergedFilter);
    }
}

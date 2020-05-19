package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

public class CustomerRentalPager implements PagedCollectionAdapter.Pager<Rental>, EntityLister<Rental, PropertyQuery> {
    private final Repository<Rental, PropertyQuery> repository;
    private final String customer;

    public CustomerRentalPager(Repository<Rental, PropertyQuery> repository, String customer) {
        this.repository = repository;
        this.customer = customer;
    }

    @Override
    public String unit() {
        return "Rental";
    }

    @Override
    public int maxPageSize() {
        return 1000;
    }

    @Override
    public EntityLister<Rental, PropertyQuery> lister() {
        return this;
    }

    @Override
    public PagedEntityList<Rental> all(long startIndex, long endIndex) throws RepositoryException {
        return this.repository.search(PropertyQuery.builder().filter(this.customerFilter()).build(), startIndex, endIndex);
    }

    @Override
    public PagedEntityList<Rental> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        return this.repository.search(this.withCustomerFilter(query), startIndex, endIndex);
    }

    private PropertyQuery withCustomerFilter(PropertyQuery query) {
        if(query.opt().filter().orElse("").isEmpty()) {
            return query.withFilter(this.customerFilter());
        } else {
            return query.withFilter(this.customerFilter() + " && (" + query.filter() + ")");
        }
    }

    private String customerFilter() {
        return String.format("customer == '%s'", this.customer);
    }
}

package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

public class RentalPager implements GenericResourceAdapter.Pager<Rental>, EntityLister<Rental, PropertyQuery> {
    private final Repository<Rental, PropertyQuery> repository;
    private final Movie movie;

    public RentalPager(Repository<Rental, PropertyQuery> repository, Movie movie) {
        this.repository = repository;
        this.movie = movie;
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
        return this.repository.search(PropertyQuery.builder().filter(this.movieFilter()).build(), startIndex, endIndex);
    }

    @Override
    public PagedEntityList<Rental> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        return this.repository.search(this.withMovieFilter(query), startIndex, endIndex);
    }

    private PropertyQuery withMovieFilter(PropertyQuery query) {
        if(query.opt().filter().orElse("").isEmpty()) {
            return query.withFilter(this.movieFilter());
        } else {
            return query.withFilter(this.movieFilter() + " && (" + query.filter() + ")");
        }
    }

    private String movieFilter() {
        return String.format("movie.id == '%s'", this.movie.id());
    }
}

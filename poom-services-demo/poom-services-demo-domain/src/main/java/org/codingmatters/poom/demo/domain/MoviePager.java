package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Optional;

public class MoviePager implements GenericResourceAdapter.Pager<Movie>, EntityLister<Movie, PropertyQuery> {
    private final Repository<Movie, PropertyQuery> repository;
    private final Optional<Movie.Category> category;

    public MoviePager(Repository<Movie, PropertyQuery> repository, Movie.Category category) {
        this.category = Optional.ofNullable(category);
        this.repository = repository;
    }

    @Override
    public String unit() {
        return "Movie";
    }

    @Override
    public int maxPageSize() {
        return 1000;
    }

    @Override
    public EntityLister<Movie, PropertyQuery> lister() {
        return this;
    }

    @Override
    public PagedEntityList<Movie> all(long startIndex, long endIndex) throws RepositoryException {
        if(this.category.isPresent()) {
            return this.repository.search(PropertyQuery.builder().filter(this.categoryFilter()).build(), startIndex, endIndex);
        } else {
            return this.repository.all(startIndex, endIndex);
        }
    }

    private String categoryFilter() {
        return String.format("category == '%s'", this.category.get().name());
    }

    @Override
    public PagedEntityList<Movie> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        if(! this.category.isPresent()) {
            return this.repository.search(query, startIndex, endIndex);
        } else {
            if(query.opt().filter().orElse("").isEmpty()) {
                return this.repository.search(query.withFilter(this.categoryFilter()), startIndex, endIndex);
            } else {
                return this.repository.search(query.withFilter(this.categoryFilter() + " && ( " + query.filter() + " )"), startIndex, endIndex);
            }
        }
    }
}

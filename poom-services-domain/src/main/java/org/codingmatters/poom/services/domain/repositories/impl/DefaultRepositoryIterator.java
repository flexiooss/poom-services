package org.codingmatters.poom.services.domain.repositories.impl;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.RepositoryIterator;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class DefaultRepositoryIterator<V, Q> implements RepositoryIterator<V, Q> {
    static private final Logger log = LoggerFactory.getLogger(DefaultRepositoryIterator.class);

    protected final EntityLister<V, Q> repository;
    private final int pageSize;

    private PagedEntityList<V> currentPage = null;
    private Integer positionInPage = null;

    public DefaultRepositoryIterator(EntityLister<V, Q> repository, int pageSize) {
        this.repository = repository;
        this.pageSize = pageSize;
    }

    @Override
    public boolean hasNext() {
        try {
            this.preparePage();
        } catch (RepositoryException e) {
            log.error("error accessing repository in iterator", e);
            return false;
        }
        if(this.currentPage == null) {
            return false;
        }
        return this.positionInPage < this.currentPage.size();
    }

    @Override
    public Entity<V> next() {
        try {
            this.preparePage();
        } catch (RepositoryException e) {
            log.error("error accessing repository in iterator", e);
            throw new NoSuchElementException("problem accessing repository");
        }
        Entity<V> result = this.currentPage.get(this.positionInPage);
        this.positionInPage++;
        if(result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    private void preparePage() throws RepositoryException {
        if(this.currentPage == null) {
            this.currentPage = this.queryPage(0L, this.pageSize - 1);
            this.positionInPage = 0;
        } else if(this.positionInPage >= this.currentPage.size()){
            this.currentPage = this.queryPage(this.currentPage.endIndex() + 1, this.currentPage.endIndex() + this.pageSize);
            this.positionInPage = 0;
        }
    }

    protected PagedEntityList<V> queryPage(long start, long end) throws RepositoryException {
        return this.repository.all(start, end);
    }

    public static class SearchRepositoryIterator<V, Q> extends DefaultRepositoryIterator<V, Q> {

        private final Q search;

        public SearchRepositoryIterator(EntityLister<V, Q> repository, Q search, int pageSize) {
            super(repository, pageSize);
            this.search = search;
        }

        @Override
        protected PagedEntityList<V> queryPage(long start, long end) throws RepositoryException {
            return this.repository.search(this.search, start, end);
        }
    }
}

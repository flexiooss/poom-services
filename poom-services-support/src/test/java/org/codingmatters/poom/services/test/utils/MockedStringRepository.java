package org.codingmatters.poom.services.test.utils;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

/**
 * Created by nelt on 7/17/17.
 */
public class MockedStringRepository implements Repository<String, String> {

    @Override
    public Entity<String> create(String withValue) throws RepositoryException {
        throw new RepositoryException("failure");
    }

    @Override
    public Entity<String> retrieve(String id) throws RepositoryException {
        throw new RepositoryException("failure");
    }

    @Override
    public Entity<String> update(Entity<String> entity, String withValue) throws RepositoryException {
        throw new RepositoryException("failure");
    }

    @Override
    public void delete(Entity<String> entity) throws RepositoryException {
        throw new RepositoryException("failure");
    }

    @Override
    public PagedEntityList<String> all(long startIndex, long endIndex) throws RepositoryException {
        throw new RepositoryException("failure");
    }

    @Override
    public PagedEntityList<String> search(String query, long startIndex, long endIndex) throws RepositoryException {
        throw new RepositoryException("failure");
    }
}

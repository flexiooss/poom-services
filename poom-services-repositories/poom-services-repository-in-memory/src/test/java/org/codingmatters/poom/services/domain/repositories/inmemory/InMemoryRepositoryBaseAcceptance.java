package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.generated.QAValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.RepositoryBaseAcceptance;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

public class InMemoryRepositoryBaseAcceptance extends RepositoryBaseAcceptance {
    @Override
    protected Repository<QAValue, Void> createRepository() {
        return new InMemoryRepository<>() {
            @Override
            public PagedEntityList<QAValue> search(Void query, long startIndex, long endIndex) throws RepositoryException {
                throw new RepositoryException("not implemented");
            }
        };
    }
}

package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.generated.QAValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.RepositoryOptimisticLockingAcceptanceTest;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

public class InMemoryRepositoryOptimisticLockingAcceptanceTest extends RepositoryOptimisticLockingAcceptanceTest {
    @Override
    protected Repository<QAValue, Void> createRepository() throws Exception {
        return new InMemoryRepository<>(true) {
            @Override
            public PagedEntityList<QAValue> search(Void query, long startIndex, long endIndex) throws RepositoryException {
                throw new RepositoryException("not implemented");
            }
        };
    }
}

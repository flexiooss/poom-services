package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.generated.QAValue;
import org.junit.Before;

public abstract class RepositoryOptimisticLockingAcceptanceTest {

    private Repository<QAValue, Void> repository;
    protected abstract Repository<QAValue, Void> createRepositoryWithOptimisticLocking() throws Exception;

    protected Repository<QAValue, Void> repository() {
        return this.repository;
    }

    @Before
    public void setUp() throws Exception {
        this.repository = this.createRepositoryWithOptimisticLocking();
    }

    
}

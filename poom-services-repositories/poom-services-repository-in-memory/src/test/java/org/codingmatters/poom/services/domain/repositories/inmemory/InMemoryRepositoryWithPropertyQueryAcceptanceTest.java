package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.generated.QAValue;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.PropertyQueryAcceptanceTest;
import org.codingmatters.poom.services.domain.repositories.Repository;

public class InMemoryRepositoryWithPropertyQueryAcceptanceTest extends PropertyQueryAcceptanceTest {
    @Override
    protected Repository<QAValue, PropertyQuery> createRepository() {
        return InMemoryRepositoryWithPropertyQuery.validating(QAValue.class);
    }
}

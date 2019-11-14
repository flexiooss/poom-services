package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.test.Simple;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class InMemoryRepositoryWithPropertyQueryTest {

    private Repository<Simple, PropertyQuery> repository = new InMemoryRepositoryWithPropertyQuery<>(Simple.class);

    @Before
    public void setUp() throws Exception {
        for (long i = 0; i < 1000; i++) {
            this.repository.create(Simple.builder().a(i).build());
        }
    }

    @Test
    public void search() throws Exception {
        assertThat(this.repository.search(PropertyQuery.builder().filter("a >= 500").build(), 0, 1000).total(), is(500L));
    }

    @Test
    public void deleteFrom() throws Exception {
        this.repository.deleteFrom(PropertyQuery.builder().filter("a >= 500").build());

        assertThat(this.repository.all(0, 1000).total(), is(500L));
    }
}
package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.tests.Dummy1;
import org.codingmatters.poom.tests.Dummy2;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class EnrichedListerTest {

    private final Repository<Dummy1, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Dummy1.class);

    private final EnrichedLister<Dummy1, Dummy2> enriched = new EnrichedLister<>(this.repository, entity -> new ImmutableEntity<>(
            entity.id(),
            entity.version(),
            Dummy2.builder().p(entity.value().p() + "-enriched").build()
    ));

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create(Dummy1.builder().p("" + i).build());
        }
    }

    @Test
    public void whenAll__thenEntitiesAreEnriched() throws Exception {
        for (Entity<Dummy2> dummy2Entity : this.enriched.all(0, 100)) {
            assertThat(dummy2Entity.value().p(), is(this.repository.retrieve(dummy2Entity.id()).value().p() + "-enriched"));
        }
    }

    @Test
    public void whenSearch__thenEntitiesAreSearched_andEnriched() throws Exception {
        PagedEntityList<Dummy2> search = this.enriched.search(PropertyQuery.builder().filter("p >= '5'").build(), 0, 100);

        assertThat(search.total(), is(5L));
        for (Entity<Dummy2> dummy2Entity : search) {
            assertThat(dummy2Entity.value().p(), is(this.repository.retrieve(dummy2Entity.id()).value().p() + "-enriched"));
        }
    }
}
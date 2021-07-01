package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.tests.Certificate;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class FilteredEntityListerTest {

    private final Repository<Certificate, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Certificate.class);

    @Before
    public void setUp() throws Exception {
        repository.createWithId("a1", Certificate.builder().fqdn("a").status(Certificate.Status.RENEWING).build());
        repository.createWithId("a2", Certificate.builder().fqdn("a").status(Certificate.Status.READY).build());
        repository.createWithId("a3", Certificate.builder().fqdn("a").status(Certificate.Status.ERRORED).build());
        repository.createWithId("b1", Certificate.builder().fqdn("b").status(Certificate.Status.RENEWING).build());
        repository.createWithId("b2", Certificate.builder().fqdn("b").status(Certificate.Status.READY).build());
        repository.createWithId("b3", Certificate.builder().fqdn("b").status(Certificate.Status.ERRORED).build());
    }

    @Test
    public void givenSimpleFilterPartitionsRepository__whenAll__thenPartitionReturned() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "fqdn == 'a'");

        assertThat(
                lister.all(0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3")
        );
    }

    @Test
    public void givenSimpleFilterPartitionsRepository__whenSearchWithoutFilter__thenPartitionReturned() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "fqdn == 'a'");

        assertThat(
                lister.search(PropertyQuery.builder().build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3")
        );
    }

    @Test
    public void givenSimpleFilterPartitionsRepository__whenSearchWithSimpleFilter__thenFiltersAreMerged() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "fqdn == 'a'");

        assertThat(
                lister.search(PropertyQuery.builder().filter("status == 'ERRORED'").build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a3")
        );
    }

    @Test
    public void givenSimpleFilterPartitionsRepository__whenSearchWithComplexFilter__thenFiltersAreMerged() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "fqdn == 'a'");

        assertThat(
                lister.search(PropertyQuery.builder().filter("status == 'ERRORED' || status == 'READY'").build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a2", "a3")
        );
    }

    @Test
    public void givenComplexFilterPartitionsRepository__whenSearchWithSimpleFilter__thenFiltersAreMerged() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "status == 'ERRORED' || status == 'READY'");

        assertThat(
                lister.search(PropertyQuery.builder().filter("fqdn == 'a'").build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a2", "a3")
        );
    }




    @Test
    public void givenNullFilter__whenAll__thenAll() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> null);

        assertThat(
                lister.all(0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3", "b1", "b2", "b3")
        );
    }



    @Test
    public void givenNullFilter__whenSearchWithoutFilter__thenAll() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> null);

        assertThat(
                lister.search(PropertyQuery.builder().build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3", "b1", "b2", "b3")
        );
    }

    @Test
    public void givenNullFilter__whenSearchWithSimpleFilter__thenFiltered() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> null);

        assertThat(
                lister.search(PropertyQuery.builder().filter("status == 'ERRORED'").build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a3", "b3")
        );
    }

    @Test
    public void givenEmptyFilter__whenAll__thenAll() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "  ");

        assertThat(
                lister.all(0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3", "b1", "b2", "b3")
        );
    }

    @Test
    public void givenEmptyFilter__whenSearchWithoutFilter__thenAll() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "   ");

        assertThat(
                lister.search(PropertyQuery.builder().build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3", "b1", "b2", "b3")
        );
    }

    @Test
    public void givenEmptyFilter__whenSearchWithSimpleFilter__thenFiltered() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, () -> "   ");

        assertThat(
                lister.search(PropertyQuery.builder().filter("status == 'ERRORED'").build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a3", "b3")
        );
    }





    @Test
    public void givenNullFilterProvider__whenAll__thenAll() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, null);

        assertThat(
                lister.all(0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3", "b1", "b2", "b3")
        );
    }

    @Test
    public void givenNullFilterProvider__whenSearchWithoutFilter__thenAll() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, null);

        assertThat(
                lister.search(PropertyQuery.builder().build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a1", "a2", "a3", "b1", "b2", "b3")
        );
    }

    @Test
    public void givenNullFilterProvider__whenSearchWithSimpleFilter__thenFiltered() throws Exception {
        EntityLister<Certificate, PropertyQuery> lister = new FilteredEntityLister<>(this.repository, null);

        assertThat(
                lister.search(PropertyQuery.builder().filter("status == 'ERRORED'").build(), 0, 100).stream().map(e -> e.id()).collect(Collectors.toList()),
                containsInAnyOrder("a3", "b3")
        );
    }
}
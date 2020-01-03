package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.test.Simple;
import org.codingmatters.test.simple.E;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class InMemoryRepositoryWithPropertyQueryTest {

    private Repository<Simple, PropertyQuery> repository = new InMemoryRepositoryWithPropertyQuery<>(Simple.class);

    @Before
    public void setUp() throws Exception {
        for (long i = 0; i < 1000; i++) {
            this.repository.create(Simple.builder().a(i).e(E.builder().f("" + i).build()).build());
        }
    }

    @Test
    public void search() throws Exception {
        assertThat(this.repository.search(PropertyQuery.builder().filter("a >= 500").build(), 0, 1000).total(), is(500L));
    }

    @Test
    public void searchNested() throws Exception {
        assertThat(this.repository.search(PropertyQuery.builder().filter("e.f starts with 50").build(), 0, 1000).total(), is(11L));
    }

    @Test
    public void deleteFrom() throws Exception {
        this.repository.deleteFrom(PropertyQuery.builder().filter("a >= 500").build());

        assertThat(this.repository.all(0, 1000).total(), is(500L));
    }

    @Test
    public void givenNoFilter_sortAsc() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().sort("a asc").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(i));
        }
    }

    @Test
    public void givenNoFilter_sortNoDirection() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().sort("a").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(i));
        }
    }

    @Test
    public void givenNoFilter_sortDesc() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().sort("a desc").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(999 - i));
        }
    }

    @Test
    public void givenFilter_sortAsc() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().filter("a >= 500").sort("a asc").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(500 + i));
        }
    }
}
package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/6/17.
 */
public class InMemoryRepositoryTest {

    private Repository<String, String> repository = new InMemoryRepository<String, String>() {
        @Override
        public PagedEntityList<String> search(String query, int page, int pageSize) throws RepositoryException {
            return null;
        }
    };

    @Test
    public void create() throws Exception {
        Entity<String> entity = this.repository.create("yopyop");

        assertThat(entity, is(notNullValue()));
        assertThat(entity.id(), is(notNullValue()));
        assertThat(entity.version().intValue(), is(1));
        assertThat(entity.value(), is("yopyop"));
    }

    @Test
    public void retrieve() throws Exception {
        String id = this.repository.create("yopyop").id();

        assertThat(this.repository.retrieve(id), is(notNullValue()));
        assertThat(this.repository.retrieve(id).id(), is(id));
        assertThat(this.repository.retrieve(id).version().intValue(), is(1));
        assertThat(this.repository.retrieve(id).value(), is("yopyop"));
    }

    @Test
    public void retrieve_unexistent_returnsNull() throws Exception {
        assertThat(this.repository.retrieve("not-stored"), is(nullValue()));
    }

    @Test
    public void update() throws Exception {
        Entity<String> entity = this.repository.create("yopyop");

        this.repository.update(entity, "yipyip");

        assertThat(entity.version().intValue(), is(2));
        assertThat(entity.value(), is("yipyip"));

        assertThat(this.repository.retrieve(entity.id()).version().intValue(), is(2));
        assertThat(this.repository.retrieve(entity.id()).value(), is("yipyip"));
    }

    @Test
    public void name() throws Exception {
        Entity<String> entity = this.repository.create("yopyop");
        this.repository.delete(entity);

        assertThat(this.repository.retrieve(entity.id()), is(nullValue()));
    }

    @Test
    public void all_empty() throws Exception {
        PagedEntityList<String> list = this.repository.all(0, 100);

        assertThat(list, is(notNullValue()));
        assertThat(list, hasSize(0));
        assertThat(list.page(), is(0));
        assertThat(list.hasNextPage(), is(false));
    }

    @Test
    public void all_oneElement() throws Exception {
        this.repository.create("one");

        PagedEntityList<String> list = this.repository.all(0, 100);

        assertThat(list, is(notNullValue()));
        assertThat(list, hasSize(1));
        assertThat(list.page(), is(0));
        assertThat(list.hasNextPage(), is(false));
        assertThat(list.get(0).value(), is("one"));
    }

    @Test
    public void all_firstPageOfTwo() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create("elt-" + i);
        }

        PagedEntityList<String> list = this.repository.all(0, 5);

        assertThat(list, hasSize(5));
        assertThat(list.hasNextPage(), is(true));
        for (int i = 0; i < list.size(); i++) {
            assertThat(list.get(i).value(), is("elt-" + i));
        }
    }

    @Test
    public void all_secondOfTwo() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create("elt-" + i);
        }

        PagedEntityList<String> list = this.repository.all(1, 5);

        assertThat(list, hasSize(5));
        assertThat(list.hasNextPage(), is(false));
        for (int i = 0; i < list.size(); i++) {
            assertThat(list.get(i).value(), is("elt-" + (5 + i)));
        }
    }

    @Test
    public void all_partialLastPage() throws Exception {
        for (int i = 0; i < 12; i++) {
            this.repository.create("elt-" + i);
        }

        PagedEntityList<String> list = this.repository.all(2, 5);

        assertThat(list, hasSize(2));
        assertThat(list.hasNextPage(), is(false));
        for (int i = 0; i < list.size(); i++) {
            assertThat(list.get(i).value(), is("elt-" + (10 + i)));
        }
    }

    @Test
    public void all_thirdOfTwo() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create("elt-" + i);
        }

        PagedEntityList<String> list = this.repository.all(2, 5);

        assertThat(list, hasSize(0));
        assertThat(list.hasNextPage(), is(false));
    }
}
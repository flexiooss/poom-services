package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class RepositoryIteratorTest {

    private LinkedList<PagedEntityList<String>> nextPages = new LinkedList<>();
    private LinkedList<Request> requests = new LinkedList<>();

    private EntityLister<String, String> lister = new EntityLister<String, String>() {
        @Override
        public PagedEntityList<String> all(long startIndex, long endIndex) throws RepositoryException {
            requests.add(new Request(null, startIndex, endIndex));
            return nextPages.removeFirst();
        }

        @Override
        public PagedEntityList<String> search(String query, long startIndex, long endIndex) throws RepositoryException {
            requests.add(new Request(query, startIndex, endIndex));
            return nextPages.removeFirst();
        }
    };

    @Test
    public void whenSearching__thenSearchIsPassedToTheRepository() throws Exception {
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<>(0L, 0L, 0L, Collections.emptyList()));
        RepositoryIterator iterator = RepositoryIterator.search(this.lister, "searched", 10);
        assertThat(iterator.hasNext(), is(false));

        System.out.println(this.requests);
        assertThat(this.requests, contains(new Request("searched", 0L, 9L)));
    }

    @Test
    public void givenRepositoryEmpty__whenIterating__thenNoElement() throws Exception {
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<>(0L, 0L, 0L, Collections.emptyList()));

        RepositoryIterator iterator = RepositoryIterator.all(this.lister, 10);

        assertThat(iterator.hasNext(), is(false));

        assertThat(this.requests, contains(new Request(null, 0L, 9L)));
    }

    @Test
    public void givenRepositoryWithOnePage__whenIterating__thenAllElementsReturned() throws Exception {
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<String>(0L, 5L, 10L, this.elements(0, 5)));
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<>(0L, 0L, 10L, Collections.emptyList()));

        RepositoryIterator<String, String> iterator = RepositoryIterator.all(this.lister, 10);

        for (int i = 0; i <= 5; i++) {
            iterator.hasNext();
            Entity<String> next = iterator.next();
            assertThat(next.value(), is("element-" + i));
        }
        assertThat(iterator.hasNext(), is(false));

        assertThat(this.requests, contains(
                new Request(null, 0L, 9L),
                new Request(null, 6L, 15L)
        ));
    }

    @Test
    public void givenRepositoryWithTwoPage__whenIterating__thenAllElementsReturned() throws Exception {
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<String>(0L, 9L, 15L, this.elements(0, 9)));
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<String>(10L, 14L, 15L, this.elements(10, 14)));
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<>(0L, 0L, 15L, Collections.emptyList()));

        RepositoryIterator<String, String> iterator = RepositoryIterator.all(this.lister, 10);

        for (int i = 0; i < 15; i++) {
            iterator.hasNext();
            Entity<String> next = iterator.next();
            assertThat(next.value(), is("element-" + i));
        }
        assertThat(iterator.hasNext(), is(false));

        assertThat(this.requests, contains(
                new Request(null, 0L, 9L),
                new Request(null, 10L, 19L),
                new Request(null, 15L, 24L)
        ));
    }

    @Test
    public void givenRepositoryWithTwoPage__whenStreamingIterating__thenAllElementsAreStreamed() throws Exception {
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<String>(0L, 9L, 15L, this.elements(0, 9)));
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<String>(10L, 14L, 15L, this.elements(10, 14)));
        this.nextPages.add(new PagedEntityList.DefaultPagedEntityList<>(0L, 0L, 15L, Collections.emptyList()));

        List<Entity<String>> elements = RepositoryIterator.allStreamed(this.lister, 10).collect(Collectors.toList());
        assertThat(elements, hasSize(15));

        assertThat(this.requests, contains(
                new Request(null, 0L, 9L),
                new Request(null, 10L, 19L),
                new Request(null, 15L, 24L)
        ));
    }

    private List<Entity<String>> elements(int start, int end) {
        List<Entity<String>> result = new LinkedList<>();
        for (int i = 0; i <= end ; i++) {
            result.add(new ImmutableEntity<>("" + i, BigInteger.ONE, "element-" + i));
        }
        return result;
    }


    class Request {
        final String query;
        final Long startIndex;
        final Long endIndex;

        public Request(String query, Long startIndex, Long endIndex) {
            this.query = query;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Request request = (Request) o;
            return Objects.equals(query, request.query) &&
                    Objects.equals(startIndex, request.startIndex) &&
                    Objects.equals(endIndex, request.endIndex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(query, startIndex, endIndex);
        }

        @Override
        public String toString() {
            return "Request{" +
                    "query='" + query + '\'' +
                    ", startIndex=" + startIndex +
                    ", endIndex=" + endIndex +
                    '}';
        }
    }


}
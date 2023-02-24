package org.codingmatters.poom.api;

import org.codingmatters.poom.api.paged.collection.api.ValueList;
import org.codingmatters.poom.services.support.paging.client.Rfc7233Helper;

import java.util.List;
import java.util.stream.Collectors;

public interface Rfc7233PageResponse<E> {
    String contentRange();
    String acceptRange();
    ValueList<E> payload();

    default Page<E> page() throws Rfc7233Helper.UnparseableRfc7233Query {
        return new Page<>(this);
    }

    class Page<E> {
        private final Rfc7233Helper helper;
        private final List<E> elements;

        private Page(Rfc7233PageResponse<E> response) throws Rfc7233Helper.UnparseableRfc7233Query {
            this.helper = new Rfc7233Helper(response.contentRange(), response.acceptRange());
            this.elements = response.payload().stream().collect(Collectors.toList());
        }

        public String nextRange() {
            return helper.nextRange();
        }

        public long first() {
            return helper.first();
        }

        public long last() {
            return helper.last();
        }

        public long total() {
            return helper.total();
        }

        public long pageSize() {
            return helper.pageSize();
        }

        public List<E> elements() {
            return this.elements;
        }

        public boolean hasNext() {
            return this.last() <  this.total();
        }
    }
}

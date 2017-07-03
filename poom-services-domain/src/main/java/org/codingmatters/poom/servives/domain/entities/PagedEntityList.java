package org.codingmatters.poom.servives.domain.entities;


import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nelt on 6/5/17.
 */
public interface PagedEntityList<V> extends List<Entity<V>> {
    int page();
    boolean hasNextPage();
    long totalSize();

    class DefaultPagedEntityList<V> extends LinkedList<Entity<V>> implements PagedEntityList<V> {
        private final int page;
        private final boolean nextPage;
        private final long totalSize;

        public DefaultPagedEntityList(int page, boolean nextPage, long totalSize, Collection<Entity<V>> collection) {
            super(collection);
            this.page = page;
            this.nextPage = nextPage;
            this.totalSize = totalSize;
        }

        @Override
        public int page() {
            return this.page;
        }

        @Override
        public boolean hasNextPage() {
            return this.nextPage;
        }

        @Override
        public long totalSize() {
            return this.totalSize;
        }
    }
}

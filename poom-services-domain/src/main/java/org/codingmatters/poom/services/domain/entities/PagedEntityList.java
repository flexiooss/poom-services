package org.codingmatters.poom.services.domain.entities;


import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by nelt on 6/5/17.
 */
public interface PagedEntityList<V> extends List<Entity<V>> {
    long startIndex();
    long endIndex();
    long total();
    List<V> valueList();
    List<V> valueList(Function<Entity<V>, V> entityMapper);

    class DefaultPagedEntityList<V> extends LinkedList<Entity<V>> implements PagedEntityList<V> {
        private final long startIndex;
        private final long endIndex;
        private final long total;

        public DefaultPagedEntityList(long startIndex, long endIndex, long total, Collection<Entity<V>> collection) {
            super(collection);
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.total = total;
        }

        @Override
        public long startIndex() {
            return this.startIndex;
        }

        @Override
        public long endIndex() {
            return this.endIndex;
        }

        @Override
        public long total() {
            return this.total;
        }

        @Override
        public List<V> valueList() {
            return this.valueList(entity -> entity.value());
        }

        @Override
        public List<V> valueList(Function<Entity<V>, V> entityMapper) {
            return this.stream().map(entityMapper).collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return "DefaultPagedEntityList{" +
                    "startIndex=" + startIndex +
                    ", endIndex=" + endIndex +
                    ", total=" + total +
                    ", elements" + super.toString() +
                    '}';
        }
    }
}

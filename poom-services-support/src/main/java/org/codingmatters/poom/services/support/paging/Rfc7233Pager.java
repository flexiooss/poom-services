package org.codingmatters.poom.services.support.paging;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nelt on 7/5/17.
 */
public class Rfc7233Pager<V,Q> {

    static public UnitBuilder forRequestedRange(String requestedRange) {
        return new UnitBuilder(requestedRange);
    }

    static public class UnitBuilder {

        private final String requestedRange;
        private UnitBuilder(String requestedRange) {
            this.requestedRange = requestedRange;
        }

        public MaxPageSizeBuilder unit(String unit) {
            return new MaxPageSizeBuilder(unit, this.requestedRange);
        }

    }

    static public class MaxPageSizeBuilder {

        private final String unit;
        private final String requestedRange;
        private MaxPageSizeBuilder(String unit, String requestedRange) {
            this.unit = unit;
            this.requestedRange = requestedRange;
        }

        public Builder maxPageSize(int maxPageSize) {
            return new Builder(this.unit, maxPageSize, this.requestedRange);
        }

    }


    static public class Builder {

        private final String unit;
        private final int maxPageSize;
        private final String requestedRange;
        private Builder(String unit, int maxPageSize, String requestedRange) {
            this.unit = unit;
            this.maxPageSize = maxPageSize;
            this.requestedRange = requestedRange;
        }

        public <V, Q> Rfc7233Pager<V,Q> pager(EntityLister<V, Q> repository) {
            return new Rfc7233Pager<>(repository, this.unit, this.maxPageSize, this.requestedRange);
        }


    }
    private final EntityLister<V,Q> repository;
    private final String unit;
    private final int maxPageSize;
    private final String requestedRange;
    private final Range range;

    public Rfc7233Pager(EntityLister<V,Q> repository, String unit, int maxPageSize, String requestedRange) {
        this.repository = repository;
        this.unit = unit;
        this.maxPageSize = maxPageSize;
        this.requestedRange = requestedRange;

        this.range = Range.fromRequestedRange(requestedRange, maxPageSize);

    }

    private long start() {
        return this.range.start();
    }

    private long end() {
        return this.range.end();
    }

    public Page<V> page() throws RepositoryException {
        return this.page(() -> this.repository.all(this.start(), this.end()));
    }

    public Page<V> page(Q query) throws RepositoryException {
        return this.page(() -> this.repository.search(query, this.start(), this.end()));
    }

    public Page<V> page(Optional<Q> query) throws RepositoryException {
        if(query.isPresent()) {
            return page(query.get());
        } else {
            return this.page();
        }
    }

    private Page<V> page(ResultListSupplier<V> requester) throws RepositoryException {
        String acceptRange = String.format("%s %d", this.unit, this.maxPageSize);
        if(this.range.isValid()) {
            PagedEntityList<V> list = requester.get();
            String contentRange = String.format("%s %d-%d/%d",
                    this.unit,
                    list.startIndex(),
                    list.endIndex(),
                    list.total()
            );
            return new Page<>(list, contentRange, acceptRange, this.range.isValid(), this.range.validationMessage(), requestedRange);
        } else {
            PagedEntityList<V> list = this.repository.all(0, 0);
            String contentRange = String.format("%s */%d",
                    this.unit,
                    list.total()
            );
            return new Page<>(list, contentRange, acceptRange, this.range.isValid(), this.range.validationMessage(), requestedRange);
        }
    }


    static public class Page<V> {
        private final PagedEntityList<V> list;
        private final String contentRange;
        private final String acceptRange;
        private final boolean valid;
        private final String validationMessage;
        private final String requestedRange;

        private Page(PagedEntityList<V> list, String contentRange, String acceptRange, boolean valid, String validationMessage, String requestedRange) {
            this.list = list;
            this.contentRange = contentRange;
            this.acceptRange = acceptRange;
            this.valid = valid;
            this.validationMessage = validationMessage;
            this.requestedRange = requestedRange;
        }

        public String contentRange() {
            return this.contentRange;
        }

        public String acceptRange() {
            return this.acceptRange;
        }

        public boolean isPartial() {
            if(this.isRequestOutOfRange()) {
                return false;
            } else {
                return this.list.endIndex() < this.list.total() - 1;
            }
        }

        private boolean isRequestOutOfRange() {
            return this.list.startIndex() == 0 && this.list.endIndex() == 0;
        }

        public PagedEntityList<V> list() {
            return this.list;
        }

        public boolean isValid() {
            return this.valid;
        }

        public String validationMessage() {
            return this.validationMessage;
        }

        public String requestedRange() {
            return this.requestedRange;
        }
    }

    @FunctionalInterface
    private interface ResultListSupplier<V> {
        PagedEntityList<V> get() throws RepositoryException;
    }
}

package org.codingmatters.poom.services.support.paging;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

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

        public <V, Q> Rfc7233Pager<V,Q> pager(Repository<V, Q> repository) {
            return new Rfc7233Pager<>(repository, this.unit, this.maxPageSize, this.requestedRange);
        }
    }

    private final Repository<V,Q> repository;
    private final String unit;
    private final int maxPageSize;
    private final String requestedRange;
    private final String validationMessage;
    private long start;
    private long end;
    private boolean valid;

    public Rfc7233Pager(Repository<V,Q> repository, String unit, int maxPageSize, String range) {
        this.repository = repository;
        this.unit = unit;
        this.maxPageSize = maxPageSize;
        this.requestedRange = range;

        long startIndex = 0;
        long endIndex = startIndex + this.maxPageSize - 1;

        if(range != null) {
            Pattern RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
            Matcher rangeMatcher = RANGE_PATTERN.matcher(range);
            if(rangeMatcher.matches()) {
                startIndex = Long.parseLong(rangeMatcher.group(1));
                endIndex = Long.parseLong(rangeMatcher.group(2));
            }
        }

        if(endIndex - startIndex > this.maxPageSize) {
            endIndex = startIndex + this.maxPageSize - 1;
        }

        this.start = startIndex;
        this.end = endIndex;

        if(this.start > this.end) {
            this.valid = false;
            this.validationMessage = "start must be before end of range";
        } else {
            this.valid = true;
            this.validationMessage = null;
        }
    }

    private long start() {
        return this.start;
    }

    private long end() {
        return this.end;
    }

    public Page<V> page() throws RepositoryException {
        return this.page(() -> this.repository.all(this.start(), this.end()));
    }

    public Page<V> page(Q query) throws RepositoryException {
        return this.page(() -> this.repository.search(query, this.start(), this.end()));
    }

    private Page<V> page(ResultListSupplier<V> requester) throws RepositoryException {
        String acceptRange = String.format("%s %d", this.unit, this.maxPageSize);
        if(this.valid) {
            PagedEntityList<V> list = requester.get();
            String contentRange = String.format("%s %d-%d/%d",
                    this.unit,
                    list.startIndex(),
                    list.endIndex(),
                    list.total()
            );
            return new Page<>(list, contentRange, acceptRange, this.valid, this.validationMessage, requestedRange);
        } else {
            PagedEntityList<V> list = this.repository.all(0, 0);
            String contentRange = String.format("%s */%d",
                    this.unit,
                    list.total()
            );
            return new Page<>(list, contentRange, acceptRange, this.valid, this.validationMessage, requestedRange);
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
            return this.list.size() < this.list.total();
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

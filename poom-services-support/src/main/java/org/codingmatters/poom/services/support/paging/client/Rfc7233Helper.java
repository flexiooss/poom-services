package org.codingmatters.poom.services.support.paging.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rfc7233Helper {

    private final long first;
    private final long last;
    private final long total;
    private final long pageSize;

    public Rfc7233Helper(String contentRange, String acceptRange) throws UnparseableRfc7233Query {
        this(contentRange, acceptRange, 0);
    }

    public Rfc7233Helper(String contentRange, String acceptRange, long maxPageSize) throws UnparseableRfc7233Query {
        // content-range: FlexioEvent 0-0/3014846
        Matcher contentRangeMatcher = Pattern.compile("([a-zA-Z_\\-.0-9]+) (\\d+)-(\\d+)/(\\d+)").matcher(contentRange);
        int contentRangeMatcherGroupOffset = 0;
        if (!contentRangeMatcher.matches()) {
            contentRangeMatcherGroupOffset = 1;
            contentRangeMatcher = Pattern.compile("(\\d+)-(\\d+)/(\\d+)").matcher(contentRange);
            if (!contentRangeMatcher.matches()) {
                throw new Rfc7233Helper.UnparseableRfc7233Query("cannot parse content range from : " + contentRange);
            }
        }

        this.first = Long.parseLong(contentRangeMatcher.group(2 - contentRangeMatcherGroupOffset));
        this.last = Long.parseLong(contentRangeMatcher.group(3 - contentRangeMatcherGroupOffset));
        this.total = Long.parseLong(contentRangeMatcher.group(4 - contentRangeMatcherGroupOffset));

        // accept-range: FlexioEvent 1000
        Matcher acceptRangeMatcher = Pattern.compile("([a-zA-Z_\\-.0-9]+) (\\d+)").matcher(acceptRange);
        int acceptRangeMatcherGroupOffset = 0;
        if (!acceptRangeMatcher.matches()) {
            acceptRangeMatcher = Pattern.compile("(\\d+)").matcher(acceptRange);
            acceptRangeMatcherGroupOffset = 1;
            if (!acceptRangeMatcher.matches()) {
                throw new Rfc7233Helper.UnparseableRfc7233Query("cannot parse accept range from : " + acceptRange);
            }
        }

        long servicePageSize = Long.parseLong(acceptRangeMatcher.group(2 - acceptRangeMatcherGroupOffset));
        this.pageSize = maxPageSize > 0 ? Math.min(maxPageSize, servicePageSize) : servicePageSize;
    }

    public String nextRange() {
        return String.format("%s-%s", this.last + 1, this.last + this.pageSize);
    }

    public long first() {
        return this.first;
    }

    public long last() {
        return this.last;
    }

    public long total() {
        return this.total;
    }

    public long pageSize() {
        return this.pageSize;
    }

    public static class UnparseableRfc7233Query extends Exception {
        public UnparseableRfc7233Query(String s) {
            super(s);
        }

        public UnparseableRfc7233Query(String s, Throwable throwable) {
            super(s, throwable);
        }
    }
}

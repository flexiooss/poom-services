package org.codingmatters.poom.services.support.paging.client;

import org.codingmatters.rest.api.client.ResponseDelegate;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rfc7233ResponseMatcher {

    private final long first;
    private final long last;
    private final long total;
    private final long pageSize;

    public Rfc7233ResponseMatcher(ResponseDelegate response) throws IOException {
        // content-range: FlexioEvent 0-0/3014846
        String contentRange = response.header("content-range")[0];
        Matcher contentRangeMatcher = Pattern.compile("(\\w+) (\\d+)-(\\d+)/(\\d+)").matcher(contentRange);
        if(! contentRangeMatcher.matches()) {
            throw new IOException("cannont parse content range from : " + contentRange);
        }

        this.first = Long.parseLong(contentRangeMatcher.group(2));
        this.last = Long.parseLong(contentRangeMatcher.group(3));
        this.total = Long.parseLong(contentRangeMatcher.group(4));

        // accept-range: FlexioEvent 1000
        String acceptRange = response.header("accept-range")[0];
        Matcher acceptRangeMatcher = Pattern.compile("(\\w+) (\\d+)").matcher(acceptRange);
        if(! acceptRangeMatcher.matches()) {
            throw new IOException("cannont parse content range from : " + acceptRange);
        }

        this.pageSize = Long.parseLong(acceptRangeMatcher.group(2));
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
}

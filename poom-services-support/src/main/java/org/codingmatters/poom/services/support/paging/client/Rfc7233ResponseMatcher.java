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
        Rfc7233Helper helper = null;
        try {
            helper = new Rfc7233Helper(response.header("content-range")[0], response.header("accept-range")[0]);
        } catch (Exception e) {
            throw new IOException("failed parsing range query response", e);
        }
        this.first = helper.first();
        this.last = helper.last();
        this.total = helper.total();
        this.pageSize = helper.pageSize();
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

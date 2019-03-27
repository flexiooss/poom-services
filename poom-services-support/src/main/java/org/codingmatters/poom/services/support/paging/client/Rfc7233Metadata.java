package org.codingmatters.poom.services.support.paging.client;

import org.codingmatters.rest.api.client.RequesterFactory;
import org.codingmatters.rest.api.client.ResponseDelegate;

import java.io.IOException;

public class Rfc7233Metadata {

    private RequesterFactory requesterFactory;
    private final Rfc7233Iterator.RequesterConfig requesterConfig;

    private final long pageSize;
    private final long total;

    public Rfc7233Metadata(RequesterFactory requesterFactory, Rfc7233Iterator.RequesterConfig requesterConfig) throws IOException {
        this.requesterFactory = requesterFactory;
        this.requesterConfig = requesterConfig;

        ResponseDelegate response = this.requesterConfig.config(this.requesterFactory.create()).header("range", "0-0").get();
        Rfc7233ResponseMatcher matcher = new Rfc7233ResponseMatcher(response);

        this.total = matcher.total();
        this.pageSize = matcher.pageSize();
    }

    public long pageSize() {
        return pageSize;
    }

    public long total() {
        return total;
    }

}

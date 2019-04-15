package org.codingmatters.poom.services.support.paging.client;

import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.RequesterFactory;
import org.codingmatters.rest.api.client.ResponseDelegate;

import java.io.IOException;
import java.util.*;

public class Rfc7233Iterator<E> implements Iterator<E> {

    private final RequesterFactory requesterFactory;
    private final RequesterConfig requesterConfig;
    private final BodyReader<E> bodyReader;
    private List<E> page = new LinkedList<>();

    private final Optional<Long> startAt;
    private final Optional<Long> maxSize;

    private int pageIndex;
    private long iteratedCount = 0;

    private Rfc7233ResponseMatcher lastMatcher;

    public Rfc7233Iterator(RequesterFactory requesterFactory, BodyReader<E> bodyReader, RequesterConfig requesterConfig) throws IOException {
        this(requesterFactory, bodyReader, requesterConfig, Optional.empty(), Optional.empty());
    }

    public Rfc7233Iterator(RequesterFactory requesterFactory, BodyReader<E> bodyReader, RequesterConfig requesterConfig, long startAt, long maxSize) throws IOException {
        this(requesterFactory, bodyReader, requesterConfig, Optional.of(startAt), Optional.of(maxSize));
    }

    private Rfc7233Iterator(RequesterFactory requesterFactory, BodyReader<E> bodyReader, RequesterConfig requesterConfig, Optional<Long> startAt, Optional<Long> maxSize) throws IOException {
        this.requesterFactory = requesterFactory;
        this.bodyReader = bodyReader;
        this.requesterConfig = requesterConfig;
        this.startAt = startAt;
        this.maxSize = maxSize;

        this.query(String.format("%s-%s", this.startAt.orElse(0L), this.startAt.orElse(0L)));
    }

    private void query(String range) throws IOException {
        ResponseDelegate response = this.requesterConfig.config(this.requesterFactory.create()).header("range", range).get();
        this.lastMatcher = new Rfc7233ResponseMatcher(response);
        this.readPage(response);
        this.pageIndex = 0;
    }

    private void readPage(ResponseDelegate response) throws IOException {
        this.page = this.bodyReader.read(response.body());
    }

    @Override
    public boolean hasNext() {
        if(this.maxSize.isPresent() && this.iteratedCount >= this.maxSize.get()) {
            return false;
        }
        return this.lastMatcher.first() + this.pageIndex < this.lastMatcher.total();
    }

    @Override
    public E next() {
        if(! this.hasNext()) {
            throw new NoSuchElementException();
        }
        if(this.lastMatcher.first() + this.pageIndex > this.lastMatcher.last()) {
            try {
                this.query(String.format("%s-%s",
                        this.lastMatcher.last() + 1,
                        this.lastMatcher.last() + this.lastMatcher.pageSize())
                );
            } catch (IOException e) {
                throw new NoSuchElementException("failed to get next page");
            }
        }
        this.pageIndex++;
        this.iteratedCount++;
        return this.page.get(this.pageIndex - 1);
    }

    @FunctionalInterface
    public interface BodyReader<E> {
        List<E> read(byte[] body) throws IOException;
    }

    public interface RequesterConfig {
        Requester config(Requester requester);
    }
}

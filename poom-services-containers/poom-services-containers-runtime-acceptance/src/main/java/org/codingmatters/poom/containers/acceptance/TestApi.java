package org.codingmatters.poom.containers.acceptance;

import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;

public class TestApi implements Api {
    private final String path;
    private final Processor processor;
    private final boolean hasDoc;

    public TestApi(String path, Processor processor) {
        this(path, processor, false);
    }
    public TestApi(String path, Processor processor, boolean hasDoc) {
        this.path = path;
        this.processor = processor;
        this.hasDoc = hasDoc;
    }

    @Override
    public String name() {
        return "test-api";
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public String version() {
        return "12";
    }

    @Override
    public Processor processor() {
        return this.processor;
    }

    @Override
    public String docResource() {
        if(this.hasDoc) {
            return "doc-index.html";
        } else {
            return null;
        }
    }
}

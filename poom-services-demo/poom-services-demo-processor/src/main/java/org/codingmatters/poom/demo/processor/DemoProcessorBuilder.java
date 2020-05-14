package org.codingmatters.poom.demo.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.processor.DemoProcessor;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.processor.handlers.StoreBrowse;
import org.codingmatters.poom.generic.resource.processor.GenericResourceProcessorBuilder;
import org.codingmatters.rest.api.Processor;

public class DemoProcessorBuilder {

    private final GenericResourceProcessorBuilder processorBuilder;
    private final String apiPath;
    private final JsonFactory jsonFactory;
    private final StoreManager storeManager;
    private final DemoHandlers handlers;

    public DemoProcessorBuilder(String apiPath, JsonFactory jsonFactory, StoreManager storeManager) {
        this.apiPath = apiPath;
        this.jsonFactory = jsonFactory;
        this.storeManager = storeManager;
        this.processorBuilder = new GenericResourceProcessorBuilder(apiPath, jsonFactory);

        this.handlers = this.buildHandlers();
    }

    private DemoHandlers buildHandlers() {
        return new DemoHandlers.Builder()
                .storesGetHandler(new StoreBrowse(this.storeManager))
                .build();
    }

    public Processor build() {
        return processorBuilder.build(new DemoProcessor(this.apiPath, this.jsonFactory, this.handlers));
    }
}

package org.codingmatters.poom.demo.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.processor.DemoProcessor;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.processor.handlers.StoreBrowse;
import org.codingmatters.poom.generic.resource.processor.GenericResourceProcessorBuilder;
import org.codingmatters.rest.api.Processor;

import java.util.concurrent.atomic.AtomicReference;

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
        this.interceptMovies();
    }

    private DemoHandlers buildHandlers() {
        return new DemoHandlers.Builder()
                .storesGetHandler(new StoreBrowse(this.storeManager))
                .build();
    }

    private void interceptMovies() {
        ThreadLocal<AtomicReference<String>> storeContext = new ThreadLocal();
        ThreadLocal<AtomicReference<String>> categoryContext = new ThreadLocal();

        this.processorBuilder.preprocessedResourceAt("/{store}/movies",
                (requestDelegate, responseDelegate) -> storeContext.get().set(requestDelegate.uriParameters("/{store}/movies").get("store").get(0)),
                () -> this.storeManager.storeMoviesAdpter(storeContext.get().get())
        );
        this.processorBuilder.preprocessedResourceAt("/{store}/category/{category}",
                (requestDelegate, responseDelegate) -> {
                    storeContext.get().set(requestDelegate.uriParameters("/{store}/category/{category}").get("store").get(0));
                    categoryContext.get().set(requestDelegate.uriParameters("/{store}/category/{category}").get("category").get(0));
                },
                () -> this.storeManager.categoryMoviesAdpter(storeContext.get().get(), categoryContext.get().get()));
    }

    public Processor build() {
        return this.processorBuilder.build(new DemoProcessor(this.apiPath, this.jsonFactory, this.handlers));
    }
}

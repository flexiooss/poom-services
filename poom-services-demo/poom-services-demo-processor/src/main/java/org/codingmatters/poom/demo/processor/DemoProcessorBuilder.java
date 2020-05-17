package org.codingmatters.poom.demo.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.MovieCreationData;
import org.codingmatters.poom.apis.demo.processor.DemoProcessor;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.processor.handlers.StoreBrowse;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.handlers.bridge.BridgedAdapter;
import org.codingmatters.poom.generic.resource.processor.GenericResourceProcessorBuilder;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.value.objects.values.ObjectValue;

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
        ThreadLocal<AtomicReference<String>> storeContext = new ThreadLocal<>() {
            @Override
            protected AtomicReference<String> initialValue() {
                return new AtomicReference<>();
            }
        };
        ThreadLocal<AtomicReference<String>> categoryContext = new ThreadLocal<>() {
            @Override
            protected AtomicReference<String> initialValue() {
                return new AtomicReference<>();
            }
        };

        this.processorBuilder.preprocessedResourceAt("/{store}/movies",
                (requestDelegate, responseDelegate) -> {
                    storeContext.get().set(requestDelegate.uriParameters("/{store}/movies").get("store").get(0));
                },
                () -> this.bridged(this.storeManager.storeMoviesAdpter(storeContext.get().get()))
        );
        this.processorBuilder.preprocessedResourceAt("/{store}/category/{category}",
                (requestDelegate, responseDelegate) -> {
                    storeContext.get().set(requestDelegate.uriParameters("/{store}/category/{category}").get("store").get(0));
                    categoryContext.get().set(requestDelegate.uriParameters("/{store}/category/{category}").get("category").get(0));
                },
                () -> this.bridged(this.storeManager.categoryMoviesAdpter(storeContext.get().get(), categoryContext.get().get())));
    }

    private GenericResourceAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> bridged(
            GenericResourceAdapter<Movie, MovieCreationData, Movie, Void> moviesAdpter
    ) {
        return new BridgedAdapter<>(
                moviesAdpter,
                movie -> movie == null ? null : ObjectValue.fromMap(movie.toMap()).build(),
                value -> value == null ? null : MovieCreationData.fromMap(value.toMap()).build(),
                value -> value == null ? null : Movie.fromMap(value.toMap()).build(),
                value -> null
        );
    }

    public Processor build() {
        return this.processorBuilder.build(new DemoProcessor(this.apiPath, this.jsonFactory, this.handlers));
    }
}

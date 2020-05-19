package org.codingmatters.poom.demo.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.api.types.*;
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
        ThreadLocal<AtomicReference<String>> movieContext = new ThreadLocal<>() {
            @Override
            protected AtomicReference<String> initialValue() {
                return new AtomicReference<>();
            }
        };
        ThreadLocal<AtomicReference<String>> customerContext = new ThreadLocal<>() {
            @Override
            protected AtomicReference<String> initialValue() {
                return new AtomicReference<>();
            }
        };

        this.processorBuilder.resourceAt("/{store}/movies",
                (requestDelegate, responseDelegate) -> {
                    storeContext.get().set(requestDelegate.uriParameters("/{store}/movies").get("store").get(0));
                },
                () -> this.bridgedMovieAdapter(this.storeManager.storeMoviesAdpter(storeContext.get().get()))
        );
        this.processorBuilder.resourceAt("/{store}/movies/{movie-id}/rentals",
                (requestDelegate, responseDelegate) -> {
                    storeContext.get().set(requestDelegate.uriParameters("/{store}/movies/{movie-id}/rentals").get("store").get(0));
                    movieContext.get().set(requestDelegate.uriParameters("/{store}/movies/{movie-id}/rentals").get("movie-id").get(0));
                },
                () -> this.bridgedRentalAdapter(this.storeManager.movieRentalsAdapter(storeContext.get().get(), movieContext.get().get()))
        );
        this.processorBuilder.resourceAt("/{store}/category/{category}",
                (requestDelegate, responseDelegate) -> {
                    storeContext.get().set(requestDelegate.uriParameters("/{store}/category/{category}").get("store").get(0));
                    categoryContext.get().set(requestDelegate.uriParameters("/{store}/category/{category}").get("category").get(0));
                },
                () -> this.bridgedMovieAdapter(this.storeManager.categoryMoviesAdpter(storeContext.get().get(), categoryContext.get().get()))
        );
        this.processorBuilder.resourceAt("/{store}/customers/{customer}/rentals",
                (requestDelegate, responseDelegate) -> {
                    storeContext.get().set(requestDelegate.uriParameters("/{store}/customers/{customer}/rentals").get("store").get(0));
                    customerContext.get().set(requestDelegate.uriParameters("/{store}/customers/{customer}/rentals").get("customer").get(0));
                },
                () -> this.bridgedRentalAdapter(this.storeManager.customerRentalsAdapter(storeContext.get().get(), customerContext.get().get()))
        );
        this.processorBuilder.resourceAt("/late-rental-tasks",
                () -> this.bridgedLateRentalTaskAdapter(this.storeManager.lateRentalTaskAdapter())
        );
    }

    private GenericResourceAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> bridgedMovieAdapter(
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

    private GenericResourceAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> bridgedRentalAdapter(
            GenericResourceAdapter<Rental, RentalRequest, Void, RentalAction> rentalAdapter
    ) {
        return new BridgedAdapter<>(
                rentalAdapter,
                rental -> rental == null ? null : ObjectValue.fromMap(rental.toMap()).build(),
                value -> value == null ? null : RentalRequest.fromMap(value.toMap()).build(),
                value -> null,
                value -> value == null ? null : RentalAction.fromMap(value.toMap()).build()
        );
    }

    private GenericResourceAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> bridgedLateRentalTaskAdapter(
            GenericResourceAdapter<LateRentalTask, ObjectValue, Void, Void> lateRentalTaskAdapter
    ) {
        return new BridgedAdapter<>(
                lateRentalTaskAdapter,
                lateRentalTask -> lateRentalTask == null ? null : ObjectValue.fromMap(lateRentalTask.toMap()).build(),
                value -> value,
                value -> null,
                value -> null
        );
    }

    public Processor build() {
        return this.processorBuilder.build(new DemoProcessor(this.apiPath, this.jsonFactory, this.handlers));
    }
}

package org.codingmatters.poom.demo.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.api.types.*;
import org.codingmatters.poom.apis.demo.processor.DemoProcessor;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.processor.handlers.StoreBrowse;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.handlers.bridge.BridgedAdapter;
import org.codingmatters.poom.generic.resource.processor.PagedCollectionsProcessorBuilder;
import org.codingmatters.poom.generic.resource.processor.utils.RequestContext;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.value.objects.values.ObjectValue;

public class DemoProcessorBuilder {

    private final PagedCollectionsProcessorBuilder processorBuilder;
    private final String apiPath;
    private final JsonFactory jsonFactory;
    private final StoreManager storeManager;
    private final DemoHandlers handlers;

    public DemoProcessorBuilder(String apiPath, JsonFactory jsonFactory, StoreManager storeManager) {
        this.apiPath = apiPath;
        this.jsonFactory = jsonFactory;
        this.storeManager = storeManager;
        this.processorBuilder = new PagedCollectionsProcessorBuilder(apiPath, jsonFactory);

        this.handlers = this.buildHandlers();
        this.interceptMovies();
    }

    private DemoHandlers buildHandlers() {
        return new DemoHandlers.Builder()
                .storesGetHandler(new StoreBrowse(this.storeManager))
                .build();
    }

    private void interceptMovies() {
        RequestContext requestContext = new RequestContext();

        this.processorBuilder.collectionAt("/{store}/movies",
                requestContext::setup,
                () -> this.bridgedMovieAdapter(this.storeManager.storeMoviesAdpter(requestContext.requestDelegate()
                        .uriParameters("/{store}/movies.*")
                        .get("store").get(0)))
        );
        this.processorBuilder.collectionAt("/{store}/movies/{movie-id}/rentals",
                requestContext::setup,
                () -> this.bridgedRentalAdapter(this.storeManager.movieRentalsAdapter(
                        requestContext.requestDelegate().uriParameters("/{store}/movies/{movie-id}/rentals.*").get("store").get(0),
                        requestContext.requestDelegate().uriParameters("/{store}/movies/{movie-id}/rentals.*").get("movie-id").get(0)
                ))
        );
        this.processorBuilder.collectionAt("/{store}/category/{category}",
                requestContext::setup,
                () -> this.bridgedMovieAdapter(this.storeManager.categoryMoviesAdpter(
                        requestContext.requestDelegate().uriParameters("/{store}/category/{category}.*").get("store").get(0),
                        requestContext.requestDelegate().uriParameters("/{store}/category/{category}.*").get("category").get(0)
                ))
        );
        this.processorBuilder.collectionAt("/{store}/customers/{customer}/rentals",
                requestContext::setup,
                () -> this.bridgedRentalAdapter(this.storeManager.customerRentalsAdapter(
                        requestContext.requestDelegate().uriParameters("/{store}/customers/{customer}/rentals.*").get("store").get(0),
                        requestContext.requestDelegate().uriParameters("/{store}/customers/{customer}/rentals.*").get("customer").get(0)
                ))
        );
        this.processorBuilder.collectionAt("/late-rental-tasks",
                () -> this.bridgedLateRentalTaskAdapter(this.storeManager.lateRentalTaskAdapter())
        );
    }

    private PagedCollectionAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> bridgedMovieAdapter(
            PagedCollectionAdapter<Movie, MovieCreationData, Movie, Void> moviesAdpter
    ) {
        return new BridgedAdapter<>(
                moviesAdpter,
                movie -> movie == null ? null : ObjectValue.fromMap(movie.toMap()).build(),
                value -> value == null ? null : MovieCreationData.fromMap(value.toMap()).build(),
                value -> value == null ? null : Movie.fromMap(value.toMap()).build(),
                value -> null
        );
    }

    private PagedCollectionAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> bridgedRentalAdapter(
            PagedCollectionAdapter<Rental, RentalRequest, Void, RentalAction> rentalAdapter
    ) {
        return new BridgedAdapter<>(
                rentalAdapter,
                rental -> rental == null ? null : ObjectValue.fromMap(rental.toMap()).build(),
                value -> value == null ? null : RentalRequest.fromMap(value.toMap()).build(),
                value -> null,
                value -> value == null ? null : RentalAction.fromMap(value.toMap()).build()
        );
    }

    private PagedCollectionAdapter<ObjectValue, ObjectValue, ObjectValue, ObjectValue> bridgedLateRentalTaskAdapter(
            PagedCollectionAdapter<LateRentalTask, ObjectValue, Void, Void> lateRentalTaskAdapter
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

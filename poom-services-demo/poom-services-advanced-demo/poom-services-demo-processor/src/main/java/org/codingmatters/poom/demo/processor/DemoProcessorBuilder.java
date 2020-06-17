package org.codingmatters.poom.demo.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.api.types.*;
import org.codingmatters.poom.apis.demo.processor.DemoProcessor;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.processor.handlers.StoreBrowse;
import org.codingmatters.poom.generic.resource.handlers.bridge.BridgedAdapterBuilder;
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
                () -> new BridgedAdapterBuilder()
                                .entityType(Movie.class)
                                .creationType(MovieCreationData.class)
                                .replaceType(Movie.class)
                                .build(
                                        this.storeManager.storeMoviesAdpter(
                                                requestContext.requestDelegate().uriParameters("/{store}/movies.*").get("store").get(0)
                                        )
                                )
        );
        this.processorBuilder.collectionAt("/{store}/movies/{movie-id}/rentals",
                requestContext::setup,
                () -> new BridgedAdapterBuilder()
                                .entityType(Rental.class)
                                .creationType(RentalRequest.class)
                                .updateType(RentalAction.class)
                                .build(this.storeManager.movieRentalsAdapter(
                                        requestContext.requestDelegate().uriParameters("/{store}/movies/{movie-id}/rentals.*").get("store").get(0),
                                        requestContext.requestDelegate().uriParameters("/{store}/movies/{movie-id}/rentals.*").get("movie-id").get(0)
                                ))
        );
        this.processorBuilder.collectionAt("/{store}/rentals",
                requestContext::setup,
                () -> new BridgedAdapterBuilder()
                                .entityType(Rental.class)
                                .build(this.storeManager.rentalsAdapter(
                                        requestContext.requestDelegate().uriParameters("/{store}/rentals.*").get("store").get(0)
                                ))
        );
        this.processorBuilder.collectionAt("/{store}/category/{category}",
                requestContext::setup,
                () -> new BridgedAdapterBuilder()
                                .entityType(Movie.class)
                                .creationType(MovieCreationData.class)
                                .replaceType(Movie.class)
                                .build(this.storeManager.categoryMoviesAdpter(
                                        requestContext.requestDelegate().uriParameters("/{store}/category/{category}.*").get("store").get(0),
                                        requestContext.requestDelegate().uriParameters("/{store}/category/{category}.*").get("category").get(0)
                                ))
        );
        this.processorBuilder.collectionAt("/{store}/customers/{customer}/rentals",
                requestContext::setup,
                () -> new BridgedAdapterBuilder()
                                .entityType(Rental.class)
                                .creationType(RentalRequest.class)
                                .updateType(RentalAction.class)
                                .build(this.storeManager.customerRentalsAdapter(
                                        requestContext.requestDelegate().uriParameters("/{store}/customers/{customer}/rentals.*").get("store").get(0),
                                        requestContext.requestDelegate().uriParameters("/{store}/customers/{customer}/rentals.*").get("customer").get(0)
                                ))
        );
        this.processorBuilder.collectionAt("/late-rental-tasks",
                () -> new BridgedAdapterBuilder()
                                .entityType(LateRentalTask.class)
                                .creationType(ObjectValue.class)
                                .build(this.storeManager.lateRentalTaskAdapter())
        );
    }

    public Processor build() {
        return this.processorBuilder.build(new DemoProcessor(this.apiPath, this.jsonFactory, this.handlers));
    }
}

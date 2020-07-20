package org.codingmatters.poom.demo.processor;

import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.api.collection.handlers.*;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.processor.handlers.StoreBrowse;

public class DemoHandlersBuilder extends DemoHandlers.Builder {
    public DemoHandlersBuilder(StoreManager storeManager) {
        // not a collection -> should refactor
        this.storesGetHandler(new StoreBrowse(storeManager));

        this.storeMoviesGetHandler(new StoreMoviesBrowse(request -> storeManager.storeMoviesAdpter(request.store()).pager()));
        this.movieGetHandler(new StoreMoviesRetrieve(request -> storeManager.storeMoviesAdpter(request.store()).crud()));
        this.moviePutHandler(new StoreMoviesReplace(request -> storeManager.storeMoviesAdpter(request.store()).crud()));
        this.movieDeleteHandler(new StoreMoviesDelete(request -> storeManager.storeMoviesAdpter(request.store()).crud()));

        this.movieRentalsGetHandler(new MovieRentalsBrowse(request -> storeManager.movieRentalsAdapter(request.store(), request.movieId()).pager()));
        this.movieRentalsPostHandler(new MovieRentalsCreate(request -> storeManager.movieRentalsAdapter(request.store(), request.movieId()).crud()));
        this.rentalGetHandler(new MovieRentalsRetrieve(request -> storeManager.movieRentalsAdapter(request.store(), request.movieId()).crud()));
        this.rentalPatchHandler(new MovieRentalsUpdate(request -> storeManager.movieRentalsAdapter(request.store(), request.movieId()).crud()));

        this.categoryMoviesGetHandler(new CategoryMoviesBrowse(request -> storeManager.categoryMoviesAdpter(request.store(), request.category()).pager()));
        this.categoryMoviesPostHandler(new CategoryMoviesCreate(request -> storeManager.categoryMoviesAdpter(request.store(), request.category()).crud()));

        this.storeRentalsGetHandler(new StoreRentalsBrowse(request -> storeManager.rentalsAdapter(request.store()).pager()));
        this.aRentalGetHandler(new StoreRentalsRetrieve(request -> storeManager.rentalsAdapter(request.store()).crud()));

        this.customerRentalsGetHandler(new CustomerRentalsBrowse(request -> storeManager.customerRentalsAdapter(request.store(), request.customer()).pager()));

        this.lateRentalTasksGetHandler(new LateRentalTasksBrowse(reques -> storeManager.lateRentalTaskAdapter().pager()));
        this.lateRentalTasksPostHandler(new LateRentalTasksCreate(request -> storeManager.lateRentalTaskAdapter().crud()));
        this.lateRentalTaskGetHandler(new LateRentalTasksRetrieve(request -> storeManager.lateRentalTaskAdapter().crud()));
    }
}

package org.codingmatters.poom.demo.processor;

import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.api.collection.handlers.StoreMoviesBrowse;
import org.codingmatters.poom.apis.demo.api.collection.handlers.StoresBrowse;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.processor.handlers.StoreBrowse;

public class DemoHandlersBuilder extends DemoHandlers.Builder {
    public DemoHandlersBuilder(StoreManager storeManager) {
        // not a collection -> should refactor
        this.storesGetHandler(new StoreBrowse(storeManager));

//        this.storeMoviesGetHandler(new StoreMoviesBrowse(request -> storeManager.storeMoviesAdpter(request.store()));
    }
}

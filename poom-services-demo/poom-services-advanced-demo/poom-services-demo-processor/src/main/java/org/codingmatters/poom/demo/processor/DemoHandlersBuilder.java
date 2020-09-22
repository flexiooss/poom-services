package org.codingmatters.poom.demo.processor;

import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.api.collection.handlers.*;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.stream.Collectors;

public class DemoHandlersBuilder extends DemoHandlers.Builder {
    public DemoHandlersBuilder(StoreManager storeManager) {

        this.storesGetHandler(new StoresBrowse(request -> new PagedCollectionAdapter.Pager<>() {
            @Override
            public String unit() {
                return "Store";
            }

            @Override
            public int maxPageSize() {
                return 10;
            }

            @Override
            public EntityLister<ObjectValue, PropertyQuery> lister() {
                return new EntityLister<ObjectValue, PropertyQuery>() {
                    @Override
                    public PagedEntityList<ObjectValue> all(long startIndex, long endIndex) throws RepositoryException {
                        return this.search(null, startIndex, endIndex);
                    }

                    @Override
                    public PagedEntityList<ObjectValue> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
                        PagedEntityList<Store> result = storeManager.storeLister().search(query, startIndex, endIndex);

                        return new PagedEntityList.DefaultPagedEntityList<ObjectValue>(
                                result.startIndex(), result.endIndex(), result.total(),
                                result.stream().map(e -> new ImmutableEntity<>(e.id(), e.version(),
                                        ObjectValue.fromMap(e.value().toMap()).build())).collect(Collectors.toUnmodifiableList())
                        );
                    }
                };
            }
        }));

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

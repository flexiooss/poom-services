package org.codingmatters.poom.demo.service.support;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.MovieCreationData;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.demo.domain.spec.store.Address;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.util.Optional;
import java.util.concurrent.Executors;

public class DemoData {
    public static final Store NETFLIX = Store.builder()
            .name("NETFLIX")
            .address(Address.builder()
                    .street("100 Winchester Circle")
                    .postalCode("95032")
                    .town("Los Gatos, CA")
                    .country("USA")
                    .build()).build();

    public static final Store VIDEO_FUTUR = Store.builder()
            .name("VIDEO FUTUR")
            .address(Address.builder()
                    .street("Cimetière du Père Lachaise")
                    .postalCode("75020")
                    .town("Paris")
                    .country("FRANCE")
                    .build()).build();

    private final StoreManagerSupport storeManagerSupport;

    public DemoData(StoreManagerSupport storeManagerSupport) {
        this.storeManagerSupport = storeManagerSupport;
    }

    public void create() throws RuntimeException {
        try {
            Repository<Store, PropertyQuery> stores = this.storeManagerSupport.storeRepository();
            stores.createWithId(NETFLIX.name(), NETFLIX);
            stores.createWithId(VIDEO_FUTUR.name(), VIDEO_FUTUR);

            StoreManager manager = this.storeManagerSupport.createStoreManager(Executors.newSingleThreadExecutor());

            PagedCollectionAdapter.CRUD<Movie, MovieCreationData, Movie, Void> crud = manager.categoryMoviesAdpter(NETFLIX.name(), Movie.Category.REGULAR.name()).crud();
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("The Great Day").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("The Call of Youth").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("Dangerous Lies").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("The White Shadow").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("The Lady Vanishes").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("The Fighting Generation").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("Vertigo").build());

            crud = manager.categoryMoviesAdpter(NETFLIX.name(), Movie.Category.HORROR.name()).crud();
            Entity<Movie> psycho = crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("Psycho").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Jonathan Demme").title("The silence of the lambs").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Rosemary's baby").title("Roman Polanski").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Alfred Hitchcock").title("The birds").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("The fly").title("David Cronenberg").build());

            crud = manager.categoryMoviesAdpter(NETFLIX.name(), Movie.Category.CHILDREN.name()).crud();
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Andrew Stanton, Lee Unkrich").title("Finding Nemo").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Chris Buck, Jennifer Lee").title("Frozen").build());
            Entity<Movie> myNeighborTotoro = crud.createEntityFrom(MovieCreationData.builder().filmMaker("Hayao Miyazaki").title("My neighbor Totoro").build());
            crud.createEntityFrom(MovieCreationData.builder().filmMaker("Marck Burton, Richard Starzak").title("Shaun the sheep").build());
            Entity<Movie> toyStory = crud.createEntityFrom(MovieCreationData.builder().filmMaker("John Lasseter").title("Toy Story").build());


            this.createRental(Rental.builder()
                    .movie(psycho.value())
                    .start(UTC.now().minusDays(100L))
                    .dueDate(UTC.now().minusDays(100L).plusDays(3L))
                    .status(Rental.Status.OUT)
                    .customer("picsou")
                    .build());

            this.createRental(Rental.builder()
                    .movie(toyStory.value())
                    .start(UTC.now().minusDays(1L))
                    .dueDate(UTC.now().minusDays(1L).plusDays(3L))
                    .status(Rental.Status.OUT)
                    .customer("picsou")
                    .build());

            this.createRental(Rental.builder()
                    .movie(myNeighborTotoro.value())
                    .start(UTC.now().minusDays(3L))
                    .dueDate(UTC.now().minusDays(3L).plusDays(6L))
                    .status(Rental.Status.OUT)
                    .customer("picsou")
                    .build());



        } catch (Exception e) {
            throw new RuntimeException("failed creating demo data", e);
        }
    }

    private void createRental(Rental rentalValue) throws org.codingmatters.poom.services.domain.exceptions.RepositoryException {
        Repository<Rental, PropertyQuery> rentals = this.storeManagerSupport.rentalRepositoryForStore(NETFLIX.name()).get();

        Entity<Rental> rental = rentals.create(rentalValue);
        rentals.update(rental, rental.value().withId(rental.id()));
    }
}

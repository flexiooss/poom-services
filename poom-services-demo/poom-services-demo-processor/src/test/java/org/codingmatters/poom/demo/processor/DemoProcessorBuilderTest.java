package org.codingmatters.poom.demo.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.demo.api.*;
import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.Rental;
import org.codingmatters.poom.apis.demo.client.DemoClient;
import org.codingmatters.poom.apis.demo.client.DemoRequesterClient;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.demo.domain.spec.store.Address;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class DemoProcessorBuilderTest {
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


    static public Movie A_MOVIE = Movie.builder()
            .id(UUID.randomUUID().toString())
            .title("Vertigo")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.REGULAR)
            .build();
    static public Movie ANOTHER_MOVIE = Movie.builder()
            .id(UUID.randomUUID().toString())
            .title("Psycho")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.HORROR)
            .build();


    private JsonFactory jsonFactory = new JsonFactory();

    private final Repository<Store, PropertyQuery> storeRepository = InMemoryRepositoryWithPropertyQuery.validating(Store.class);
    private final Map<String, Repository<Movie, PropertyQuery>> movieRepositories = new HashMap<>();
    private final Map<String, Repository<Rental, PropertyQuery>> rentalRepositories = new HashMap<>();

    public StoreManager storeManager = new StoreManager(
            storeRepository,
            this::movieRepository,
            this::rentalRepository);

    private Optional<Repository<Movie, PropertyQuery>> movieRepository(String store) {
        return Optional.of(movieRepositories.computeIfAbsent(store, storeName -> InMemoryRepositoryWithPropertyQuery.validating(Movie.class)));
    }

    private Optional<Repository<Rental, PropertyQuery>> rentalRepository(String store) {
        return Optional.of(rentalRepositories.computeIfAbsent(store, storeName -> InMemoryRepositoryWithPropertyQuery.validating(Rental.class)));
    }

    @Rule
    public UndertowResource server = new UndertowResource(new CdmHttpUndertowHandler(new DemoProcessorBuilder("/", this.jsonFactory, this.storeManager).build()));

    private DemoClient client;

    @Before
    public void setUp() throws Exception {
        this.client = new DemoRequesterClient(
                new OkHttpRequesterFactory(OkHttpClientWrapper.build() , () -> this.server.baseUrl()),
                this.jsonFactory,
                () -> this.server.baseUrl()
        );

        this.storeRepository.create(NETFLIX);
        this.movieRepository(NETFLIX.name()).get().createWithId(A_MOVIE.id(), A_MOVIE);
        this.movieRepository(NETFLIX.name()).get().createWithId(ANOTHER_MOVIE.id(), ANOTHER_MOVIE);

        this.storeRepository.create(VIDEO_FUTUR);

        this.rentalRepository(NETFLIX.name()).get().createWithId("r0001", Rental.builder().id("r0001")
                .customer("john-doe")
                .movie(A_MOVIE)
                .start(UTC.now().minusDays(4))
                .status(Rental.Status.OUT)
                .dueDate(UTC.now().minusDays(1))
                .build());
        this.rentalRepository(NETFLIX.name()).get().createWithId("r0002", Rental.builder().id("r0002")
                .customer("john-doe")
                .movie(ANOTHER_MOVIE)
                .start(UTC.now().minusDays(1))
                .status(Rental.Status.OUT)
                .dueDate(UTC.now().plusDays(2))
                .build());
    }

    @Test
    public void stores() throws Exception {
        StoresGetResponse response = this.client.stores().get(StoresGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayContaining(
                ObjectValue.builder().property("store name", PropertyValue.builder().stringValue(NETFLIX.name()).build()).build(),
                ObjectValue.builder().property("store name", PropertyValue.builder().stringValue(VIDEO_FUTUR.name()).build()).build()
        )));

        System.out.println(response);
    }

    @Test
    public void storeMovies() throws Exception {
        StoreMoviesGetResponse response = this.client.stores().aStore().storeMovies().get(StoreMoviesGetRequest.builder()
                .store(NETFLIX.name())
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayWithSize(2)));

        System.out.println(response.status200().payload());
    }

    @Test
    public void perCategoryMovies() throws Exception {
        CategoryMoviesGetResponse response = this.client.stores().aStore().categoryMovies().get(CategoryMoviesGetRequest.builder()
                .store(NETFLIX.name())
                .category(Movie.Category.HORROR.name())
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayWithSize(1)));

        System.out.println(response.status200().payload());
    }

    @Test
    public void perMovieRentals() throws Exception {
        MovieRentalsGetResponse response = this.client.stores().aStore().storeMovies().movie().movieRentals().get(MovieRentalsGetRequest.builder()
                .store(NETFLIX.name())
                .movieId(A_MOVIE.id())
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));
        assertThat(response.status200().payload().toArray(), is(arrayWithSize(1)));

        System.out.println(response.status200().payload());
    }

    @Test
    public void perCustomerRentals() throws Exception {
        CustomerRentalsGetResponse response = this.client.stores().aStore().customerRentals().get(CustomerRentalsGetRequest.builder()
                .store(NETFLIX.name())
                .customer("john-doe")
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayWithSize(2)));

        System.out.println(response.status200().payload());
    }
}
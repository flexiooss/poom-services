package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.*;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.demo.domain.spec.store.Address;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

public class StoreManagerTest {

    public static final Store STORE = Store.builder()
            .name("NETFLIX")
            .address(Address.builder()
                    .street("100 Winchester Circle")
                    .postalCode("95032")
                    .town("Los Gatos, CA")
                    .country("USA")
                    .build()).build();

    static public Movie MOVIE = Movie.builder()
            .id("42")
            .title("Psycho")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.HORROR)
            .build();


    private final Repository<Store, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Store.class);
    private AtomicReference<Repository<Movie, PropertyQuery>> nextMovieRepository = new AtomicReference<>();
    private AtomicReference<Repository<Rental, PropertyQuery>> nextRentalRepository = new AtomicReference<>();
    private final StoreManager manager = new StoreManager(
            repository,
            store -> Optional.ofNullable(this.nextMovieRepository.get()),
            store -> Optional.ofNullable(this.nextRentalRepository.get())
    );

    @Test
    public void givenRepositoryIsEmpty__whenLookingUpExistenceOfAStore__thenFalse() throws Exception {
        assertThat(manager.storeExists("whatever"), is(false));
    }

    @Test
    public void givenRepositoryHasValues__whenLookingUpExistenceOfAStore_andAStoreWithThatNameExists__thenTrue() throws Exception {
        this.repository.create(STORE);
        assertThat(manager.storeExists(STORE.name()), is(true));
    }

    @Test
    public void givenRepositoryHasValues__whenLookingUpExistenceOfAStore_andNoStoreWithThatNameExists__thenFalse() throws Exception {
        this.repository.create(STORE);
        assertThat(manager.storeExists("no sich store"), is(false));
    }

    @Test
    public void givenStoreExists__whenGettingMovieAdapter__thenMovieCRUD_andMoviePager() throws Exception {
        this.repository.create(STORE);
        this.nextMovieRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Movie.class));

        GenericResourceAdapter<Movie, MovieCreationData, Movie, Void> actual = this.manager.storeMoviesAdpter(STORE.name());

        assertThat(actual.crud(), isA(MovieCRUD.class));
        assertThat(actual.pager(), isA(MoviePager.class));
        assertThat(actual.pager().unit(), is("Movie"));
        assertThat(actual.pager().maxPageSize(), is(1000));
    }

    @Test
    public void givenStoreExists__whenGettingMovieAdapter_andProblemGettingMovieRepo__thenUnexpectedExceptionAdapter() throws Exception {
        this.repository.create(STORE);
        this.nextMovieRepository.set(null);

        assertThat(this.manager.storeMoviesAdpter(STORE.name()), isA(GenericResourceAdapter.UnexpectedExceptionAdapter.class));
    }

    @Test
    public void givenStoreDoesntExists__whenGettingMovieAdapter__thenNotFoundADapter() throws Exception {
        assertThat(this.manager.storeMoviesAdpter("whatever"), isA(GenericResourceAdapter.NotFoundAdapter.class));
    }

    @Test
    public void givenStoreExists__whenGettingRentalAdapter_andProblemGettingMovieRepo__thenUnexpectedExceptionAdapter() throws Exception {
        this.repository.create(STORE);
        this.nextMovieRepository.set(null);
        this.nextRentalRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Rental.class));

        assertThat(this.manager.movieRentalsAdapter(STORE.name(), MOVIE.id()), isA(GenericResourceAdapter.UnexpectedExceptionAdapter.class));
    }

    @Test
    public void givenStoreExists_andMovieExists__whenGettingRentalAdapter_andProblemGettingRentalRepo__thenUnexpectedExceptionAdapter() throws Exception {
        this.repository.create(STORE);
        this.nextMovieRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Movie.class));
        this.nextMovieRepository.get().createWithId(MOVIE.id(), MOVIE);
        this.nextRentalRepository.set(null);

        assertThat(this.manager.movieRentalsAdapter(STORE.name(), MOVIE.id()), isA(GenericResourceAdapter.UnexpectedExceptionAdapter.class));
    }

    @Test
    public void givenStoreExists__whenGettingRentalAdapter_andMovieDoesntExists__thenNotFoundAdapter() throws Exception {
        this.repository.create(STORE);
        this.nextMovieRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Movie.class));
        this.nextRentalRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Rental.class));

        assertThat(this.manager.movieRentalsAdapter(STORE.name(), MOVIE.id()), isA(GenericResourceAdapter.NotFoundAdapter.class));
    }

    @Test
    public void givenStoreDoesntExist__whenGettingRentalAdapter__thenNotFoundAdapter() throws Exception {
        this.nextMovieRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Movie.class));
        this.nextMovieRepository.get().createWithId(MOVIE.id(), MOVIE);
        this.nextRentalRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Rental.class));

        assertThat(this.manager.movieRentalsAdapter(STORE.name(), MOVIE.id()), isA(GenericResourceAdapter.NotFoundAdapter.class));
    }

    @Test
    public void givenStoreExists_andMovieExists_andRentalRepoExists__whenGettingRentalAdapter__thenRentalCRUD_andPager() throws Exception {
        this.repository.create(STORE);
        this.nextMovieRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Movie.class));
        this.nextMovieRepository.get().createWithId(MOVIE.id(), MOVIE);
        this.nextRentalRepository.set(InMemoryRepositoryWithPropertyQuery.validating(Rental.class));

        GenericResourceAdapter<Rental, RentalRequest, Void, RentalAction> actual = this.manager.movieRentalsAdapter(STORE.name(), MOVIE.id());

        assertThat(actual.crud(), isA(RentalCRUD.class));
        assertThat(actual.pager(), isA(RentalPager.class));
        assertThat(actual.pager().unit(), is("Rental"));
        assertThat(actual.pager().maxPageSize(), is(1000));
    }
}
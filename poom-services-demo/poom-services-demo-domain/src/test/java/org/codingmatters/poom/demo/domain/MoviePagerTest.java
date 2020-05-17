package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class MoviePagerTest {

    static public Movie REGULAR_MOVIE = Movie.builder()
            .id("12")
            .title("Vertigo")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.REGULAR)
            .build();

    static public Movie HORROR_MOVIE_1 = Movie.builder()
            .id("42")
            .title("Psycho")
            .filmMaker("Alfred Hitchcock")
            .category(Movie.Category.HORROR)
            .build();

    static public Movie HORROR_MOVIE_2 = Movie.builder()
            .id("72")
            .title("Shining")
            .filmMaker("Stanley Kubrick")
            .category(Movie.Category.HORROR)
            .build();

    private Repository<Movie, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Movie.class);

    @Before
    public void setUp() throws Exception {
        this.repository.createWithId(REGULAR_MOVIE.id(), REGULAR_MOVIE);
        this.repository.createWithId(HORROR_MOVIE_1.id(), HORROR_MOVIE_1);
        this.repository.createWithId(HORROR_MOVIE_2.id(), HORROR_MOVIE_2);
    }

    @Test
    public void unitIsMovie() throws Exception {
        assertThat(new MoviePager(this.repository, null).unit(), is("Movie"));
    }

    @Test
    public void maxPageSizeIs1000() throws Exception {
        assertThat(new MoviePager(this.repository, null).maxPageSize(), is(1000));
    }

    @Test
    public void givenNoCategory__whenAll__thenAllMoviesReturned() throws Exception {
        assertThat(
                new MoviePager(this.repository, null).lister().all(0, 1000).valueList(),
                contains(REGULAR_MOVIE, HORROR_MOVIE_1, HORROR_MOVIE_2)
        );
    }

    @Test
    public void givenNoCategory__whenSearch_andNoFilter__thenAllMoviesReturned() throws Exception {
        assertThat(
                new MoviePager(this.repository, null).lister().search(PropertyQuery.builder().build(), 0, 1000).valueList(),
                contains(REGULAR_MOVIE, HORROR_MOVIE_1, HORROR_MOVIE_2)
        );
    }

    @Test
    public void givenNoCategory__whenSearch_andFilter__thenFilteredMoviesReturned() throws Exception {
        assertThat(
                new MoviePager(this.repository, null).lister().search(PropertyQuery.builder()
                        .filter("title == 'Vertigo'")
                        .build(), 0, 1000).valueList(),
                contains(REGULAR_MOVIE)
        );
    }




    @Test
    public void givenCategory__whenAll__thenCategoryMoviesReturned() throws Exception {
        assertThat(
                new MoviePager(this.repository, Movie.Category.HORROR).lister().all(0, 1000).valueList(),
                contains(HORROR_MOVIE_1, HORROR_MOVIE_2)
        );
    }

    @Test
    public void givenCategory__whenSearch_andNoFilter__thenCategoryMoviesReturned() throws Exception {
        assertThat(
                new MoviePager(this.repository, Movie.Category.HORROR).lister().search(PropertyQuery.builder().build(), 0, 1000).valueList(),
                contains(HORROR_MOVIE_1, HORROR_MOVIE_2)
        );
    }

    @Test
    public void givenNoCategory__whenSearch_andFilter__thenFilteredCategoryMoviesReturned() throws Exception {
        assertThat(
                new MoviePager(this.repository, Movie.Category.HORROR).lister().search(PropertyQuery.builder()
                        .filter("title == 'Shining'")
                        .build(), 0, 1000).valueList(),
                contains(HORROR_MOVIE_2)
        );
    }
}
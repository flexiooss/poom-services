package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MovieOrderedListerTest {

    private static Movie movie(String id, String date) {
        return Movie.builder()
            .id(id)
            .title("Movie " + id)
            .category(Movie.Category.REGULAR)
            .facts(f -> f.releaseDate(LocalDate.parse(date)))
            .build();
    }

    // 10 movies in ascending (releaseDate, id) order.
    // M2/M3 and M6/M7 deliberately share the same releaseDate.
    static final Movie M1  = movie("m-01", "1990-01-01");
    static final Movie M2  = movie("m-02", "1995-06-15");
    static final Movie M3  = movie("m-03", "1995-06-15");
    static final Movie M4  = movie("m-04", "2000-01-01");
    static final Movie M5  = movie("m-05", "2005-03-20");
    static final Movie M6  = movie("m-06", "2010-07-04");
    static final Movie M7  = movie("m-07", "2010-07-04");
    static final Movie M8  = movie("m-08", "2015-11-30");
    static final Movie M9  = movie("m-09", "2020-01-01");
    static final Movie M10 = movie("m-10", "2024-06-15");

    private Repository<Movie, PropertyQuery> repository;
    private MovieOrderedLister lister;

    @BeforeEach
    public void setUp() throws Exception {
        this.repository = InMemoryRepositoryWithPropertyQuery.validating(Movie.class);
        // Insert shuffled — ordering must come from sort, not insertion order
        for (Movie m : new Movie[]{M5, M3, M8, M1, M10, M6, M2, M9, M7, M4}) {
            this.repository.createWithId(m.id(), m);
        }
        this.lister = new MovieOrderedLister(this.repository);
    }

    // cursor = "YYYY-MM-DD|id"
    private static String cursor(Movie m) {
        return m.facts().releaseDate().toString() + "|" + m.id();
    }

    private static List<Movie> values(PagedCollectionAdapter.OrderedPage<Movie> page) {
        return page.list().stream().map(Entity::value).collect(Collectors.toList());
    }

    // ---- initLatest ----

    @Test
    public void givenEmptyRepo__whenInitLatest__thenEmptyPageAndNoCursors() throws Exception {
        Repository<Movie, PropertyQuery> emptyRepo = InMemoryRepositoryWithPropertyQuery.validating(Movie.class);
        PagedCollectionAdapter.OrderedPage<Movie> page =
            new MovieOrderedLister(emptyRepo).initLatest(Optional.empty(), 0, 49);
        assertThat(values(page), is(empty()));
        assertThat(page.since(), is(Optional.empty()));
        assertThat(page.before(), is(Optional.empty()));
    }

    @Test
    public void whenInitLatest__thenLastMoviesReturnedOldestFirst() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initLatest(Optional.empty(), 0, 4);
        assertThat(values(page), contains(M6, M7, M8, M9, M10));
    }

    @Test
    public void whenInitLatest__thenSinceCursorIsNewest() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initLatest(Optional.empty(), 0, 4);
        assertThat(page.since(), is(Optional.of(cursor(M10))));
    }

    @Test
    public void whenInitLatest__thenBeforeCursorIsOldest() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initLatest(Optional.empty(), 0, 4);
        assertThat(page.before(), is(Optional.of(cursor(M6))));
    }

    @Test
    public void whenInitLatest__andAllFitInPage__thenBeforeCursorIsEmpty() throws Exception {
        // page larger than collection → from == 0 → nothing older exists outside the page
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initLatest(Optional.empty(), 0, 49);
        assertThat(page.before(), is(Optional.empty()));
    }

    @Test
    public void whenInitLatest__thenPagedListIndicesAreAdjusted() throws Exception {
        // 10 total; desc positions 0-4 → asc positions 5-9
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initLatest(Optional.empty(), 0, 4);
        assertThat(page.list().startIndex(), is(5L));
        assertThat(page.list().endIndex(), is(9L));
        assertThat(page.list().total(), is(10L));
    }

    // ---- initOldest ----

    @Test
    public void whenInitOldest__thenFirstMoviesReturnedOldestFirst() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initOldest(Optional.empty(), 0, 4);
        assertThat(values(page), contains(M1, M2, M3, M4, M5));
    }

    @Test
    public void whenInitOldest__thenSinceCursorIsNewest() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initOldest(Optional.empty(), 0, 4);
        assertThat(page.since(), is(Optional.of(cursor(M5))));
    }

    @Test
    public void whenInitOldest__thenBeforeCursorIsEmpty() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page = lister.initOldest(Optional.empty(), 0, 4);
        assertThat(page.before(), is(Optional.empty()));
    }

    // ---- since ----

    @Test
    public void givenSinceCursor__whenSince__thenMoviesAfterCursorReturnedOldestFirst() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.since(cursor(M5), Optional.empty(), Optional.empty(), 0, 49);
        assertThat(values(page), contains(M6, M7, M8, M9, M10));
    }

    @Test
    public void givenSinceCursorOnSharedDate__whenSince__thenIdBoundaryRespected() throws Exception {
        // M2 and M3 share "1995-06-15"; since(M2) must return M3 (exclusive bound on M2)
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.since(cursor(M2), Optional.empty(), Optional.empty(), 0, 49);
        assertThat(values(page).get(0), is(M3));
        assertThat(values(page), not(hasItem(M2)));
    }

    @Test
    public void givenSinceCursorAndBeforeCursor__whenSince__thenWindowedResultReturned() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.since(cursor(M5), Optional.of(cursor(M8)), Optional.empty(), 0, 49);
        assertThat(values(page), contains(M6, M7));
    }

    @Test
    public void givenSinceCursorAndBeforeCursor__whenSince__thenOptBeforePreservedInResponse() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.since(cursor(M5), Optional.of(cursor(M8)), Optional.empty(), 0, 49);
        assertThat(page.before(), is(Optional.of(cursor(M8))));
    }

    @Test
    public void givenSinceCursor__whenSince__thenSinceCursorIsNewest() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.since(cursor(M5), Optional.empty(), Optional.empty(), 0, 49);
        assertThat(page.since(), is(Optional.of(cursor(M10))));
    }

    // ---- before ----

    @Test
    public void givenBeforeCursor__whenBefore__thenMoviesBeforeCursorReturnedNewestFirst() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.before(cursor(M6), Optional.empty(), 0, 49);
        assertThat(values(page), contains(M5, M4, M3, M2, M1));
    }

    @Test
    public void givenBeforeCursor__whenBefore__thenSinceCursorIsEmpty() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.before(cursor(M6), Optional.empty(), 0, 49);
        assertThat(page.since(), is(Optional.empty()));
    }

    @Test
    public void givenBeforeCursor__whenBefore__thenBeforeCursorIsOldest() throws Exception {
        PagedCollectionAdapter.OrderedPage<Movie> page =
            lister.before(cursor(M6), Optional.empty(), 0, 49);
        assertThat(page.before(), is(Optional.of(cursor(M1))));
    }

    // ---- round-trips ----

    @Test
    public void givenInitLatestPage__whenBeforeOnBeforeCursor__thenPreviousPageReturned() throws Exception {
        // initLatest(0-4) = [M6,M7,M8,M9,M10], before = cursor(M6)
        PagedCollectionAdapter.OrderedPage<Movie> page1 = lister.initLatest(Optional.empty(), 0, 4);
        PagedCollectionAdapter.OrderedPage<Movie> page2 =
            lister.before(page1.before().get(), Optional.empty(), 0, 4);
        assertThat(values(page2), contains(M5, M4, M3, M2, M1));
    }

    @Test
    public void givenInitOldestPage__whenSinceOnSinceCursor__thenNextPageReturned() throws Exception {
        // initOldest(0-4) = [M1,M2,M3,M4,M5], since = cursor(M5)
        PagedCollectionAdapter.OrderedPage<Movie> page1 = lister.initOldest(Optional.empty(), 0, 4);
        PagedCollectionAdapter.OrderedPage<Movie> page2 =
            lister.since(page1.since().get(), Optional.empty(), Optional.empty(), 0, 4);
        assertThat(values(page2), contains(M6, M7, M8, M9, M10));
    }

    // ---- null releaseDate tolerance ----

    @Test
    void givenMovieWithNoFacts__whenSince__thenNoNPEAndNullDateMovieAppearsAfterDatedMovies() throws Exception {
        // Movies without facts.releaseDate use sentinel 9999-12-31 (null-last, consistent with repository ASC sort).
        // since(cursor of a 2020 movie) → null-date movie appears in results (9999-12-31 > 2020-06-01).
        Repository<Movie, PropertyQuery> repo = InMemoryRepositoryWithPropertyQuery.validating(Movie.class);
        Movie withDate    = movie("dated",   "2020-06-01");
        Movie withoutDate = Movie.builder().id("nodates").title("No Facts Movie").category(Movie.Category.REGULAR).build();
        repo.createWithId(withDate.id(), withDate);
        repo.createWithId(withoutDate.id(), withoutDate);

        String sinceCursor = withDate.facts().releaseDate() + "|" + withDate.id();
        PagedCollectionAdapter.OrderedPage<Movie> page =
            new MovieOrderedLister(repo).since(sinceCursor, Optional.empty(), Optional.empty(), 0, 49);
        assertThat(values(page), contains(withoutDate));
        assertThat(values(page), not(hasItem(withDate)));
    }

    @Test
    void givenMovieWithNoFacts__whenInitOldest__thenNullDateMovieAppearsLast() throws Exception {
        // InMemoryRepository sorts nulls LAST in ASC order — null-date movies appear at end of initOldest.
        Repository<Movie, PropertyQuery> repo = InMemoryRepositoryWithPropertyQuery.validating(Movie.class);
        Movie withDate    = movie("dated",   "2020-06-01");
        Movie withoutDate = Movie.builder().id("aaaa-nodates").title("No Facts Movie").category(Movie.Category.REGULAR).build();
        repo.createWithId(withDate.id(), withDate);
        repo.createWithId(withoutDate.id(), withoutDate);

        PagedCollectionAdapter.OrderedPage<Movie> page = new MovieOrderedLister(repo).initOldest(Optional.empty(), 0, 49);
        List<Movie> movies = values(page);
        assertThat(movies.get(0), is(withDate));      // 2020 sorts before null (null-last ASC)
        assertThat(movies.get(1), is(withoutDate));
    }

    // ---- category scoping ----

    @Test
    public void givenCategoryFilter__whenInitLatest__thenOnlyCategoryMoviesReturned() throws Exception {
        // Add a HORROR movie to the repository
        Movie horrorMovie = Movie.builder()
            .id("h-01")
            .title("Horror Movie")
            .category(Movie.Category.HORROR)
            .facts(f -> f.releaseDate(LocalDate.parse("2012-05-01")))
            .build();
        this.repository.createWithId(horrorMovie.id(), horrorMovie);

        // Category-scoped lister should only return HORROR movies
        MovieOrderedLister horrorLister = new MovieOrderedLister(this.repository, Movie.Category.HORROR);
        PagedCollectionAdapter.OrderedPage<Movie> page = horrorLister.initLatest(Optional.empty(), 0, 49);
        assertThat(values(page), contains(horrorMovie));
    }

    @Test
    public void givenCategoryFilter__whenSince__thenOnlyCategoryMoviesReturned() throws Exception {
        // Add two HORROR movies
        Movie horror1 = Movie.builder()
            .id("h-01")
            .title("Horror 1")
            .category(Movie.Category.HORROR)
            .facts(f -> f.releaseDate(LocalDate.parse("2012-05-01")))
            .build();
        Movie horror2 = Movie.builder()
            .id("h-02")
            .title("Horror 2")
            .category(Movie.Category.HORROR)
            .facts(f -> f.releaseDate(LocalDate.parse("2018-09-15")))
            .build();
        this.repository.createWithId(horror1.id(), horror1);
        this.repository.createWithId(horror2.id(), horror2);

        // since cursor on horror1 should return horror2, not any REGULAR movies
        String sinceCursor = horror1.facts().releaseDate().toString() + "|" + horror1.id();
        MovieOrderedLister horrorLister = new MovieOrderedLister(this.repository, Movie.Category.HORROR);
        PagedCollectionAdapter.OrderedPage<Movie> page =
            horrorLister.since(sinceCursor, Optional.empty(), Optional.empty(), 0, 49);
        assertThat(values(page), contains(horror2));
    }
}

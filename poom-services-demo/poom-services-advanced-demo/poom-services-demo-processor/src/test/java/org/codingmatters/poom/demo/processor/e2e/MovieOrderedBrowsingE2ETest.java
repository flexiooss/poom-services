package org.codingmatters.poom.demo.processor.e2e;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.apis.demo.api.types.json.MovieReader;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end ordered browsing tests against the running demo at http://localhost:8889/demo.
 *
 * The NETFLIX store has 17 movies:
 *   - "The Great Day" (releaseDate = 1930-01-01) — the only dated movie
 *   - 16 other movies with no releaseDate (treated as 9999-12-31 by MovieOrderedLister)
 *
 * The DemoProcessor reads all three ordered-browsing request headers from HTTP
 * (`since`, `before`, `init-ordered`) — they are declared by the rfc7233 browse trait
 * since poom-api-specs 1.44.0, so the generated StoreMoviesGetRequest exposes
 * since()/before()/initOrdered() and the processor maps each one.
 *
 * NOTE: the demo server under test must be built and (re)started from the 1.44.0 spec —
 * a server still running an older build will answer 400 BAD_REQUEST on these headers
 * because its MoviePager has no orderedLister() and StoreMoviesBrowse falls through to
 * "provider does not support ordered browsing".
 *
 * All tests are skipped automatically if the demo is not reachable.
 */
@Tag("e2e")
public class MovieOrderedBrowsingE2ETest {

    private static final String MOVIES_URL = "http://localhost:8889/demo/NETFLIX/movies";
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static HttpClient client;

    @BeforeAll
    static void checkServerRunning() {
        try {
            client = HttpClient.newHttpClient();
            HttpResponse<String> probe = client.send(
                HttpRequest.newBuilder(URI.create(MOVIES_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            Assumptions.assumeTrue(
                probe.statusCode() < 500,
                "Demo server not available at " + MOVIES_URL + " (status " + probe.statusCode() + ")"
            );
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Demo server not reachable: " + e.getMessage());
        }
    }

    // ---- helpers ----

    private static Movie[] parseMovies(String body) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(body)) {
            return new MovieReader().readArray(parser);
        }
    }

    private static String responseHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    private static Movie findByTitle(Movie[] movies, String title) {
        return Arrays.stream(movies)
            .filter(m -> title.equals(m.title()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Movie '" + title + "' not found in: "
                + Arrays.toString(Arrays.stream(movies).map(Movie::title).toArray())));
    }

    private static HttpResponse<String> get(String url, String... headerPairs) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).GET();
        for (int i = 0; i < headerPairs.length - 1; i += 2) {
            req.header(headerPairs[i], headerPairs[i + 1]);
        }
        return client.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    // ---- working tests (`since` header is read by the processor) ----

    @Test
    void givenTheGreatDayCursor__whenSince__then16UndatedMoviesReturned() throws Exception {
        // Discover The Great Day's generated id
        Movie[] all = parseMovies(get(MOVIES_URL).body());
        Movie theGreatDay = findByTitle(all, "The Great Day");
        String cursor = theGreatDay.facts().releaseDate() + "|" + theGreatDay.id();

        // since(cursor) → everything after 1930-01-01 = the 16 undated movies (sentinel 9999-12-31)
        HttpResponse<String> response = get(MOVIES_URL, "since", cursor);

        assertThat("status 200 or 206", response.statusCode(), anyOf(is(200), is(206)));
        Movie[] result = parseMovies(response.body());
        assertThat("16 undated movies", result, arrayWithSize(16));
        assertThat("The Great Day is excluded",
            Arrays.stream(result).noneMatch(m -> "The Great Day".equals(m.title())));
    }

    @Test
    void givenSince__whenRoundTrip__thenNextPageIsEmpty() throws Exception {
        // Page 1: since(The Great Day cursor) → 16 undated movies, response carries `since` cursor
        Movie[] all = parseMovies(get(MOVIES_URL).body());
        Movie theGreatDay = findByTitle(all, "The Great Day");
        String firstCursor = theGreatDay.facts().releaseDate() + "|" + theGreatDay.id();

        HttpResponse<String> page1 = get(MOVIES_URL, "since", firstCursor);
        String responseSince = responseHeader(page1, "since");
        assertThat("page 1 has since response header", responseSince, notNullValue());

        // Page 2: since(responseSince) → nothing newer than the last undated movie
        HttpResponse<String> page2 = get(MOVIES_URL, "since", responseSince);

        assertThat("status 200 or 206", page2.statusCode(), anyOf(is(200), is(206)));
        assertThat("no movies after the last page", parseMovies(page2.body()), arrayWithSize(0));
    }

    @Test
    void givenSince__whenResponseContainsSinceHeader__thenHeaderPointsToNewestReturnedMovie() throws Exception {
        Movie[] all = parseMovies(get(MOVIES_URL).body());
        Movie theGreatDay = findByTitle(all, "The Great Day");
        String cursor = theGreatDay.facts().releaseDate() + "|" + theGreatDay.id();

        HttpResponse<String> response = get(MOVIES_URL, "since", cursor);
        String responseSince = responseHeader(response, "since");

        assertThat("response has since header", responseSince, notNullValue());
        // Cursor format: "YYYY-MM-DD|movieId" — null-date movies use sentinel 9999-12-31
        assertThat("since header starts with sentinel date for undated movies",
            responseSince, startsWith("9999-12-31|"));
    }

    // ---- init-ordered and before ----

    @Test
    void whenInitOrderedOldest__thenTheGreatDayIsFirst() throws Exception {
        HttpResponse<String> response = get(MOVIES_URL, "init-ordered", "OLDEST");

        assertThat(response.statusCode(), anyOf(is(200), is(206)));
        Movie[] movies = parseMovies(response.body());
        assertThat("17 movies total", movies, arrayWithSize(17));
        assertThat("The Great Day is first (oldest releaseDate 1930-01-01)",
            movies[0].title(), is("The Great Day"));
    }

    @Test
    void whenInitOrderedLatest__andAllFit__thenTheGreatDayIsFirstAndNoBeforeCursor() throws Exception {
        HttpResponse<String> response = get(MOVIES_URL, "init-ordered", "LATEST");

        assertThat(response.statusCode(), anyOf(is(200), is(206)));
        Movie[] movies = parseMovies(response.body());
        assertThat("17 movies total", movies, arrayWithSize(17));
        // initLatest returns ASC order; The Great Day (1930-01-01) is the oldest → position 0
        assertThat("The Great Day is first (oldest releaseDate 1930-01-01)",
            movies[0].title(), is("The Great Day"));
        // page covers the full collection (from == 0) → no older items outside the page → before absent
        assertThat("no before cursor when all items fit in the page",
            responseHeader(response, "before"), nullValue());
    }

    @Test
    void givenBeforeCursorOnOldestMovie__whenBefore__thenEmptyResult() throws Exception {
        Movie[] all = parseMovies(get(MOVIES_URL).body());
        Movie theGreatDay = findByTitle(all, "The Great Day");
        String cursor = theGreatDay.facts().releaseDate() + "|" + theGreatDay.id();

        // before(The Great Day) → nothing exists before 1930-01-01
        HttpResponse<String> response = get(MOVIES_URL, "before", cursor);

        assertThat(response.statusCode(), anyOf(is(200), is(206)));
        assertThat(parseMovies(response.body()), arrayWithSize(0));
    }
}

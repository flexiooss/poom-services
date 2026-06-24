# Advanced Demo Ordered Browsing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add cursor-based ordered browsing to `/{store}/movies` in `poom-services-advanced-demo` using `facts.releaseDate + id` as a composite cursor.

**Architecture:** A new `MovieOrderedLister` class implements `PagedCollectionAdapter.OrderedLister<Movie>`. It uses PropertyQuery for sort only (`facts.releaseDate asc/desc, id asc/desc`), then applies cursor boundaries in Java (PropertyQuery can't compare `LocalDate` fields against string literals — `Operators.normalized()` converts `LocalDate` to `LocalDateTime`, causing `ClassCastException`). The `MoviePager.orderedLister()` default is overridden to return a `MovieOrderedLister`.

**Tech Stack:** Java 25, Maven, `poom-services-paged-collection-domain`, `InMemoryRepositoryWithPropertyQuery`, `PropertyQuery` DSL (sort only)

---

## File Map

| Action | File |
|---|---|
| Modify | `poom-services-demo/poom-services-advanced-demo/poom-services-demo-api/poom-services-demo-api-spec/src/main/resources/demo-api.raml` |
| Create | `poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MovieOrderedLister.java` |
| Create | `poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/test/java/org/codingmatters/poom/demo/domain/MovieOrderedListerTest.java` |
| Modify | `poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MoviePager.java` |

---

## Task 1: Bump poom-api-specs version in demo RAML

The demo RAML currently references `poom-api-specs:1.36.0`. Version `1.44.0` added the `before` and `init-ordered` request headers. Without this bump, the generated `MoviesGetRequest` won't have `before()` / `initOrdered()` methods, and the generated browse handler (which now references them) won't compile.

**Files:**
- Modify: `poom-services-demo/poom-services-advanced-demo/poom-services-demo-api/poom-services-demo-api-spec/src/main/resources/demo-api.raml`

- [ ] **Step 1: Replace all 9 occurrences of `1.36.0` with `1.44.0` in `demo-api.raml`**

  The file has exactly 9 URLs containing `1.36.0` (line 144, 213, 214, 216–221). Replace each one.
  After edit, `demo-api.raml` must contain `1.44.0` everywhere and no remaining `1.36.0`.

  Verification:
  ```bash
  grep "1.36.0" poom-services-demo/poom-services-advanced-demo/poom-services-demo-api/poom-services-demo-api-spec/src/main/resources/demo-api.raml
  ```
  Expected: no output.

- [ ] **Step 2: Rebuild API types and verify new headers are present**

  ```bash
  mvn generate-sources -pl poom-services-demo/poom-services-advanced-demo/poom-services-demo-api/poom-services-demo-api-types -am -DskipTests
  ```

  Then verify the generated request type for movies has `before()` and `initOrdered()`:
  ```bash
  find poom-services-demo/poom-services-advanced-demo/poom-services-demo-api/poom-services-demo-api-types/target/generated-sources \
    -name "*.java" | xargs grep -l "before\(\)" | head -5
  ```
  Expected: at least one file listing `before()`.

- [ ] **Step 3: Full build of the demo to catch any compile errors**

  ```bash
  mvn install -pl poom-services-demo/poom-services-advanced-demo -am -DskipTests
  ```
  Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

  ```bash
  git add poom-services-demo/poom-services-advanced-demo/poom-services-demo-api/poom-services-demo-api-spec/src/main/resources/demo-api.raml
  git commit -m "feat(demo): bump poom-api-specs to 1.44.0 for ordered browsing headers"
  ```

---

## Task 2: Implement `MovieOrderedLister` with TDD

`MovieOrderedLister` implements `PagedCollectionAdapter.OrderedLister<Movie>`. It uses a `Repository<Movie, PropertyQuery>` for sorted fetching, and applies cursor comparisons in Java.

**Cursor format:** `"YYYY-MM-DD|movieId"` (e.g. `"2010-07-04|m-06"`)

**Files:**
- Create: `poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/test/java/org/codingmatters/poom/demo/domain/MovieOrderedListerTest.java`
- Create: `poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MovieOrderedLister.java`

- [ ] **Step 1: Write the test class with fixture**

  Create `poom-services-demo-domain/src/test/java/org/codingmatters/poom/demo/domain/MovieOrderedListerTest.java`:

  ```java
  package org.codingmatters.poom.demo.domain;

  import org.codingmatters.poom.apis.demo.api.types.Movie;
  import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
  import org.codingmatters.poom.services.domain.entities.Entity;
  import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
  import org.codingmatters.poom.services.domain.repositories.Repository;
  import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
  import org.junit.Before;
  import org.junit.Test;

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

      @Before
      public void setUp() throws Exception {
          this.repository = InMemoryRepositoryWithPropertyQuery.validating(Movie.class);
          // Insert shuffled — ordering must come from sort, not insertion order
          for (Movie m : new Movie[]{M5, M3, M8, M1, M10, M6, M2, M9, M7, M4}) {
              this.repository.createWithId(m.id(), m);
          }
          this.lister = new MovieOrderedLister(this.repository);
      }

      // cursor = "YYYY-MM-DD|id" — mirrors the MovieOrderedLister.cursorFor() logic
      private static String cursor(Movie m) {
          return m.facts().releaseDate().toString() + "|" + m.id();
      }

      // Unwrap Entity<Movie> → Movie values from an OrderedPage
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
          // before() uses DESC sort; last element = oldest returned → response before cursor
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
  }
  ```

- [ ] **Step 2: Run the test to verify it fails (MovieOrderedLister does not exist yet)**

  ```bash
  mvn test -pl poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain \
    -Dtest=MovieOrderedListerTest
  ```
  Expected: FAIL with compilation error (cannot find symbol `MovieOrderedLister`)

- [ ] **Step 3: Create `MovieOrderedLister.java`**

  Create `poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MovieOrderedLister.java`:

  ```java
  package org.codingmatters.poom.demo.domain;

  import org.codingmatters.poom.apis.demo.api.types.Movie;
  import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
  import org.codingmatters.poom.services.domain.entities.Entity;
  import org.codingmatters.poom.services.domain.entities.PagedEntityList;
  import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
  import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
  import org.codingmatters.poom.services.domain.repositories.Repository;

  import java.time.LocalDate;
  import java.util.ArrayList;
  import java.util.Collections;
  import java.util.List;
  import java.util.Optional;
  import java.util.stream.Collectors;

  public class MovieOrderedLister implements PagedCollectionAdapter.OrderedLister<Movie> {

      private static final String ASC_SORT = "facts.releaseDate asc, id asc";
      private static final String DESC_SORT = "facts.releaseDate desc, id desc";

      private final Repository<Movie, PropertyQuery> repository;

      public MovieOrderedLister(Repository<Movie, PropertyQuery> repository) {
          this.repository = repository;
      }

      @Override
      public PagedCollectionAdapter.OrderedPage<Movie> initLatest(Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
          List<Entity<Movie>> all = fetchAll(ASC_SORT, userQuery);
          long total = all.size();
          if (total == 0) return emptyPage();
          long from = Math.max(0, total - end - 1);
          long to = total - start - 1;
          if (from > to) return emptyPage();
          List<Entity<Movie>> page = all.subList((int) from, (int) to + 1);
          return orderedPage(
              toPagedList(page, from, to, total),
              Optional.of(cursorFor(page.get(page.size() - 1))),
              Optional.of(cursorFor(page.get(0)))
          );
      }

      @Override
      public PagedCollectionAdapter.OrderedPage<Movie> initOldest(Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
          List<Entity<Movie>> all = fetchAll(ASC_SORT, userQuery);
          long total = all.size();
          if (total == 0) return emptyPage();
          long to = Math.min(end, total - 1);
          if (start > to) return emptyPage();
          List<Entity<Movie>> page = all.subList((int) start, (int) to + 1);
          return orderedPage(
              toPagedList(page, start, to, total),
              Optional.of(cursorFor(page.get(page.size() - 1))),
              Optional.empty()
          );
      }

      @Override
      public PagedCollectionAdapter.OrderedPage<Movie> since(String since, Optional<String> optBefore, Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
          String[] sinceParts = parseCursor(since);
          List<Entity<Movie>> all = fetchAll(ASC_SORT, userQuery);
          List<Entity<Movie>> filtered = all.stream()
              .filter(e -> isAfterCursor(e.value(), sinceParts))
              .filter(e -> optBefore.map(b -> isBeforeCursor(e.value(), parseCursor(b))).orElse(true))
              .collect(Collectors.toList());
          long total = filtered.size();
          if (total == 0 || start >= total) return orderedPage(emptyPagedList(), Optional.empty(), optBefore);
          long to = Math.min(end, total - 1);
          List<Entity<Movie>> page = filtered.subList((int) start, (int) to + 1);
          return orderedPage(
              toPagedList(page, start, to, total),
              Optional.of(cursorFor(page.get(page.size() - 1))),
              optBefore
          );
      }

      @Override
      public PagedCollectionAdapter.OrderedPage<Movie> before(String before, Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
          String[] beforeParts = parseCursor(before);
          List<Entity<Movie>> all = fetchAll(DESC_SORT, userQuery);
          List<Entity<Movie>> filtered = all.stream()
              .filter(e -> isBeforeCursor(e.value(), beforeParts))
              .collect(Collectors.toList());
          long total = filtered.size();
          if (total == 0 || start >= total) return emptyPage();
          long to = Math.min(end, total - 1);
          List<Entity<Movie>> page = filtered.subList((int) start, (int) to + 1);
          return orderedPage(
              toPagedList(page, start, to, total),
              Optional.empty(),
              Optional.of(cursorFor(page.get(page.size() - 1)))
          );
      }

      private List<Entity<Movie>> fetchAll(String sort, Optional<PropertyQuery> userQuery) throws RepositoryException {
          PropertyQuery.Builder builder = PropertyQuery.builder().sort(sort);
          userQuery.flatMap(q -> q.opt().filter()).filter(f -> !f.isEmpty()).ifPresent(builder::filter);
          return new ArrayList<>(this.repository.search(builder.build(), 0, 9999));
      }

      private String cursorFor(Entity<Movie> entity) {
          Movie m = entity.value();
          return m.facts().releaseDate().toString() + "|" + m.id();
      }

      private String[] parseCursor(String cursor) {
          return cursor.split("\\|", 2);
      }

      private boolean isAfterCursor(Movie movie, String[] cursorParts) {
          LocalDate cursorDate = LocalDate.parse(cursorParts[0]);
          int dateCmp = movie.facts().releaseDate().compareTo(cursorDate);
          if (dateCmp != 0) return dateCmp > 0;
          return movie.id().compareTo(cursorParts[1]) > 0;
      }

      private boolean isBeforeCursor(Movie movie, String[] cursorParts) {
          LocalDate cursorDate = LocalDate.parse(cursorParts[0]);
          int dateCmp = movie.facts().releaseDate().compareTo(cursorDate);
          if (dateCmp != 0) return dateCmp < 0;
          return movie.id().compareTo(cursorParts[1]) < 0;
      }

      private PagedEntityList<Movie> toPagedList(List<Entity<Movie>> items, long startIndex, long endIndex, long total) {
          return new PagedEntityList.DefaultPagedEntityList<>(startIndex, endIndex, total, items);
      }

      private PagedEntityList<Movie> emptyPagedList() {
          return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, Collections.emptyList());
      }

      private PagedCollectionAdapter.OrderedPage<Movie> emptyPage() {
          return orderedPage(emptyPagedList(), Optional.empty(), Optional.empty());
      }

      private PagedCollectionAdapter.OrderedPage<Movie> orderedPage(PagedEntityList<Movie> list, Optional<String> since, Optional<String> before) {
          return new PagedCollectionAdapter.OrderedPage<Movie>() {
              @Override public PagedEntityList<Movie> list() { return list; }
              @Override public Optional<String> since() { return since; }
              @Override public Optional<String> before() { return before; }
          };
      }
  }
  ```

- [ ] **Step 4: Run the tests**

  ```bash
  mvn test -pl poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain \
    -Dtest=MovieOrderedListerTest
  ```
  Expected: all 18 tests PASS.

- [ ] **Step 5: Run the full domain module tests (regression check)**

  ```bash
  mvn test -pl poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain
  ```
  Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 6: Commit**

  ```bash
  git add \
    poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MovieOrderedLister.java \
    poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/test/java/org/codingmatters/poom/demo/domain/MovieOrderedListerTest.java
  git commit -m "feat(demo): add MovieOrderedLister with cursor-based ordered browsing"
  ```

---

## Task 3: Wire `MovieOrderedLister` into `MoviePager`

Override `orderedLister()` in `MoviePager` so that ordered browsing requests on `/{store}/movies` are dispatched to `MovieOrderedLister`.

**Files:**
- Modify: `poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MoviePager.java`

The current `MoviePager` is at `/home/nel/workspaces/poom/poom-services/poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MoviePager.java`. It has a field `private final Repository<Movie, PropertyQuery> repository` and implements `PagedCollectionAdapter.Pager<Movie>`. The `orderedLister()` default returns `null` — we override it here.

- [ ] **Step 1: Add a test to `MoviePagerTest` verifying `orderedLister()` returns a `MovieOrderedLister`**

  In `poom-services-demo-domain/src/test/java/org/codingmatters/poom/demo/domain/MoviePagerTest.java`, add at the end of the class:

  ```java
  @Test
  public void orderedListerIsMovieOrderedLister() {
      assertThat(
          new MoviePager(this.repository, null).orderedLister(),
          is(instanceOf(MovieOrderedLister.class))
      );
  }
  ```

  Add import at the top of the file:
  ```java
  import static org.hamcrest.Matchers.instanceOf;
  ```

- [ ] **Step 2: Run the test to verify it fails**

  ```bash
  mvn test -pl poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain \
    -Dtest=MoviePagerTest#orderedListerIsMovieOrderedLister
  ```
  Expected: FAIL (orderedLister() returns null, not MovieOrderedLister)

- [ ] **Step 3: Override `orderedLister()` in `MoviePager`**

  In `MoviePager.java`, add the override method after the `lister()` method (after line 35):

  ```java
  @Override
  public PagedCollectionAdapter.OrderedLister<Movie> orderedLister() {
      return new MovieOrderedLister(this.repository);
  }
  ```

  The full `MoviePager.java` after this change:

  ```java
  package org.codingmatters.poom.demo.domain;

  import org.codingmatters.poom.apis.demo.api.types.Movie;
  import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
  import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
  import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
  import org.codingmatters.poom.services.domain.repositories.EntityLister;
  import org.codingmatters.poom.services.domain.repositories.Repository;
  import org.codingmatters.poom.services.domain.entities.PagedEntityList;

  import java.util.Optional;

  public class MoviePager implements PagedCollectionAdapter.Pager<Movie>, EntityLister<Movie, PropertyQuery> {
      private final Repository<Movie, PropertyQuery> repository;
      private final Optional<Movie.Category> category;

      public MoviePager(Repository<Movie, PropertyQuery> repository, Movie.Category category) {
          this.category = Optional.ofNullable(category);
          this.repository = repository;
      }

      @Override
      public String unit() {
          return Movie.class.getSimpleName();
      }

      @Override
      public int maxPageSize() {
          return 1000;
      }

      @Override
      public EntityLister<Movie, PropertyQuery> lister() {
          return this;
      }

      @Override
      public PagedCollectionAdapter.OrderedLister<Movie> orderedLister() {
          return new MovieOrderedLister(this.repository);
      }

      @Override
      public PagedEntityList<Movie> all(long startIndex, long endIndex) throws RepositoryException {
          if(this.category.isPresent()) {
              return this.repository.search(PropertyQuery.builder().filter(this.categoryFilter()).build(), startIndex, endIndex);
          } else {
              return this.repository.all(startIndex, endIndex);
          }
      }

      private String categoryFilter() {
          return String.format("category == '%s'", this.category.get().name());
      }

      @Override
      public PagedEntityList<Movie> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
          if(! this.category.isPresent()) {
              return this.repository.search(query, startIndex, endIndex);
          } else {
              if(query.opt().filter().orElse("").isEmpty()) {
                  return this.repository.search(query.withFilter(this.categoryFilter()), startIndex, endIndex);
              } else {
                  return this.repository.search(query.withFilter(this.categoryFilter() + " && ( " + query.filter() + " )"), startIndex, endIndex);
              }
          }
      }
  }
  ```

- [ ] **Step 4: Run the full domain module tests**

  ```bash
  mvn test -pl poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain
  ```
  Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 5: Full demo build with tests**

  ```bash
  mvn install -pl poom-services-demo/poom-services-advanced-demo -am
  ```
  Expected: BUILD SUCCESS. This verifies the generated browse handler compiles and links against the new RAML types and the wired `MovieOrderedLister`.

- [ ] **Step 6: Commit**

  ```bash
  git add \
    poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/main/java/org/codingmatters/poom/demo/domain/MoviePager.java \
    poom-services-demo/poom-services-advanced-demo/poom-services-demo-domain/src/test/java/org/codingmatters/poom/demo/domain/MoviePagerTest.java
  git commit -m "feat(demo): wire MovieOrderedLister into MoviePager"
  ```

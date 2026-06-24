# Paged Collection Browse — Ordered Headers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add cursor-based ordered browsing (`since`, `before`, `init-ordered` headers) to the paged collection browse handler generator without breaking existing offset-based browsing.

**Architecture:** Two new nested interfaces (`OrderedPage`, `OrderedLister`) are added to `PagedCollectionAdapter`. `Pager` gets a `default orderedLister()` method returning `null` (no breaking change). `BrowseHandlerGenerator` adds a cursor branch before the existing `Rfc7233Pager` offset branch — if any cursor header is present the generated handler routes to the `OrderedLister`, otherwise falls through to the existing logic.

**Tech Stack:** Java, JavaPoet (code generation), Maven, JUnit 4, poom-api-specs 1.44.0

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `poom-services-paged-collection-domain/src/main/java/org/codingmatters/poom/generic/resource/domain/PagedCollectionAdapter.java` | Add `OrderedPage`, `OrderedLister` nested interfaces; `default orderedLister()` on `Pager` |
| Modify | `poom-services-paged-collection-generation-generators/src/test/resources/test-api-spec.raml` | Bump poom-api-specs `1.36.0` → `1.44.0` to get `before`/`initOrdered` on generated request/response types |
| Modify | `poom-services-paged-collection-api-spec/src/main/resources/generic-resource.raml` | Same version bump for the canonical API spec |
| Modify | `poom-services-paged-collection-generation-generators/src/test/java/…/test/TestPager.java` | Add constructor variant accepting `OrderedLister`, override `orderedLister()` |
| Create | `poom-services-paged-collection-generation-generators/src/test/java/…/test/TestOrderedLister.java` | Records which method was called and with what args; returns configurable `OrderedPage` |
| Modify | `poom-services-paged-collection-generation-generators/src/test/java/…/BrowseHandlerGeneratorTest.java` | New test cases for cursor routing, error cases, response headers |
| Modify | `poom-services-paged-collection-generation-generators/src/main/java/…/BrowseHandlerGenerator.java` | Add `orderedBrowsingBlock()`, helper methods, updated `applyBody()` and `privateMethods()` |

Full paths (rooted at `poom-services/`):
- `poom-services-paged-collection/poom-services-paged-collection-domain/src/main/java/org/codingmatters/poom/generic/resource/domain/PagedCollectionAdapter.java`
- `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/resources/test-api-spec.raml`
- `poom-services-paged-collection/poom-services-paged-collection-api/poom-services-paged-collection-api-spec/src/main/resources/generic-resource.raml`
- `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/test/TestPager.java`
- `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/test/TestOrderedLister.java`
- `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/BrowseHandlerGeneratorTest.java`
- `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/main/java/org/codingmatters/poom/paged/collection/generation/generators/source/BrowseHandlerGenerator.java`

---

## Task 1 — Bump poom-api-specs to 1.44.0 in RAML specs

**Why:** The `test-api-spec.raml` and `generic-resource.raml` reference poom-api-specs `1.36.0`. Version `1.44.0` adds `before` and `init-ordered` request headers and `before` response header to the `rfc7233Browsing` trait. The generated test types must have `before()` and `initOrdered()` methods for the generator tests to compile.

**Files:**
- Modify: `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/resources/test-api-spec.raml`
- Modify: `poom-services-paged-collection/poom-services-paged-collection-api/poom-services-paged-collection-api-spec/src/main/resources/generic-resource.raml`

- [ ] **Step 1: Update test-api-spec.raml**

Replace every occurrence of `1.36.0` with `1.44.0` in `test-api-spec.raml` (5 occurrences — annotationTypes, Message, Error, BatchCreateResponse, rfc7233Browsing trait).

- [ ] **Step 2: Update generic-resource.raml**

Replace every occurrence of `1.36.0` with `1.44.0` in `generic-resource.raml` (all trait and type includes).

- [ ] **Step 3: Regenerate test sources and verify**

```bash
cd /home/nel/workspaces/poom/poom-services
mvn generate-test-sources -pl poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators -am -DskipTests
```

Then check that the generated `NoParamsGetRequest.java` has `before()` and `initOrdered()`:

```bash
grep -n "before\|initOrdered" poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/target/generated-test-sources/org/generated/api/NoParamsGetRequest.java
```

Expected: lines containing `String before();`, `String initOrdered();`, `builder.before(...)`, `builder.initOrdered(...)`.

Also verify `Status200` and `Status206` have `before()`:

```bash
grep -n "before" poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/target/generated-test-sources/org/generated/api/noparamsgetresponse/Status200.java
```

Expected: lines containing `String before();` and `Builder.before(...)`.

- [ ] **Step 4: Commit**

```bash
git add poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/resources/test-api-spec.raml
git add poom-services-paged-collection/poom-services-paged-collection-api/poom-services-paged-collection-api-spec/src/main/resources/generic-resource.raml
git commit -m "chore: bump poom-api-specs to 1.44.0 in RAML specs (adds before/init-ordered browse headers)"
```

---

## Task 2 — Add `OrderedPage` and `OrderedLister` to `PagedCollectionAdapter`

**Files:**
- Modify: `poom-services-paged-collection/poom-services-paged-collection-domain/src/main/java/org/codingmatters/poom/generic/resource/domain/PagedCollectionAdapter.java`

- [ ] **Step 1: Write the failing compilation check**

Before editing, verify the module builds:

```bash
cd /home/nel/workspaces/poom/poom-services
mvn install -pl poom-services-paged-collection/poom-services-paged-collection-domain -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: Add the two interfaces and the default method**

Add the following to `PagedCollectionAdapter.java`. The two new interfaces go **inside** the `PagedCollectionAdapter` interface body, after the existing `Pager` interface. The `default orderedLister()` method goes **inside** `Pager`.

```java
// At the top of PagedCollectionAdapter.java, add this import:
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import java.util.Optional;
```

New `OrderedPage` interface (nested in `PagedCollectionAdapter`):

```java
interface OrderedPage<EntityType> {
    PagedEntityList<EntityType> list();
    Optional<String> since();
    Optional<String> before();
}
```

New `OrderedLister` interface (nested in `PagedCollectionAdapter`):

```java
interface OrderedLister<EntityType> {
    /**
     * Returns the last elements of the collection, ordered oldest-to-newest.
     * Response since: cursor before the first returned element (for forward paging).
     * Response before: cursor after the last returned element (for backward paging).
     */
    OrderedPage<EntityType> initLatest(Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;

    /**
     * Returns the first elements of the collection, ordered oldest-to-newest.
     * Response since: cursor before the first returned element (for forward paging).
     * Response before: Optional.empty() — not meaningful for head initialization.
     */
    OrderedPage<EntityType> initOldest(Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;

    /**
     * Returns elements after the since cursor, ordered oldest-to-newest.
     * optionalBefore, if present, acts as an exclusive upper bound (filter only, ordering unchanged).
     * Response since: cursor after the last returned element (for continued forward paging).
     * Response before: value of optionalBefore if present, otherwise Optional.empty().
     */
    OrderedPage<EntityType> since(String since, Optional<String> before,
                                   Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;

    /**
     * Returns elements before the before cursor, ordered newest-to-oldest.
     * Response before: cursor before the oldest returned element (for continued backward paging).
     * Response since: Optional.empty().
     */
    OrderedPage<EntityType> before(String before,
                                    Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;
}
```

Updated `Pager` interface — add the `default orderedLister()` method:

```java
interface Pager<EntityType> {
    String unit();
    int maxPageSize();
    default int defaultPageSize() { return this.maxPageSize(); }
    EntityLister<EntityType, PropertyQuery> lister();
    /** Returns null if this pager does not support cursor-based ordered browsing. */
    default OrderedLister<EntityType> orderedLister() { return null; }
}
```

Also add this import at the top of `PagedCollectionAdapter.java`:

```java
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
```

(Check if it's already present — it may not be since the current domain module doesn't use it directly.)

- [ ] **Step 3: Build the domain module**

```bash
cd /home/nel/workspaces/poom/poom-services
mvn install -pl poom-services-paged-collection/poom-services-paged-collection-domain -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add poom-services-paged-collection/poom-services-paged-collection-domain/src/main/java/org/codingmatters/poom/generic/resource/domain/PagedCollectionAdapter.java
git commit -m "feat: add OrderedPage and OrderedLister to PagedCollectionAdapter"
```

---

## Task 3 — Create `TestOrderedLister`

**Files:**
- Create: `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/test/TestOrderedLister.java`

- [ ] **Step 1: Create the file**

```java
package org.codingmatters.poom.paged.collection.generation.generators.source.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.generated.api.types.Entity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class TestOrderedLister implements PagedCollectionAdapter.OrderedLister<Entity> {

    public enum Method { INIT_LATEST, INIT_OLDEST, SINCE, BEFORE }

    public static class LastCall {
        public final Method method;
        public final String cursor;      // since value for Method.SINCE, before value for Method.BEFORE, null for INIT_*
        public final Optional<String> optBefore;  // only meaningful for Method.SINCE
        public final Optional<PropertyQuery> query;
        public final long start;
        public final long end;

        public LastCall(Method method, String cursor, Optional<String> optBefore,
                        Optional<PropertyQuery> query, long start, long end) {
            this.method = method;
            this.cursor = cursor;
            this.optBefore = optBefore;
            this.query = query;
            this.start = start;
            this.end = end;
        }
    }

    public final AtomicReference<LastCall> lastCall = new AtomicReference<>();
    private PagedCollectionAdapter.OrderedPage<Entity> result;

    public TestOrderedLister(PagedCollectionAdapter.OrderedPage<Entity> result) {
        this.result = result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> initLatest(Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.INIT_LATEST, null, Optional.empty(), query, start, end));
        return result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> initOldest(Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.INIT_OLDEST, null, Optional.empty(), query, start, end));
        return result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> since(String since, Optional<String> before,
                                                             Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.SINCE, since, before, query, start, end));
        return result;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Entity> before(String before,
                                                              Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException {
        lastCall.set(new LastCall(Method.BEFORE, before, Optional.empty(), query, start, end));
        // cursor field = before value; optBefore is empty (not applicable for the 'before' method)
        return result;
    }

    /** Convenience factory for a minimal OrderedPage returning an empty list with given cursor values. */
    public static PagedCollectionAdapter.OrderedPage<Entity> orderedPage(
            PagedEntityList<Entity> list, String since, String before) {
        return new PagedCollectionAdapter.OrderedPage<Entity>() {
            @Override public PagedEntityList<Entity> list() { return list; }
            @Override public Optional<String> since() { return Optional.ofNullable(since); }
            @Override public Optional<String> before() { return Optional.ofNullable(before); }
        };
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd /home/nel/workspaces/poom/poom-services
mvn test-compile -pl poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/test/TestOrderedLister.java
git commit -m "test: add TestOrderedLister for cursor browse tests"
```

---

## Task 4 — Update `TestPager` to support `orderedLister()`

**Files:**
- Modify: `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/test/TestPager.java`

- [ ] **Step 1: Add orderedLister field and constructor variant**

Add to `TestPager`:
- A new field `private final PagedCollectionAdapter.OrderedLister<Entity> orderedLister;`
- Update the existing `(unit, lister, maxPageSize)` constructor to call `this(unit, lister, maxPageSize, maxPageSize, null)`
- Update the existing `(unit, lister, maxPageSize, defaultPageSize)` constructor to call `this(unit, lister, maxPageSize, defaultPageSize, null)`
- New full constructor:

```java
public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize,
                 int defaultPageSize, PagedCollectionAdapter.OrderedLister<Entity> orderedLister) {
    this.unit = unit;
    this.lister = lister;
    this.maxPageSize = maxPageSize;
    this.defaultPageSize = defaultPageSize;
    this.orderedLister = orderedLister;
}
```

- Override `orderedLister()`:

```java
@Override
public PagedCollectionAdapter.OrderedLister<Entity> orderedLister() {
    return this.orderedLister;
}
```

The full updated `TestPager.java`:

```java
package org.codingmatters.poom.paged.collection.generation.generators.source.test;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.generated.api.types.Entity;

public class TestPager implements PagedCollectionAdapter.Pager<Entity> {
    private final String unit;
    private final EntityLister<Entity, PropertyQuery> lister;
    private final int maxPageSize;
    private final int defaultPageSize;
    private final PagedCollectionAdapter.OrderedLister<Entity> orderedLister;

    public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize) {
        this(unit, lister, maxPageSize, maxPageSize, null);
    }

    public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize, int defaultPageSize) {
        this(unit, lister, maxPageSize, defaultPageSize, null);
    }

    public TestPager(String unit, EntityLister<Entity, PropertyQuery> lister, int maxPageSize,
                     int defaultPageSize, PagedCollectionAdapter.OrderedLister<Entity> orderedLister) {
        this.unit = unit;
        this.lister = lister;
        this.maxPageSize = maxPageSize;
        this.defaultPageSize = defaultPageSize;
        this.orderedLister = orderedLister;
    }

    @Override public String unit() { return this.unit; }
    @Override public int maxPageSize() { return this.maxPageSize; }
    @Override public int defaultPageSize() { return this.defaultPageSize; }
    @Override public EntityLister<Entity, PropertyQuery> lister() { return this.lister; }

    @Override
    public PagedCollectionAdapter.OrderedLister<Entity> orderedLister() {
        return this.orderedLister;
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd /home/nel/workspaces/poom/poom-services
mvn test-compile -pl poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/test/TestPager.java
git commit -m "test: update TestPager to support OrderedLister"
```

---

## Task 5 — Write failing tests for cursor browsing

**Files:**
- Modify: `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/BrowseHandlerGeneratorTest.java`

- [ ] **Step 1: Add test infrastructure — orderedLister helper and empty page constant**

At the top of `BrowseHandlerGeneratorTest`, after the existing `ENTITY_LISTER` field, add:

```java
private static final PagedEntityList<org.generated.api.types.Entity> EMPTY_LIST =
    new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, new LinkedList<>());

private static final PagedEntityList<org.generated.api.types.Entity> FULL_LIST =
    new PagedEntityList.DefaultPagedEntityList<>(0, 44, 45, new LinkedList<>());

private Function<NoParamsGetRequest, NoParamsGetResponse> handlerWithOrderedLister(
        TestOrderedLister orderedLister) {
    return this.handler((request) -> new TestAdapter(
        new TestPager("Unit", ENTITY_LISTER, 100, 100, orderedLister)
    ));
}
```

- [ ] **Step 2: Add routing tests**

Add these test methods to `BrowseHandlerGeneratorTest`:

```java
@Test
public void givenOrderedListerPresent__whenSinceHeader__thenOrderedListerSinceCalled() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(EMPTY_LIST, "cursor-since", null));
    this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().since("cursor-since").build());

    assertThat(orderedLister.lastCall.get().method, is(TestOrderedLister.Method.SINCE));
    assertThat(orderedLister.lastCall.get().cursor, is("cursor-since"));
    assertThat(orderedLister.lastCall.get().optBefore, is(Optional.empty()));
}

@Test
public void givenOrderedListerPresent__whenBeforeHeader__thenOrderedListerBeforeCalled() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(EMPTY_LIST, null, "cursor-before"));
    this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().before("cursor-before").build());

    assertThat(orderedLister.lastCall.get().method, is(TestOrderedLister.Method.BEFORE));
    assertThat(orderedLister.lastCall.get().cursor, is("cursor-before"));
}

@Test
public void givenOrderedListerPresent__whenInitOrderedLatest__thenInitLatestCalled() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(EMPTY_LIST, "s", "b"));
    this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().initOrdered("LATEST").build());

    assertThat(orderedLister.lastCall.get().method, is(TestOrderedLister.Method.INIT_LATEST));
}

@Test
public void givenOrderedListerPresent__whenInitOrderedOldest__thenInitOldestCalled() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(EMPTY_LIST, "s", null));
    this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().initOrdered("OLDEST").build());

    assertThat(orderedLister.lastCall.get().method, is(TestOrderedLister.Method.INIT_OLDEST));
}

@Test
public void givenOrderedListerPresent__whenSinceAndBefore__thenSinceCalledWithOptBefore() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(EMPTY_LIST, "s", "b"));
    this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().since("s").before("b").build());

    assertThat(orderedLister.lastCall.get().method, is(TestOrderedLister.Method.SINCE));
    assertThat(orderedLister.lastCall.get().cursor, is("s"));
    assertThat(orderedLister.lastCall.get().optBefore, is(Optional.of("b")));
}

@Test
public void givenNoCursorHeaders__thenRegularListerCalled_notOrderedLister() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(EMPTY_LIST, null, null));
    this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().build());

    assertThat(orderedLister.lastCall.get(), is(nullValue()));
    assertThat(lastRequest.get(), is(new ListerRequest(0L, 99L, null)));
}
```

- [ ] **Step 3: Add error case tests**

```java
@Test
public void givenCursorHeaderPresent__whenOrderedListerIsNull__then400() throws Exception {
    // TestPager without orderedLister (null by default)
    NoParamsGetResponse response = this.handler((request) ->
        new TestAdapter(new TestPager("Unit", ENTITY_LISTER, 100)))
        .apply(NoParamsGetRequest.builder().since("any").build());

    response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));
    assertThat(response.status400().payload().code(), is(Error.Code.BAD_REQUEST));
}

@Test
public void givenOrderedListerPresent__whenInvalidRange__then416() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(EMPTY_LIST, null, null));
    NoParamsGetResponse response = this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().since("s").range("not-a-range").build());

    response.opt().status416().orElseThrow(() -> new AssertionError("expected 416, got " + response));
}

@Test
public void givenOrderedListerPresent__whenQueryParsingException__then400() throws Exception {
    NoParamsGetResponse response = this.handler((request) -> new TestAdapter(new TestPager(
        "Unit", ENTITY_LISTER, 100, 100,
        new TestOrderedLister(null) {
            @Override
            public PagedCollectionAdapter.OrderedPage<org.generated.api.types.Entity> since(
                    String since, Optional<String> before, Optional<PropertyQuery> query,
                    long start, long end) throws RepositoryException {
                throw new RepositoryQueryParsingException("bad query");
            }
        }
    ))).apply(NoParamsGetRequest.builder().since("s").build());

    response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));
}

@Test
public void givenOrderedListerPresent__whenRepositoryException__then500() throws Exception {
    NoParamsGetResponse response = this.handler((request) -> new TestAdapter(new TestPager(
        "Unit", ENTITY_LISTER, 100, 100,
        new TestOrderedLister(null) {
            @Override
            public PagedCollectionAdapter.OrderedPage<org.generated.api.types.Entity> initLatest(
                    Optional<PropertyQuery> query, long start, long end) throws RepositoryException {
                throw new RepositoryException("boom");
            }
            @Override
            public PagedCollectionAdapter.OrderedPage<org.generated.api.types.Entity> since(
                    String since, Optional<String> before, Optional<PropertyQuery> query,
                    long start, long end) throws RepositoryException {
                throw new RepositoryException("boom");
            }
        }
    ))).apply(NoParamsGetRequest.builder().since("s").build());

    response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));
}

@Test
public void givenOrderedListerPresent__whenAccessDeniedException__then403() throws Exception {
    NoParamsGetResponse response = this.handler((request) -> new TestAdapter(new TestPager(
        "Unit", ENTITY_LISTER, 100, 100,
        new TestOrderedLister(null) {
            @Override
            public PagedCollectionAdapter.OrderedPage<org.generated.api.types.Entity> since(
                    String since, Optional<String> before, Optional<PropertyQuery> query,
                    long start, long end) throws RepositoryException {
                throw new RepositoryAccessDeniedException("denied");
            }
        }
    ))).apply(NoParamsGetRequest.builder().since("s").build());

    response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));
}
```

- [ ] **Step 4: Add response header tests**

```java
@Test
public void givenOrderedBrowse__whenOrderedPageHasCursors__thenResponseIncludesSinceAndBefore() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(FULL_LIST, "since-val", "before-val"));
    NoParamsGetResponse response = this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().since("cursor").build());

    response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));
    assertThat(response.status200().since(), is("since-val"));
    assertThat(response.status200().before(), is("before-val"));
}

@Test
public void givenOrderedBrowse__whenOrderedPageHasNoCursors__thenResponseHeadersAbsent() throws Exception {
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(FULL_LIST, null, null));
    NoParamsGetResponse response = this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().since("cursor").build());

    response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));
    assertThat(response.status200().since(), is(nullValue()));
    assertThat(response.status200().before(), is(nullValue()));
}

@Test
public void givenOrderedBrowse__whenPartialList__then206WithCursors() throws Exception {
    PagedEntityList<org.generated.api.types.Entity> partialList =
        new PagedEntityList.DefaultPagedEntityList<>(0, 99, 200, entities(100));
    TestOrderedLister orderedLister = new TestOrderedLister(
        TestOrderedLister.orderedPage(partialList, "s", "b"));
    NoParamsGetResponse response = this.handlerWithOrderedLister(orderedLister)
        .apply(NoParamsGetRequest.builder().since("cursor").build());

    response.opt().status206().orElseThrow(() -> new AssertionError("expected 206, got " + response));
    assertThat(response.status206().since(), is("s"));
    assertThat(response.status206().before(), is("b"));
}
```

Also add these missing imports to `BrowseHandlerGeneratorTest`:

```java
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.source.test.TestOrderedLister;
import org.codingmatters.poom.services.domain.exceptions.RepositoryAccessDeniedException;
import java.util.Optional;
```

- [ ] **Step 5: Run tests — expect failures**

```bash
cd /home/nel/workspaces/poom/poom-services
mvn test -pl poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators -am 2>&1 | grep -E "Tests run|FAIL|ERROR|BUILD"
```

Expected: new tests fail (methods not yet implemented), existing tests still pass.

- [ ] **Step 6: Commit**

```bash
git add poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/test/java/org/codingmatters/poom/paged/collection/generation/generators/source/BrowseHandlerGeneratorTest.java
git commit -m "test: add failing tests for cursor-based browse (since/before/init-ordered)"
```

---

## Task 6 — Implement cursor branching in `BrowseHandlerGenerator`

**Files:**
- Modify: `poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/main/java/org/codingmatters/poom/paged/collection/generation/generators/source/BrowseHandlerGenerator.java`

### What changes

1. Add imports: `Range`, `PagedEntityList`, `Optional`
2. Replace `applyBody()` with a version that: moves the `lister == null` check AFTER the new cursor block; calls `this.orderedBrowsingBlock()` before the offset path
3. Add `orderedBrowsingBlock()` — the new CodeBlock for cursor handling
4. Add helper methods: `orderedListerClass()`, `orderedPageClass()`, `entityListClass()`
5. Add `orderedBrowsingNotAllowed` to `privateMethods()`

- [ ] **Step 1: Add new imports to `BrowseHandlerGenerator`**

At the top of the file, the existing imports include `Rfc7233Pager`. Add:

```java
import org.codingmatters.poom.services.support.paging.Range;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import java.util.Optional;
```

- [ ] **Step 2: Replace `applyBody()`**

Replace the existing `applyBody()` method with:

```java
private CodeBlock applyBody() {
    return CodeBlock.builder()

            .addStatement("$T pager", this.pagerClass())
            .beginControlFlow("try")
                .addStatement("pager = this.provider.pager(request)")
            .nextControlFlow("catch($T e)", Exception.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "failed getting pager for ")
                .addStatement("return this.unexpectedError(token)")
            .endControlFlow()

            .beginControlFlow("if(pager == null)")
                .addStatement("$T token = log.tokenized().info($S, this.provider.getClass(), request)",
                        String.class, "provider {} has no pager, browsing method not allowed, request was : {}"
                )
                .addStatement("return this.browsingNotAllowed(token)")
            .endControlFlow()

            .beginControlFlow("if(pager.unit() == null)")
                .addStatement("$T token = log.tokenized().error($S, this.provider.getClass(), request)",
                        String.class, "provider {} implementation breaks contract, pager unit cannot be null, request was : {}"
                )
                .addStatement("return this.unexpectedError(token)")
            .endControlFlow()

            .beginControlFlow("if(pager.maxPageSize() <= 0)")
                .addStatement("$T token = log.tokenized().error($S, this.provider.getClass(), request)",
                        String.class, "provider {} implementation breaks contract, pager max page size  cannot be lower or equal to 0, request was : {}"
                )
                .addStatement("return this.unexpectedError(token)")
            .endControlFlow()

            // cursor path — inserted before lister check
            .add(this.orderedBrowsingBlock())

            // offset path — lister null check only reached when no cursor headers
            .beginControlFlow("if(pager.lister() == null)")
                .addStatement("$T token = log.tokenized().error($S, this.provider.getClass(), request)",
                        String.class, "provider {} implementation breaks contract, pager lister cannot be null, request was : {}"
                )
                .addStatement("return this.unexpectedError(token)")
            .endControlFlow()

            .addStatement("$T page", this.pageClass())
            .beginControlFlow("try")
                .addStatement("page = $T.forRequestedRange(request.range())" +
                                ".unit(pager.unit())" +
                                ".maxPageSize(pager.maxPageSize())" +
                                ".defaultPageSize(pager.defaultPageSize())" +
                                ".pager(pager.lister())" +
                                ".page(this.parseQuery(request))",
                        Rfc7233Pager.class
                )
            .nextControlFlow("catch($T e)", RepositoryQueryParsingException.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "query parsing failed while listing entities : ")
                .addStatement("return this.queryParsingError(token)")
            .nextControlFlow("catch($T e)", RepositoryAccessDeniedException.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "repository access denied while listing entities : ")
                .addStatement("return this.accessDeniedError(token)")
            .nextControlFlow("catch($T e)", RepositoryException.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "unexpected error listing entities : ")
                .addStatement("return this.unexpectedError(token)")
            .endControlFlow()

            .beginControlFlow("if(! page.isValid())")
                .addStatement("$T token = log.tokenized().info($S, request)",
                        String.class, "illegal search for entities, request was {}"
                )
                .addStatement("return $T.builder().status416($T.builder()" +
                        ".acceptRange(page.acceptRange())" +
                        ".contentRange(page.contentRange())" +
                        ".payload($T.builder()" +
                            ".code($T.Code.ILLEGAL_RANGE_SPEC)" +
                            ".token(token)" +
                            ".messages(" +
                                "$T.builder().key($S).args(request.range(), request.filter(), request.orderBy()).build()," +
                                "$T.builder().key($S).args(token).build()" +
                            ")" +
                            ".build())" +
                        ".build()).build()",
                        this.className(this.collectionDescriptor.browse().responseValueObject()),
                        this.relatedClassName("Status416", this.collectionDescriptor.browse().responseValueObject()),
                        this.className(this.collectionDescriptor.types().error()),
                        this.className(this.collectionDescriptor.types().error()),
                        this.className(this.collectionDescriptor.types().message()), MessageKeys.ILLEGAL_SEARCH_QUERY,
                        this.className(this.collectionDescriptor.types().message()), MessageKeys.SEE_LOGS_WITH_TOKEN
                )
            .endControlFlow()

            .beginControlFlow("if(page.isPartial())")
                .addStatement("return $T.builder().status206($T.builder()" +
                        ".acceptRange(page.acceptRange())" +
                        ".contentRange(page.contentRange())" +
                        ".xEntityType(pager.unit())" +
                        ".payload(page.list().valueList())" +
                        ".build()).build()",
                        this.className(this.collectionDescriptor.browse().responseValueObject()),
                        this.relatedClassName("Status206", this.collectionDescriptor.browse().responseValueObject())
                )
            .nextControlFlow("else")
                .addStatement("return $T.builder().status200($T.builder()" +
                            ".acceptRange(page.acceptRange())" +
                            ".contentRange(page.contentRange())" +
                            ".xEntityType(pager.unit())" +
                            ".payload(page.list().valueList())" +
                            ".build()).build()",
                    this.className(this.collectionDescriptor.browse().responseValueObject()),
                    this.relatedClassName("Status200", this.collectionDescriptor.browse().responseValueObject())
                )
            .endControlFlow()
            .build();
}
```

- [ ] **Step 3: Add `orderedBrowsingBlock()`**

Add this new private method to `BrowseHandlerGenerator`:

```java
private CodeBlock orderedBrowsingBlock() {
    return CodeBlock.builder()
        .beginControlFlow(
            "if(request.opt().since().isPresent() || request.opt().before().isPresent() || request.opt().initOrdered().isPresent())")

            .addStatement("$T orderedLister = pager.orderedLister()", this.orderedListerClass())
            .beginControlFlow("if(orderedLister == null)")
                .addStatement("$T token = log.tokenized().info($S, this.provider.getClass(), request)",
                    String.class, "provider {} does not support ordered browsing, request was : {}")
                .addStatement("return this.orderedBrowsingNotAllowed(token)")
            .endControlFlow()

            .addStatement("$T range = $T.fromRequestedRange(request.range(), pager.maxPageSize(), pager.defaultPageSize())",
                Range.class, Range.class)
            .beginControlFlow("if(! range.isValid())")
                .addStatement("$T token = log.tokenized().info($S, request)",
                    String.class, "illegal range for ordered browsing, request was {}")
                .addStatement("return $T.builder().status416($T.builder()" +
                    ".acceptRange($T.format($S, pager.unit(), pager.maxPageSize()))" +
                    ".contentRange($T.format($S, pager.unit()))" +
                    ".payload($T.builder()" +
                        ".code($T.Code.ILLEGAL_RANGE_SPEC)" +
                        ".token(token)" +
                        ".messages(" +
                            "$T.builder().key($S).args(request.range(), request.filter(), request.orderBy()).build()," +
                            "$T.builder().key($S).args(token).build()" +
                        ")" +
                        ".build())" +
                    ".build()).build()",
                    this.className(this.collectionDescriptor.browse().responseValueObject()),
                    this.relatedClassName("Status416", this.collectionDescriptor.browse().responseValueObject()),
                    String.class, "%s %d",
                    String.class, "%s */%d",
                    this.className(this.collectionDescriptor.types().error()),
                    this.className(this.collectionDescriptor.types().error()),
                    this.className(this.collectionDescriptor.types().message()), MessageKeys.ILLEGAL_SEARCH_QUERY,
                    this.className(this.collectionDescriptor.types().message()), MessageKeys.SEE_LOGS_WITH_TOKEN
                )
            .endControlFlow()

            .addStatement("$T orderedPage", this.orderedPageClass())
            .beginControlFlow("try")
                .addStatement("$T<$T> query = this.parseQuery(request)", Optional.class, PropertyQuery.class)
                .beginControlFlow("if(request.opt().initOrdered().isPresent())")
                    .beginControlFlow("if($S.equals(request.initOrdered()))", "LATEST")
                        .addStatement("orderedPage = orderedLister.initLatest(query, range.start(), range.end())")
                    .nextControlFlow("else")
                        .addStatement("orderedPage = orderedLister.initOldest(query, range.start(), range.end())")
                    .endControlFlow()
                .nextControlFlow("else if(request.opt().since().isPresent())")
                    .addStatement("orderedPage = orderedLister.since(request.since(), $T.ofNullable(request.before()), query, range.start(), range.end())",
                        Optional.class)
                .nextControlFlow("else")
                    .addStatement("orderedPage = orderedLister.before(request.before(), query, range.start(), range.end())")
                .endControlFlow()
            .nextControlFlow("catch($T e)", RepositoryQueryParsingException.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "query parsing failed during ordered browsing : ")
                .addStatement("return this.queryParsingError(token)")
            .nextControlFlow("catch($T e)", RepositoryAccessDeniedException.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "repository access denied during ordered browsing : ")
                .addStatement("return this.accessDeniedError(token)")
            .nextControlFlow("catch($T e)", RepositoryException.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "unexpected error during ordered browsing : ")
                .addStatement("return this.unexpectedError(token)")
            .endControlFlow()

            .addStatement("$T orderedList = orderedPage.list()", this.entityListClass())
            .addStatement("$T orderedContentRange = $T.format($S, pager.unit(), orderedList.startIndex(), orderedList.endIndex(), orderedList.total())",
                String.class, String.class, "%s %d-%d/%d")
            .addStatement("$T orderedAcceptRange = $T.format($S, pager.unit(), pager.maxPageSize())",
                String.class, String.class, "%s %d")

            .beginControlFlow("if(orderedList.endIndex() < orderedList.total() - 1)")
                .addStatement("$T orderedStatus206Builder = $T.builder()" +
                    ".acceptRange(orderedAcceptRange)" +
                    ".contentRange(orderedContentRange)" +
                    ".xEntityType(pager.unit())" +
                    ".payload(orderedList.valueList())",
                    this.relatedClassName("Status206", this.collectionDescriptor.browse().responseValueObject()),
                    this.relatedClassName("Status206", this.collectionDescriptor.browse().responseValueObject())
                )
                .beginControlFlow("if(orderedPage.since().isPresent())")
                    .addStatement("orderedStatus206Builder.since(orderedPage.since().get())")
                .endControlFlow()
                .beginControlFlow("if(orderedPage.before().isPresent())")
                    .addStatement("orderedStatus206Builder.before(orderedPage.before().get())")
                .endControlFlow()
                .addStatement("return $T.builder().status206(orderedStatus206Builder.build()).build()",
                    this.className(this.collectionDescriptor.browse().responseValueObject()))
            .nextControlFlow("else")
                .addStatement("$T orderedStatus200Builder = $T.builder()" +
                    ".acceptRange(orderedAcceptRange)" +
                    ".contentRange(orderedContentRange)" +
                    ".xEntityType(pager.unit())" +
                    ".payload(orderedList.valueList())",
                    this.relatedClassName("Status200", this.collectionDescriptor.browse().responseValueObject()),
                    this.relatedClassName("Status200", this.collectionDescriptor.browse().responseValueObject())
                )
                .beginControlFlow("if(orderedPage.since().isPresent())")
                    .addStatement("orderedStatus200Builder.since(orderedPage.since().get())")
                .endControlFlow()
                .beginControlFlow("if(orderedPage.before().isPresent())")
                    .addStatement("orderedStatus200Builder.before(orderedPage.before().get())")
                .endControlFlow()
                .addStatement("return $T.builder().status200(orderedStatus200Builder.build()).build()",
                    this.className(this.collectionDescriptor.browse().responseValueObject()))
            .endControlFlow()

        .endControlFlow()
        .build();
}
```

- [ ] **Step 4: Add three helper type methods**

Add these three private methods to `BrowseHandlerGenerator`:

```java
private ParameterizedTypeName orderedListerClass() {
    return ParameterizedTypeName.get(
            ClassName.get(PagedCollectionAdapter.OrderedLister.class),
            this.className(this.collectionDescriptor.types().entity())
    );
}

private ParameterizedTypeName orderedPageClass() {
    return ParameterizedTypeName.get(
            ClassName.get(PagedCollectionAdapter.OrderedPage.class),
            this.className(this.collectionDescriptor.types().entity())
    );
}

private ParameterizedTypeName entityListClass() {
    return ParameterizedTypeName.get(
            ClassName.get(PagedEntityList.class),
            this.className(this.collectionDescriptor.types().entity())
    );
}
```

- [ ] **Step 5: Add `orderedBrowsingNotAllowed` to `privateMethods()`**

In `privateMethods()`, add this entry to the returned list:

```java
this.errorResponseMethod("orderedBrowsingNotAllowed", "Status400", "BAD_REQUEST")
```

The full updated list becomes:
```java
return Arrays.asList(
    MethodSpec.methodBuilder("parseQuery")...,
    this.errorResponseMethod("unexpectedError", "Status500", "UNEXPECTED_ERROR"),
    this.errorResponseMethod("queryParsingError", "Status400", "UNEXPECTED_ERROR"),
    this.errorResponseMethod("accessDeniedError", "Status403", "UNAUTHORIZED"),
    this.errorResponseMethod("browsingNotAllowed", "Status405", "COLLECTION_BROWSING_NOT_ALLOWED"),
    this.errorResponseMethod("orderedBrowsingNotAllowed", "Status400", "BAD_REQUEST")
);
```

- [ ] **Step 6: Run all tests**

```bash
cd /home/nel/workspaces/poom/poom-services
mvn test -pl poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators -am 2>&1 | tail -30
```

Expected: `BUILD SUCCESS`, all tests including new ones pass.

If tests fail with `cannot find symbol` on `request.before()` or `request.initOrdered()`, it means the test types were not regenerated — run `mvn generate-test-sources ... -am` first (Task 1, Step 3).

If tests fail with `cannot find symbol` on `PagedCollectionAdapter.OrderedLister`, the domain module was not installed — run `mvn install -pl poom-services-paged-collection/poom-services-paged-collection-domain -am -DskipTests` first (Task 2, Step 3).

- [ ] **Step 7: Commit**

```bash
git add poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators/src/main/java/org/codingmatters/poom/paged/collection/generation/generators/source/BrowseHandlerGenerator.java
git commit -m "feat: add cursor-based ordered browse (since/before/init-ordered) to BrowseHandlerGenerator"
```

---

## Task 7 — Full build verification

- [ ] **Step 1: Build the entire paged-collection sub-tree**

```bash
cd /home/nel/workspaces/poom/poom-services
mvn install -pl poom-services-paged-collection -am 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: Verify existing tests still pass**

```bash
mvn test -pl poom-services-paged-collection/poom-services-paged-collection-generation/poom-services-paged-collection-generation-generators -am 2>&1 | grep -E "Tests run"
```

Expected: no FAILURES, no ERRORS. The count should include the new test methods.

---

## Notes for implementors

**poom-api-specs version bump is a one-way door for downstream APIs.** After this change, `BrowseHandlerGenerator` generates handler code that calls `request.before()` and `request.initOrdered()`. Any API that uses `rfc7233Browsing` trait and hasn't updated to poom-api-specs `1.44.0` will get a compilation error when regenerating their handler. This is intentional — update the API's RAML includes to `1.44.0` to resolve it.

**`orderedBrowsingBlock()` contains a `return` on every path.** The outer `if` block for cursor detection ends with a `return` on every branch (orderedLister null → 400, invalid range → 416, exception → 5xx, and the 200/206 response). So the offset path (`Rfc7233Pager`) is only reached when NO cursor header is present.

**`lister()` null check moved after cursor block.** A pager that supports only ordered browsing may legitimately return `null` from `lister()`. The null check is now only reached when offset browsing is needed (no cursor headers).

package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.test.Ref;
import org.codingmatters.test.Simple;
import org.codingmatters.test.simple.E;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class InMemoryRepositoryWithPropertyQueryTest {

    private Repository<Simple, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Simple.class);

    @Before
    public void setUp() throws Exception {
        for (long i = 0; i < 1000; i++) {
            this.repository.create(Simple.builder()
                    .a(i)
                    .e(E.builder()
                            .f("" + i)
                            .g(i % 2 == 0 ? "" + i : null)
                            .h(i)
                            .build())
                    .ref(Ref.builder().a(i).build())
                    .build());
        }
    }

    @Test
    public void search() throws Exception {
        assertThat(this.repository.search(PropertyQuery.builder().filter("a >= 500").build(), 0, 1000).total(), is(500L));
    }

    @Test
    public void searchNested() throws Exception {
        assertThat(this.repository.search(PropertyQuery.builder().filter("e.f starts with 50").build(), 0, 1000).total(), is(11L));
    }

    @Test
    public void givenSearchingNested__whenAnEntityHasNullFirstLevel__thenNoException() throws Exception {
        this.repository.create(Simple.builder().build());
        this.repository.search(PropertyQuery.builder().filter("e.f starts with 50").build(), 0, 1000);
    }
    @Test
    public void givenSearchingNested__whenAnEntityHasNullSecondLevel__thenNoException() throws Exception {
        this.repository.create(Simple.builder().e(E.builder().build()).build());
        this.repository.search(PropertyQuery.builder().filter("e.f starts with 50").build(), 0, 1000);
    }

    @Test
    public void givenSearchingObjectProperty__whenAnEntityHasNullFirstLevel__thenNoException() throws Exception {
        this.repository.search(PropertyQuery.builder().filter("o.nested == 12").build(), 0, 1000);
    }

    @Test
    public void givenSearchingObjectProperty__whenAnEntityHasNullSecondLevel__thenNoException() throws Exception {
        this.repository.create(Simple.builder().o(ObjectValue.builder().build()).build());
        this.repository.search(PropertyQuery.builder().filter("o.nested == 12").build(), 0, 1000);
    }

    @Test
    public void givenPropertyIsNestedAndNumeric__whenSortingWithLowerCaseDesc__thenResultsAreSortedDescNumerically() throws Exception {
        System.out.println(this.repository.search(PropertyQuery.builder().sort("e.h desc").build(), 0, 2).stream().map(e -> e.value().e().f()).collect(Collectors.toList()));
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("e.h desc").build(), 0, 2).stream().map(e -> e.value().e().h()).collect(Collectors.toList()),
                contains(999L, 998L, 997L)
        );
    }

    @Test
    public void givenPropertyIsNestedAndString__whenSortingOnNullWithDesc__thenResultsAreSortedNullFirst() throws Exception {
        List<String> actual = this.repository.search(PropertyQuery.builder().sort("e.g DESC").build(), 0, 2).stream().map(e -> e.value().e().g()).collect(Collectors.toList());
        System.out.println(actual);
        assertThat(
                actual,
                contains(nullValue(), nullValue(), nullValue())
        );
    }

    @Test
    public void givenPropertyIsNestedAndString__whenSortingWithNullWithAsc__thenResultsAreSortedDescAlphabetically() throws Exception {
        System.out.println(this.repository.search(PropertyQuery.builder().sort("e.g ASC").build(), 0, 2).stream().map(e -> e.value().e().g()).collect(Collectors.toList()));
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("e.g ASC").build(), 0, 2).stream().map(e -> e.value().e().g()).collect(Collectors.toList()),
                contains("0", "10", "100")
        );
    }

    @Test
    public void givenPropertyIsNestedAndString__whenSortingWithLowerCaseDesc__thenResultsAreSortedDescAlphabetically() throws Exception {
        System.out.println(this.repository.search(PropertyQuery.builder().sort("e.f desc").build(), 0, 2).stream().map(e -> e.value().e().f()).collect(Collectors.toList()));
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("e.f desc").build(), 0, 2).stream().map(e -> e.value().e().f()).collect(Collectors.toList()),
                contains("999", "998", "997")
        );
    }

    @Test
    public void sortNestedAsc() throws Exception {
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("e.h asc").build(), 0, 2).stream().map(e -> e.value().e().h()).collect(Collectors.toList()),
                contains(0L, 1L, 2L)
        );
    }
    @Test
    public void givenPropertyIsNestedAndNumeric__whenSortingWithUpperCaseDesc__thenResultsAreSortedDescNumerically() throws Exception {
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("e.h DESC").build(), 0, 2).stream().map(e -> e.value().e().h()).collect(Collectors.toList()),
                contains(999L, 998L, 997L)
        );
    }

    @Test
    public void sortNestedASC() throws Exception {
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("e.h ASC").build(), 0, 2).stream().map(e -> e.value().e().h()).collect(Collectors.toList()),
                contains(0L, 1L, 2L)
        );
    }

    @Test
    public void givenNestedInReference__whenDesc__thenSortedDescending() throws Exception {
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("ref.a desc").build(), 0, 2).stream().map(e -> e.value().ref().a()).collect(Collectors.toList()),
                contains(999L, 998L, 997L)
        );
    }
    @Test
    public void givenNestedInReference__whenAsc__thenSortedAscending() throws Exception {
        assertThat(
                this.repository.search(PropertyQuery.builder().sort("ref.a asc").build(), 0, 2).stream().map(e -> e.value().ref().a()).collect(Collectors.toList()),
                contains(0L, 1L, 2L)
        );
    }

    @Test
    public void deleteFrom() throws Exception {
        this.repository.deleteFrom(PropertyQuery.builder().filter("a >= 500").build());

        assertThat(this.repository.all(0, 1000).total(), is(500L));
    }

    @Test
    public void givenNoFilter_sortAsc() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().sort("a asc").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(i));
        }
    }

    @Test
    public void givenNoFilter_sortNoDirection() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().sort("a").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(i));
        }
    }

    @Test
    public void givenNoFilter_sortDesc() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().sort("a desc").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(999 - i));
        }
    }

    @Test
    public void givenFilter_sortAsc() throws Exception {
        PagedEntityList<Simple> actual = this.repository.search(PropertyQuery.builder().filter("a >= 500").sort("a asc").build(), 0, 10);
        for (long i = 0; i < 10; i++) {
            assertThat(i + "th", actual.get((int) i).value().a(), is(500 + i));
        }
    }

    @Test
    public void whenTwoSortCriteria__thenSecondAppliedWhenFirstCriterionIsEquals() throws Exception {
        this.repository.deleteFrom(PropertyQuery.builder().build());
        assertThat(this.repository.all(0L, 0L).total(), is(0L));

        this.repository.create(Simple.builder().a(3L).b(3L).build());
        this.repository.create(Simple.builder().a(3L).b(2L).build());
        this.repository.create(Simple.builder().a(3L).b(1L).build());

        this.repository.create(Simple.builder().a(2L).b(3L).build());
        this.repository.create(Simple.builder().a(2L).b(2L).build());
        this.repository.create(Simple.builder().a(2L).b(1L).build());

        this.repository.create(Simple.builder().a(1L).b(3L).build());
        this.repository.create(Simple.builder().a(1L).b(2L).build());
        this.repository.create(Simple.builder().a(1L).b(1L).build());

        List<String> actual = this.repository.search(PropertyQuery.builder().sort("a asc, b asc").build(), 0L, 100L)
                .valueList().stream().map(s -> s.a() + "/" + s.b())
                .collect(Collectors.toList());
        System.out.println(actual);
        assertThat(
                actual,
                contains(
                    "1/1", "1/2", "1/3",
                    "2/1", "2/2", "2/3",
                    "3/1", "3/2", "3/3"
                )
        );
    }
}
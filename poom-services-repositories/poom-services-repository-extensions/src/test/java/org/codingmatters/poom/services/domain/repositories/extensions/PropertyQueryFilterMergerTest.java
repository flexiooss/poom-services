package org.codingmatters.poom.services.domain.repositories.extensions;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PropertyQueryFilterMergerTest {

    @Test
    public void testNullFilterNullQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters(null, null);
        assertThat(mergedQuery, is(PropertyQuery.builder().build()));
    }

    @Test
    public void testEmptyFilterNullQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters("", null);
        assertThat(mergedQuery, is(PropertyQuery.builder().build()));
    }

    @Test
    public void testNullFilterEmptyQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters(null, PropertyQuery.builder().filter("").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().build()));
    }

    @Test
    public void testNullFilterQueryWithSort() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters(null, PropertyQuery.builder().sort("sortProp asc").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().sort("sortProp asc").build()));
    }

    @Test
    public void testEmptyFilterEmptyQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters("", PropertyQuery.builder().filter("").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().build()));
    }

    @Test
    public void testFilterNullQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters("a<2", null);
        assertThat(mergedQuery, is(PropertyQuery.builder().filter("a<2").build()));
    }

    @Test
    public void testFilterEmptyQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters("a<2", PropertyQuery.builder().filter("").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().filter("a<2").build()));
    }

    @Test
    public void testNullFilterQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters(null, PropertyQuery.builder().filter("a<2").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().filter("a<2").build()));
    }

    @Test
    public void testEmptyFilterQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters("", PropertyQuery.builder().filter("a<2").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().filter("a<2").build()));
    }

    @Test
    public void testFilterAndQuery() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters("a<2", PropertyQuery.builder().filter("b>3").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().filter("(a<2) && (b>3)").build()));
    }

    @Test
    public void testFilterAndQueryWithSort() {
        PropertyQuery mergedQuery = PropertyQueryFilterMerger.mergeFilters("a<2", PropertyQuery.builder().filter("b>3").sort("sortProp asc").build());
        assertThat(mergedQuery, is(PropertyQuery.builder().filter("(a<2) && (b>3)").sort("sortProp asc").build()));
    }
}
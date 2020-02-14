package org.codingmatters.poom.services.domain.property.query.rewrite;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PropertyPrefixerTest {

    @Test
    public void filter() throws Exception {
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto == titi").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto == property.prefix.titi ").build())
        );
    }

    @Test
    public void sort() throws Exception {
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().sort("toto, tutu, titi").build()),
                is(PropertyQuery.builder().sort("property.prefix.toto , property.prefix.tutu , property.prefix.titi ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().sort("toto asc, tutu, titi desc").build()),
                is(PropertyQuery.builder().sort("property.prefix.toto asc , property.prefix.tutu , property.prefix.titi desc ").build())
        );
    }
}
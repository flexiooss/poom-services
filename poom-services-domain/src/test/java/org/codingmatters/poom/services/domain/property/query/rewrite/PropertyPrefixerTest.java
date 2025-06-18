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
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto in ('titi','tutu')").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto in ( 'titi' , 'tutu' ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto in ()").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto in ( ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto contains all ('titi','tutu')").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto contains all ( 'titi' , 'tutu' ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto contains all ()").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto contains all ( ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto is empty").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto is empty ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto ends with any ('o','t')").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto ends with any ( 'o' , 't' ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto ends with any ()").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto ends with any ( ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto contains any ('o','t')").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto contains any ( 'o' , 't' ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto contains any ()").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto contains any ( ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto starts with any ('o','t')").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto starts with any ( 'o' , 't' ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto starts with any ()").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto starts with any ( ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto any in ('o','t')").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto any in ( 'o' , 't' ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto any in ()").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto any in ( ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("(toto == titi)").build()),
                is(PropertyQuery.builder().filter("( property.prefix.toto == property.prefix.titi ) ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto == titi || toto == tutu").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto == property.prefix.titi || property.prefix.toto == property.prefix.tutu ").build())
        );

        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto =~ /.*/").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto =~ /.*/ ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("! toto == tutu").build()),
                is(PropertyQuery.builder().filter("! property.prefix.toto == property.prefix.tutu ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto == titi && toto == tutu").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto == property.prefix.titi && property.prefix.toto == property.prefix.tutu ").build())
        );
        assertThat(
                new PropertyPrefixer("property.prefix.").rewrite(PropertyQuery.builder().filter("toto is not empty").build()),
                is(PropertyQuery.builder().filter("property.prefix.toto is not empty ").build())
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
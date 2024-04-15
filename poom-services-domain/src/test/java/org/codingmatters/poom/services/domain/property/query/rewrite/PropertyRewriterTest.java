package org.codingmatters.poom.services.domain.property.query.rewrite;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class PropertyRewriterTest {


    @Test
    public void whenProperties__thenProperetiesRewritten() throws Exception {
        assertThat(
                new PropertyRewriter(Map.of("p1", "f1", "p2", "f2", "p3", "f3")).rewrite(PropertyQuery.builder()
                        .filter("p1 == '12' && p2 in ('12', '43') || p3 != '18'")
                        .sort("p1 asc, p2 desc, p3")
                        .build()),
                is(PropertyQuery.builder()
                        .filter("f1 == '12' && f2 in ( '12' , '43' ) || f3 != '18' ")
                        .sort("f1 asc , f2 desc , f3 ")
                        .build()
                )
        );
    }

    @Test
    public void givenFilter__whenIdEqualsSmtg__then_idEqualsSmtg() throws Exception {
        assertThat(
                new PropertyRewriter(Map.of("id", "_id")).rewrite(PropertyQuery.builder().filter("id == '12'").build()).filter(),
                is("_id == '12' ")
        );
    }

    @Test
    public void givenFilter__whenIdInSmtg__then_idInSmtg() throws Exception {
        assertThat(
                new PropertyRewriter(Map.of("id", "_id")).rewrite(PropertyQuery.builder().filter("id in ('12', '42')").build()).filter(),
                is("_id in ( '12' , '42' ) ")
        );
    }

    @Test
    public void givenSort__whenId__then_id() throws Exception {
        assertThat(
                new PropertyRewriter(Map.of("id", "_id")).rewrite(PropertyQuery.builder().sort("id").build()).sort(),
                is("_id ")
        );
    }

    @Test
    public void givenSort__whenIdAsc__then_idAsc() throws Exception {
        assertThat(
                new PropertyRewriter(Map.of("id", "_id")).rewrite(PropertyQuery.builder().sort("id asc").build()).sort(),
                is("_id asc ")
        );
    }

    @Test
    public void givenSort__whenIdDesc__then_idDesc() throws Exception {
        assertThat(
                new PropertyRewriter(Map.of("id", "_id")).rewrite(PropertyQuery.builder().sort("id desc").build()).sort(),
                is("_id desc ")
        );
    }


}
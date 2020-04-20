package org.codingmatters.poom.services.domain.repositories.inmemory;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.test.Ref;
import org.codingmatters.test.Simple;
import org.codingmatters.test.simple.E;
import org.junit.Before;
import org.junit.Test;

public class InMemoryRepositoryWithPropertyQuerySortTest {

    private Repository<Simple, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Simple.class);

    @Before
    public void setUp() throws Exception {
        System.out.println("***INSERTION***");
        for (long i = 0; i < 50; i++) {
            Simple build = Simple.builder()
                    .a( i )
                    .e( E.builder()
                            .f( i % 4 > 1 ? "wb" : "wa" )
                            .g( i % 2 == 0 ? "fa" : "fb" )
                            .build() )
                    .ref( Ref.builder().a( i ).build() )
                    .build();
            System.out.println( "f=" + build.e().f() + "\tg=" + build.e().g() );
            this.repository.create( build );
        }
    }

    @Test
    public void test() throws Exception {
        String sort = "e.f asc , e.g asc";
        System.out.println("***TRI => " + sort + " \t***");
        PagedEntityList<Simple> search = this.repository.search( PropertyQuery.builder().sort( sort ).build(), 0, 1005 );
        search.stream().forEach( s -> System.out.println( "f=" + s.value().e().f() + "\tg=" + s.value().e().g() ) );

    }
}

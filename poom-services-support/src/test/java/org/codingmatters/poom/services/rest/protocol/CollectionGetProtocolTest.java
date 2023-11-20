package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.poom.services.test.utils.MockedStringRepository;
import org.codingmatters.poom.services.test.utils.StringInMemoryRepository;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/13/17.
 */
public class CollectionGetProtocolTest {

    private Repository<String, String> repository;
    private TestCollectionGetHandler handler;

    @Before
    public void setUp() throws Exception {
        this.repository = new StringInMemoryRepository();
        this.handler = new TestCollectionGetHandler(this.repository);
    }

    @Test
    public void whenNoRangeRequested__ifRepositoryIsEmpty__thenReturnCompleteEmptyList() throws Exception {
        Response response = this.handler.apply(new Request(null, null));

        assertThat(response.page.isPartial(), is(false));
        assertThat(response.page.list(), is(empty()));
        assertThat(response.page.contentRange(), is("String 0-0/0"));
        assertThat(response.page.acceptRange(), is("String 100"));
    }

    @Test
    public void whenNoRangeRequested__ifRepositorySmallerThanDefaultRange__thenReturnCompleteJobList() throws Exception {
        Entity<String> stored = this.repository.create("test");
        Response response = this.handler.apply(new Request(null, null));

        assertThat(response.page.isPartial(), is(false));
        assertThat(response.page.contentRange(), is("String 0-0/1"));
        assertThat(response.page.acceptRange(), is("String 100"));

        assertThat(response.page.list(), hasSize(1));
        assertThat(response.page.list().get(0).id(), is(stored.id()));
    }

    @Test
    public void whenNoRangeRequested__ifRepositoryLargerThanDefaultRange__thenReturnPartialJobList() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create("test-" + i);
        }
        Response response = this.handler.apply(new Request(null, null));

        assertThat(response.page.isPartial(), is(true));
        assertThat(response.page.contentRange(), is("String 0-99/150"));
        assertThat(response.page.acceptRange(), is("String 100"));

        assertThat(response.page.list(), hasSize(100));
    }

    @Test
    public void whenRangeRequested__ifRepositoryIsEmpty__thenReturnStatus200_andEmptyJobList() throws Exception {
        Response response = this.handler.apply(new Request("10-19", null));

        assertThat(response.page.isPartial(), is(false));
        assertThat(response.page.contentRange(), is("String 0-0/0"));
        assertThat(response.page.acceptRange(), is("String 100"));

        assertThat(response.page.list(), is(empty()));
    }

    @Test
    public void whenRangeRequested__ifRangeIsLargerRepository__thenReturnCompleteList() throws Exception {
        Entity<String> storedJob = this.repository.create("test");
        Response response = this.handler.apply(new Request("0-10", null));

        assertThat(response.page.isPartial(), is(false));
        assertThat(response.page.contentRange(), is("String 0-0/1"));
        assertThat(response.page.acceptRange(), is("String 100"));

        assertThat(response.page.list(), hasSize(1));
        assertThat(response.page.list().get(0).id(), is(storedJob.id()));
    }

    @Test
    public void whenRangeRequested__ifRepositoryLargerThanRange__thenReturnPartialJobList() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create("test-" + i);
        }

        Response response = this.handler.apply(new Request("0-9", null));

        assertThat(response.page.isPartial(), is(true));
        assertThat(response.page.contentRange(), is("String 0-9/150"));
        assertThat(response.page.acceptRange(), is("String 100"));

        assertThat(response.page.list(), hasSize(10));
    }

    @Test
    public void whenRangeRequested__ifRangeStartsAfter0__thenReturnPartialList_offsettedList() throws Exception {
        Entity<String> startEntity = null;
        for(int i = 0 ; i < 150 ; i++) {
            Entity<String> entity = this.repository.create("test-" + i);
            if(i == 50) {
                startEntity = entity;
            }
        }

        Response response = this.handler.apply(new Request("50-99", null));

        assertThat(response.page.isPartial(), is(true));
        assertThat(response.page.contentRange(), is("String 50-99/150"));
        assertThat(response.page.acceptRange(), is("String 100"));

        assertThat(response.page.list(), hasSize(50));
        assertThat(response.page.list().get(0).id(), is(startEntity.id()));
    }

    @Test
    public void whenRangeRequested__ifRangeIsInvalid__thenResturnInvalidPage_withErrorToken() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create("test-" + i);
        }

        Response response = this.handler.apply(new Request("9-0", null));


        assertThat(response.page.isValid(), is(false));
        assertThat(response.page.acceptRange(), is("String 100"));
        assertThat(response.page.contentRange(), is("String */150"));
        assertThat(response.page.validationMessage(), is("malformed range expression, start is after end : 9-0"));

        assertThat(response.errorToken, is(notNullValue()));
        assertThat(response.e, is(nullValue()));
    }

    @Test
    public void whenUnexpectedRepositoryException__willReturnAnError_withAnErrorToken() throws Exception {
        Response response = new TestCollectionGetHandler(new MockedStringRepository()).apply(new Request(null, null));

        assertThat(response.page, is(nullValue()));
        assertThat(response.e, is(notNullValue()));
        assertThat(response.e, isA(RepositoryException.class));
        assertThat(response.errorToken, is(notNullValue()));
    }

    @Test
    public void whenQuery_andNoRange__thenOnlyFilteredEntitiesAreReturned_andCompleteList() throws Exception {
        this.repository.create("matches 1");
        this.repository.create("matches 2");
        this.repository.create("doesn't match 3");
        this.repository.create("doesn't match 4");

        Response response = this.handler.apply(new Request(null, "matches"));

        assertThat(response.page.list(), hasSize(2));
        assertThat(response.page.list().get(0).value(), is("matches 1"));
        assertThat(response.page.list().get(1).value(), is("matches 2"));

        assertThat(response.page.isPartial(), is(false));
    }

    @Test
    public void whenQuery_withARange__thenOnlyFilteredEntitiesAreReturned_andPartialList() throws Exception {
        for(int i = 0 ; i < 75 ; i++) {
            this.repository.create("matches " + i);
            this.repository.create("doesn't match " + i);
        }

        Response response = this.handler.apply(new Request("0-9", "matches"));

        assertThat(response.page.list(), hasSize(10));
        for(int i = 0 ; i < 10 ; i++) {
            assertThat(i + "th result", response.page.list().get(i).value(), is("matches " + i));
        }

        assertThat(response.page.isPartial(), is(true));
    }

    class Request {
        private final String range;
        private final String query;

        public Request(String range, String query) {
            this.range = range;
            this.query = query;
        }
    }

    class Response {
        private final Rfc7233Pager.Page<String> page;
        private final String errorToken;
        private final RepositoryException e;

        public Response(Rfc7233Pager.Page<String> page, String errorToken, RepositoryException e) {
            this.page = page;
            this.errorToken = errorToken;
            this.e = e;
        }
    }

    class TestCollectionGetHandler implements CollectionGetProtocol<String, String, Request, Response> {

        private final Repository<String, String> repository;

        TestCollectionGetHandler(Repository<String, String> repository) {
            this.repository = repository;
        }

        @Override
        public Logger log() {
            return LoggerFactory.getLogger(TestCollectionGetHandler.class);
        }

        @Override
        public Repository<String, String> repository(Request request) {
            return this.repository;
        }

        @Override
        public int maxPageSize() {
            return 100;
        }

        @Override
        public String rfc7233Unit() {
            return "String";
        }

        @Override
        public String rfc7233Range(Request request) {
            return request.range;
        }

        @Override
        public String parseQuery(Request request) {
            return request.query;
        }

        @Override
        public Response partialList(Rfc7233Pager.Page<String> page, Request request) {
            return new Response(page, null, null);
        }

        @Override
        public Response completeList(Rfc7233Pager.Page<String> page, Request request) {
            return new Response(page, null, null);
        }

        @Override
        public Response invalidRangeQuery(Rfc7233Pager.Page<String> page, String errorToken, Request request) {
            return new Response(page, errorToken, null);
        }

        @Override
        public Response unexpectedError(RepositoryException e, String errorToken) {
            return new Response(null, errorToken, e);
        }
    }

}
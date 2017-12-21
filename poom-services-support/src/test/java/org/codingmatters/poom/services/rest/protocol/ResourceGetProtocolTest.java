package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.test.utils.MockedStringRepository;
import org.codingmatters.poom.services.test.utils.StringInMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/18/17.
 */
public class ResourceGetProtocolTest {

    private Repository<String, String> repository;
    private ResourceGetProtocol<String, String, Request, Response> handler;

    @Before
    public void setUp() throws Exception {
        this.repository = new StringInMemoryRepository();
        this.handler = new TestResourceGetHandler(this.repository);
    }

    @Test
    public void whenEntityNotInRepository__thenEntityNotFoundMetodCalled() throws Exception {
        Response response = this.handler.apply(new Request("12"));

        assertThat(response.entity, is(nullValue()));
        assertThat(response.errorToken, is(notNullValue()));
        assertThat(response.method, is("entityNotFound"));
    }

    @Test
    public void whenEntityInRepository__thenEntityFoundMethodCalled() throws Exception {
        Entity<String> entity = this.repository.create("test");

        Response response = this.handler.apply(new Request(entity.id()));

        assertThat(response.entity.id(), is(entity.id()));
        assertThat(response.entity.value(), is(entity.value()));
        assertThat(response.entity.version(), is(entity.version()));

        assertThat(response.errorToken, is(nullValue()));
        assertThat(response.method, is("entityFound"));
    }

    @Test
    public void whenRepositoryExceptionOccurs__thenUnexpectedErrorMethodCalled() throws Exception {
        Response response = new TestResourceGetHandler(new MockedStringRepository()).apply(new Request("12"));

        assertThat(response.entity, is(nullValue()));
        assertThat(response.errorToken, is(notNullValue()));
        assertThat(response.method, is("unexpectedError"));
    }

    class Request {
        final String entityId;

        Request(String entityId) {
            this.entityId = entityId;
        }
    }

    class Response {
        final Entity<String> entity;
        final String errorToken;
        final String method;

        public Response(Entity<String> entity, String errorToken, String method) {
            this.entity = entity;
            this.errorToken = errorToken;
            this.method = method;
        }
    }

    private class TestResourceGetHandler implements ResourceGetProtocol<String, String, Request, Response>{
        private final Repository<String, String> repository;

        public TestResourceGetHandler(Repository<String, String> repository) {
            this.repository = repository;
        }

        @Override
        public Logger log() {
            return LoggerFactory.getLogger(TestResourceGetHandler.class);
        }

        @Override
        public Repository<String, String> repository(Request request) {
            return this.repository;
        }

        @Override
        public String entityId(Request request) {
            return request.entityId;
        }

        @Override
        public Response entityFound(Entity<String> entity) {
            return new Response(entity, null, "entityFound");
        }

        @Override
        public Response entityNotFound(String errorToken) {
            return new Response(null, errorToken, "entityNotFound");
        }

        @Override
        public Response unexpectedError(String errorToken) {
            return new Response(null, errorToken, "unexpectedError");
        }
    }
}
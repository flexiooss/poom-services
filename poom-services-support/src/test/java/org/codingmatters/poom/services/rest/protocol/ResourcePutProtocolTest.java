package org.codingmatters.poom.services.rest.protocol;

import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.Validation;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.test.utils.MockedStringRepository;
import org.codingmatters.poom.services.test.utils.StringInMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ResourcePutProtocolTest {

    private Repository<String, String> repository;
    private TestResourcePutHandler handler;

    @Before
    public void setUp() throws Exception {
        this.repository = new StringInMemoryRepository();
        this.handler = new TestResourcePutHandler(this.repository);
    }

    @Test
    public void whenEntityDoesntExist__thenEntityNotFoundCalled() throws Exception {
        assertThat(this.repository.all(0, 1), is(empty()));

        Response response = this.handler.apply(new Request("not-found", new Validation(true, null)));

        assertThat(response.method, is("entityNotFound"));
        assertThat(response.errorToken, is(notNullValue()));

        assertThat(this.repository.all(0, 1), is(empty()));
    }

    @Test
    public void whenEntityExists_andUpdateIsValid__thenEntityIsUpdated_andEntityUpdatedCalled() throws Exception {
        String entityId = this.repository.create("original").id();

        Response response = this.handler.apply(new Request(entityId, new Validation(true, null)));

        assertThat(this.repository.retrieve(entityId).version(), is(BigInteger.valueOf(2)));
        assertThat(this.repository.retrieve(entityId).value(), is("changed"));

        assertThat(response.method, is("entityUpdated"));
        assertThat(response.entity.id(), is(entityId));
    }

    @Test
    public void whenEntityExists_andUpdateIsInvalid__thenEntityIsUnchanged_andInvalidUpdateCalled() throws Exception {
        String entityId = this.repository.create("original").id();

        Response response = this.handler.apply(new Request(entityId, new Validation(false, "not quite")));

        assertThat(this.repository.retrieve(entityId).version(), is(BigInteger.ONE));
        assertThat(response.method, is("invalidUpdate"));
        assertThat(response.errorToken, is(notNullValue()));
        assertThat(response.change.currentValue(), is("original"));
        assertThat(response.change.newValue(), is("changed"));
        assertThat(response.change.validation().isValid(), is(false));
        assertThat(response.change.validation().message(), is("not quite"));
    }

    @Test
    public void whenRepositoryExceptionExists__thenUnexpectedErrorCalled() throws Exception {
        Response response = new TestResourcePutHandler(new MockedStringRepository()).apply(new Request("123", new Validation(true, null)));

        assertThat(response.method, is("unexpectedError"));
        assertThat(response.errorToken, is(notNullValue()));
        assertThat(response.e, isA(RepositoryException.class));
    }

    class Request {
        final String entityId;
        final Validation validation;

        public Request(String entityId, Validation validation) {
            this.entityId = entityId;
            this.validation = validation;
        }
    }

    class Response {
        final String method;
        final Entity<String> entity;
        final Change<String> change;
        final String errorToken;
        final RepositoryException e;

        public Response(String method, Entity<String> entity, Change<String> change, String errorToken, RepositoryException e) {
            this.method = method;
            this.entity = entity;
            this.change = change;
            this.errorToken = errorToken;
            this.e = e;
        }
    }

    class TestResourcePutHandler implements ResourcePutProtocol<String, String, Request, Response> {

        private final Repository<String, String> repository;

        TestResourcePutHandler(Repository<String, String> repository) {
            this.repository = repository;
        }

        @Override
        public Logger log() {
            return LoggerFactory.getLogger(TestResourcePutHandler.class);
        }

        @Override
        public Repository<String, String> repository() {
            return this.repository;
        }

        @Override
        public String entityId(Request request) {
            return request.entityId;
        }

        @Override
        public Change<String> valueUpdate(Request request, Entity<String> entity) {
            return new Change<String>(entity.value(), "changed") {
                @Override
                protected Validation validate() {
                    return request.validation;
                }

                @Override
                public String applied() {
                    return this.newValue();
                }
            };
        }

        @Override
        public Response entityUpdated(Entity<String> entity) {
            return new Response("entityUpdated", entity, null, null, null);
        }

        @Override
        public Response invalidUpdate(Change<String> change, String errorToken) {
            return new Response("invalidUpdate", null, change, errorToken, null);
        }

        @Override
        public Response entityNotFound(String errorToken) {
            return new Response("entityNotFound", null, null, errorToken, null);
        }

        @Override
        public Response unexpectedError(RepositoryException e, String errorToken) {
            return new Response("unexpectedError", null, null, errorToken, e);
        }
    }
}
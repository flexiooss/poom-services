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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CollectionPostProtocolTest {

    private Repository<String, String> repository;
    private TestCollectionPostHandler handler;

    @Before
    public void setUp() throws Exception {
        this.repository = new StringInMemoryRepository();
        this.handler = new TestCollectionPostHandler(this.repository);
    }

    @Test
    public void whenRequestIsValid__thenEntityIsCreated_andEntityCreatedMethodIsCalled() throws Exception {
        assertThat(this.repository.all(0, 1), is(empty()));

        Response response = this.handler.apply(new Request(true, null));
        Entity<String> entity = this.repository.all(0, 1).get(0);

        assertThat(response.method, is("entityCreated"));
        assertThat(response.entity.id(), is(entity.id()));
        assertThat(response.entity.value(), is("new"));
        assertThat(response.creation.validation().isValid(), is(true));
        assertThat(response.creation.validation().message(), is(nullValue()));
        assertThat(response.e, is(nullValue()));
        assertThat(response.errorToken, is(nullValue()));
    }

    @Test
    public void whenRequestIsInvalid__thenEntityNotCreated_andInvalidCreationMethodCalled() throws Exception {
        assertThat(this.repository.all(0, 1), is(empty()));

        Response response = this.handler.apply(new Request(false, "invalid"));

        assertThat(this.repository.all(0, 1), is(empty()));

        assertThat(response.method, is("invalidCreation"));
        assertThat(response.creation.validation().isValid(), is(false));
        assertThat(response.creation.validation().message(), is("invalid"));
        assertThat(response.entity, is(nullValue()));
        assertThat(response.e, is(nullValue()));
        assertThat(response.errorToken, is(notNullValue()));
    }

    @Test
    public void whenRepositoryExceptionOccurs__thenEntityNotCreated_andUnexpectedErrorIsCalled() throws Exception {
        assertThat(this.repository.all(0, 1), is(empty()));

        Response response = new TestCollectionPostHandler(new MockedStringRepository()).apply(new Request(true, null));

        assertThat(this.repository.all(0, 1), is(empty()));

        assertThat(response.method, is("unexpectedError"));
        assertThat(response.creation.validation().isValid(), is(true));
        assertThat(response.creation.validation().message(), is(nullValue()));
        assertThat(response.entity, is(nullValue()));
        assertThat(response.e, isA(RepositoryException.class));
        assertThat(response.errorToken, is(notNullValue()));
    }

    class Request {
        final boolean valid;
        final String message;

        Request(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }

    class Response {
        final String method;
        final Change<String> creation;
        final Entity<String> entity;
        final RepositoryException e;
        final String errorToken;

        public Response(String method, Change<String> creation, Entity<String> entity, RepositoryException e, String errorToken) {
            this.method = method;
            this.creation = creation;
            this.entity = entity;
            this.e = e;
            this.errorToken = errorToken;
        }
    }

    private class TestCollectionPostHandler implements CollectionPostProtocol<String, String, Request, Response>{
        private final Repository<String, String> repository;

        public TestCollectionPostHandler(Repository<String, String> repository) {
            this.repository = repository;
        }

        @Override
        public Logger log() {
            return LoggerFactory.getLogger(TestCollectionPostHandler.class);
        }

        @Override
        public Repository<String, String> repository() {
            return this.repository;
        }

        @Override
        public Change<String> valueCreation(Request request) {
            return new Change<String>(null, "new") {
                @Override
                protected Validation validate() {
                    return new Validation(request.valid, request.message);
                }

                @Override
                public String applied() {
                    return this.newValue();
                }
            };
        }

        @Override
        public Response entityCreated(Change<String> creation, Entity<String> entity) {
            return new Response("entityCreated", creation, entity, null, null);
        }

        @Override
        public Response invalidCreation(Change<String> creation, String errorToken) {
            return new Response("invalidCreation", creation, null, null, errorToken);
        }

        @Override
        public Response unexpectedError(Change<String> creation, RepositoryException e, String errorToken) {
            return new Response("unexpectedError", creation, null, e, errorToken);
        }
    }
}
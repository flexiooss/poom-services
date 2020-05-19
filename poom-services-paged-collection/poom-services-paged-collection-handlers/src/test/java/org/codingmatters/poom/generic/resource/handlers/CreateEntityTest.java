package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.paged.collection.api.PagedCollectionPostRequest;
import org.codingmatters.poom.api.paged.collection.api.PagedCollectionPostResponse;
import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.generic.resource.handlers.tests.TestAdapter;
import org.codingmatters.poom.generic.resource.handlers.tests.TestCRUD;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class CreateEntityTest {

    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        PagedCollectionPostResponse response = new CreateEntity(() -> {
            throw new Exception("");
        }).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenCRUDIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter()).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }


    @Test
    public void givenAdapterGetted__whenCreatedEntityIsNull__then500() throws Exception {
        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return null;
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenNoValuePosted__thenEmptyObjectIsPassedToAdapter() throws Exception {
        AtomicReference<ObjectValue> requestedPayload = new AtomicReference<>();

        new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, ObjectValue.builder().build());
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        assertThat(requestedPayload.get(), is(ObjectValue.builder().build()));
    }

    @Test
    public void givenAdapterOK__whenValuePosted__thenPostedValueIsPassedToAdapter() throws Exception {
        ObjectValue aValue = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();
        AtomicReference<ObjectValue> requestedPayload = new AtomicReference<>();

        new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, ObjectValue.builder().build());
            }
        })).apply(PagedCollectionPostRequest.builder().payload(aValue).build());

        assertThat(requestedPayload.get(), is(aValue));
    }

    @Test
    public void givenAdapterOK__whenCREATEActionNotSupportedValueCreated__then405() throws Exception {
        ObjectValue aValue = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();

        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Set<Action> supportedActions() {
                return Action.updateReplace;
            }

            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.ONE, aValue);
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status405().orElseThrow(() -> new AssertionError("expected 405, got " + response));

        Error error = response.status405().payload();
        assertThat(error.code(), is(Error.Code.ENTITY_CREATION_NOT_ALLOWED));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenValueCreated__then201_andXEntityIdSetted_andLocationSetted_andValueReturned() throws Exception {
        ObjectValue aValue = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();

        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.ONE, aValue);
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status201().orElseThrow(() -> new AssertionError("expected 201, got " + response));

        assertThat(response.status201().xEntityId(), is("12"));
        assertThat(response.status201().location(), is(new TestCRUD().entityRepositoryUrl() + "/12"));
        assertThat(response.status201().payload(), is(aValue));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(error, msg);
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(error, msg);
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(error, msg);
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(error, msg);
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenValidationThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        PagedCollectionPostResponse response = new CreateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(error, msg);
            }
        })).apply(PagedCollectionPostRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }
}
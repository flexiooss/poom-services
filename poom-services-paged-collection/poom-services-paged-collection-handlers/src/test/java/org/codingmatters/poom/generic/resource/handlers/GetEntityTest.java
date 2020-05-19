package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.generic.resource.api.EntityGetRequest;
import org.codingmatters.poom.api.generic.resource.api.EntityGetResponse;
import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.handlers.tests.TestAdapter;
import org.codingmatters.poom.generic.resource.handlers.tests.TestCRUD;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

public class GetEntityTest {

    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityGetResponse response = new GetEntity(() -> {
            throw new Exception("");
        }).apply(EntityGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenCRUDIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityGetResponse response = new GetEntity(() -> new TestAdapter()).apply(EntityGetRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenNoEntityIdProvided__then400_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD())).apply(EntityGetRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        Error error = response.status400().payload();
        assertThat(error.code(), is(Error.Code.BAD_REQUEST));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenEntityIdProvided__thenEntityIdPassedToAdapter() throws Exception {
        AtomicReference<String> requestedEntityId = new AtomicReference<>();

        new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedEntityId.set(id);
                return Optional.empty();
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        assertThat(requestedEntityId.get(), is("12"));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenEntityNotFound__then404_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return Optional.empty();
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        Error error = response.status404().payload();
        assertThat(error.code(), is(Error.Code.RESOURCE_NOT_FOUND));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.ENTITY_NOT_FOUND));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining("12")));
        assertThat(error.messages().get(1).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(1).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenEntityFound__then200_andEntityReturned() throws Exception {
        ObjectValue aValue = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return Optional.of(new ImmutableEntity<>("12", BigInteger.ONE, aValue));
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().xEntityId(), is("12"));
        assertThat(response.status200().location(), is(new TestCRUD().entityRepositoryUrl() + "/12"));
        assertThat(response.status200().payload(), is(aValue));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(error, msg);
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(error, msg);
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(error, msg);
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(error, msg);
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityGetResponse response = new GetEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(error, msg);
            }
        })).apply(EntityGetRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }
}
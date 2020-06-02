package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.paged.collection.api.EntityPatchRequest;
import org.codingmatters.poom.api.paged.collection.api.EntityPatchResponse;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

public class UpdateEntityTest {

    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityPatchResponse response = new UpdateEntity(() -> {
            throw new Exception("");
        }).apply(EntityPatchRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenCRUDIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter()).apply(EntityPatchRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }


    @Test
    public void givenAdapterGetted_andEntityIdProvided__whenReplacedEntityIsNull__then500() throws Exception {
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter( new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return null;
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenUPDATEActionNotSupportedValueCreated__then405() throws Exception {
        ObjectValue aValue = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();

        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Set<Action> supportedActions() {
                return new HashSet<>(Arrays.asList(Action.CREATE, Action.REPLACE));
            }

            @Override
            public Entity<ObjectValue> replaceEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.ONE, aValue);
            }
        })).apply(EntityPatchRequest.builder().build());

        response.opt().status405().orElseThrow(() -> new AssertionError("expected 405, got " + response));

        Error error = response.status405().payload();
        assertThat(error.code(), is(Error.Code.ENTITY_UPDATE_NOT_ALLOWED));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenNoEntityId__then400() throws Exception {
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD()))
                .apply(EntityPatchRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        Error error = response.status400().payload();
        assertThat(error.code(), is(Error.Code.BAD_REQUEST));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenEntityIdProvided__thenEntityIdIsPassedToAdapter() throws Exception {
        AtomicReference<String> requestedId = new AtomicReference<>();

        new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedId.set(id);
                return new ImmutableEntity<>("12", BigInteger.ONE, ObjectValue.builder().build());
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        assertThat(requestedId.get(), is("12"));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenNoValuePosted__thenEmptyObjectIsPassedToAdapter() throws Exception {
        AtomicReference<ObjectValue> requestedPayload = new AtomicReference<>();

        new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, ObjectValue.builder().build());
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        assertThat(requestedPayload.get(), is(ObjectValue.builder().build()));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValuePosted__thenPostedValueIsPassedToAdapter() throws Exception {
        ObjectValue aValue = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();
        AtomicReference<ObjectValue> requestedPayload = new AtomicReference<>();

        new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedPayload.set(value);
                return new ImmutableEntity<>("12", BigInteger.ONE, ObjectValue.builder().build());
            }
        })).apply(EntityPatchRequest.builder().entityId("12").payload(aValue).build());

        assertThat(requestedPayload.get(), is(aValue));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValueReplaced__then201_andXEntityIdSetted_andLocationSetted_andValueReturned() throws Exception {
        ObjectValue aValue = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();

        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                return new ImmutableEntity<>("12", BigInteger.TWO, aValue);
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().xEntityId(), is("12"));
        assertThat(response.status200().location(), is(new TestCRUD().entityRepositoryUrl() + "/12"));
        assertThat(response.status200().payload(), is(aValue));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(error, msg);
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(error, msg);
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(error, msg);
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(error, msg);
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenValidationThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityPatchResponse response = new UpdateEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(error, msg);
            }
        })).apply(EntityPatchRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }
}
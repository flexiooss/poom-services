package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.generic.resource.api.EntityDeleteRequest;
import org.codingmatters.poom.api.generic.resource.api.EntityDeleteResponse;
import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.handlers.tests.TestAdapter;
import org.codingmatters.poom.generic.resource.handlers.tests.TestCRUD;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DeleteEntityTest {

    @Test
    public void whenExceptionGettingAdapter__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityDeleteResponse response = new DeleteEntity(() -> {
            throw new Exception("");
        }).apply(EntityDeleteRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        var error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOk__whenCRUDIsNull__then500_andErrorKeepsTrackOfLogToken() throws Exception {
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter()).apply(EntityDeleteRequest.builder().build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        Error error = response.status500().payload();
        assertThat(error.code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenNoEntityIdProvided__then400() throws Exception {
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter(new TestCRUD())).apply(EntityDeleteRequest.builder().build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        Error error = response.status400().payload();
        assertThat(error.code(), is(Error.Code.BAD_REQUEST));
        assertThat(error.token(), is(notNullValue()));
        assertThat(error.messages().get(0).key(), is(MessageKeys.SEE_LOGS_WITH_TOKEN));
        assertThat(error.messages().get(0).args().toArray(), is(arrayContaining(error.token())));
    }

    @Test
    public void givenAdapterOK__whenEntityIdProvided__thenEntityIdPassedToAdapter() throws Exception {
        AtomicReference<String> requestedId = new AtomicReference<>();

        new DeleteEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                requestedId.set(id);
            }
        })).apply(EntityDeleteRequest.builder().entityId("12").build());

        assertThat(requestedId.get(), is("12"));
    }

    @Test
    public void givenAdapterOK_andEntityIdProvided__whenEntityDeleted__then204() throws Exception {
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
            }
        })).apply(EntityDeleteRequest.builder().entityId("12").build());

        response.opt().status204().orElseThrow(() -> new AssertionError("expected 204, got " + response));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsBadRequestException__then400_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new BadRequestException(error, msg);
            }
        })).apply(EntityDeleteRequest.builder().entityId("12").build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(response.status400().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsForbiddenException__then403_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new ForbiddenException(error, msg);
            }
        })).apply(EntityDeleteRequest.builder().entityId("12").build());

        response.opt().status403().orElseThrow(() -> new AssertionError("expected 403, got " + response));

        assertThat(response.status403().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsNotFoundException__then404_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new NotFoundException(error, msg);
            }
        })).apply(EntityDeleteRequest.builder().entityId("12").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(response.status404().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnauthorizedException__then401_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnauthorizedException(error, msg);
            }
        })).apply(EntityDeleteRequest.builder().entityId("12").build());

        response.opt().status401().orElseThrow(() -> new AssertionError("expected 401, got " + response));

        assertThat(response.status401().payload(), is(error));
    }

    @Test
    public void givenAdapterOK__whenAdapterThrowsUnexpectedException__then500_andFunctionalErrorReturned() throws Exception {
        Error error = Error.builder().token("functional error message").build();
        String msg = "error";
        EntityDeleteResponse response = new DeleteEntity(() -> new TestAdapter(new TestCRUD() {
            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                throw new UnexpectedException(error, msg);
            }
        })).apply(EntityDeleteRequest.builder().entityId("12").build());

        response.opt().status500().orElseThrow(() -> new AssertionError("expected 500, got " + response));

        assertThat(response.status500().payload(), is(error));
    }
}
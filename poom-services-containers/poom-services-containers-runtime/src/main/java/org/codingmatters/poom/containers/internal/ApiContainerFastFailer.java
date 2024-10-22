package org.codingmatters.poom.containers.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.containers.internal.api.types.Error;
import org.codingmatters.poom.containers.internal.api.types.json.ErrorWriter;
import org.codingmatters.poom.fast.failing.FastFailer;
import org.codingmatters.poom.fast.failing.FastFailingInterceptor;
import org.codingmatters.poom.fast.failing.exceptions.InsufficientPermissionFFException;
import org.codingmatters.poom.fast.failing.exceptions.NeedsAuthorizationFFException;
import org.codingmatters.poom.fast.failing.exceptions.RecoverableFFException;
import org.codingmatters.poom.fast.failing.exceptions.UnrecoverableFFException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ApiContainerFastFailer implements FastFailer {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ApiContainerFastFailer.class);

    static public FastFailingInterceptor fastFailingInterceptor(ExternallyStoppable stoppable, ResponseDelegate responseDelegate, JsonFactory jsonFactory) {
        return new FastFailingInterceptor(new ApiContainerFastFailer(stoppable, responseDelegate, jsonFactory));
    }

    private final ExternallyStoppable stoppable;
    private final ResponseDelegate responseDelegate;
    private final JsonFactory jsonFactory;

    public ApiContainerFastFailer(ExternallyStoppable stoppable, ResponseDelegate responseDelegate, JsonFactory jsonFactory) {
        this.stoppable = stoppable;
        this.responseDelegate = responseDelegate;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public void failAndStop(UnrecoverableFFException e) {
        try {
            String token = log.tokenized().error("fast failing and stopping container : " + e.getMessage(), e);
            this.responseDelegate.contenType("application/json").status(500).payload(this.json(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .description(e.getMessage())
                    .token(token)
                    .build()));
        } catch (Throwable t) {
            log.error("error reporting error", e);
        }
        this.stoppable.requireStop();
    }

    @Override
    public void failAndContinue(RecoverableFFException e) {
        try {
            String token = log.tokenized().error("fast failing and continuing : " + e.getMessage(), e);
            Error.Code code = Error.Code.UNEXPECTED_ERROR;
            int status = 500;
            if(e instanceof NeedsAuthorizationFFException) {
                code = Error.Code.UNAUTHORIZED;
                status = 401;
            } else if(e instanceof InsufficientPermissionFFException) {
                code = Error.Code.BAD_REQUEST;
                status = 403;
            }
            this.responseDelegate.contenType("application/json").status(status).payload(this.json(Error.builder()
                    .code(code)
                    .description(e.getMessage())
                    .token(token)
                    .build()));
        } catch (Throwable t) {
            log.error("error reporting error", e);
        }
    }

    private byte[] json(Error error) throws IOException {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); JsonGenerator generator = this.jsonFactory.createGenerator(out)) {
            new ErrorWriter().write(generator, error);
            generator.flush();
            generator.close();
            return out.toByteArray();
        }
    }
}

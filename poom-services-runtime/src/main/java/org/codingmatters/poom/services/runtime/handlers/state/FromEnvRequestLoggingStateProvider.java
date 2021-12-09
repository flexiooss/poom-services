package org.codingmatters.poom.services.runtime.handlers.state;

import org.codingmatters.poom.services.runtime.handlers.RequestLoggingStateProvider;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.RequestDelegate;

public class FromEnvRequestLoggingStateProvider implements RequestLoggingStateProvider {

    public static final String REQUEST_LOGGING_ACTIVATED = "REQUEST_LOGGING_ACTIVATED";

    private boolean shouldLogValue = Env.optional(REQUEST_LOGGING_ACTIVATED).orElse(new Env.Var("false")).asString().equals("true");

    @Override
    public boolean shouldLog(RequestDelegate requestDelegate) {
        return this.shouldLogValue;
    }
}

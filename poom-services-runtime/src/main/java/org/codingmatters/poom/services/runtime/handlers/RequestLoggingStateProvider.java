package org.codingmatters.poom.services.runtime.handlers;

import org.codingmatters.rest.api.RequestDelegate;

public interface RequestLoggingStateProvider {
    boolean shouldLog(RequestDelegate requestDelegate);

    RequestLoggingStateProvider ALWAYS = requestDelegate -> true;
    RequestLoggingStateProvider NEVER = requestDelegate -> false;
}

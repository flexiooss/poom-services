package org.codingmatters.poom.services.support.http;

import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;

import java.util.concurrent.TimeUnit;

public class OutgoingClientWrapper {

    public static final String HTTP_CONNECTION_TIMEOUT_ENV = "HTTP_CONNECTION_TIMEOUT";
    public static final String HTTP_READ_TIMEOUT_ENV = "HTTP_READ_TIMEOUT";
    public static final String HTTP_WRITE_TIMEOUT_ENV = "HTTP_WRITE_TIMEOUT";
    public static final String HTTP_RETRY_ON_CONNECTION_FAILURE = "HTTP_RETRY_ON_CONNECTION_FAILURE";

    public static HttpClientWrapper wrappedClient(String prefix) {
        return OkHttpClientWrapper.build(builder -> builder
                .connectTimeout(Env.optional(prefix + "_" + HTTP_CONNECTION_TIMEOUT_ENV).orElse(Env.Var.value("15000")).asLong(), TimeUnit.MILLISECONDS)
                .readTimeout(Env.optional(prefix + "_" + HTTP_READ_TIMEOUT_ENV).orElse(Env.Var.value("40000")).asLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(Env.optional(prefix + "_" + HTTP_WRITE_TIMEOUT_ENV).orElse(Env.Var.value("40000")).asLong(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(Boolean.parseBoolean(Env.optional(prefix + "_" + HTTP_RETRY_ON_CONNECTION_FAILURE).orElse(Env.Var.value("true")).asString()))
        );
    }

    public static HttpClientWrapper wrappedClient() {
        return wrappedClient("OUTGOING");
    }

}

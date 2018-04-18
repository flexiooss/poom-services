package org.codingmatters.poom.services.support;

import java.util.Optional;

public interface Env {

    String SERVICE_HOST = "SERVICE_HOST";
    String SERVICE_PORT = "SERVICE_PORT";

    static String mandatory(String envVariableName) {
        return optional(envVariableName)
                .orElseThrow(() -> new RuntimeException("must provide mandatory environment variable : " + envVariableName));
    }

    static Optional<String> optional(String envVariableName) {
        return Optional.ofNullable(System.getenv(envVariableName));
    }

}

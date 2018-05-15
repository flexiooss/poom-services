package org.codingmatters.poom.services.support;

import java.util.Optional;

public interface Env {

    String SERVICE_HOST = "SERVICE_HOST";
    String SERVICE_PORT = "SERVICE_PORT";
    String SERVICE_URL = "SERVICE_URL";
    String SERVICE_REGISTRY_URL = "SERVICE_REGISTRY_URL";

    static Var mandatory(String envVariableName) {
        return optional(envVariableName)
                .orElseThrow(() -> new RuntimeException("must provide mandatory environment variable : " + envVariableName));
    }

    static Optional<Var> optional(String envVariableName) {
        String value = EnvProvider.get().apply(envVariableName);

        if(value != null) {
            return Optional.of(new Var(value));
        } else {
            return Optional.empty();
        }
    }

    class Var {
        private final String rawValue;

        public Var(String rawValue) {
            this.rawValue = rawValue;
        }

        public String asString() {
            return this.rawValue;
        }

        public Integer asInteger() {
            return Integer.valueOf(this.rawValue);
        }

        public Long asLong() {
            return Long.valueOf(this.rawValue);
        }

        public Float asFloat() {
            return Float.valueOf(this.rawValue);
        }

        public Double asDouble() {
            return Double.valueOf(this.rawValue);
        }
    }

}

package org.codingmatters.poom.services.support;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

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
        String value = null;

        String varFile = EnvProvider.get().apply(envVariableName + "_FILE");
        if(varFile != null) {
            value = readFile(varFile);
        }

        if(value == null) {
            value = EnvProvider.get().apply(envVariableName);
        }

        if(value == null) {
            value = System.getProperty(envVariableName.replaceAll("_", ".").toLowerCase());
        }

        if(value != null) {
            return Optional.of(new Var(value));
        } else {
            return Optional.empty();
        }
    }

    static String readFile(String varFile) {
        StringBuilder result = new StringBuilder();
        try {
            try (Reader in = new FileReader(varFile)) {
                char [] buffer = new char[1024];
                for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                    result.append(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("error reading environment variable from file " + varFile, e);
        }
        return result.toString().trim();
    }

    class Var {
        static public Var value(String value) {
            return new Var(value);
        }

        private final String rawValue;

        public Var(String rawValue) {
            this.rawValue = rawValue;
        }

        public String asString() {
            return this.rawValue;
        }

        public List<String> asList(String separator) {
            List<String> result = new LinkedList<>();

            String str = this.rawValue;
            while(str.indexOf(separator) != -1) {
                String val = str.substring(0, str.indexOf(separator));
                if(! val.isEmpty()) {
                    result.add(val);
                }
                str = str.substring(str.indexOf(separator) + separator.length());
            }

            if(! str.isEmpty()) {
                result.add(str);
            }

            return result;
        }

        public Set<String> asSet(String separator) {
            return new HashSet<>(this.asList(separator));
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

        public File asFile() {
            return new File(this.rawValue);
        }
    }

}

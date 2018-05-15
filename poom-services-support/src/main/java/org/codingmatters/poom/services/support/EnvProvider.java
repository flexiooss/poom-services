package org.codingmatters.poom.services.support;

import java.util.function.Function;

public class EnvProvider {

    public static final Function<String, String> DEFAULT = s -> System.getenv(s);
    private static Function<String, String> provider = DEFAULT;

    static Function<String, String> get() {
        return provider;
    }

    static public void provider(Function<String, String> p) {
        provider = p;
    }

    static public void reset() {
        provider = DEFAULT;
    }
}

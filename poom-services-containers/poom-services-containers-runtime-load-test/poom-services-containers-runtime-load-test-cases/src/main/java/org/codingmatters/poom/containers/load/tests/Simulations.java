package org.codingmatters.poom.containers.load.tests;

import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.http.HttpDsl.http;

public class Simulations {
    static public HttpProtocolBuilder sut() {
        String url = String.format("%s://%s:%s%s",
                System.getProperty("sut.protocol", "http"),
                System.getProperty("sut.host", "localhost"),
                Integer.getInteger("sut.port", 8888),
                System.getProperty("sut.path", "/raw")
        );

        HttpProtocolBuilder httpProtocol = http // 4
                .baseUrl(url) // 5
                .doNotTrackHeader("1")
                .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");
        return httpProtocol;
    }

    static public Params paramsFromPrefix(String prefix) {
        return new Params(
                Integer.getInteger(prefix + ".ramp.users", 1000),
                Long.getLong(prefix + ".during", 60)
        );
    }

    static public class Params {
        public final int rampUsers;
        public final long durationSeconds;

        public Params(int rampUsers, long durationSeconds) {
            this.rampUsers = rampUsers;
            this.durationSeconds = durationSeconds;
        }

        @Override
        public String toString() {
            return "Params{" +
                    "rampUsers=" + rampUsers +
                    ", durationSeconds=" + durationSeconds +
                    '}';
        }
    }


}

package org.codingmatters.poom.containers.load.tests;

import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.gatling.javaapi.http.HttpDsl.http;

public class Simulations {
    static public HttpProtocolBuilder sut() {
        String host = System.getProperty("sut.host", "localhost");
        String url = String.format("%s://%s:%s%s",
                System.getProperty("sut.protocol", "http"),
                host,
                Integer.getInteger("sut.port", 8888),
                System.getProperty("sut.path", "/raw")
        );

        HttpProtocolBuilder httpProtocol = http // 4
                .baseUrl(url) // 5
                .doNotTrackHeader("1")
                .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

        //https://gatling.io/docs/gatling/reference/current/http/protocol/
        boolean useH2c = System.getProperty("sut.use.h2c", "false").equals("true");
        System.out.printf("########################## H2C :: %s##########################\n", useH2c);
        if(useH2c) {
            Map<String, Boolean> priorKnowledge = new LinkedHashMap<>();
            priorKnowledge.put(host, true);
            httpProtocol.enableHttp2().http2PriorKnowledge(priorKnowledge);
        }

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

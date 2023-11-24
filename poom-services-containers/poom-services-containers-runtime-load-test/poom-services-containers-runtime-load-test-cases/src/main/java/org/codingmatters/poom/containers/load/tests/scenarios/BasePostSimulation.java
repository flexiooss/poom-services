package org.codingmatters.poom.containers.load.tests.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class BasePostSimulation extends Simulation { // 3

    HttpProtocolBuilder httpProtocol = http // 4
            .baseUrl("http://localhost:8888") // 5
            .doNotTrackHeader("1")
            .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

    File upload;

    {
        try {
            upload = File.createTempFile("upload", ".bin");
            upload.deleteOnExit();
            byte[] buffer = new byte[1024];
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = 1;
            }
            try(OutputStream out = new FileOutputStream(upload)) {
                for (int i = 0; i < 1024; i++) {
                    out.write(buffer, 0, buffer.length);
                }
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ScenarioBuilder scn = scenario("Post Simulation") // 7
            .exec(http("post")
                    .post("/sut")
                    .body(RawFileBody(upload.getAbsolutePath()))
                    .check(status().find().shouldBe(200))
                    .check(bodyString().shouldBe("ok"))
            )
            .pause(1);

    {
        setUp(
                scn.injectOpen(rampUsers(1000).during(60))
        ).protocols(httpProtocol);
    }
}
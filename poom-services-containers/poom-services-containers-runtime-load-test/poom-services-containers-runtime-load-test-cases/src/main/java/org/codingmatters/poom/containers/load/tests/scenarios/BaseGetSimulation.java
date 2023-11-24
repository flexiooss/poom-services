package org.codingmatters.poom.containers.load.tests.scenarios;


import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class BaseGetSimulation extends Simulation { // 3

    HttpProtocolBuilder httpProtocol = http // 4
            .baseUrl("http://localhost:8888") // 5
            .doNotTrackHeader("1")
            .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

    ScenarioBuilder scn = scenario("Get Simulation") // 7
            .exec(http("get")
                    .get("/sut")
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

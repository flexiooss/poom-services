package scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.codingmatters.poom.containers.load.tests.Simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class BasePostSimulation extends Simulation { // 3
    ScenarioBuilder scn = scenario("Post Simulation") // 7
            .exec(http("post")
                    .post("/structured/plouf")
                    .body(StringBody("{\"prop\":\"val\"}"))
                    .check(status().find().shouldBe(200))
                    .check(bodyString().shouldBe("{\"prop\":\"val\"}"))
            )
            .pause(1);

    {
        Simulations.Params params = Simulations.paramsFromPrefix("base.post");
        setUp(
                scn.injectOpen(rampUsers(params.rampUsers).during(params.durationSeconds))
        ).protocols(Simulations.sut());
    }
}
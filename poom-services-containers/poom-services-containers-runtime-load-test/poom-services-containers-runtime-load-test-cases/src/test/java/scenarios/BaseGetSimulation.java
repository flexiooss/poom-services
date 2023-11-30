package scenarios;


import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import org.codingmatters.poom.containers.load.tests.Simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class BaseGetSimulation extends Simulation { // 3

    ScenarioBuilder scn = scenario("Get Simulation") // 7
            .exec(http("get")
                    .get("/structured/plouf")
                    .queryParam("qparam", "plaf")
                    .check(status().find().shouldBe(200))
                    .check(bodyString().shouldBe("{\"prop\":\"uparam=plouf ; qparam=plaf\"}"))
            )
            .pause(1);

    {
        Simulations.Params params = Simulations.paramsFromPrefix("base.get");
        setUp(
                scn.injectOpen(rampUsers(params.rampUsers).during(params.durationSeconds))
        ).protocols(Simulations.sut());
    }
}

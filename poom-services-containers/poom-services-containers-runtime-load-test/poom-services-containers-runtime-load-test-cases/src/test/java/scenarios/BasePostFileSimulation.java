package scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.codingmatters.poom.containers.load.tests.Simulations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class BasePostFileSimulation extends Simulation { // 3
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

    ScenarioBuilder scn = scenario("Post File Simulation") // 7
            .exec(http("post")
                    .post("/file")
                    .body(RawFileBody(upload.getAbsolutePath()))
                    .check(status().find().shouldBe(200))
                    .check(bodyString().shouldBe(String.format("{\"prop\":\"read %s bytes\"}", 1024 * 1024)))
            )
            .pause(1);

    {
        Simulations.Params params = Simulations.paramsFromPrefix("base.post.file");
        setUp(
                scn.injectOpen(rampUsers(params.rampUsers).during(params.durationSeconds))
        ).protocols(Simulations.sut());
    }
}
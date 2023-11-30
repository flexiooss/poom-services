package org.codingmatters.poom.containers.load.tests.sut.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.ApiContainerRuntimeBuilder;
import org.codingmatters.poom.containers.load.tests.sut.api.RawApi;
import org.codingmatters.poom.containers.load.tests.sut.api.SutApi;

public class SutService {
    static public void main(ApiContainerRuntime runtime, JsonFactory jasonFactory) {
        runtime = new ApiContainerRuntimeBuilder()
                .withApi(new RawApi(jasonFactory))
                .withApi(new SutApi(jasonFactory))
                .build(runtime);
        runtime.main();
    }
}

package org.codingmatters.poom.containers.load.tests.sut.api.org.codingmatters.poom.containers.load.tests.sut.service;

import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.ApiContainerRuntimeBuilder;
import org.codingmatters.poom.containers.load.tests.sut.api.SutApi;

public class SutService {
    static public void main(ApiContainerRuntime runtime) {
        runtime = new ApiContainerRuntimeBuilder()
                .withApi(new SutApi())
                .build(runtime);
        runtime.main();
    }
}

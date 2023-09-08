package org.codingmatters.poom.containers.runtime.undertow;

import org.codingmatters.poom.containers.acceptance.ApiContainerRuntimeAcceptanceTest;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class UndertowApiContainerRuntimeTest extends ApiContainerRuntimeAcceptanceTest {

    @Override
    protected UndertowApiContainerRuntime createContainer(String host, int port, CategorizedLogger logger) {
        return new UndertowApiContainerRuntime(
                host,
                port,
                logger
        );
    }

}
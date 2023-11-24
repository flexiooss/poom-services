package org.codingmatters.poom.containers.load.tests.sut.api.org.codingmatters.poom.containers.load.tests.sut.service;

import org.codingmatters.poom.containers.runtime.undertow.UndertowApiContainerRuntime;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class UndertowSutService extends SutService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(UndertowSutService.class);

    public static void main(String[] args) {
        SutService.main(new UndertowApiContainerRuntime(
                args != null && args.length >= 1 ? args[0] : "0.0.0.0",
                args != null && args.length >= 2 ? Integer.parseInt(args[1]) : 8888,
                log
        ));
    }
}

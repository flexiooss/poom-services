package org.codingmatters.poom.containers.load.tests.sut.api.org.codingmatters.poom.containers.load.tests.sut.service;

import org.codingmatters.poom.containers.runtime.netty.NettyApiContainerRuntime;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class NettySutService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(NettySutService.class);

    public static void main(String[] args) {
        SutService.main(new NettyApiContainerRuntime(
                args != null && args.length >= 1 ? args[0] : "0.0.0.0",
                args != null && args.length >= 2 ? Integer.parseInt(args[1]) : 8888,
                log
        ));
    }
}

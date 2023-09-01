package org.codingmatters.poom.containers.runtime.undertow;

import org.codingmatters.poom.containers.ApiContainerRuntimeBuilder;
import org.codingmatters.poom.containers.RuntimeTestHandle;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class UndertowApiContainerRuntimeTest {
    private int freePort;
    private RuntimeTestHandle runtime;

    @Before
    public void setUp() throws Exception {
        try {
            ServerSocket freePortSocket = new ServerSocket(0);
            this.freePort = freePortSocket.getLocalPort();
            freePortSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.runtime = new RuntimeTestHandle(new UndertowApiContainerRuntime(
                "localhost",
                this.freePort,
                CategorizedLogger.getLogger("test-runtime"))
        );
    }

    @After
    public void tearDown() throws Exception {
        this.runtime.doStop();
    }

    @Test
    public void given__when__then() throws Exception {
        new ApiContainerRuntimeBuilder().build(this.runtime.runtime());
        this.runtime.doStart();

    }
}
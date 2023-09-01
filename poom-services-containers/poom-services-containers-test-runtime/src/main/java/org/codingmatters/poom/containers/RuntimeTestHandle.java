package org.codingmatters.poom.containers;

public class RuntimeTestHandle {
    private final ApiContainerRuntime handled;

    public RuntimeTestHandle(ApiContainerRuntime handled) {
        this.handled = handled;
    }

    public void doStart() throws ServerStartupException {
        this.handled.startup();
    }

    public void doStop() throws ServerShutdownException {
        this.handled.stop();
    }

    public ApiContainerRuntime runtime() {
        return this.handled;
    }
}

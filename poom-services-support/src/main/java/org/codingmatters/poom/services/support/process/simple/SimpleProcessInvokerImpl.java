package org.codingmatters.poom.services.support.process.simple;

import org.codingmatters.poom.services.support.process.ProcessInvoker;
import org.codingmatters.poom.services.support.process.SimpleProcessInvoker;

import java.io.IOException;
import java.util.Arrays;

public class SimpleProcessInvokerImpl implements SimpleProcessInvoker {
    private final ProcessInvoker invoker;
    private final ProcessBuilder defaults;

    public SimpleProcessInvokerImpl(ProcessInvoker invoker, ProcessBuilder defaults) {
        this.invoker = invoker;
        this.defaults = defaults;
    }

    @Override
    public ProcessResponse invoke(String... cmd) throws ProcessException {
        try {
            Lines lines = new Lines();
            int status = this.invoker.exec(this.defaults.command(cmd), lines, lines);
            return new ProcessResponse(status, lines.out(), lines.err());
        } catch (IOException | InterruptedException e) {
            throw new ProcessException("error invoking" + (cmd == null ? "" : " " + Arrays.toString(cmd)), e);
        }
    }
}

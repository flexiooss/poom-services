package org.codingmatters.poom.services.support.process.simple;

import org.codingmatters.poom.services.support.process.ProcessInvoker;

public class Lines implements ProcessInvoker.OutputListener, ProcessInvoker.ErrorListener {
    private final StringBuilder out = new StringBuilder();
    private final StringBuilder err = new StringBuilder();
    @Override
    public void output(String line) {
        out.append(line).append("\n");
    }

    @Override
    public void error(String line) {
        err.append(line).append("\n");
    }

    public String out() {
        return this.out.toString();
    }
    public String err() {
        return this.err.toString();
    }
}

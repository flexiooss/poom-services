package org.codingmatters.poom.services.support.process;

public class SimpleProcessInvoker {
    private final ProcessInvoker invoker;
    private final ProcessBuilder defaults;

    public SimpleProcessInvoker(ProcessInvoker invoker, ProcessBuilder defaults) {
        this.invoker = invoker;
        this.defaults = defaults;
    }

    static public class Response {
        private final int status;
        private final String out;
        private final String err;

        public Response(int status, String out, String err) {
            this.status = status;
            this.out = out;
            this.err = err;
        }

        public int status() {
            return status;
        }

        public String out() {
            return out;
        }

        public String err() {
            return err;
        }
    }

}

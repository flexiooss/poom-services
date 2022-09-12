package org.codingmatters.poom.services.support.process;

import org.codingmatters.poom.services.support.process.simple.SimpleProcessInvokerImpl;

public interface SimpleProcessInvoker {

    static SimpleProcessInvokerImpl invoker(ProcessInvoker invoker, ProcessBuilder defaults) {
        return new SimpleProcessInvokerImpl(invoker, defaults);
    }

    ProcessResponse invoke(String ... cmd) throws ProcessException;

    class ProcessResponse {
        private final int status;
        private final String out;
        private final String err;

        public ProcessResponse(int status, String out, String err) {
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

        public boolean isError() {
            return this.status != 0;
        }

        @Override
        public String toString() {
            return "ProcessResponse{" +
                    "status=" + status +
                    ", out='" + out + '\'' +
                    ", err='" + err + '\'' +
                    '}';
        }
    }

    class ProcessException extends Exception {
        public ProcessException(String message) {
            super(message);
        }

        public ProcessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package org.codingmatters.poom.services.support.process;

import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProcessInvoker {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ProcessInvoker.class);

    static public final long DEFAULT_TIMEOUT = 120;
    static public final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    static public final long DEFAULT_SHUTDOWN_TIMEOUT = 30;
    static public final TimeUnit DEFAULT_SHUTDOWN_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final long timeout;
    private final TimeUnit timeoutUnit;

    private final long shutdownTimeout;
    private final TimeUnit shutdownTimeoutUnit;

    public ProcessInvoker() {
        this(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
    }

    public ProcessInvoker(long timeout, TimeUnit timeoutUnit) {
        this(timeout, timeoutUnit, DEFAULT_SHUTDOWN_TIMEOUT, DEFAULT_SHUTDOWN_TIMEOUT_UNIT);
    }

    public ProcessInvoker(long timeout, TimeUnit timeoutUnit, long shutdownTimeout, TimeUnit shutdownTimeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.shutdownTimeout = shutdownTimeout;
        this.shutdownTimeoutUnit = shutdownTimeoutUnit;
    }

    public int exec(ProcessBuilder processBuilder, OutputListener outputListener, ErrorListener errorListener) throws IOException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Process process = processBuilder.start();
            pool.submit(() -> this.consumeOutput(process.getInputStream(), outputListener));
            pool.submit(() -> this.consumeError(process.getErrorStream(), errorListener));

            process.waitFor(this.timeout, this.timeoutUnit);
            if(process.isAlive()) {
                log.warn("process forcibly stopped after a {} {} timeout", this.timeout, this.timeoutUnit.name());
                process.destroyForcibly();
                return -1;
            } else {
                int exitValue = process.exitValue();
                log.info("process normally finished with status {}", exitValue);
                return exitValue;
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(this.shutdownTimeout, this.shutdownTimeoutUnit);
            if(! pool.isTerminated()) {
                pool.shutdownNow();
                log.performanceAlert().error("process invoker out/err threads are forcibly stopped");
            }
        }
    }

    private void consumeOutput(InputStream in, OutputListener outputListener) {
        try {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                for(String line = reader.readLine() ; line != null ; line = reader.readLine()) {
                    if(outputListener != null) {
                        outputListener.output(line);
                    }
                }
            }
        } catch (IOException e) {
            log.performanceAlert().error("error reading process output, this can lead to memory problems", e);
        }
    }

    private void consumeError(InputStream in, ErrorListener errorListener) {
        try {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                for(String line = reader.readLine() ; line != null ; line = reader.readLine()) {
                    if(errorListener != null) {
                        errorListener.error(line);
                    }
                }
            }
        } catch (IOException e) {
            log.performanceAlert().error("error reading process error, this can lead to memory problems", e);
        }
    }

    @FunctionalInterface
    public interface OutputListener {
        void output(String line);
    }
    @FunctionalInterface
    public interface ErrorListener {
        void error(String line);
    }
}

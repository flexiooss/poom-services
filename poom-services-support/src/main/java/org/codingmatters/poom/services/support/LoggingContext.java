package org.codingmatters.poom.services.support;

import org.slf4j.MDC;

import java.io.Closeable;
import java.util.Map;

/**
 * Created by nelt on 6/17/17.
 */
public class LoggingContext implements Closeable {

    private final Map<String, String> previous;

    static public LoggingContext start() {
        return new LoggingContext();
    }

    private LoggingContext() {
        this.previous = MDC.getCopyOfContextMap();
    }

    @Override
    public void close() {
        if(this.previous == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(this.previous);
        }
    }
}

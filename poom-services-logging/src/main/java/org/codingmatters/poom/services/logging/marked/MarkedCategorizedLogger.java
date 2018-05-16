package org.codingmatters.poom.services.logging.marked;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.logging.Log;
import org.codingmatters.poom.services.logging.TokenizedLog;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class MarkedCategorizedLogger implements CategorizedLogger {
    static private Marker PERSONAL_DATA = MarkerFactory.getMarker("PERSONAL_DATA");
    static private Marker CONFIDENTIAL = MarkerFactory.getMarker("CONFIDENTIAL");
    static private Marker MESSAGE = MarkerFactory.getMarker("MESSAGE");
    static private Marker AUDIT = MarkerFactory.getMarker("AUDIT");
    static private Marker PERF = MarkerFactory.getMarker("PERF");

    private final Log personalDataLog;
    private final Log confidentialLog;
    private final Log messageLog;
    private final Log auditLog;
    private final Log perfLog;

    public MarkedCategorizedLogger(Logger logger) {
        this.personalDataLog = new MarkedLog(PERSONAL_DATA, logger);
        this.confidentialLog = new MarkedLog(CONFIDENTIAL, logger);
        this.messageLog = new MarkedLog(MESSAGE, logger);
        this.auditLog = new MarkedLog(AUDIT, logger);
        this.perfLog = new MarkedLog(PERF, logger);
    }

    @Override
    public Log personalData() {
        return personalDataLog;
    }

    @Override
    public Log confidential() {
        return confidentialLog;
    }

    @Override
    public Log audit() {
        return auditLog;
    }

    @Override
    public Log performanceAlert() {
        return this.perfLog;
    }


    @Override
    public void trace(String msg) {
        messageLog.trace(msg);
    }

    @Override
    public void trace(String format, Object... arguments) {
        messageLog.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        messageLog.trace(msg, t);
    }

    @Override
    public void debug(String msg) {
        messageLog.debug(msg);
    }

    @Override
    public void debug(String format, Object... arguments) {
        messageLog.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        messageLog.debug(msg, t);
    }

    @Override
    public void info(String msg) {
        messageLog.info(msg);
    }

    @Override
    public void info(String format, Object... arguments) {
        messageLog.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        messageLog.info(msg, t);
    }

    @Override
    public void warn(String msg) {
        messageLog.warn(msg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        messageLog.warn(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        messageLog.warn(msg, t);
    }

    @Override
    public void error(String msg) {
        messageLog.error(msg);
    }

    @Override
    public void error(String format, Object... arguments) {
        messageLog.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        messageLog.error(msg, t);
    }

    @Override
    public TokenizedLog tokenized() {
        return messageLog.tokenized();
    }
}

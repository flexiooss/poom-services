package org.codingmatters.poom.services.logging;

public interface MessageLog {
    void trace(String msg);

    void trace(String format, Object... arguments);

    void debug(String msg);

    void debug(String format, Object... arguments);

    void info(String msg);

    void info(String format, Object... arguments);

    void warn(String msg);

    void warn(String format, Object... arguments);

    void error(String msg);

    void error(String format, Object... arguments);
}

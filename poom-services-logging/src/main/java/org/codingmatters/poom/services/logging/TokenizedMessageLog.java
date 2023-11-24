package org.codingmatters.poom.services.logging;

public interface TokenizedMessageLog {
    String trace(String msg);

    String trace(String format, Object... arguments);

    String debug(String msg);

    String debug(String format, Object... arguments);

    String info(String msg);

    String info(String format, Object... arguments);

    String warn(String msg);

    String warn(String format, Object... arguments);

    String error(String msg);

    String error(String format, Object... arguments);
}

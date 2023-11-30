package org.codingmatters.poom.services.logging;

public interface Log extends MessageLog {

    void trace(String msg, Throwable t);

    void debug(String msg, Throwable t);

    void info(String msg, Throwable t);

    void warn(String msg, Throwable t);

    void error(String msg, Throwable t);

    TokenizedLog tokenized();

    MessageLog withThrown(Throwable t);
}

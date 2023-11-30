package org.codingmatters.poom.services.logging;

public interface TokenizedLog extends TokenizedMessageLog {

    String trace(String msg, Throwable t);

    String debug(String msg, Throwable t);

    String info(String msg, Throwable t);

    String warn(String msg, Throwable t);

    String error(String msg, Throwable t);

    TokenizedMessageLog withThrown(Throwable t);

}

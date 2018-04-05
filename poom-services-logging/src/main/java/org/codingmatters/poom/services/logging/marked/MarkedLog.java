package org.codingmatters.poom.services.logging.marked;

import org.codingmatters.poom.services.logging.Log;
import org.codingmatters.poom.services.logging.TokenizedLog;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.UUID;

public class MarkedLog implements Log {
    
    private final Marker marker;
    private final Logger log;
    private final ThreadLocal<TokenizedMarkedLog> perThread = ThreadLocal.withInitial(
            () -> new TokenizedMarkedLog(this)
    );

    public MarkedLog(Marker marker, Logger log) {
        this.marker = marker;
        this.log = log;
    }
    
    @Override
    public void trace(String msg) {
        this.log.trace(this.marker, msg);
    }

    @Override
    public void trace(String format, Object... arguments) {
        this.log.trace(this.marker, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        this.log.trace(this.marker, msg, t);
    }

    @Override
    public void debug(String msg) {
        this.log.debug(this.marker, msg);
    }

    @Override
    public void debug(String format, Object... arguments) {
        this.log.debug(this.marker, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        this.log.debug(this.marker, msg, t);
    }

    @Override
    public void info(String msg) {
        this.log.info(this.marker, msg);
    }

    @Override
    public void info(String format, Object... arguments) {
        this.log.info(this.marker, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        this.log.info(this.marker, msg, t);
    }

    @Override
    public void warn(String msg) {
        this.log.warn(this.marker, msg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        this.log.warn(this.marker, format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        this.log.warn(this.marker, msg, t);
    }

    @Override
    public void error(String msg) {
        this.log.error(this.marker, msg);
    }

    @Override
    public void error(String format, Object... arguments) {
        this.log.error(this.marker, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        this.log.error(this.marker, msg, t);
    }

    @Override
    public TokenizedLog tokenized() {
        String token = UUID.randomUUID().toString();
        MDC.put("token", token);
        return this.perThread.get().token(token);
    }
    
    static private class TokenizedMarkedLog implements TokenizedLog {
        private final MarkedLog deleguate;
        private String token;

        private TokenizedMarkedLog(MarkedLog deleguate) {
            this.deleguate = deleguate;
        }

        public TokenizedMarkedLog token(String token) {
            this.token = token;
            return this;
        }

        @Override
        public String trace(String msg) {
            this.deleguate.trace(msg);
            return token;
        }

        @Override
        public String trace(String format, Object... arguments) {
            this.deleguate.trace(format, arguments);
            return token;
        }

        @Override
        public String trace(String msg, Throwable t) {
            this.deleguate.trace(msg, t);
            return token;
        }

        @Override
        public String debug(String msg) {
            this.deleguate.debug(msg);
            return token;
        }

        @Override
        public String debug(String format, Object... arguments) {
            this.deleguate.debug(format, arguments);
            return token;
        }

        @Override
        public String debug(String msg, Throwable t) {
            this.deleguate.debug(msg, t);
            return token;
        }

        @Override
        public String info(String msg) {
            this.deleguate.info(msg);
            return token;
        }

        @Override
        public String info(String format, Object... arguments) {
            this.deleguate.info(format, arguments);
            return token;
        }

        @Override
        public String info(String msg, Throwable t) {
            this.deleguate.info(msg, t);
            return token;
        }

        @Override
        public String warn(String msg) {
            this.deleguate.warn(msg);
            return token;
        }

        @Override
        public String warn(String format, Object... arguments) {
            this.deleguate.warn(format, arguments);
            return token;
        }

        @Override
        public String warn(String msg, Throwable t) {
            this.deleguate.warn(msg, t);
            return token;
        }

        @Override
        public String error(String msg) {
            this.deleguate.error(msg);
            return token;
        }

        @Override
        public String error(String format, Object... arguments) {
            this.deleguate.error(format, arguments);
            return token;
        }

        @Override
        public String error(String msg, Throwable t) {
            this.deleguate.error(msg, t);
            return token;
        }
    }
}

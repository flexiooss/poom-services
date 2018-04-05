package org.codingmatters.poom.services.logging;

import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CategorizedLoggerTest {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(CategorizedLoggerTest.class);
    private LoggingContext ctx;

    @Before
    public void setUp() throws Exception {
        this.ctx = LoggingContext.start();
    }

    @After
    public void tearDown() throws Exception {
        this.ctx.close();
    }

    @Test
    public void message() {
        log.info("yopyop");
        log.info("yopyop {} {} {}", "a", "b", "c");
        log.info("yopyop", new Throwable());
        log.info("Lorem ipsum dolor sit amet, consectetur adipiscing elit. In posuere imperdiet urna at dignissim. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nam non arcu sapien.");
    }

    @Test
    public void tokenizedMessage() {
        System.out.println(log.tokenized().info("yopyop"));
        System.out.println(log.tokenized().info("yopyop {} {} {}", "a", "b", "c"));
        System.out.println(log.tokenized().info("yopyop", new Throwable()));
    }

    @Test
    public void audit() {
        log.audit().info("yopyop");
        log.audit().info("yopyop {} {} {}", "a", "b", "c");
        log.audit().info("yopyop", new Throwable());
    }

    @Test
    public void tokenizedAudit() {
        System.out.println(log.audit().tokenized().info("yopyop"));
        System.out.println(log.audit().tokenized().info("yopyop {} {} {}", "a", "b", "c"));
        System.out.println(log.audit().tokenized().info("yopyop", new Throwable()));
    }
}
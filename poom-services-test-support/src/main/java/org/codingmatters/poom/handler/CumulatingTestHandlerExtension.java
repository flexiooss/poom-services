package org.codingmatters.poom.handler;

import org.junit.jupiter.api.extension.*;

public abstract class CumulatingTestHandlerExtension<Req, Resp> extends CumulatingTestHandler<Req, Resp> implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        System.out.println("afterEach :: " + context.getDisplayName());
        this.cleanup();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        System.out.println("beforeEach :: " + context.getDisplayName());
        this.initialize();
    }
}

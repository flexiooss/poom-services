package org.codingmatters.poom.demo.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.apis.demo.api.DemoHandlers;
import org.codingmatters.poom.apis.demo.processor.DemoProcessor;
import org.codingmatters.poom.demo.processor.DemoHandlersBuilder;
import org.codingmatters.poom.demo.service.support.DemoData;
import org.codingmatters.poom.demo.service.support.StoreManagerSupport;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DemoService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DemoService.class);
    private static final String API_PATH = "API_PATH";

    public static void main(String[] args) {
        StoreManagerSupport storeManagerSupport = new StoreManagerSupport();
        new DemoData(storeManagerSupport).create();

        DemoService service = new DemoService(
                Env.optional(Env.SERVICE_HOST).orElse(new Env.Var("0.0.0.0")).asString(),
                Env.optional(Env.SERVICE_PORT).orElse(new Env.Var("8889")).asInteger(),
                Env.optional(API_PATH).orElseGet(() -> new Env.Var("/demo")).asString(),
                storeManagerSupport);

        service.start();
        try {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } finally {
            service.stop();
        }
    }

    private final String host;
    private final int port;
    private final String apiPath;
    private JsonFactory jsonFactory = new JsonFactory();
    private DemoHandlers handlers;

    private final StoreManagerSupport storeManagerSupport;

    private final ExecutorService pool;

    private Undertow server;

    public DemoService(String host, int port, String apiPath, StoreManagerSupport storeManagerSupport) {
        this.host = host;
        this.port = port;
        this.apiPath = apiPath;
        this.storeManagerSupport = storeManagerSupport;
        this.pool = Executors.newSingleThreadExecutor();
        this.handlers = new DemoHandlersBuilder(this.storeManagerSupport.createStoreManager(this.pool)).build();
    }

    public void start() {
        log.info("starting demo service (%s:%s with base path %s)...", this.host, this.port, this.apiPath);
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(new DemoProcessor(this.apiPath, this.jsonFactory, this.handlers)))
                .build();
        this.server.start();
        log.info("demo service started.");
    }

    public void stop() {
        log.info("stopping demo service...");
        this.server.stop();
        log.info("demo service stopped.");
    }
}

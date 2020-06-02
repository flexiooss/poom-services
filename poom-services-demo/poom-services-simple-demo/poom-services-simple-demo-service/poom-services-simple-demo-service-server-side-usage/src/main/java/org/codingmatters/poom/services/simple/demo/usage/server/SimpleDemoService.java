package org.codingmatters.poom.services.simple.demo.usage.server;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.apis.simple.demo.api.SimpleDemoHandlers;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.apis.simple.demo.processor.SimpleDemoProcessor;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.simple.demo.usage.server.data.SongDemoData;
import org.codingmatters.poom.services.simple.demo.usage.server.handlers.SimpleDemoHandlersBuilder;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class SimpleDemoService {

    static public final CategorizedLogger log = CategorizedLogger.getLogger(SimpleDemoService.class);
    private static final String API_PATH = "API_PATH";

    public static void main(String[] args) {
        Repository<Song, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Song.class);

        new SongDemoData().fill(repository);

        SimpleDemoService service = new SimpleDemoService(
                Env.optional(Env.SERVICE_HOST).orElse(new Env.Var("0.0.0.0")).asString(),
                Env.optional(Env.SERVICE_PORT).orElse(new Env.Var("8888")).asInteger(),
                Env.optional(API_PATH).orElseGet(() -> new Env.Var("/simple-demo")).asString(),
                repository);

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
    private final JsonFactory jasonFactory = new JsonFactory();
    private final SimpleDemoHandlers handlers;

    private Undertow server;

    public SimpleDemoService(String host, int port, String apiPath, Repository<Song, PropertyQuery> repository) {
        this.host = host;
        this.port = port;
        this.apiPath = apiPath;
        this.handlers = new SimpleDemoHandlersBuilder(repository).build();
    }

    public void start() {
        log.info("starting demo service (%s:%s with base path %s)...", this.host, this.port, this.apiPath);
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(
                        new SimpleDemoProcessor(this.apiPath, this.jasonFactory, this.handlers)
                ))
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

package org.codingmatters.poom.mcp.demo;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.processor.McpProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.util.concurrent.Executors;

public class NoteAssistantServer {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(NoteAssistantServer.class);

    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("SERVICE_HOST", "0.0.0.0");
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVICE_PORT", "8080"));

        NoteService noteService = new NoteService(NoteRepository.create());
        McpProcessor processor = new McpProcessor(
                "/mcp",
                new JsonFactory(),
                NoteAssistantDescriptor.build(noteService),
                Executors.newFixedThreadPool(4)
        );

        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(new CdmHttpUndertowHandler(processor))
                .build();
        server.start();
        log.info("Note Assistant MCP server started at http://{}:{}/mcp", host, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop();
        }));
    }
}

package org.codingmatters.poom.mcp.demo.resources;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.ReadResourceParams;
import org.codingmatters.poom.mcp.types.ReadResourceResult;
import org.codingmatters.poom.mcp.types.ResourceContent;

import java.util.function.Function;

public class NoteResourceHandler implements Function<ReadResourceParams, ReadResourceResult> {

    private static final String URI_PREFIX = "note://";
    private final NoteService noteService;

    public NoteResourceHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public ReadResourceResult apply(ReadResourceParams params) {
        String uri = params.uri();
        String id = uri.startsWith(URI_PREFIX) ? uri.substring(URI_PREFIX.length()) : uri;
        try {
            return noteService.get(id)
                    .map(e -> {
                        var n = e.value();
                        var tags = (n.opt().tags().isPresent() && !n.tags().isEmpty())
                                ? String.join(", ", n.tags())
                                : "";
                        var markdown = "# " + n.title() + "\n\n"
                                + (tags.isBlank() ? "" : "*Tags: " + tags + "*\n\n")
                                + n.content();
                        return ReadResourceResult.builder()
                                .contents(ResourceContent.builder()
                                        .uri(uri).mimeType("text/markdown").text(markdown)
                                        .build())
                                .build();
                    })
                    .orElseGet(() -> ReadResourceResult.builder()
                            .contents(ResourceContent.builder()
                                    .uri(uri).mimeType("text/plain")
                                    .text("Note not found: " + id)
                                    .build())
                            .build());
        } catch (Exception e) {
            return ReadResourceResult.builder()
                    .contents(ResourceContent.builder()
                            .uri(uri).mimeType("text/plain").text("Error: " + e.getMessage())
                            .build())
                    .build();
        }
    }
}

package org.codingmatters.poom.mcp.demo.resources;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.ReadResourceParams;
import org.codingmatters.poom.mcp.types.ReadResourceResult;
import org.codingmatters.poom.mcp.types.ResourceContent;

import java.util.function.Function;

public class TaggedNotesResourceHandler implements Function<ReadResourceParams, ReadResourceResult> {

    private static final String URI_PREFIX = "notes://tagged/";
    private final NoteService noteService;

    public TaggedNotesResourceHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public ReadResourceResult apply(ReadResourceParams params) {
        String uri = params.uri();
        String tag = uri.startsWith(URI_PREFIX) ? uri.substring(URI_PREFIX.length()) : uri;
        try {
            var notes = noteService.list(tag);
            String text;
            if (notes.isEmpty()) {
                text = "No notes tagged '" + tag + "'.";
            } else {
                var sb = new StringBuilder();
                for (var entity : notes) {
                    sb.append(entity.id()).append(" — ").append(entity.value().title()).append("\n");
                }
                text = sb.toString().stripTrailing();
            }
            return ReadResourceResult.builder()
                    .contents(ResourceContent.builder()
                            .uri(uri).mimeType("text/plain").text(text)
                            .build())
                    .build();
        } catch (Exception e) {
            return ReadResourceResult.builder()
                    .contents(ResourceContent.builder()
                            .uri(uri).mimeType("text/plain").text("Error: " + e.getMessage())
                            .build())
                    .build();
        }
    }
}

package org.codingmatters.poom.mcp.demo;

import org.codingmatters.poom.mcp.McpPromptDescriptor;
import org.codingmatters.poom.mcp.McpResourceDescriptor;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.demo.prompts.CompareNotesPromptHandler;
import org.codingmatters.poom.mcp.demo.prompts.SummarizeNotePromptHandler;
import org.codingmatters.poom.mcp.demo.resources.NoteResourceHandler;
import org.codingmatters.poom.mcp.demo.resources.TaggedNotesResourceHandler;
import org.codingmatters.poom.mcp.demo.tools.*;
import org.codingmatters.value.objects.values.ObjectValue;

public class NoteAssistantDescriptor {

    private NoteAssistantDescriptor() {}

    public static McpServerDescriptor build(NoteService noteService) {
        return McpServerDescriptor.builder()
                .name("note-assistant")
                .version("1.0.0")
                .tools(
                        createNoteTool(noteService),
                        getNoteTool(noteService),
                        updateNoteTool(noteService),
                        deleteNoteTool(noteService),
                        listNotesTool(noteService),
                        searchNotesTool(noteService)
                )
                .resources(
                        McpResourceDescriptor.builder()
                                .uri("note://{id}")
                                .name("Note by id")
                                .mimeType("text/markdown")
                                .handler(new NoteResourceHandler(noteService))
                                .build(),
                        McpResourceDescriptor.builder()
                                .uri("notes://tagged/{tag}")
                                .name("Notes by tag")
                                .mimeType("text/plain")
                                .handler(new TaggedNotesResourceHandler(noteService))
                                .build()
                )
                .prompts(
                        McpPromptDescriptor.builder()
                                .name("summarize_note")
                                .description("Generates a prompt asking the model to summarize a note. Argument: note_id (string).")
                                .handler(new SummarizeNotePromptHandler(noteService))
                                .build(),
                        McpPromptDescriptor.builder()
                                .name("compare_notes")
                                .description("Generates a prompt asking the model to compare two notes. Arguments: note_id_1, note_id_2 (strings).")
                                .handler(new CompareNotesPromptHandler(noteService))
                                .build()
                )
                .build();
    }

    private static McpToolDescriptor createNoteTool(NoteService s) {
        return McpToolDescriptor.builder()
                .name("create_note")
                .description("Creates a new note. Returns the note id.")
                .inputSchema(schema(
                        Note.names_().title(), "string", "Title of the note (required)",
                        Note.names_().content(), "string", "Body of the note (required)",
                        Note.names_().tags(), "array", "Optional list of string tags"
                ))
                .handler(new CreateNoteTool(s))
                .build();
    }

    private static McpToolDescriptor getNoteTool(NoteService s) {
        return McpToolDescriptor.builder()
                .name("get_note")
                .description("Retrieves a note by id. Returns its full content.")
                .inputSchema(schema("id", "string", "Note id (required)"))
                .handler(new GetNoteTool(s))
                .build();
    }

    private static McpToolDescriptor updateNoteTool(NoteService s) {
        return McpToolDescriptor.builder()
                .name("update_note")
                .description("Updates one or more fields of an existing note. Only provided fields are changed.")
                .inputSchema(schema(
                        "id", "string", "Note id (required)",
                        Note.names_().title(), "string", "New title (optional)",
                        Note.names_().content(), "string", "New content (optional)",
                        Note.names_().tags(), "array", "New tag list (optional, replaces all existing tags)"
                ))
                .handler(new UpdateNoteTool(s))
                .build();
    }

    private static McpToolDescriptor deleteNoteTool(NoteService s) {
        return McpToolDescriptor.builder()
                .name("delete_note")
                .description("Permanently deletes a note by id.")
                .inputSchema(schema("id", "string", "Note id (required)"))
                .handler(new DeleteNoteTool(s))
                .build();
    }

    private static McpToolDescriptor listNotesTool(NoteService s) {
        return McpToolDescriptor.builder()
                .name("list_notes")
                .description("Lists all notes, optionally filtered by tag.")
                .inputSchema(schema("tag", "string", "Optional tag to filter by"))
                .handler(new ListNotesTool(s))
                .build();
    }

    private static McpToolDescriptor searchNotesTool(NoteService s) {
        return McpToolDescriptor.builder()
                .name("search_notes")
                .description(
                        "Full-text search across note titles and content. " +
                        "May take several seconds — result is pushed asynchronously via SSE.")
                .inputSchema(schema("query", "string", "Search terms (required)"))
                .handler(new SearchNotesTool(s))
                .build();
    }

    /**
     * Builds a minimal JSON Schema ObjectValue from (name, type, description) triples.
     * Each triple declares one property in the schema's "properties" object.
     * Note: the "required" array is intentionally omitted — required/optional semantics
     * are enforced at runtime inside each tool handler.
     */
    private static ObjectValue schema(String... tripleNameTypeDesc) {
        ObjectValue.Builder props = ObjectValue.builder();
        for (int i = 0; i < tripleNameTypeDesc.length; i += 3) {
            final String n = tripleNameTypeDesc[i];
            final String t = tripleNameTypeDesc[i + 1];
            final String d = tripleNameTypeDesc[i + 2];
            props.property(n, v -> v.objectValue(ObjectValue.builder()
                    .property("type", tt -> tt.stringValue(t))
                    .property("description", dd -> dd.stringValue(d))
                    .build()));
        }
        return ObjectValue.builder()
                .property("type", v -> v.stringValue("object"))
                .property("properties", v -> v.objectValue(props.build()))
                .build();
    }
}

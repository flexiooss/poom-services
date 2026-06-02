package org.codingmatters.poom.mcp.demo;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.processor.McpProcessor;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.codingmatters.rest.tests.api.TestSseChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class NoteAssistantIntegrationTest {

    private static final String URL = "http://test/mcp";

    private final JsonFactory jsonFactory = new JsonFactory();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private McpProcessor processor;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        NoteService noteService = new NoteService(NoteRepository.create());
        processor = new McpProcessor(
                "/mcp",
                jsonFactory,
                NoteAssistantDescriptor.build(noteService),
                pool,
                500
        );
        sessionId = initialize();
    }

    // -------------------------------------------------------------------------
    // Session lifecycle
    // -------------------------------------------------------------------------

    @Test
    void initialize_returnsSessionId() {
        assertThat(sessionId, notNullValue());
        assertThat(sessionId, not(emptyString()));
    }

    @Test
    void deleteSession_thenSubsequentPostReturns400() throws Exception {
        deleteSession();
        TestResponseDeleguate resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"1\"}");
        assertThat(resp.status(), is(400));
    }

    // -------------------------------------------------------------------------
    // tools/list
    // -------------------------------------------------------------------------

    @Test
    void toolsList_returnsAll6Tools() throws Exception {
        TestResponseDeleguate resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"1\"}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("\"create_note\""));
        assertThat(body, containsString("\"get_note\""));
        assertThat(body, containsString("\"update_note\""));
        assertThat(body, containsString("\"delete_note\""));
        assertThat(body, containsString("\"list_notes\""));
        assertThat(body, containsString("\"search_notes\""));
    }

    // -------------------------------------------------------------------------
    // tools/call — CRUD roundtrip
    // -------------------------------------------------------------------------

    @Test
    void createNote_thenGetNote_roundtrip() throws Exception {
        String id = createNoteAndGetId("Integration Test", "This is a test note");

        TestResponseDeleguate getResp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"get_note\",\"arguments\":{\"id\":\"" + id + "\"}},"
                        + "\"id\":\"3\"}"
        );
        assertThat(getResp.status(), is(200));
        String body = new String(getResp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Integration Test"));
        assertThat(body, containsString("This is a test note"));
    }

    @Test
    void updateNote_changesContent() throws Exception {
        String id = createNoteAndGetId("Original Title", "Original content");

        post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"update_note\","
                        + "\"arguments\":{\"id\":\"" + id + "\",\"content\":\"Updated content\"}},"
                        + "\"id\":\"3\"}"
        );

        TestResponseDeleguate getResp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"get_note\",\"arguments\":{\"id\":\"" + id + "\"}},"
                        + "\"id\":\"4\"}"
        );
        String body = new String(getResp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Updated content"));
        assertThat(body, containsString("Original Title"));
    }

    @Test
    void deleteNote_thenGetReturnsError() throws Exception {
        String id = createNoteAndGetId("To Delete", "Delete me");

        post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"delete_note\",\"arguments\":{\"id\":\"" + id + "\"}},"
                        + "\"id\":\"3\"}"
        );

        TestResponseDeleguate getResp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"get_note\",\"arguments\":{\"id\":\"" + id + "\"}},"
                        + "\"id\":\"4\"}"
        );
        String body = new String(getResp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("\"isError\":true"));
    }

    // -------------------------------------------------------------------------
    // tools/call — list + search
    // -------------------------------------------------------------------------

    @Test
    void listNotes_withTagFilter() throws Exception {
        createNoteWithTag("Work Note", "Work content", "work");
        createNoteWithTag("Personal Note", "Personal content", "personal");

        TestResponseDeleguate listResp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"list_notes\",\"arguments\":{\"tag\":\"work\"}},"
                        + "\"id\":\"3\"}"
        );
        assertThat(listResp.status(), is(200));
        String body = new String(listResp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Work Note"));
        assertThat(body, not(containsString("Personal Note")));
    }

    @Test
    void searchNotes_triggers202AndSseResult() throws Exception {
        createNoteAndGetId("Searchable Note", "This note contains the search term findme");

        // Open SSE channel in background thread
        TestResponseDeleguate sseResp = new TestResponseDeleguate();
        Thread sseThread = new Thread(() -> {
            try {
                processor.process(
                        TestRequestDeleguate.request(RequestDelegate.Method.GET, URL)
                                .addHeader("Accept", "text/event-stream")
                                .addHeader("Mcp-Session-Id", sessionId)
                                .build(),
                        sseResp
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        sseThread.setDaemon(true);
        sseThread.start();

        // Wait for channel to open
        Eventually.timeout(2000).assertThat(() -> sseResp.sseChannel(), notNullValue());
        // Drain initial ping
        sseResp.sseChannel().poll(2000);

        // Call search_notes — expect 202 (async, exceeds syncTimeoutMillis=500ms)
        TestResponseDeleguate toolResp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"search_notes\",\"arguments\":{\"query\":\"findme\"}},"
                        + "\"id\":\"42\"}"
        );
        assertThat(toolResp.status(), is(202));

        // Wait for SSE result (search sleeps 600ms)
        TestSseChannel.SseEvent event = sseResp.sseChannel().poll(3000);
        assertThat(event, notNullValue());
        assertThat(event.event(), is("message"));
        assertThat(event.data(), containsString("findme"));

        sseResp.sseChannel().close();
    }

    // -------------------------------------------------------------------------
    // resources/list + resources/read
    // -------------------------------------------------------------------------

    @Test
    void resourcesList_returnsBothResources() throws Exception {
        TestResponseDeleguate resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"resources/list\",\"id\":\"1\"}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("note://"));
        assertThat(body, containsString("notes://tagged/"));
    }

    @Test
    void resourcesRead_noteUri_returnsMarkdown() throws Exception {
        String id = createNoteAndGetId("Resource Test Title", "Resource test content");

        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\","
                        + "\"params\":{\"uri\":\"note://" + id + "\"},"
                        + "\"id\":\"2\"}"
        );
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Resource Test Title"));
        assertThat(body, containsString("Resource test content"));
    }

    @Test
    void resourcesRead_taggedUri_returnsNoteList() throws Exception {
        createNoteWithTag("Tagged Resource Note", "Tagged content", "mytag");

        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\","
                        + "\"params\":{\"uri\":\"notes://tagged/mytag\"},"
                        + "\"id\":\"2\"}"
        );
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Tagged Resource Note"));
    }

    // -------------------------------------------------------------------------
    // prompts/list + prompts/get
    // -------------------------------------------------------------------------

    @Test
    void promptsList_returnsBothPrompts() throws Exception {
        TestResponseDeleguate resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"prompts/list\",\"id\":\"1\"}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("\"summarize_note\""));
        assertThat(body, containsString("\"compare_notes\""));
    }

    @Test
    void promptsGet_summarize_returnsPromptWithContent() throws Exception {
        String id = createNoteAndGetId("Summarize Me", "Content to summarize");

        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"prompts/get\","
                        + "\"params\":{\"name\":\"summarize_note\","
                        + "\"arguments\":{\"note_id\":\"" + id + "\"}},"
                        + "\"id\":\"2\"}"
        );
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Summarize Me"));
        assertThat(body, containsString("Content to summarize"));
    }

    @Test
    void promptsGet_compare_containsBothNotes() throws Exception {
        String id1 = createNoteAndGetId("First Note", "First note content");
        String id2 = createNoteAndGetId("Second Note", "Second note content");

        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"prompts/get\","
                        + "\"params\":{\"name\":\"compare_notes\","
                        + "\"arguments\":{\"note_id_1\":\"" + id1 + "\","
                        + "\"note_id_2\":\"" + id2 + "\"}},"
                        + "\"id\":\"2\"}"
        );
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("First note content"));
        assertThat(body, containsString("Second note content"));
    }

    // -------------------------------------------------------------------------
    // Error cases
    // -------------------------------------------------------------------------

    @Test
    void callTool_unknownTool_returnsMethodNotFound() throws Exception {
        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"nonexistent_tool\",\"arguments\":{}},"
                        + "\"id\":\"1\"}"
        );
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("-32601"));
    }

    @Test
    void resourcesRead_unknownUri_returnsMethodNotFound() throws Exception {
        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\","
                        + "\"params\":{\"uri\":\"unknown://xyz\"},"
                        + "\"id\":\"1\"}"
        );
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("-32601"));
    }

    @Test
    void promptsGet_unknownPrompt_returnsMethodNotFound() throws Exception {
        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"prompts/get\","
                        + "\"params\":{\"name\":\"nonexistent_prompt\",\"arguments\":{}},"
                        + "\"id\":\"1\"}"
        );
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("-32601"));
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private String initialize() throws Exception {
        TestResponseDeleguate initResp = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(body("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{},\"id\":\"0\"}"))
                        .build(),
                initResp
        );
        return initResp.headers().get("Mcp-Session-Id")[0];
    }

    private TestResponseDeleguate post(String json) throws Exception {
        TestResponseDeleguate resp = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(body(json))
                        .build(),
                resp
        );
        return resp;
    }

    private void deleteSession() throws Exception {
        TestResponseDeleguate resp = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.DELETE, URL)
                        .addHeader("Mcp-Session-Id", sessionId)
                        .build(),
                resp
        );
    }

    private String createNoteAndGetId(String title, String content) throws Exception {
        TestResponseDeleguate resp = post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"create_note\","
                        + "\"arguments\":{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}},"
                        + "\"id\":\"create\"}"
        );
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        return extractNoteId(body);
    }

    private void createNoteWithTag(String title, String content, String tag) throws Exception {
        post(
                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"create_note\","
                        + "\"arguments\":{\"title\":\"" + title + "\","
                        + "\"content\":\"" + content + "\","
                        + "\"tags\":[\"" + tag + "\"]}},"
                        + "\"id\":\"create-tagged\"}"
        );
    }

    private String extractNoteId(String responseBody) {
        int idx = responseBody.indexOf("Note created with id: ");
        if (idx < 0) throw new AssertionError("No id in: " + responseBody);
        String after = responseBody.substring(idx + "Note created with id: ".length());
        int end = after.indexOf('"');
        return end >= 0 ? after.substring(0, end) : after.trim();
    }

    private ByteArrayInputStream body(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}

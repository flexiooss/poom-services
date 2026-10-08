package org.codingmatters.poom.mcp.demo;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.processor.McpProcessor;
import org.codingmatters.poom.mcp.processor.McpTimings;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.tests.api.TestRequestDeleguate;
import org.codingmatters.rest.tests.api.TestResponseDeleguate;
import org.codingmatters.rest.tests.api.TestSseChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class NoteAssistantIntegrationTest {

    private static final String URL = "http://test/mcp";
    private static final String VERSION = "2026-07-28";

    private final JsonFactory jsonFactory = new JsonFactory();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private NoteService noteService;
    private McpProcessor processor;

    @BeforeEach
    void setUp() {
        this.noteService = new NoteService(NoteRepository.create());
        this.processor = new McpProcessor(this.jsonFactory, NoteAssistantDescriptor.build(this.noteService), this.pool);
    }

    @AfterEach
    void tearDown() {
        this.pool.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // Transport
    // -------------------------------------------------------------------------

    @Test
    void givenLegacyInitialize__thenUnsupportedVersion() throws Exception {
        TestResponseDeleguate resp = new TestResponseDeleguate();
        this.processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("MCP-Protocol-Version", "2025-06-18")
                        .addHeader("Mcp-Method", "initialize")
                        .payload(stream("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{},\"id\":\"0\"}"))
                        .build(),
                resp
        );
        assertThat(resp.status(), is(400));
        assertThat(new String(resp.payload(), StandardCharsets.UTF_8), containsString("-32022"));
    }

    @Test
    void givenGet__then405() throws Exception {
        TestResponseDeleguate resp = new TestResponseDeleguate();
        this.processor.process(TestRequestDeleguate.request(RequestDelegate.Method.GET, URL).build(), resp);
        assertThat(resp.status(), is(405));
    }

    @Test
    void givenServerDiscover__thenServerInfoAndCapabilities() throws Exception {
        TestResponseDeleguate resp = post("1", "server/discover", null, "{}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("\"supportedVersions\""));
        assertThat(body, containsString(VERSION));
    }

    // -------------------------------------------------------------------------
    // tools/list
    // -------------------------------------------------------------------------

    @Test
    void toolsList_returnsAll6Tools() throws Exception {
        TestResponseDeleguate resp = post("1", "tools/list", null, "{}");
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

        TestResponseDeleguate getResp = callTool("3", "get_note", "{\"id\":\"" + id + "\"}");
        assertThat(getResp.status(), is(200));
        String body = new String(getResp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Integration Test"));
        assertThat(body, containsString("This is a test note"));
    }

    @Test
    void updateNote_changesContent() throws Exception {
        String id = createNoteAndGetId("Original Title", "Original content");

        callTool("3", "update_note", "{\"id\":\"" + id + "\",\"content\":\"Updated content\"}");

        TestResponseDeleguate getResp = callTool("4", "get_note", "{\"id\":\"" + id + "\"}");
        String body = new String(getResp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Updated content"));
        assertThat(body, containsString("Original Title"));
    }

    @Test
    void deleteNote_thenGetReturnsError() throws Exception {
        String id = createNoteAndGetId("To Delete", "Delete me");

        callTool("3", "delete_note", "{\"id\":\"" + id + "\"}");

        TestResponseDeleguate getResp = callTool("4", "get_note", "{\"id\":\"" + id + "\"}");
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

        TestResponseDeleguate listResp = callTool("3", "list_notes", "{\"tag\":\"work\"}");
        assertThat(listResp.status(), is(200));
        String body = new String(listResp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Work Note"));
        assertThat(body, not(containsString("Personal Note")));
    }

    @Test
    void searchNotes_answersAsJsonUnderTheJsonWindow() throws Exception {
        createNoteAndGetId("Searchable Note", "This note contains the search term findme");

        // search_notes sleeps 600 ms, under the default 1 s jsonWindow
        TestResponseDeleguate toolResp = callTool("42", "search_notes", "{\"query\":\"findme\"}");

        assertThat(toolResp.status(), is(200));
        assertThat(toolResp.contentType(), containsString("application/json"));
        assertThat(new String(toolResp.payload(), StandardCharsets.UTF_8), containsString("findme"));
    }

    @Test
    void givenJsonWindowShorterThanTheTool__whenSearchNotes__thenAnswersAsSse() throws Exception {
        McpTimings timings = new McpTimings(Duration.ofMillis(100), Duration.ofSeconds(15), Duration.ofSeconds(20),
                Duration.ofMinutes(5), Duration.ofSeconds(1), Duration.ofHours(1), Duration.ofSeconds(2));
        McpProcessor sseProcessor = new McpProcessor(this.jsonFactory, NoteAssistantDescriptor.build(this.noteService), this.pool, timings);
        createNoteAndGetId("Searchable Note", "This note contains the search term findme");

        TestResponseDeleguate resp = new TestResponseDeleguate();
        sseProcessor.process(request("42", "tools/call", "search_notes",
                "{\"name\":\"search_notes\",\"arguments\":{\"query\":\"findme\"}}"), resp);

        assertThat(resp.status(), is(200));
        // the test response records no content type for an SSE reply: opening the channel is the SSE witness
        assertThat(resp.sseChannel(), notNullValue());
        TestSseChannel.SseEvent event = resp.sseChannel().poll(3000);
        assertThat(event, notNullValue());
        assertThat(event.event(), is("message"));
        assertThat(event.data(), containsString("findme"));
    }

    // -------------------------------------------------------------------------
    // resources/list + resources/read
    // -------------------------------------------------------------------------

    @Test
    void resourcesList_returnsBothResources() throws Exception {
        TestResponseDeleguate resp = post("1", "resources/list", null, "{}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("note://"));
        assertThat(body, containsString("notes://tagged/"));
    }

    @Test
    void resourcesRead_noteUri_returnsMarkdown() throws Exception {
        String id = createNoteAndGetId("Resource Test Title", "Resource test content");

        TestResponseDeleguate resp = post("2", "resources/read", "note://" + id, "{\"uri\":\"note://" + id + "\"}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Resource Test Title"));
        assertThat(body, containsString("Resource test content"));
    }

    @Test
    void resourcesRead_taggedUri_returnsNoteList() throws Exception {
        createNoteWithTag("Tagged Resource Note", "Tagged content", "mytag");

        TestResponseDeleguate resp = post("2", "resources/read", "notes://tagged/mytag", "{\"uri\":\"notes://tagged/mytag\"}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Tagged Resource Note"));
    }

    // -------------------------------------------------------------------------
    // prompts/list + prompts/get
    // -------------------------------------------------------------------------

    @Test
    void promptsList_returnsBothPrompts() throws Exception {
        TestResponseDeleguate resp = post("1", "prompts/list", null, "{}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("\"summarize_note\""));
        assertThat(body, containsString("\"compare_notes\""));
    }

    @Test
    void promptsGet_summarize_returnsPromptWithContent() throws Exception {
        String id = createNoteAndGetId("Summarize Me", "Content to summarize");

        TestResponseDeleguate resp = post("2", "prompts/get", "summarize_note",
                "{\"name\":\"summarize_note\",\"arguments\":{\"note_id\":\"" + id + "\"}}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("Summarize Me"));
        assertThat(body, containsString("Content to summarize"));
    }

    @Test
    void promptsGet_compare_containsBothNotes() throws Exception {
        String id1 = createNoteAndGetId("First Note", "First note content");
        String id2 = createNoteAndGetId("Second Note", "Second note content");

        TestResponseDeleguate resp = post("2", "prompts/get", "compare_notes",
                "{\"name\":\"compare_notes\",\"arguments\":{\"note_id_1\":\"" + id1 + "\",\"note_id_2\":\"" + id2 + "\"}}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("First note content"));
        assertThat(body, containsString("Second note content"));
    }

    // -------------------------------------------------------------------------
    // Error cases
    // -------------------------------------------------------------------------

    @Test
    void callTool_unknownTool_returnsInvalidParams() throws Exception {
        TestResponseDeleguate resp = callTool("1", "nonexistent_tool", "{}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("-32602"));
    }

    @Test
    void resourcesRead_unknownUri_returnsMethodNotFound() throws Exception {
        TestResponseDeleguate resp = post("1", "resources/read", "unknown://xyz", "{\"uri\":\"unknown://xyz\"}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("-32601"));
    }

    @Test
    void promptsGet_unknownPrompt_returnsMethodNotFound() throws Exception {
        TestResponseDeleguate resp = post("1", "prompts/get", "nonexistent_prompt",
                "{\"name\":\"nonexistent_prompt\",\"arguments\":{}}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload(), StandardCharsets.UTF_8);
        assertThat(body, containsString("-32601"));
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private TestResponseDeleguate callTool(String id, String tool, String argumentsJson) throws Exception {
        return post(id, "tools/call", tool, "{\"name\":\"" + tool + "\",\"arguments\":" + argumentsJson + "}");
    }

    /** @param name the Mcp-Name header value (null when the method has none), @param paramsJson params object without _meta */
    private TestResponseDeleguate post(String id, String method, String name, String paramsJson) throws Exception {
        TestResponseDeleguate resp = new TestResponseDeleguate();
        this.processor.process(request(id, method, name, paramsJson), resp);
        return resp;
    }

    private RequestDelegate request(String id, String method, String name, String paramsJson) {
        TestRequestDeleguate.Builder builder = TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                .contentType("application/json")
                .addHeader("MCP-Protocol-Version", VERSION)
                .addHeader("Mcp-Method", method)
                .payload(stream(body(id, method, paramsJson)));
        if (name != null) builder.addHeader("Mcp-Name", name);
        return builder.build();
    }

    /** Same construction as McpTestRequests.body of the processor tests: _meta is injected in params. */
    private String body(String id, String method, String paramsJson) {
        String meta = "\"_meta\":{"
                + "\"io.modelcontextprotocol/protocolVersion\":\"" + VERSION + "\","
                + "\"io.modelcontextprotocol/clientInfo\":{\"name\":\"test\",\"version\":\"1\"},"
                + "\"io.modelcontextprotocol/clientCapabilities\":{}}";
        String rest = paramsJson.trim().substring(1).trim();
        String params = rest.equals("}") ? "{" + meta + "}" : "{" + meta + "," + rest;
        return "{\"jsonrpc\":\"2.0\",\"id\":\"" + id + "\",\"method\":\"" + method + "\",\"params\":" + params + "}";
    }

    private String createNoteAndGetId(String title, String content) throws Exception {
        TestResponseDeleguate resp = callTool("create", "create_note",
                "{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}");
        return extractNoteId(new String(resp.payload(), StandardCharsets.UTF_8));
    }

    private void createNoteWithTag(String title, String content, String tag) throws Exception {
        callTool("create-tagged", "create_note",
                "{\"title\":\"" + title + "\",\"content\":\"" + content + "\",\"tags\":[\"" + tag + "\"]}");
    }

    private String extractNoteId(String responseBody) {
        int idx = responseBody.indexOf("Note created with id: ");
        if (idx < 0) throw new AssertionError("No id in: " + responseBody);
        String after = responseBody.substring(idx + "Note created with id: ".length());
        int end = after.indexOf('"');
        return end >= 0 ? after.substring(0, end) : after.trim();
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}

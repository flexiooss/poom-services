# poom-mcp

MCP (Model Context Protocol) Streamable HTTP server support for poom-services.

---

## What is MCP?

The [Model Context Protocol](https://spec.modelcontextprotocol.io/) is a standard that lets AI models (Claude, GPT, etc.) call external capabilities at runtime. A **MCP server** exposes three kinds of primitives to the AI client:

| Primitive | What it is | Example |
|-----------|-----------|---------|
| **Tool** | A function the model can invoke | `search_database`, `send_email`, `run_query` |
| **Resource** | A URI-addressable piece of content | `file:///config.json`, `db://customers/42` |
| **Prompt** | A reusable prompt template | `summarize_document`, `review_code` |

The model discovers available tools/resources/prompts, calls them during generation, and incorporates the results into its response — without the user doing anything.

`poom-mcp` implements MCP over **Streamable HTTP**: each client session is a persistent SSE channel on which the server can push results asynchronously, while synchronous short-running calls are answered inline on the POST response.

---

## Module layout

```
poom-mcp/
  poom-mcp-types/        generated value objects for MCP protocol messages
  poom-mcp-descriptors/  McpServerDescriptor — declares your server's capabilities
  poom-mcp-processor/    McpProcessor — the HTTP handler (implements Processor)
```

---

## Maven setup

```xml
<dependencies>
    <!-- declare your server capabilities -->
    <dependency>
        <groupId>org.codingmatters.poom.mcp</groupId>
        <artifactId>poom-mcp-descriptors</artifactId>
    </dependency>
    <!-- the HTTP processor -->
    <dependency>
        <groupId>org.codingmatters.poom.mcp</groupId>
        <artifactId>poom-mcp-processor</artifactId>
    </dependency>
</dependencies>
```

---

## Building a MCP server

### 1 — Describe your server

`McpServerDescriptor` declares everything a MCP client needs to know about your server.

```java
McpServerDescriptor descriptor = McpServerDescriptor.builder()
        .name("my-assistant")
        .version("1.0.0")
        .tools(
            // one McpToolDescriptor per tool
        )
        .resources(
            // one McpResourceDescriptor per resource (optional)
        )
        .prompts(
            // one McpPromptDescriptor per prompt (optional)
        )
        .build();
```

### 2 — Declare tools

Each tool needs a name, a description (shown to the model to help it decide when to call the tool), an optional JSON Schema for its input, and a **handler**.

The handler is a `Function<CallToolParams, CallToolResult>`. `CallToolParams` carries the tool name and the `arguments` `ObjectValue` sent by the model.

```java
McpToolDescriptor wordCountTool = McpToolDescriptor.builder()
        .name("word_count")
        .description("Counts the number of words in a text string.")
        .handler((CallToolParams params) -> {
            String text = params.arguments().property("text").single().stringValue();
            int count = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            return CallToolResult.builder()
                    .content(ToolContent.builder()
                            .type("text")
                            .text(String.valueOf(count))
                            .build())
                    .isError(false)
                    .build();
        })
        .build();
```

To describe the expected input to the model, attach a JSON Schema as an `ObjectValue`:

```java
McpToolDescriptor searchTool = McpToolDescriptor.builder()
        .name("search_notes")
        .description("Full-text search over stored notes. Returns matching note ids.")
        .inputSchema(ObjectValue.builder()
                .property("type", v -> v.stringValue("object"))
                .property("properties", v -> v.objectValue(ObjectValue.builder()
                        .property("query", pv -> pv.objectValue(ObjectValue.builder()
                                .property("type", t -> t.stringValue("string"))
                                .property("description", d -> d.stringValue("Search terms"))
                                .build()))
                        .build()))
                .property("required", v -> v.stringValue("query"))
                .build())
        .handler((CallToolParams params) -> {
            String query = params.arguments().property("query").single().stringValue();
            List<String> ids = noteRepository.search(query);
            return CallToolResult.builder()
                    .content(ToolContent.builder()
                            .type("text")
                            .text(String.join(", ", ids))
                            .build())
                    .isError(false)
                    .build();
        })
        .build();
```

### 3 — Declare resources

Resources are URI-addressable content the model can read. Declare them so they appear in `resources/list`. The handler receives `ReadResourceParams` (not yet dispatched by the processor — see [Limitations](#limitations)).

```java
McpResourceDescriptor configResource = McpResourceDescriptor.builder()
        .uri("config://app/settings")
        .name("Application settings")
        .mimeType("application/json")
        .handler(params -> /* not yet called by the processor */ null)
        .build();
```

### 4 — Declare prompts

Prompts are reusable templates. Declare them for `prompts/list`. The handler receives `GetPromptParams` (not yet dispatched — see [Limitations](#limitations)).

```java
McpPromptDescriptor reviewPrompt = McpPromptDescriptor.builder()
        .name("review_code")
        .description("Generates a structured code review request for a given snippet.")
        .handler(params -> /* not yet called by the processor */ null)
        .build();
```

### 5 — Create the processor

`McpProcessor` is a `Processor` — the standard poom-services HTTP handler interface.

```java
ExecutorService toolExecutor = Executors.newFixedThreadPool(4);
JsonFactory jsonFactory = new JsonFactory();

McpProcessor mcpProcessor = new McpProcessor(
        "/mcp",           // URL path this processor is mounted at
        jsonFactory,
        descriptor,
        toolExecutor
);
```

The optional fifth argument `syncTimeoutMillis` (default: 500 ms) controls whether a tool call is answered synchronously or asynchronously (see [Async tool calls](#async-tool-calls)).

### 6 — Wire into an HTTP server

#### Using `Service.fromEnv` (simplest)

```java
import org.codingmatters.poom.services.runtime.Service;

public static void main(String[] args) {
    McpProcessor processor = new McpProcessor("/mcp", new JsonFactory(), descriptor,
            Executors.newFixedThreadPool(4));

    Service.fromEnv(processor, "my-mcp-server", new JsonFactory())
            .main(log);
    // reads SERVICE_HOST / SERVICE_PORT from environment
}
```

#### Using Undertow directly

```java
import io.undertow.Undertow;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

Undertow server = Undertow.builder()
        .addHttpListener(8080, "0.0.0.0")
        .setHandler(new CdmHttpUndertowHandler(mcpProcessor))
        .build();
server.start();
```

---

## Session lifecycle

The MCP Streamable HTTP protocol is session-based. Each client goes through this sequence:

```
Client                                    Server
  |                                          |
  |  POST /mcp  (initialize)                 |
  |  Content-Type: application/json          |
  |  {"jsonrpc":"2.0","method":"initialize"} |
  |----------------------------------------->|
  |  200 + Mcp-Session-Id: <uuid>            |
  |<-----------------------------------------|
  |                                          |
  |  GET /mcp                                |
  |  Accept: text/event-stream               |
  |  Mcp-Session-Id: <uuid>                  |
  |----------------------------------------->|
  |  200 text/event-stream (SSE channel)     |
  |  event: ping                             |  ← keepalive every 30s
  |<-----------------------------------------|
  |                                          |
  |  POST /mcp  (tools/list, tools/call...)  |
  |  Mcp-Session-Id: <uuid>                  |
  |----------------------------------------->|
  |  200 (sync result) or 202 (async)        |
  |<-----------------------------------------|
  |                      event: message      |  ← async result via SSE
  |<-----------------------------------------|
  |                                          |
  |  DELETE /mcp                             |
  |  Mcp-Session-Id: <uuid>                  |
  |----------------------------------------->|
  |  200                                     |
  |<-----------------------------------------|
```

The SSE channel (`GET`) must be open before any async tool call can push a result. The client is responsible for opening it after initialization.

**Session headers:**
- `Mcp-Session-Id` — assigned by the server on `initialize`, must be sent on all subsequent requests.
- Protocol version negotiated: `2024-11-05`.

---

## Async tool calls

By default, `McpProcessor` waits up to **500 ms** for a tool handler to complete. If the handler returns before the timeout, the result is sent inline in the `POST` response (HTTP 200). If the handler is still running at the timeout, the server responds with HTTP **202** and sends the result as a `message` SSE event once the handler completes.

```
Tool finishes within 500ms    →  POST responds 200 with result
Tool takes longer than 500ms  →  POST responds 202 (accepted)
                                 SSE: event: message\ndata: <json-rpc result>
```

Tune the timeout at construction time:

```java
new McpProcessor("/mcp", jsonFactory, descriptor, toolExecutor, 2_000); // 2s
```

For a tool that will always be slow, setting `syncTimeoutMillis = 0` forces 202 immediately.

---

## Complete example — Note assistant

This example implements a small in-memory note-taking MCP server with two tools and a resource listing.

```java
import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.mcp.McpResourceDescriptor;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.processor.McpProcessor;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class NoteAssistantServer {

    // in-memory store (replace with real persistence)
    private static final Map<String, String> notes = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        McpServerDescriptor descriptor = McpServerDescriptor.builder()
                .name("note-assistant")
                .version("1.0.0")
                .tools(createNoteTool(), searchNotesTool())
                .resources(notesResource())
                .build();

        McpProcessor processor = new McpProcessor(
                "/mcp",
                new JsonFactory(),
                descriptor,
                Executors.newFixedThreadPool(4)
        );

        Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setHandler(new CdmHttpUndertowHandler(processor))
                .build()
                .start();

        System.out.println("MCP server started on http://localhost:8080/mcp");
    }

    // --- tools ---

    private static McpToolDescriptor createNoteTool() {
        return McpToolDescriptor.builder()
                .name("create_note")
                .description("Creates a new note with the given text. Returns the note's id.")
                .inputSchema(ObjectValue.builder()
                        .property("type", v -> v.stringValue("object"))
                        .property("properties", v -> v.objectValue(ObjectValue.builder()
                                .property("text", t -> t.objectValue(ObjectValue.builder()
                                        .property("type", tt -> tt.stringValue("string"))
                                        .property("description", d -> d.stringValue("Content of the note"))
                                        .build()))
                                .build()))
                        .property("required", v -> v.stringValue("text"))
                        .build())
                .handler((CallToolParams params) -> {
                    String text = params.arguments().property("text").single().stringValue();
                    String id = UUID.randomUUID().toString().substring(0, 8);
                    notes.put(id, text);
                    return success("Note created with id: " + id);
                })
                .build();
    }

    private static McpToolDescriptor searchNotesTool() {
        return McpToolDescriptor.builder()
                .name("search_notes")
                .description(
                    "Searches all notes for a given keyword. " +
                    "Returns matching note ids and a short excerpt.")
                .inputSchema(ObjectValue.builder()
                        .property("type", v -> v.stringValue("object"))
                        .property("properties", v -> v.objectValue(ObjectValue.builder()
                                .property("query", t -> t.objectValue(ObjectValue.builder()
                                        .property("type", tt -> tt.stringValue("string"))
                                        .property("description", d -> d.stringValue("Search keyword"))
                                        .build()))
                                .build()))
                        .property("required", v -> v.stringValue("query"))
                        .build())
                .handler((CallToolParams params) -> {
                    String query = params.arguments().property("query").single().stringValue()
                                        .toLowerCase();
                    StringBuilder results = new StringBuilder();
                    notes.forEach((id, text) -> {
                        if (text.toLowerCase().contains(query)) {
                            String excerpt = text.length() > 60 ? text.substring(0, 60) + "…" : text;
                            results.append(id).append(": ").append(excerpt).append("\n");
                        }
                    });
                    String output = results.isEmpty()
                            ? "No notes match \"" + query + "\"."
                            : results.toString().trim();
                    return success(output);
                })
                .build();
    }

    // --- resources ---

    private static McpResourceDescriptor notesResource() {
        return McpResourceDescriptor.builder()
                .uri("notes://all")
                .name("All notes")
                .mimeType("text/plain")
                .handler(params -> null) // read dispatch not yet implemented
                .build();
    }

    // --- helpers ---

    private static CallToolResult success(String text) {
        return CallToolResult.builder()
                .content(ToolContent.builder().type("text").text(text).build())
                .isError(false)
                .build();
    }

    private static CallToolResult error(String message) {
        return CallToolResult.builder()
                .content(ToolContent.builder().type("text").text(message).build())
                .isError(true)
                .build();
    }
}
```

---

## Error handling in tools

Return an error result (rather than throwing) to signal a handled failure. The model receives the error text as content and can decide how to proceed.

```java
.handler((CallToolParams params) -> {
    try {
        // ... do work
        return success("Done.");
    } catch (NotFoundException e) {
        return error("Not found: " + e.getMessage());
    } catch (Exception e) {
        log.error("unexpected error in my_tool", e);
        return error("Internal error — please try again.");
    }
})
```

If the handler throws an unchecked exception, `McpProcessor` catches it and returns a JSON-RPC `-32603 Internal error` response — the session is not terminated.

---

## Testing

Use `TestRequestDeleguate` and `TestResponseDeleguate` from `cdm-rest-tests-support` to test your tool handlers directly against `McpProcessor` without a running HTTP server:

```java
@Test
void createNoteReturnssId() throws Exception {
    McpProcessor processor = new McpProcessor("/mcp", new JsonFactory(),
            descriptor, Executors.newSingleThreadExecutor());

    // initialize to get a session
    TestResponseDeleguate initResp = new TestResponseDeleguate();
    processor.process(
            TestRequestDeleguate.request(RequestDelegate.Method.POST, "http://test/mcp")
                    .contentType("application/json")
                    .payload(new ByteArrayInputStream(
                        "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{},\"id\":\"1\"}"
                        .getBytes(StandardCharsets.UTF_8)))
                    .build(),
            initResp);
    String sessionId = initResp.headers().get("Mcp-Session-Id")[0];

    // call the tool
    TestResponseDeleguate resp = new TestResponseDeleguate();
    processor.process(
            TestRequestDeleguate.request(RequestDelegate.Method.POST, "http://test/mcp")
                    .contentType("application/json")
                    .addHeader("Mcp-Session-Id", sessionId)
                    .payload(new ByteArrayInputStream(
                        "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\"," +
                        "\"params\":{\"name\":\"create_note\",\"arguments\":{\"text\":\"hello\"}}," +
                        "\"id\":\"2\"}"
                        .getBytes(StandardCharsets.UTF_8)))
                    .build(),
            resp);

    assertThat(resp.status(), is(200));
    assertThat(new String(resp.payload()), containsString("\"result\""));
}
```

---

## Limitations

| Feature | Status |
|---------|--------|
| `tools/list` | ✅ implemented |
| `tools/call` (sync) | ✅ implemented |
| `tools/call` (async via SSE) | ✅ implemented |
| `resources/list` | ✅ implemented (listing only) |
| `resources/read` | ❌ not yet dispatched to handler |
| `prompts/list` | ✅ implemented (listing only) |
| `prompts/get` | ❌ not yet dispatched to handler |
| Numeric / null `id` | ❌ `id` is typed `string`; numeric ids cause a parse error |
| Tool call cancellation | ❌ not implemented |
| Roots negotiation | ❌ not implemented |

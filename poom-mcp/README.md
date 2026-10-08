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

`poom-mcp` implements MCP over **Streamable HTTP**, protocol version **`2026-07-28`**. The server is **stateless**: there is no session and no `initialize` handshake. Every request is self-describing and can be served by any replica behind a load balancer.

---

## The transport (protocol 2026-07-28)

A single endpoint accepts **`POST` only**. `GET` and `DELETE` answer `405 Method Not Allowed` (`Allow: POST`); a request whose `Content-Type` is not `application/json` answers `415`. A JSON-RPC notification (a message without `id`) is accepted with `202` and an empty body.

### JSON-RPC 2.0 as the message format

All `POST` bodies and all server responses are **JSON-RPC 2.0** messages. A tool call looks like:

```
POST /mcp
Content-Type: application/json
MCP-Protocol-Version: 2026-07-28
Mcp-Method: tools/call
Mcp-Name: search_notes
```
```json
{"jsonrpc":"2.0","id":"3","method":"tools/call","params":{
  "name":"search_notes","arguments":{"query":"meeting"},
  "_meta":{
    "io.modelcontextprotocol/protocolVersion":"2026-07-28",
    "io.modelcontextprotocol/clientInfo":{"name":"my-client","version":"1.0"},
    "io.modelcontextprotocol/clientCapabilities":{}
  }}}
```

and its response:
```json
{"jsonrpc":"2.0","id":"3","result":{"content":[{"type":"text","text":"note-42: team meeting notes…"}],"isError":false}}
```

An error follows the standard JSON-RPC shape:
```json
{"jsonrpc":"2.0","id":"3","error":{"code":-32602,"message":"…"}}
```

A numeric `id` is accepted; the response echoes it as a string.

**Note — `McpProcessor` and `poom-json-rpc`:** `poom-services` already has a JSON-RPC 2.0 processor (`poom-json-rpc`). `McpProcessor` does not reuse it, because `poom-json-rpc` writes a single response once all handlers return, which is incompatible with the SSE reply of a slow `tools/call`. `McpProcessor` therefore handles JSON-RPC parsing and serialization directly — the overlap is intentional.

### Required headers and `_meta`

Each request carries its context, and the server checks it against the body:

| Where | Name | Content |
|-------|------|---------|
| Header | `MCP-Protocol-Version` | `2026-07-28` |
| Header | `Mcp-Method` | the JSON-RPC `method` |
| Header | `Mcp-Name` | `params.name` for `tools/call` and `prompts/get`, `params.uri` for `resources/read`, `params.taskId` for `tasks/*`. May be encoded `=?base64?<base64 of the UTF-8 value>?=`. Not required for other methods. |
| `params._meta` | `io.modelcontextprotocol/protocolVersion` | must equal the header |
| `params._meta` | `io.modelcontextprotocol/clientInfo` | `{name, version}` |
| `params._meta` | `io.modelcontextprotocol/clientCapabilities` | e.g. `{"extensions":{"io.modelcontextprotocol/tasks":{}}}` to accept tasks |

Rejections (HTTP `400`, JSON-RPC error):

| Code | Cause | `error.data` |
|------|-------|--------------|
| `-32022` | `MCP-Protocol-Version` missing or not `2026-07-28` (e.g. a legacy `initialize` from a 2025 client) | `{"supported":["2026-07-28"],"requested":"<value>"}` |
| `-32020` | `MCP-Protocol-Version` differs from `_meta`, `Mcp-Method` differs from `method`, or `Mcp-Name` differs from the body | — |

Other errors: `-32700` parse error and `-32600` invalid request (HTTP `400`); `-32601` unknown method, unknown resource and unknown prompt, `-32602` invalid params (unknown tool, unknown task, missing required param) and `-32603` internal error (HTTP `200`).

### `server/discover`

Replaces `initialize`. It returns what the server supports:

```json
{"jsonrpc":"2.0","id":"1","result":{
  "resultType":"complete",
  "supportedVersions":["2026-07-28"],
  "capabilities":{"tools":{},"resources":{},"prompts":{},"extensions":{"io.modelcontextprotocol/tasks":{}}},
  "_meta":{"io.modelcontextprotocol/serverInfo":{"name":"my-assistant","version":"1.0.0"}}}}
```

`tools`, `resources` and `prompts` appear when the descriptor declares at least one; `extensions` appears when at least one tool is a task tool (see [Task tools](#task-tools)).

### The shape of a `tools/call` response

The `POST` thread stays on the request until it has the answer. The shape depends on how long the tool takes, governed by `McpTimings`:

| Threshold | Default | Meaning |
|-----------|---------|---------|
| `jsonWindow` | 1 s | Below it, the answer is a plain `application/json` response. Past it, the response switches to an SSE stream. |
| `keepalive` | 15 s | Interval of SSE keepalive comments, under the 40 s idle limit of clients and proxies. |
| `taskAfter` | 20 s | Past it, a task tool answers with a `CreateTaskResult` — if the client declared the tasks extension. |
| `streamMax` | 5 min | Maximum stream duration. Past it, the stream ends with an `isError` result whose text is `{"error":"no_result",…}`. |

`McpTimings` also holds `toolPollInterval` (how often a task tool is read while the stream is open, 1 s), `taskTtl` (1 h) and `clientPollInterval` (2 s), the last two being advertised to the client in `CreateTaskResult`.

```
Client                                  Server
  │  POST /mcp  { tools/call }             │
  │───────────────────────────────────────►│
  │                                        │  tool finishes within jsonWindow
  │  200 application/json { result }       │
  │◄───────────────────────────────────────│
  │                                        │
  │  POST /mcp  { tools/call (slow) }      │
  │───────────────────────────────────────►│
  │                                        │  tool still running at jsonWindow
  │  200 text/event-stream                 │  headers: X-Accel-Buffering: no
  │◄───────────────────────────────────────│
  │  :                                     │  keepalive comment, every keepalive
  │  :                                     │
  │  event: message                        │  one single event: the JSON-RPC response
  │  data: { result }                      │
  │◄───────────────────────────────────────│
  │                                        │  stream closed
```

The stream carries exactly one `event: message`, then closes. If the client closes the connection first, the tool is interrupted and, for a task tool, its task is cancelled.

### Why not just use WebSocket?

WebSocket is bidirectional but requires a dedicated upgrade handshake and persistent connection management at the load balancer / proxy layer. Streamable HTTP is standard request/response, optionally upgraded to SSE for a single reply, and works with any HTTP/1.1 infrastructure.

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

Resources are URI-addressable content the model can read. Declare them so they appear in `resources/list`. `resources/read` is routed by URI prefix (the literal part before the first template variable) to the handler, which receives `ReadResourceParams` carrying the full URI and returns a `ReadResourceResult`. It runs synchronously on the request thread. An unknown URI answers `-32601`.

```java
McpResourceDescriptor configResource = McpResourceDescriptor.builder()
        .uri("config://app/settings")
        .name("Application settings")
        .mimeType("application/json")
        .handler(params -> /* build a ReadResourceResult from params.uri() */ null)
        .build();
```

### 4 — Declare prompts

Prompts are reusable templates. Declare them for `prompts/list`. `prompts/get` calls the handler with `GetPromptParams` (the prompt name and its arguments) and the handler returns a `GetPromptResult`. It runs synchronously on the request thread. An unknown prompt answers `-32601`.

```java
McpPromptDescriptor reviewPrompt = McpPromptDescriptor.builder()
        .name("review_code")
        .description("Generates a structured code review request for a given snippet.")
        .handler(params -> /* build the result from the params */ null)
        .build();
```

### 5 — Create the processor

`McpProcessor` is a `Processor` — the standard poom-services HTTP handler interface.

```java
ExecutorService toolExecutor = Executors.newFixedThreadPool(4);
JsonFactory jsonFactory = new JsonFactory();

McpProcessor mcpProcessor = new McpProcessor(
        jsonFactory,
        descriptor,
        toolExecutor
);
```

The processor is mounted wherever you route it: it no longer takes a path. To change the transport thresholds, pass an `McpTimings` as a fourth argument (see [The shape of a `tools/call` response](#the-shape-of-a-toolscall-response)):

```java
McpTimings timings = McpTimings.defaults().withStreamMax(Duration.ofMinutes(2));
McpProcessor mcpProcessor = new McpProcessor(jsonFactory, descriptor, toolExecutor, timings);
```

The constructors are `McpProcessor(JsonFactory, McpServerDescriptor, ExecutorService)` and `McpProcessor(JsonFactory, McpServerDescriptor, ExecutorService, McpTimings)`. The executor runs the tool handlers; the processor also owns a daemon scheduler for the SSE keepalives.

### 6 — Wire into an HTTP server

#### Using `Service.fromEnv` (simplest)

```java
import org.codingmatters.poom.services.runtime.Service;

public static void main(String[] args) {
    McpProcessor processor = new McpProcessor(new JsonFactory(), descriptor,
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

## Task tools

A tool whose work outlives the request (a long export, a batch job) can be declared a **task tool** by giving its descriptor a `McpToolTasks` instead of a `handler`. The server is stateless, so the tool owns the durable storage: `get` and `cancel` may be called on a different replica than the one that ran `start`.

```java
McpToolDescriptor exportTool = McpToolDescriptor.builder()
        .name("export_notes")
        .description("Exports all notes. Long running.")
        .tasks(new McpToolTasks() {
            @Override public Start start(CallToolParams params) {
                String jobId = jobs.submit(params);           // durable storage
                return new Start.Running(jobId);              // or new Start.Done(result) for an immediate answer/refusal
            }
            @Override public McpTaskState get(String jobId) {
                // throw McpTaskNotFoundException if jobId is unknown or belongs to another context
                return jobs.state(jobId);
            }
            @Override public void cancel(String jobId) { jobs.cancel(jobId); }
        })
        .build();
```

When a descriptor carries `tasks`, the processor uses it and ignores `handler`.

**Contract**

- `start` returns `Start.Running(toolTaskId)` or `Start.Done(CallToolResult)` for an answer or a refusal that needs no task.
- `get` returns an `McpTaskState`: `WORKING`, `COMPLETED` (with a `CallToolResult`) or `FAILED` (with a JSON-RPC error code and message). It throws `McpTaskNotFoundException` for an unknown id, or one that is foreign to the current request context — the implementation checks ownership.
- `cancel` requests the stop without waiting for it, and must not throw when the stop is impossible.
- **`COMPLETED` + `isError: true` is for a business failure** (the work ran and its outcome is an error the model should read). **`FAILED` is reserved for the failure of the task itself** and is carried as a JSON-RPC error.

**What the client sees**

1. A client that declared `io.modelcontextprotocol/tasks` in `_meta.io.modelcontextprotocol/clientCapabilities.extensions` calls the tool with a plain `tools/call`. It follows the usual `jsonWindow` then SSE flow; if the task is still running at `taskAfter`, the stream ends with a flat `CreateTaskResult`:

```json
{"jsonrpc":"2.0","id":"7","result":{"resultType":"task","taskId":"ZXhwb3J0X25vdGVz.1790000000000.job-17",
  "status":"working","createdAt":"2026-10-08T10:00:00Z","lastUpdatedAt":"2026-10-08T10:00:20Z",
  "ttlMs":3600000,"pollIntervalMs":2000}}
```

   A client that did not declare the extension keeps the plain stream, bounded by `streamMax`.

2. The public `taskId` is `base64url(toolName).createdAtEpochMillis.toolTaskId`, so any replica can find the tool and the creation date from the id alone.

3. `tasks/get` (`Mcp-Name` = the `taskId`) returns the state; `resultType` is `"complete"`:

```json
{"jsonrpc":"2.0","id":"8","result":{"resultType":"complete","taskId":"…","status":"completed",
  "createdAt":"…","lastUpdatedAt":"…","ttlMs":3600000,"pollIntervalMs":2000,
  "result":{"content":[{"type":"text","text":"done"}],"isError":false}}}
```

   `status` is `working`, `completed` or `failed`; a completed task carries `result`, a failed one carries `error: {code, message}`. `lastUpdatedAt` is the time of the read.

4. `tasks/cancel` asks the tool to stop and answers `{"resultType":"complete"}`; it is acknowledged even if the stop failed.

An unknown, malformed or foreign `taskId` answers `-32602`. If the client closes the stream before the answer, the tool is interrupted and its task is cancelled.

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
                .handler(params -> null) // replace with a real read handler returning a ReadResourceResult
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

If the handler throws an unchecked exception, `McpProcessor` catches it and returns a JSON-RPC `-32603 Internal error` response — the server stays up and the next request is unaffected.

---

## Testing

Use `TestRequestDeleguate` and `TestResponseDeleguate` from `cdm-rest-tests-support` to test your tools against `McpProcessor` without an HTTP server. A request must carry the 2026-07-28 headers and `_meta`; the processor tests show how:

- `McpTestRequests` (in `poom-mcp-processor/src/test`) builds conformant requests: `post(method, name, body)` sets `MCP-Protocol-Version`, `Mcp-Method` and `Mcp-Name`, and `body(id, method, paramsJson, tasks)` injects `_meta` into `params`. It is package-private to the processor tests, so copy its approach into your own test (the demo's `NoteAssistantIntegrationTest` does).
- `RecordingResponse` extends `TestResponseDeleguate` with an SSE channel that counts keepalive comments and can simulate a client that closed the stream. Use it to check the SSE path.

```java
@Test
void givenCreateNote__whenToolsCall__thenIdReturned() throws Exception {
    McpProcessor processor = new McpProcessor(new JsonFactory(), descriptor, Executors.newSingleThreadExecutor());

    String body = "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"method\":\"tools/call\",\"params\":{"
            + "\"name\":\"create_note\",\"arguments\":{\"text\":\"hello\"},"
            + "\"_meta\":{\"io.modelcontextprotocol/protocolVersion\":\"2026-07-28\","
            + "\"io.modelcontextprotocol/clientInfo\":{\"name\":\"test\",\"version\":\"1\"},"
            + "\"io.modelcontextprotocol/clientCapabilities\":{}}}}";

    TestResponseDeleguate resp = new TestResponseDeleguate();
    processor.process(
            TestRequestDeleguate.request(RequestDelegate.Method.POST, "http://test/mcp")
                    .contentType("application/json")
                    .addHeader("MCP-Protocol-Version", "2026-07-28")
                    .addHeader("Mcp-Method", "tools/call")
                    .addHeader("Mcp-Name", "create_note")
                    .payload(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))
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
| `server/discover` | ✅ implemented (replaces `initialize`) |
| `tools/list` | ✅ implemented |
| `tools/call` (JSON, or SSE past `jsonWindow`) | ✅ implemented |
| Task tools (`CreateTaskResult`, `tasks/get`, `tasks/cancel`) | ✅ implemented |
| `resources/list`, `resources/read` | ✅ implemented |
| `prompts/list`, `prompts/get` | ✅ implemented |
| Numeric / string `id` | ✅ echoed as received |
| Cancellation by closing the stream | ✅ the tool is interrupted, its task cancelled |
| `notifications/progress` | ❌ not emitted |
| Pagination (`cursor`) of list methods | ❌ not implemented |
| Roots negotiation | ❌ not implemented |

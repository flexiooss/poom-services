# poom-mcp-demo — Note Assistant MCP server

A fully working MCP server that manages in-memory notes.

---

## Purpose

This module is the canonical reference implementation for building MCP servers with `poom-mcp` at Flexio. It intentionally exemplifies every MCP primitive with real working code — tools (sync and async), resources (listing and read), prompts (listing and get), session lifecycle, and the repository pattern — so that any developer can read it and immediately understand how to wire their own MCP server.

---

## What this demo covers

| MCP feature | Example in this demo |
|-------------|----------------------|
| Tool (sync) | `create_note`, `get_note`, `update_note`, `delete_note`, `list_notes` |
| Tool (async via SSE) | `search_notes` (intentional 600 ms delay → 202 + SSE push) |
| Resource (listing) | `note://{id}`, `notes://tagged/{tag}` |
| Resource (read) | `NoteResourceHandler`, `TaggedNotesResourceHandler` |
| Prompt (listing) | `summarize_note`, `compare_notes` |
| Prompt (get) | `SummarizeNotePromptHandler`, `CompareNotesPromptHandler` |
| Session lifecycle | `NoteAssistantIntegrationTest` |
| Repository pattern | `Repository<Note, PropertyQuery>` with in-memory impl |

---

## Package structure

```
org.codingmatters.poom.mcp.demo/
  domain/
    NoteRepository.java             — factory: InMemoryRepositoryWithPropertyQuery.validating(Note.class)
    NoteService.java                — business logic facade (CRUD + list + search)
    types/                          — generated Note value object (from note.yaml)
  tools/
    CreateNoteTool.java             — creates a note, returns its id
    GetNoteTool.java                — retrieves a note as formatted markdown
    UpdateNoteTool.java             — partial update (only provided fields change)
    DeleteNoteTool.java             — removes a note permanently
    ListNotesTool.java              — lists all notes, optional tag filter
    SearchNotesTool.java            — full-text search (slow — demonstrates async path)
    ToolHelper.java                 — shared: success(), error(), arg(), argList()
  resources/
    NoteResourceHandler.java        — reads note://{id} as markdown
    TaggedNotesResourceHandler.java — lists notes://tagged/{tag}
  prompts/
    SummarizeNotePromptHandler.java — generates a summarization prompt
    CompareNotesPromptHandler.java  — generates a comparison prompt
  NoteAssistantDescriptor.java      — assembles the McpServerDescriptor
  NoteAssistantServer.java          — main class (Undertow HTTP server)
```

---

## Running the server

### Build

```bash
# From repo root:
mvn install -pl poom-mcp/poom-mcp-demo -am -DskipTests
```

### Start

```bash
SERVICE_HOST=0.0.0.0 SERVICE_PORT=8080 java \
    -cp poom-mcp/poom-mcp-demo/target/poom-mcp-demo-*-jar-with-dependencies.jar \
    org.codingmatters.poom.mcp.demo.NoteAssistantServer
```

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVICE_HOST` | `0.0.0.0` | Bind address |
| `SERVICE_PORT` | `8080` | Listen port |

The server exposes a single endpoint at `http://host:port/mcp`.

---

## Example curl session

```bash
# 1. Initialize — get a session id
SESSION=$(curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"initialize","params":{},"id":"1"}' \
  -D - | grep -i "mcp-session-id" | awk '{print $2}' | tr -d '\r')
echo "Session: $SESSION"

# 2. List tools
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: $SESSION" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":"2"}' | python3 -m json.tool

# 3. Create a note
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: $SESSION" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"create_note","arguments":{"title":"My first note","content":"Hello from poom-mcp!"}},"id":"3"}' | python3 -m json.tool

# 4. Open SSE channel (in background) then search
curl -s -N -X GET http://localhost:8080/mcp \
  -H "Accept: text/event-stream" \
  -H "Mcp-Session-Id: $SESSION" &
SSE_PID=$!

# 5. Search (triggers async path — result arrives on SSE channel above)
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: $SESSION" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"search_notes","arguments":{"query":"Hello"}},"id":"4"}' | python3 -m json.tool

# Wait for SSE result, then cleanup
sleep 2 && kill $SSE_PID 2>/dev/null || true
```

The `search_notes` call returns HTTP 202 immediately. The actual result (a JSON-RPC `result` message) arrives on the SSE channel opened in step 4.

---

## Adding a new tool

To add a new tool to the Note Assistant:

1. **Create the handler** — add a class in `tools/` implementing `Function<CallToolParams, CallToolResult>`. Use `ToolHelper.arg()` / `ToolHelper.argList()` to extract arguments. Return `ToolHelper.success(text)` or `ToolHelper.error(text)`.

2. **Register it** — in `NoteAssistantDescriptor`, add a `McpToolDescriptor` entry inside the `.tools(...)` call with `name`, `description`, `inputSchema(...)`, and `handler(new YourTool(noteService))`.

3. **Test it** — create a test class in `test/.../tools/` that creates the tool with a fresh `NoteService` and covers happy path, missing required args, and not-found cases.

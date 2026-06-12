# MCP JSON-RPC `id` Normalization — Design Spec
**Date:** 2026-06-12  
**Branch:** `develop`

## Problem

JSON-RPC 2.0 allows `id` to be `string | number | null`. The `mcp-types.yaml` defines `id: string` (strong typing, intentional). The generated `McpRequestReader` therefore throws `IOException` when it encounters a numeric `id`, causing a 500 instead of graceful handling.

Affected call-sites:
- `McpProcessor.handlePost()` — catches IOException but returns `-32700` (parse error), which is wrong
- `McpGatewayProcessor.process()` — IOException propagates uncaught → HTTP 500

## Solution

Add a `McpIdNormalizingParser` (`JsonParserDelegate`) that intercepts `VALUE_NUMBER_INT` / `VALUE_NUMBER_FLOAT` tokens on the `id` field and surfaces them as `VALUE_STRING` to the generated reader. Business code always receives `McpRequest.id()` as a `String`.

No change to `mcp-types.yaml`, no change to generated code.

---

## New File

**`poom-mcp-types/src/main/java/org/codingmatters/poom/mcp/types/json/McpIdNormalizingParser.java`**

Extends Jackson's `JsonParserDelegate`. Tracks the current field name via `nextToken()`. When positioned at `id` and the next token is `VALUE_NUMBER_INT` or `VALUE_NUMBER_FLOAT`:
- `nextToken()` returns `VALUE_STRING`
- `getText()` returns `delegate.getValueAsString()` (the number as a string)

All other fields and tokens pass through unchanged.

---

## Call-site Changes

### `McpProcessor.handlePost()` — `poom-mcp-processor`

Before:
```java
try (JsonParser parser = jsonFactory.createParser(request.payload())) {
    mcpRequest = new McpRequestReader().read(parser);
}
```

After:
```java
try (JsonParser parser = new McpIdNormalizingParser(jsonFactory.createParser(request.payload()))) {
    mcpRequest = new McpRequestReader().read(parser);
}
```

### `McpGatewayProcessor.process()` — `flexio-mcp-gateway`

Before:
```java
mcpRequest = new McpRequestReader().readString(jsonFactory, new String(body, StandardCharsets.UTF_8));
```

After:
```java
try (JsonParser parser = new McpIdNormalizingParser(jsonFactory.createParser(body))) {
    mcpRequest = new McpRequestReader().read(parser);
}
```

---

## Tests

**`poom-mcp-processor` — `McpIdNormalizingParserTest`** (unit, in `poom-mcp-types` or alongside):
- `numericIdIsNormalizedToString` — input `"id":1`, assert `mcpRequest.id().equals("1")`
- `stringIdPassesThrough` — input `"id":"abc"`, assert `mcpRequest.id().equals("abc")`
- `numericIdInParamsIsUntouched` — a params field named `id` with a number stays a number

**`McpProcessorIntegrationTest`** (existing or new):
- Existing `initialize` test extended or a new test: sends `"id":1`, expects valid response (not `-32700`)

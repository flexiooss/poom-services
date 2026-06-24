# MCP JSON-RPC `id` Normalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Accept numeric JSON-RPC `id` values in MCP messages by normalizing them to strings before the generated `McpRequestReader` sees them.

**Architecture:** A `McpIdNormalizingParser extends JsonParserDelegate` (Jackson) intercepts `VALUE_NUMBER_INT/FLOAT` tokens on the top-level `id` field and surfaces them as `VALUE_STRING`. Both `McpProcessor` and `McpGatewayProcessor` use this wrapper. Business code always receives `McpRequest.id()` as a `String`.

**Tech Stack:** Jackson `JsonParserDelegate`, poom-mcp-types (generated value objects), JUnit 5 + Hamcrest.

**Repos involved:**
- `/home/nel/workspaces/poom/poom-services` (branch `develop`) — Tasks 1 & 2
- `/home/nel/workspaces/ia/flexio-mcps` (branch `feature/records-filter-1.0.0-dev`) — Task 3

---

## File Structure

| Action | Path |
|--------|------|
| Create | `poom-mcp/poom-mcp-types/src/main/java/org/codingmatters/poom/mcp/types/json/McpIdNormalizingParser.java` |
| Create | `poom-mcp/poom-mcp-processor/src/test/java/org/codingmatters/poom/mcp/processor/McpIdNormalizingParserTest.java` |
| Modify | `poom-mcp/poom-mcp-processor/src/main/java/org/codingmatters/poom/mcp/processor/McpProcessor.java` (line 106) |
| Modify | `poom-mcp/poom-mcp-processor/src/test/java/org/codingmatters/poom/mcp/processor/McpProcessorValidationTest.java` |
| Modify | `flexio-mcp-gateway/flexio-mcp-gateway-service/pom.xml` (line 27-30) |
| Modify | `flexio-mcp-gateway/flexio-mcp-gateway-service/src/main/java/io/flexio/ia/mcps/gateway/service/McpGatewayProcessor.java` (lines 82-85) |

---

## Task 1: McpIdNormalizingParser

**Repos:** `poom-services`

**Files:**
- Create: `poom-mcp/poom-mcp-types/src/main/java/org/codingmatters/poom/mcp/types/json/McpIdNormalizingParser.java`
- Create (test): `poom-mcp/poom-mcp-processor/src/test/java/org/codingmatters/poom/mcp/processor/McpIdNormalizingParserTest.java`

- [ ] **Step 1: Write the failing tests**

```java
// poom-mcp/poom-mcp-processor/src/test/java/org/codingmatters/poom/mcp/processor/McpIdNormalizingParserTest.java
package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.mcp.types.McpRequest;
import org.codingmatters.poom.mcp.types.json.McpIdNormalizingParser;
import org.codingmatters.poom.mcp.types.json.McpRequestReader;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpIdNormalizingParserTest {
    private final JsonFactory jsonFactory = new JsonFactory();

    @Test
    void numericIntIdIsNormalizedToString() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":42,\"method\":\"initialize\",\"params\":{}}";
        assertThat(parse(json).id(), is("42"));
    }

    @Test
    void stringIdPassesThrough() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":\"abc\",\"method\":\"initialize\",\"params\":{}}";
        assertThat(parse(json).id(), is("abc"));
    }

    @Test
    void numericIdInsideNestedParamsIsUntouched() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/call\","
                + "\"params\":{\"arguments\":{\"id\":999}}}";
        McpRequest req = parse(json);
        assertThat(req.id(), is("1"));
        assertThat(
            req.params()
               .property("arguments").single().objectValue()
               .property("id").single().longValue(),
            is(999L)
        );
    }

    private McpRequest parse(String json) throws Exception {
        try (JsonParser parser = new McpIdNormalizingParser(jsonFactory.createParser(json))) {
            return new McpRequestReader().read(parser);
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /home/nel/workspaces/poom/poom-services/poom-mcp/poom-mcp-processor
mvn test -Dtest=McpIdNormalizingParserTest -pl . 2>&1 | tail -20
```

Expected: compilation error — `McpIdNormalizingParser` does not exist yet.

- [ ] **Step 3: Implement McpIdNormalizingParser**

```java
// poom-mcp/poom-mcp-types/src/main/java/org/codingmatters/poom/mcp/types/json/McpIdNormalizingParser.java
package org.codingmatters.poom.mcp.types.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;

import java.io.IOException;

public class McpIdNormalizingParser extends JsonParserDelegate {
    private int depth = 0;
    private boolean awaitingIdValue = false;
    private String idStringValue = null;

    public McpIdNormalizingParser(JsonParser delegate) {
        super(delegate);
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonToken token = super.nextToken();
        idStringValue = null;

        if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
            depth++;
        } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
            depth--;
        }

        if (depth == 1 && token == JsonToken.FIELD_NAME && "id".equals(getCurrentName())) {
            awaitingIdValue = true;
        } else if (awaitingIdValue) {
            awaitingIdValue = false;
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                idStringValue = super.getText();
                return JsonToken.VALUE_STRING;
            }
        }
        return token;
    }

    @Override
    public JsonToken currentToken() {
        return idStringValue != null ? JsonToken.VALUE_STRING : super.currentToken();
    }

    @Override
    public String getText() throws IOException {
        return idStringValue != null ? idStringValue : super.getText();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /home/nel/workspaces/poom/poom-services/poom-mcp/poom-mcp-processor
mvn test -Dtest=McpIdNormalizingParserTest -pl . 2>&1 | tail -20
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
cd /home/nel/workspaces/poom/poom-services
git add poom-mcp/poom-mcp-types/src/main/java/org/codingmatters/poom/mcp/types/json/McpIdNormalizingParser.java \
        poom-mcp/poom-mcp-processor/src/test/java/org/codingmatters/poom/mcp/processor/McpIdNormalizingParserTest.java
git commit -m "feat: McpIdNormalizingParser normalizes numeric JSON-RPC id to string"
```

---

## Task 2: Wire McpProcessor

**Repos:** `poom-services`

**Files:**
- Modify: `poom-mcp/poom-mcp-processor/src/main/java/org/codingmatters/poom/mcp/processor/McpProcessor.java`
- Modify: `poom-mcp/poom-mcp-processor/src/test/java/org/codingmatters/poom/mcp/processor/McpProcessorValidationTest.java`

- [ ] **Step 1: Add failing integration test**

Add the following test to the existing `McpProcessorValidationTest` class (after the last `@Test` method, before the `private ByteArrayInputStream asStream` method):

```java
@Test
void whenInitializeWithNumericId__thenSuccessAndIdEchoedAsString() throws Exception {
    TestResponseDeleguate response = new TestResponseDeleguate();
    processor.process(
            TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                    .contentType("application/json")
                    .payload(asStream("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{},\"id\":1}"))
                    .build(),
            response
    );
    assertThat(response.status(), is(200));
    String body = new String(response.payload());
    assertThat(body, containsString("\"id\":\"1\""));
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /home/nel/workspaces/poom/poom-services/poom-mcp/poom-mcp-processor
mvn test -Dtest=McpProcessorValidationTest#whenInitializeWithNumericId__thenSuccessAndIdEchoedAsString 2>&1 | tail -20
```

Expected: FAIL — response contains `-32700` (parse error) instead of 200.

- [ ] **Step 3: Update McpProcessor.handlePost() to use the normalizing parser**

In `McpProcessor.java`, replace lines 105–111:

```java
// Before
McpRequest mcpRequest;
try (JsonParser parser = jsonFactory.createParser(request.payload())) {
    mcpRequest = new McpRequestReader().read(parser);
} catch (IOException e) {
    writeJsonRpcError(response, null, -32700, "Parse error");
    return;
}
```

```java
// After
McpRequest mcpRequest;
try (JsonParser parser = new McpIdNormalizingParser(jsonFactory.createParser(request.payload()))) {
    mcpRequest = new McpRequestReader().read(parser);
} catch (IOException e) {
    writeJsonRpcError(response, null, -32700, "Parse error");
    return;
}
```

Add the import at the top of `McpProcessor.java` (with the other `json` imports):
```java
import org.codingmatters.poom.mcp.types.json.McpIdNormalizingParser;
```

- [ ] **Step 4: Run all processor tests**

```bash
cd /home/nel/workspaces/poom/poom-services/poom-mcp/poom-mcp-processor
mvn test 2>&1 | tail -20
```

Expected: all tests pass, including `McpIdNormalizingParserTest` and the new `whenInitializeWithNumericId__thenSuccessAndIdEchoedAsString`.

- [ ] **Step 5: Install poom-mcp locally**

```bash
cd /home/nel/workspaces/poom/poom-services/poom-mcp
mvn install -DskipTests 2>&1 | tail -10
```

Expected: `BUILD SUCCESS` and `poom-mcp-types:1.291.0-SNAPSHOT` installed in local Maven repo.

- [ ] **Step 6: Commit**

```bash
cd /home/nel/workspaces/poom/poom-services
git add poom-mcp/poom-mcp-processor/src/main/java/org/codingmatters/poom/mcp/processor/McpProcessor.java \
        poom-mcp/poom-mcp-processor/src/test/java/org/codingmatters/poom/mcp/processor/McpProcessorValidationTest.java
git commit -m "fix: McpProcessor uses McpIdNormalizingParser to accept numeric JSON-RPC id"
```

---

## Task 3: Update McpGatewayProcessor (flexio-mcps)

**Repo:** `/home/nel/workspaces/ia/flexio-mcps`

**Files:**
- Modify: `flexio-mcp-gateway/flexio-mcp-gateway-service/pom.xml` (lines 27–30)
- Modify: `flexio-mcp-gateway/flexio-mcp-gateway-service/src/main/java/io/flexio/ia/mcps/gateway/service/McpGatewayProcessor.java` (lines 82–85)

- [ ] **Step 1: Update poom-mcp-types version in gateway pom**

In `flexio-mcp-gateway/flexio-mcp-gateway-service/pom.xml`, replace lines 27–30:

```xml
<!-- Before -->
<dependency>
    <groupId>org.codingmatters.poom.mcp</groupId>
    <artifactId>poom-mcp-types</artifactId>
</dependency>
```

```xml
<!-- After -->
<dependency>
    <groupId>org.codingmatters.poom.mcp</groupId>
    <artifactId>poom-mcp-types</artifactId>
    <version>1.291.0-SNAPSHOT</version>
</dependency>
```

- [ ] **Step 2: Update McpGatewayProcessor to use the normalizing parser**

In `McpGatewayProcessor.java`, replace lines 82–85:

```java
// Before
McpRequest mcpRequest = null;
if (body != null && body.length > 0) {
    mcpRequest = new McpRequestReader().readString(jsonFactory, new String(body, StandardCharsets.UTF_8));
}
```

```java
// After
McpRequest mcpRequest = null;
if (body != null && body.length > 0) {
    try (com.fasterxml.jackson.core.JsonParser p =
                 new McpIdNormalizingParser(jsonFactory.createParser(body))) {
        mcpRequest = new McpRequestReader().read(p);
    }
}
```

Add these two imports at the top of `McpGatewayProcessor.java` (with the other `json` imports):
```java
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.mcp.types.json.McpIdNormalizingParser;
```

And simplify the `try` block (remove `com.fasterxml.jackson.core.JsonParser` qualified name now that it's imported):
```java
McpRequest mcpRequest = null;
if (body != null && body.length > 0) {
    try (JsonParser p = new McpIdNormalizingParser(jsonFactory.createParser(body))) {
        mcpRequest = new McpRequestReader().read(p);
    }
}
```

- [ ] **Step 3: Build the gateway module**

```bash
cd /home/nel/workspaces/ia/flexio-mcps/flexio-mcp-gateway
mvn compile 2>&1 | tail -20
```

Expected: `BUILD SUCCESS` — no compilation errors.

- [ ] **Step 4: Verify with curl**

Start the debug instance, then run:

```bash
# initialize with numeric id
curl -s -X POST http://127.0.0.1:50000/mcps/flexio-mcp-user \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0.1"}}}' \
  | python3 -m json.tool
```

Expected: HTTP 200, `"id": "1"` in the response body (numeric input echoed as string).

- [ ] **Step 5: Commit**

```bash
cd /home/nel/workspaces/ia/flexio-mcps
git add flexio-mcp-gateway/flexio-mcp-gateway-service/pom.xml \
        flexio-mcp-gateway/flexio-mcp-gateway-service/src/main/java/io/flexio/ia/mcps/gateway/service/McpGatewayProcessor.java
git commit -m "fix: McpGatewayProcessor uses McpIdNormalizingParser to accept numeric JSON-RPC id"
```

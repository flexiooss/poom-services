# poom-mcp-demo — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `poom-mcp-demo`, a complete, production-quality note-taking MCP server that serves as the canonical reference implementation for Flexio MCP servers. Every MCP primitive (tools sync + async, resources, prompts) is exemplified with tests.

**Repo:** `poom-services` (`/home/nel/workspaces/poom/poom-services`), branch `feature/poom-mcp-1.289.0-dev`.

**Prerequisite:** This plan also completes two missing dispatches in `McpProcessor` (`resources/read`, `prompts/get`) — these must be done first (Tasks 1–2).

---

## Functional description — The Note Assistant

The Note Assistant is a server that lets an AI model manage a collection of short notes. Notes have a title, free-form content, and optional tags. The server exposes:

### Tools

| Tool | Arguments | Result | Dispatch |
|------|-----------|--------|----------|
| `create_note` | `title` (str, required), `content` (str, required), `tags` (str[], optional) | `id` of the created note | **sync** |
| `get_note` | `id` (str, required) | Formatted note text (title, content, tags, dates) | **sync** |
| `update_note` | `id` (str, required), `title` (str, opt), `content` (str, opt), `tags` (str[], opt) | Confirmation message | **sync** |
| `delete_note` | `id` (str, required) | Confirmation message | **sync** |
| `list_notes` | `tag` (str, optional filter) | Newline-separated `{id} — {title} [{tags}]` | **sync** |
| `search_notes` | `query` (str, required) | Newline-separated matching notes with excerpt | **async** (simulated slow, demonstrates 202 + SSE push) |

`search_notes` adds a 600ms artificial delay to reliably exceed the 500ms default `syncTimeoutMillis` and trigger the async path. In a real implementation this would be a proper full-text search engine.

### Resources

| URI pattern | MIME type | Content |
|-------------|-----------|---------|
| `note://{id}` | `text/markdown` | Full note as markdown: `# title\n\ntags: …\n\ncontent` |
| `notes://tagged/{tag}` | `text/plain` | Newline-separated summary of all notes with that tag |

### Prompts

| Name | Arguments | Generated prompt |
|------|-----------|-----------------|
| `summarize_note` | `note_id` (str) | "Please summarize the following note in 2-3 sentences: [note content]" |
| `compare_notes` | `note_id_1` (str), `note_id_2` (str) | "Compare and contrast the following two notes: [note 1 content] / [note 2 content]" |

---

## File map

```
Root pom.xml
  + <module>poom-mcp/poom-mcp-demo</module>
  + BOM entry for poom-mcp-demo

poom-mcp/pom.xml
  + <module>poom-mcp-demo</module>

poom-mcp/poom-mcp-processor/src/main/java/.../McpProcessor.java
  ~ add resources/read dispatch (handleResourceRead)
  ~ add prompts/get dispatch (handlePromptGet)
  ~ add uriPrefix() helper
  ~ add buildReadResourceResultObject() helper
  ~ add buildGetPromptResultObject() helper

poom-mcp/poom-mcp-demo/pom.xml
poom-mcp/poom-mcp-demo/src/main/resources/note.yaml
poom-mcp/poom-mcp-demo/src/main/java/org/codingmatters/poom/mcp/demo/
  NoteRepository.java              factory: InMemoryRepositoryWithPropertyQuery
  NoteService.java                 business logic facade
  tools/CreateNoteTool.java
  tools/GetNoteTool.java
  tools/UpdateNoteTool.java
  tools/DeleteNoteTool.java
  tools/ListNotesTool.java
  tools/SearchNotesTool.java
  resources/NoteResourceHandler.java
  resources/TaggedNotesResourceHandler.java
  prompts/SummarizeNotePromptHandler.java
  prompts/CompareNotesPromptHandler.java
  NoteAssistantDescriptor.java
  NoteAssistantServer.java

poom-mcp/poom-mcp-demo/src/test/java/org/codingmatters/poom/mcp/demo/
  NoteServiceTest.java
  tools/CreateNoteToolTest.java
  tools/GetNoteToolTest.java
  tools/UpdateNoteToolTest.java
  tools/DeleteNoteToolTest.java
  tools/ListNotesToolTest.java
  tools/SearchNotesToolTest.java
  resources/NoteResourceHandlerTest.java
  resources/TaggedNotesResourceHandlerTest.java
  prompts/SummarizeNotePromptHandlerTest.java
  prompts/CompareNotesPromptHandlerTest.java
  NoteAssistantIntegrationTest.java

poom-mcp/poom-mcp-demo/README.md
poom-mcp/README.md  (update Limitations table)
```

---

## Task 1 — `resources/read` dispatch in `McpProcessor`

**File:** `poom-mcp-processor/src/main/java/org/codingmatters/poom/mcp/processor/McpProcessor.java`

- [ ] **Step 1: Add `resources/read` case in `dispatchMethod`**

```java
case "resources/read" -> handleResourceRead(response, mcpRequest);
```

- [ ] **Step 2: Add `handleResourceRead`**

```java
@SuppressWarnings("unchecked")
private void handleResourceRead(ResponseDelegate response, McpRequest mcpRequest) throws IOException {
    String uri = mcpRequest.params() != null && mcpRequest.params().property("uri") != null
            ? mcpRequest.params().property("uri").single().stringValue()
            : null;
    if (uri == null) {
        writeJsonRpcError(response, mcpRequest.id(), -32602, "Invalid params: uri required");
        return;
    }
    Optional<org.codingmatters.poom.mcp.McpResourceDescriptor> resource =
            descriptor.opt().resources().safe().stream()
                    .filter(r -> r.uri() != null && uri.startsWith(uriPrefix(r.uri())))
                    .findFirst();
    if (resource.isEmpty()) {
        writeJsonRpcError(response, mcpRequest.id(), -32601, "Resource not found: " + uri);
        return;
    }
    org.codingmatters.poom.mcp.types.ReadResourceParams params =
            org.codingmatters.poom.mcp.types.ReadResourceParams.builder().uri(uri).build();
    try {
        org.codingmatters.poom.mcp.types.ReadResourceResult result =
                (org.codingmatters.poom.mcp.types.ReadResourceResult) resource.get().handler().apply(params);
        writeJsonRpcResponse(response, McpResponse.builder()
                .jsonrpc("2.0").id(mcpRequest.id())
                .result(buildReadResourceResultObject(result))
                .build());
    } catch (RuntimeException e) {
        log.error("resource handler threw for uri {}", uri, e);
        writeJsonRpcError(response, mcpRequest.id(), -32603, "Internal error");
    }
}

private String uriPrefix(String uriTemplate) {
    int idx = uriTemplate.indexOf('{');
    return idx >= 0 ? uriTemplate.substring(0, idx) : uriTemplate;
}

private ObjectValue buildReadResourceResultObject(org.codingmatters.poom.mcp.types.ReadResourceResult result) {
    List<ObjectValue> contents = result.opt().contents().safe().stream()
            .map(c -> ObjectValue.builder()
                    .property("uri", v -> v.stringValue(c.uri()))
                    .property("mimeType", v -> v.stringValue(c.mimeType() != null ? c.mimeType() : ""))
                    .property("text", v -> v.stringValue(c.text() != null ? c.text() : ""))
                    .build())
            .toList();
    return ObjectValue.builder()
            .property("contents", PropertyValue.multipleObject(contents.toArray(new ObjectValue[0])))
            .build();
}
```

- [ ] **Step 3: Build and verify existing tests still pass**

```bash
mvn test -pl poom-mcp/poom-mcp-processor
```

Expected: `BUILD SUCCESS`, 11 tests pass.

---

## Task 2 — `prompts/get` dispatch in `McpProcessor`

**File:** `poom-mcp-processor/src/main/java/org/codingmatters/poom/mcp/processor/McpProcessor.java`

- [ ] **Step 1: Add `prompts/get` case in `dispatchMethod`**

```java
case "prompts/get" -> handlePromptGet(response, mcpRequest);
```

- [ ] **Step 2: Add `handlePromptGet`**

```java
@SuppressWarnings("unchecked")
private void handlePromptGet(ResponseDelegate response, McpRequest mcpRequest) throws IOException {
    String name = mcpRequest.params() != null && mcpRequest.params().property("name") != null
            ? mcpRequest.params().property("name").single().stringValue()
            : null;
    Optional<org.codingmatters.poom.mcp.McpPromptDescriptor> prompt =
            descriptor.opt().prompts().safe().stream()
                    .filter(p -> name != null && name.equals(p.name()))
                    .findFirst();
    if (prompt.isEmpty()) {
        writeJsonRpcError(response, mcpRequest.id(), -32601, "Prompt not found: " + name);
        return;
    }
    ObjectValue arguments = mcpRequest.params() != null && mcpRequest.params().property("arguments") != null
            ? mcpRequest.params().property("arguments").single().objectValue()
            : ObjectValue.builder().build();
    if (arguments == null) arguments = ObjectValue.builder().build();

    org.codingmatters.poom.mcp.types.GetPromptParams params =
            org.codingmatters.poom.mcp.types.GetPromptParams.builder()
                    .name(name).arguments(arguments).build();
    try {
        org.codingmatters.poom.mcp.types.GetPromptResult result =
                (org.codingmatters.poom.mcp.types.GetPromptResult) prompt.get().handler().apply(params);
        writeJsonRpcResponse(response, McpResponse.builder()
                .jsonrpc("2.0").id(mcpRequest.id())
                .result(buildGetPromptResultObject(result))
                .build());
    } catch (RuntimeException e) {
        log.error("prompt handler threw for name {}", name, e);
        writeJsonRpcError(response, mcpRequest.id(), -32603, "Internal error");
    }
}

private ObjectValue buildGetPromptResultObject(org.codingmatters.poom.mcp.types.GetPromptResult result) {
    List<ObjectValue> messages = result.opt().messages().safe().stream()
            .map(m -> ObjectValue.builder()
                    .property("role", v -> v.stringValue(m.role()))
                    .property("content", v -> v.objectValue(m.content() != null ? m.content() : ObjectValue.builder().build()))
                    .build())
            .toList();
    return ObjectValue.builder()
            .property("description", v -> v.stringValue(result.description() != null ? result.description() : ""))
            .property("messages", PropertyValue.multipleObject(messages.toArray(new ObjectValue[0])))
            .build();
}
```

- [ ] **Step 3: Run tests + commit**

```bash
mvn test -pl poom-mcp/poom-mcp-processor
```

```bash
git add poom-mcp/poom-mcp-processor/src
git commit -m "feat: McpProcessor — add resources/read and prompts/get dispatch"
```

---

## Task 3 — Module scaffold + `Note` value object

**Files:**
- Modify: `poom-mcp/pom.xml` (add `<module>poom-mcp-demo</module>`)
- Modify: root `pom.xml` (add module + BOM entry)
- Create: `poom-mcp/poom-mcp-demo/pom.xml`
- Create: `poom-mcp/poom-mcp-demo/src/main/resources/note.yaml`

- [ ] **Step 1: Add `poom-mcp-demo` to `poom-mcp/pom.xml`**

```xml
<modules>
    <module>poom-mcp-types</module>
    <module>poom-mcp-descriptors</module>
    <module>poom-mcp-processor</module>
    <module>poom-mcp-demo</module>    <!-- add this -->
</modules>
```

- [ ] **Step 2: Add to root `pom.xml` `<modules>`** (after `<module>poom-mcp</module>`)

The reactor resolves sub-modules transitively — no change needed if the root already includes `poom-mcp`. Verify with `mvn help:evaluate` if needed.

- [ ] **Step 3: Add BOM entry to root `pom.xml`** (after `<!--  //MCP-->` comment)

```xml
            <dependency>
                <groupId>org.codingmatters.poom.mcp</groupId>
                <artifactId>poom-mcp-demo</artifactId>
                <version>1.289.0-SNAPSHOT</version>
            </dependency>
```

- [ ] **Step 4: Create `poom-mcp-demo/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.codingmatters.poom.mcp</groupId>
        <artifactId>poom-mcp</artifactId>
        <version>1.289.0-SNAPSHOT</version>
    </parent>

    <artifactId>poom-mcp-demo</artifactId>

    <dependencies>
        <!-- MCP -->
        <dependency>
            <groupId>org.codingmatters.poom.mcp</groupId>
            <artifactId>poom-mcp-descriptors</artifactId>
        </dependency>
        <dependency>
            <groupId>org.codingmatters.poom.mcp</groupId>
            <artifactId>poom-mcp-processor</artifactId>
        </dependency>
        <dependency>
            <groupId>org.codingmatters.poom.mcp</groupId>
            <artifactId>poom-mcp-types</artifactId>
        </dependency>
        <!-- Domain + Repository -->
        <dependency>
            <groupId>org.codingmatters.poom</groupId>
            <artifactId>poom-services-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>org.codingmatters.poom</groupId>
            <artifactId>poom-services-repository-in-memory</artifactId>
        </dependency>
        <!-- Runtime -->
        <dependency>
            <groupId>org.codingmatters.value.objects</groupId>
            <artifactId>cdm-value-objects-values</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.codingmatters.rest</groupId>
            <artifactId>cdm-rest-undertow</artifactId>
        </dependency>
        <dependency>
            <groupId>org.codingmatters.poom</groupId>
            <artifactId>poom-services-logging</artifactId>
        </dependency>
        <!-- Test -->
        <dependency>
            <groupId>org.codingmatters.rest</groupId>
            <artifactId>cdm-rest-tests-support</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.codingmatters.poom</groupId>
            <artifactId>poom-services-test-support</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codingmatters.value.objects</groupId>
                <artifactId>cdm-value-objects-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>note-types</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>generate</goal>
                            <goal>json</goal>
                        </goals>
                        <configuration>
                            <destination-package>org.codingmatters.poom.mcp.demo.domain.types</destination-package>
                            <input-spec>src/main/resources/note.yaml</input-spec>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>add-generated-sources</id>
                        <phase>generate-sources</phase>
                        <goals><goal>add-source</goal></goals>
                        <configuration>
                            <sources><source>target/generated-sources</source></sources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 5: Create `note.yaml`**

```yaml
---
Note:
  title: string
  content: string
  tags:
    $list: string
  created-at: date-time
  updated-at: date-time
```

- [ ] **Step 6: Verify generation**

```bash
mvn generate-sources -pl poom-mcp/poom-mcp-demo -am -DskipTests
ls poom-mcp/poom-mcp-demo/target/generated-sources/org/codingmatters/poom/mcp/demo/domain/types/
```

Expected: `Note.java`, `NoteImpl.java` + json readers/writers.

```bash
git add poom-mcp/poom-mcp-demo/ poom-mcp/pom.xml pom.xml
git commit -m "feat: poom-mcp-demo module scaffold + Note value object"
```

---

## Task 4 — `NoteRepository` + `NoteService`

**Files:**
- Create: `NoteRepository.java`
- Create: `NoteService.java`

- [ ] **Step 1: Create `NoteRepository.java`**

Factory that builds a `Repository<Note, PropertyQuery>` backed by an in-memory store.

```java
package org.codingmatters.poom.mcp.demo.domain;

import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;

public class NoteRepository {
    private NoteRepository() {}

    public static Repository<Note, PropertyQuery> create() {
        return InMemoryRepositoryWithPropertyQuery.validating(Note.class);
    }
}
```

- [ ] **Step 2: Create `NoteService.java`**

Business logic facade — all methods translate between the domain (Entity/Repository) and simple Java types. Tool handlers never touch the Repository directly; they go through `NoteService`.

```java
package org.codingmatters.poom.mcp.demo.domain;

import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteService {

    private final Repository<Note, PropertyQuery> repository;

    public NoteService(Repository<Note, PropertyQuery> repository) {
        this.repository = repository;
    }

    public Entity<Note> create(String title, String content, List<String> tags) throws RepositoryException {
        Note note = Note.builder()
                .title(title)
                .content(content)
                .tags(tags != null ? tags.toArray(new String[0]) : new String[0])
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.create(note);
    }

    public Optional<Entity<Note>> get(String id) throws RepositoryException {
        Entity<Note> entity = repository.retrieve(id);
        return Optional.ofNullable(entity);
    }

    /** Partial update — null fields are left unchanged. */
    public Optional<Entity<Note>> update(String id, String title, String content, List<String> tags)
            throws RepositoryException {
        Entity<Note> existing = repository.retrieve(id);
        if (existing == null) return Optional.empty();

        Note updated = Note.builder()
                .from(existing.value())
                .title(title != null ? title : existing.value().title())
                .content(content != null ? content : existing.value().content())
                .tags(tags != null ? tags.toArray(new String[0]) : existing.value().tags().toArray(new String[0]))
                .updatedAt(LocalDateTime.now())
                .build();
        return Optional.of(repository.update(existing, updated));
    }

    public boolean delete(String id) throws RepositoryException {
        Entity<Note> existing = repository.retrieve(id);
        if (existing == null) return false;
        repository.delete(existing);
        return true;
    }

    /** Lists all notes, optionally filtered by tag. */
    public List<Entity<Note>> list(String tag) throws RepositoryException {
        PagedEntityList<Note> all = repository.all(null, 0, Integer.MAX_VALUE);
        List<Entity<Note>> result = new ArrayList<>();
        for (Entity<Note> entity : all) {
            if (tag == null || containsTag(entity.value(), tag)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * Full-text search in title and content (case-insensitive).
     * Intentionally adds a 600ms delay to simulate a slow operation
     * and demonstrate the MCP async tool call path.
     */
    public List<Entity<Note>> search(String query) throws RepositoryException, InterruptedException {
        Thread.sleep(600);
        String lq = query.toLowerCase();
        PagedEntityList<Note> all = repository.all(null, 0, Integer.MAX_VALUE);
        List<Entity<Note>> result = new ArrayList<>();
        for (Entity<Note> entity : all) {
            Note n = entity.value();
            if ((n.title() != null && n.title().toLowerCase().contains(lq))
                    || (n.content() != null && n.content().toLowerCase().contains(lq))) {
                result.add(entity);
            }
        }
        return result;
    }

    private boolean containsTag(Note note, String tag) {
        if (note.opt().tags().isAbsent()) return false;
        for (String t : note.tags()) {
            if (tag.equalsIgnoreCase(t)) return true;
        }
        return false;
    }
}
```

- [ ] **Step 3: Write `NoteServiceTest`**

Tests for: create, get (found + not found), update (full + partial + not found), delete (found + not found), list (all + by tag), search (match + no match).

```java
package org.codingmatters.poom.mcp.demo.domain;

import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class NoteServiceTest {

    private NoteService service;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
    }

    @Test
    void create_returnsEntityWithId() throws Exception {
        Entity<Note> note = service.create("Hello", "World content", List.of("greetings"));
        assertThat(note.id(), notNullValue());
        assertThat(note.value().title(), is("Hello"));
        assertThat(note.value().content(), is("World content"));
        assertThat(note.value().tags().toArray(), arrayContaining("greetings"));
        assertThat(note.value().createdAt(), notNullValue());
    }

    @Test
    void get_existingNote_returnsIt() throws Exception {
        Entity<Note> created = service.create("T", "C", null);
        Optional<Entity<Note>> found = service.get(created.id());
        assertThat(found.isPresent(), is(true));
        assertThat(found.get().value().title(), is("T"));
    }

    @Test
    void get_unknownId_returnsEmpty() throws Exception {
        assertThat(service.get("no-such-id").isPresent(), is(false));
    }

    @Test
    void update_changesOnlyProvidedFields() throws Exception {
        Entity<Note> created = service.create("Original", "Body", List.of("tag1"));
        Optional<Entity<Note>> updated = service.update(created.id(), "New title", null, null);
        assertThat(updated.isPresent(), is(true));
        assertThat(updated.get().value().title(), is("New title"));
        assertThat(updated.get().value().content(), is("Body")); // unchanged
        assertThat(updated.get().value().tags().toArray(), arrayContaining("tag1")); // unchanged
    }

    @Test
    void update_unknownId_returnsEmpty() throws Exception {
        assertThat(service.update("no-such-id", "T", "C", null).isPresent(), is(false));
    }

    @Test
    void delete_existingNote_returnsTrue() throws Exception {
        Entity<Note> created = service.create("T", "C", null);
        assertThat(service.delete(created.id()), is(true));
        assertThat(service.get(created.id()).isPresent(), is(false));
    }

    @Test
    void delete_unknownId_returnsFalse() throws Exception {
        assertThat(service.delete("no-such-id"), is(false));
    }

    @Test
    void list_noFilter_returnsAll() throws Exception {
        service.create("A", "c", List.of("x"));
        service.create("B", "c", List.of("y"));
        assertThat(service.list(null), hasSize(2));
    }

    @Test
    void list_withTag_returnsOnlyMatching() throws Exception {
        service.create("A", "c", List.of("work"));
        service.create("B", "c", List.of("personal"));
        List<Entity<Note>> result = service.list("work");
        assertThat(result, hasSize(1));
        assertThat(result.get(0).value().title(), is("A"));
    }

    @Test
    void search_matchesInTitleOrContent() throws Exception {
        service.create("Meeting notes", "agenda for thursday", null);
        service.create("Shopping list", "milk eggs bread", null);
        List<Entity<Note>> result = service.search("thursday");
        assertThat(result, hasSize(1));
        assertThat(result.get(0).value().title(), is("Meeting notes"));
    }

    @Test
    void search_noMatch_returnsEmpty() throws Exception {
        service.create("T", "C", null);
        assertThat(service.search("xyzzy"), empty());
    }
}
```

- [ ] **Step 4: Run NoteServiceTest**

```bash
mvn test -pl poom-mcp/poom-mcp-demo -Dtest=NoteServiceTest
```

Expected: `BUILD SUCCESS`, all tests pass.

```bash
git add poom-mcp/poom-mcp-demo/src
git commit -m "feat: poom-mcp-demo NoteRepository + NoteService"
```

---

## Task 5 — Tool handlers

Each tool is a standalone class implementing `Function<CallToolParams, CallToolResult>`. This makes them independently testable without the MCP stack.

**Files:** `tools/CreateNoteTool.java`, `GetNoteTool.java`, `UpdateNoteTool.java`, `DeleteNoteTool.java`, `ListNotesTool.java`, `SearchNotesTool.java`

### Helper base pattern

All tools share the same result helpers. Put them in a package-private `ToolHelper`:

```java
package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;

class ToolHelper {
    static CallToolResult success(String text) {
        return CallToolResult.builder()
                .content(ToolContent.builder().type("text").text(text).build())
                .isError(false).build();
    }
    static CallToolResult error(String text) {
        return CallToolResult.builder()
                .content(ToolContent.builder().type("text").text(text).build())
                .isError(true).build();
    }
    // extract string from ObjectValue argument, null-safe
    static String arg(org.codingmatters.value.objects.values.ObjectValue args, String name) {
        if (args == null || args.property(name) == null) return null;
        return args.property(name).single().stringValue();
    }
    // extract string[] from ObjectValue argument
    static java.util.List<String> argList(org.codingmatters.value.objects.values.ObjectValue args, String name) {
        if (args == null || args.property(name) == null) return null;
        org.codingmatters.value.objects.values.PropertyValue pv = args.property(name);
        if (pv.isMultiple()) {
            java.util.List<String> result = new java.util.ArrayList<>();
            for (org.codingmatters.value.objects.values.PropertyValue.Value v : pv.multiple()) {
                result.add(v.stringValue());
            }
            return result;
        }
        return java.util.List.of(pv.single().stringValue());
    }
}
```

- [ ] **Step 1: `CreateNoteTool.java`**

```java
package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;

import java.util.function.Function;

public class CreateNoteTool implements Function<CallToolParams, CallToolResult> {

    private final NoteService noteService;

    public CreateNoteTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String title   = ToolHelper.arg(params.arguments(), "title");
        String content = ToolHelper.arg(params.arguments(), "content");
        if (title == null || title.isBlank()) return ToolHelper.error("title is required");
        if (content == null)                  return ToolHelper.error("content is required");
        try {
            var entity = noteService.create(title, content, ToolHelper.argList(params.arguments(), "tags"));
            return ToolHelper.success("Note created with id: " + entity.id());
        } catch (Exception e) {
            return ToolHelper.error("Failed to create note: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: `GetNoteTool.java`**

```java
package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class GetNoteTool implements Function<CallToolParams, CallToolResult> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final NoteService noteService;

    public GetNoteTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public CallToolResult apply(CallToolParams params) {
        String id = ToolHelper.arg(params.arguments(), "id");
        if (id == null) return ToolHelper.error("id is required");
        try {
            return noteService.get(id)
                    .map(e -> {
                        var n = e.value();
                        var tags = n.opt().tags().isAbsent() ? "(none)" : String.join(", ", n.tags());
                        var text = "# " + n.title() + "\n\n"
                                 + "**id:** " + e.id() + "\n"
                                 + "**tags:** " + tags + "\n"
                                 + "**created:** " + (n.createdAt() != null ? n.createdAt().format(FMT) : "?") + "\n"
                                 + "**updated:** " + (n.updatedAt() != null ? n.updatedAt().format(FMT) : "?") + "\n\n"
                                 + n.content();
                        return ToolHelper.success(text);
                    })
                    .orElseGet(() -> ToolHelper.error("Note not found: " + id));
        } catch (Exception e) {
            return ToolHelper.error("Failed to get note: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 3: `UpdateNoteTool.java`**

```java
public class UpdateNoteTool implements Function<CallToolParams, CallToolResult> {
    // extract id, title?, content?, tags? from params.arguments()
    // call noteService.update(id, title, content, tags)
    // return success("Note <id> updated.") or error("Note not found: <id>")
}
```

- [ ] **Step 4: `DeleteNoteTool.java`**

```java
public class DeleteNoteTool implements Function<CallToolParams, CallToolResult> {
    // extract id from params.arguments()
    // call noteService.delete(id)
    // return success("Note <id> deleted.") or error("Note not found: <id>")
}
```

- [ ] **Step 5: `ListNotesTool.java`**

```java
public class ListNotesTool implements Function<CallToolParams, CallToolResult> {
    // extract optional tag from params.arguments()
    // call noteService.list(tag)
    // format as: "{id} — {title} [{tags}]\n" per note
    // if empty: "No notes found."
}
```

- [ ] **Step 6: `SearchNotesTool.java`**

```java
public class SearchNotesTool implements Function<CallToolParams, CallToolResult> {
    // extract query from params.arguments()
    // call noteService.search(query) — this sleeps 600ms internally
    // format results as: "{id}: {excerpt}\n" (first 80 chars of content)
    // if empty: "No notes match \"{query}\"."
    // catch InterruptedException → Thread.currentThread().interrupt(); return error(...)
}
```

- [ ] **Step 7: Write tool tests**

For each tool, create `tools/{ToolName}Test.java` covering:
- Happy path with all arguments
- Missing required argument → error result (isError=true)
- Entity not found → error result
- Multi-value tags list

Pattern for `CreateNoteToolTest`:

```java
class CreateNoteToolTest {

    private NoteService service;
    private CreateNoteTool tool;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        tool = new CreateNoteTool(service);
    }

    private CallToolParams params(String json) {
        // build CallToolParams with arguments parsed from JSON string
        // use Jackson ObjectMapper or build ObjectValue manually
    }

    @Test
    void givenTitleAndContent__thenNoteCreated() throws Exception {
        var result = tool.apply(paramsWithArgs("title", "My note", "content", "Some text"));
        assertThat(result.isError(), is(false));
        assertThat(result.content().get(0).text(), containsString("Note created with id:"));
        assertThat(service.list(null), hasSize(1));
    }

    @Test
    void givenMissingTitle__thenErrorResult() {
        var result = tool.apply(paramsWithArgs("content", "text"));
        assertThat(result.isError(), is(true));
        assertThat(result.content().get(0).text(), containsString("title is required"));
    }

    @Test
    void givenTags__thenNoteHasTags() throws Exception {
        tool.apply(paramsWithArgs("title", "T", "content", "C", "tags", List.of("work", "meeting")));
        var notes = service.list("work");
        assertThat(notes, hasSize(1));
    }
}
```

**Helper to build params in tests** (put in test base class or utility):

```java
// build ObjectValue from pairs of key-value
static ObjectValue objectValue(Object... kvPairs) {
    var b = ObjectValue.builder();
    for (int i = 0; i < kvPairs.length; i += 2) {
        String k = (String) kvPairs[i];
        Object v = kvPairs[i + 1];
        if (v instanceof String s) b.property(k, pv -> pv.stringValue(s));
        else if (v instanceof List<?> list) {
            // multipleString
            var arr = ((List<String>) list).toArray(new String[0]);
            b.property(k, PropertyValue.multipleString(arr));
        }
    }
    return b.build();
}
static CallToolParams paramsWithArgs(Object... kvPairs) {
    return CallToolParams.builder().name("tool").arguments(objectValue(kvPairs)).build();
}
```

- [ ] **Step 8: Run tool tests**

```bash
mvn test -pl poom-mcp/poom-mcp-demo -Dtest="*ToolTest"
```

```bash
git add poom-mcp/poom-mcp-demo/src
git commit -m "feat: poom-mcp-demo tool handlers + tests (6 tools)"
```

---

## Task 6 — Resource handlers

**Files:** `resources/NoteResourceHandler.java`, `resources/TaggedNotesResourceHandler.java`

Each resource handler implements `Function<ReadResourceParams, ReadResourceResult>`.

- [ ] **Step 1: `NoteResourceHandler.java`**

Handles `note://{id}` — reads the note and renders it as markdown.

```java
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
                        var tags = n.opt().tags().isAbsent() ? "" : String.join(", ", n.tags());
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
                                    .uri(uri).mimeType("text/plain").text("Note not found: " + id)
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
```

- [ ] **Step 2: `TaggedNotesResourceHandler.java`**

Handles `notes://tagged/{tag}` — lists all notes with that tag.

```java
// extract tag from uri: uri.substring("notes://tagged/".length())
// call noteService.list(tag)
// format as text/plain: "id — title\n" per note, or "No notes tagged '{tag}'." if empty
```

- [ ] **Step 3: Write resource handler tests**

```java
class NoteResourceHandlerTest {
    @Test void givenExistingNote__whenRead__thenMarkdownReturned();
    @Test void givenUnknownId__whenRead__thenNotFoundText();
}

class TaggedNotesResourceHandlerTest {
    @Test void givenTaggedNotes__whenRead__thenListReturned();
    @Test void givenNoNotesWithTag__whenRead__thenEmptyMessage();
}
```

- [ ] **Step 4: Run + commit**

```bash
mvn test -pl poom-mcp/poom-mcp-demo -Dtest="*ResourceHandlerTest"
git add poom-mcp/poom-mcp-demo/src
git commit -m "feat: poom-mcp-demo resource handlers + tests"
```

---

## Task 7 — Prompt handlers

**Files:** `prompts/SummarizeNotePromptHandler.java`, `prompts/CompareNotesPromptHandler.java`

Each handler implements `Function<GetPromptParams, GetPromptResult>`. Prompts return `PromptMessage` objects with role `user` and content as an `ObjectValue` with `{"type":"text","text":"..."}`.

- [ ] **Step 1: `SummarizeNotePromptHandler.java`**

Fetches the note by `note_id`, inlines its content into a prompt asking the model to summarize it.

```java
package org.codingmatters.poom.mcp.demo.prompts;

import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.GetPromptParams;
import org.codingmatters.poom.mcp.types.GetPromptResult;
import org.codingmatters.poom.mcp.types.PromptMessage;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

public class SummarizeNotePromptHandler implements Function<GetPromptParams, GetPromptResult> {

    private final NoteService noteService;

    public SummarizeNotePromptHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public GetPromptResult apply(GetPromptParams params) {
        String noteId = params.arguments() != null && params.arguments().property("note_id") != null
                ? params.arguments().property("note_id").single().stringValue()
                : null;
        if (noteId == null) {
            return error("note_id argument is required");
        }
        try {
            return noteService.get(noteId)
                    .map(e -> {
                        var n = e.value();
                        var promptText = "Please summarize the following note in 2-3 sentences:\n\n"
                                + "Title: " + n.title() + "\n\n"
                                + n.content();
                        return GetPromptResult.builder()
                                .description("Summarize note '" + n.title() + "'")
                                .messages(userMessage(promptText))
                                .build();
                    })
                    .orElseGet(() -> error("Note not found: " + noteId));
        } catch (Exception e) {
            return error("Error: " + e.getMessage());
        }
    }

    private static GetPromptResult error(String msg) {
        return GetPromptResult.builder()
                .description("Error")
                .messages(userMessage(msg))
                .build();
    }

    static PromptMessage userMessage(String text) {
        return PromptMessage.builder()
                .role("user")
                .content(ObjectValue.builder()
                        .property("type", v -> v.stringValue("text"))
                        .property("text", v -> v.stringValue(text))
                        .build())
                .build();
    }
}
```

- [ ] **Step 2: `CompareNotesPromptHandler.java`**

Fetches `note_id_1` and `note_id_2`, generates a comparison prompt.

```java
// extract note_id_1, note_id_2 from params.arguments()
// fetch both notes → error if either not found
// generate: "Compare and contrast these two notes:\n\n## Note 1: {title1}\n{content1}\n\n## Note 2: {title2}\n{content2}"
```

- [ ] **Step 3: Write prompt handler tests**

```java
class SummarizeNotePromptHandlerTest {
    @Test void givenExistingNote__thenPromptContainsContent();
    @Test void givenMissingNoteId__thenErrorDescription();
    @Test void givenUnknownNoteId__thenErrorDescription();
}
class CompareNotesPromptHandlerTest {
    @Test void givenTwoNotes__thenPromptContainsBoth();
    @Test void givenOneNotFound__thenErrorDescription();
}
```

- [ ] **Step 4: Run + commit**

```bash
mvn test -pl poom-mcp/poom-mcp-demo -Dtest="*PromptHandlerTest"
git add poom-mcp/poom-mcp-demo/src
git commit -m "feat: poom-mcp-demo prompt handlers + tests"
```

---

## Task 8 — `NoteAssistantDescriptor` + `NoteAssistantServer`

**Files:** `NoteAssistantDescriptor.java`, `NoteAssistantServer.java`

- [ ] **Step 1: `NoteAssistantDescriptor.java`**

Assembles the full `McpServerDescriptor` from the handlers. This is the single place where tools, resources, and prompts are wired.

```java
package org.codingmatters.poom.mcp.demo;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.mcp.McpPromptDescriptor;
import org.codingmatters.poom.mcp.McpResourceDescriptor;
import org.codingmatters.poom.mcp.McpServerDescriptor;
import org.codingmatters.poom.mcp.McpToolDescriptor;
import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.prompts.CompareNotesPromptHandler;
import org.codingmatters.poom.mcp.demo.prompts.SummarizeNotePromptHandler;
import org.codingmatters.poom.mcp.demo.resources.NoteResourceHandler;
import org.codingmatters.poom.mcp.demo.resources.TaggedNotesResourceHandler;
import org.codingmatters.poom.mcp.demo.tools.*;
import org.codingmatters.value.objects.values.ObjectValue;

public class NoteAssistantDescriptor {

    public static McpServerDescriptor build(NoteService noteService) {
        return McpServerDescriptor.builder()
                .name("note-assistant")
                .version("1.0.0")
                // --- tools ---
                .tools(
                    createNoteTool(noteService),
                    getNoteTool(noteService),
                    updateNoteTool(noteService),
                    deleteNoteTool(noteService),
                    listNotesTool(noteService),
                    searchNotesTool(noteService)
                )
                // --- resources ---
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
                // --- prompts ---
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
                    "title",   "string", "Title of the note (required)",
                    "content", "string", "Body of the note (required)",
                    "tags",    "array",  "Optional list of string tags"
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
                    "id",      "string", "Note id (required)",
                    "title",   "string", "New title (optional)",
                    "content", "string", "New content (optional)",
                    "tags",    "array",  "New tag list (optional, replaces all existing tags)"
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
                .description("Lists all notes, optionally filtered by tag. Returns id, title, and tags for each note.")
                .inputSchema(schema("tag", "string", "Optional tag to filter by"))
                .handler(new ListNotesTool(s))
                .build();
    }

    private static McpToolDescriptor searchNotesTool(NoteService s) {
        return McpToolDescriptor.builder()
                .name("search_notes")
                .description(
                    "Full-text search across note titles and content. " +
                    "May take several seconds for large note collections — result is pushed asynchronously via SSE.")
                .inputSchema(schema("query", "string", "Search terms (required)"))
                .handler(new SearchNotesTool(s))
                .build();
    }

    /** Builds a minimal JSON Schema ObjectValue for a flat list of (name, type, description) triples. */
    private static ObjectValue schema(String... tripleNameTypeDesc) {
        var props = ObjectValue.builder();
        for (int i = 0; i < tripleNameTypeDesc.length; i += 3) {
            String name = tripleNameTypeDesc[i];
            String type = tripleNameTypeDesc[i + 1];
            String desc = tripleNameTypeDesc[i + 2];
            props.property(name, v -> v.objectValue(ObjectValue.builder()
                    .property("type", t -> t.stringValue(type))
                    .property("description", d -> d.stringValue(desc))
                    .build()));
        }
        return ObjectValue.builder()
                .property("type", v -> v.stringValue("object"))
                .property("properties", v -> v.objectValue(props.build()))
                .build();
    }
}
```

- [ ] **Step 2: `NoteAssistantServer.java`**

Runnable main class. Reads `SERVICE_HOST`/`SERVICE_PORT` from env (or defaults to `0.0.0.0:8080`) and starts an Undertow server.

```java
package org.codingmatters.poom.mcp.demo;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.processor.McpProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.util.concurrent.Executors;

public class NoteAssistantServer {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(NoteAssistantServer.class);

    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("SERVICE_HOST", "0.0.0.0");
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVICE_PORT", "8080"));

        NoteService noteService = new NoteService(NoteRepository.create());
        McpProcessor processor = new McpProcessor(
                "/mcp",
                new JsonFactory(),
                NoteAssistantDescriptor.build(noteService),
                Executors.newFixedThreadPool(4)
        );

        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(new CdmHttpUndertowHandler(processor))
                .build();
        server.start();
        log.info("Note Assistant MCP server started at http://{}:{}/mcp", host, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop();
        }));
    }
}
```

- [ ] **Step 3: Build**

```bash
mvn install -pl poom-mcp/poom-mcp-demo -am -DskipTests
```

Expected: `BUILD SUCCESS`.

```bash
git add poom-mcp/poom-mcp-demo/src
git commit -m "feat: NoteAssistantDescriptor + NoteAssistantServer"
```

---

## Task 9 — Integration tests

**File:** `NoteAssistantIntegrationTest.java`

Tests the full MCP stack end-to-end: `McpProcessor` with the complete `NoteAssistantDescriptor`, driven by `TestRequestDeleguate`/`TestResponseDeleguate`. No real HTTP server needed.

```java
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
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class NoteAssistantIntegrationTest {

    private static final String URL = "http://test/mcp";
    private final JsonFactory jsonFactory = new JsonFactory();
    private ExecutorService pool;
    private McpProcessor processor;
    private NoteService noteService;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        pool = Executors.newFixedThreadPool(4);
        noteService = new NoteService(NoteRepository.create());
        processor = new McpProcessor("/mcp", jsonFactory,
                NoteAssistantDescriptor.build(noteService), pool, 500);
        sessionId = initialize();
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    // --- Session lifecycle ---

    @Test
    void initialize_returnsSessionId() {
        assertThat(sessionId, notNullValue());
        assertThat(sessionId, not(emptyString()));
    }

    @Test
    void delete_terminatesSession() throws Exception {
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"delete\",\"id\":\"x\"}");
        delete();
        // subsequent call should fail
        var resp2 = post("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"y\"}");
        assertThat(resp2.status(), is(400));
    }

    // --- tools/list ---

    @Test
    void toolsList_returnsAll6Tools() throws Exception {
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"1\"}");
        assertThat(resp.status(), is(200));
        String body = new String(resp.payload());
        assertThat(body, containsString("create_note"));
        assertThat(body, containsString("get_note"));
        assertThat(body, containsString("update_note"));
        assertThat(body, containsString("delete_note"));
        assertThat(body, containsString("list_notes"));
        assertThat(body, containsString("search_notes"));
    }

    // --- tools/call — create + get ---

    @Test
    void createNote_thenGetNote_roundtrip() throws Exception {
        var create = callTool("create_note", "{\"name\":\"create_note\",\"arguments\":{\"title\":\"Test\",\"content\":\"Hello world\"}}");
        assertThat(create.status(), is(200));
        String body = new String(create.payload());
        assertThat(body, containsString("Note created with id:"));

        String id = extractId(body);
        var get = callTool("get_note", "{\"name\":\"get_note\",\"arguments\":{\"id\":\"" + id + "\"}}");
        String getBody = new String(get.payload());
        assertThat(getBody, containsString("Hello world"));
        assertThat(getBody, containsString("Test"));
    }

    @Test
    void updateNote_changesContent() throws Exception {
        String id = createNoteAndGetId("Original", "Old content");
        callTool("update_note", "{\"name\":\"update_note\",\"arguments\":{\"id\":\"" + id + "\",\"content\":\"New content\"}}");
        var get = callTool("get_note", "{\"name\":\"get_note\",\"arguments\":{\"id\":\"" + id + "\"}}");
        assertThat(new String(get.payload()), containsString("New content"));
        assertThat(new String(get.payload()), containsString("Original")); // title unchanged
    }

    @Test
    void deleteNote_thenGetReturnsError() throws Exception {
        String id = createNoteAndGetId("T", "C");
        callTool("delete_note", "{\"name\":\"delete_note\",\"arguments\":{\"id\":\"" + id + "\"}}");
        var get = callTool("get_note", "{\"name\":\"get_note\",\"arguments\":{\"id\":\"" + id + "\"}}");
        assertThat(new String(get.payload()), containsString("isError\":true"));
    }

    @Test
    void listNotes_withTagFilter() throws Exception {
        createNoteWithTags("Work note", "work content", "work");
        createNoteWithTags("Personal note", "personal content", "personal");
        var list = callTool("list_notes", "{\"name\":\"list_notes\",\"arguments\":{\"tag\":\"work\"}}");
        String body = new String(list.payload());
        assertThat(body, containsString("Work note"));
        assertThat(body, not(containsString("Personal note")));
    }

    // --- Async tool call (search_notes) ---

    @Test
    void searchNotes_triggers202AndSseResult() throws Exception {
        createNoteAndGetId("Meeting notes", "agenda for the quarterly review");

        // open SSE channel
        TestResponseDeleguate sseResp = new TestResponseDeleguate();
        Thread sseThread = new Thread(() -> {
            try {
                processor.process(
                        TestRequestDeleguate.request(RequestDelegate.Method.GET, URL)
                                .addHeader("Accept", "text/event-stream")
                                .addHeader("Mcp-Session-Id", sessionId).build(),
                        sseResp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        sseThread.setDaemon(true);
        sseThread.start();
        Eventually.timeout(2000).assertThat(() -> sseResp.sseChannel(), notNullValue());
        sseResp.sseChannel().poll(2000); // drain ping

        // call slow tool — expect 202
        var toolResp = callTool("search_notes",
                "{\"name\":\"search_notes\",\"arguments\":{\"query\":\"quarterly\"}}");
        assertThat(toolResp.status(), is(202));

        // result arrives on SSE within 3s (600ms handler + margin)
        TestSseChannel.SseEvent event = sseResp.sseChannel().poll(3000);
        assertThat(event, notNullValue());
        assertThat(event.event(), is("message"));
        assertThat(event.data(), containsString("quarterly"));

        sseResp.sseChannel().close();
    }

    // --- resources/list + resources/read ---

    @Test
    void resourcesList_returnsBothResources() throws Exception {
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"resources/list\",\"id\":\"r1\"}");
        String body = new String(resp.payload());
        assertThat(body, containsString("note://"));
        assertThat(body, containsString("notes://tagged/"));
    }

    @Test
    void resourcesRead_noteUri_returnsMarkdown() throws Exception {
        String id = createNoteAndGetId("Resource test", "Some content");
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\","
                + "\"params\":{\"uri\":\"note://" + id + "\"},\"id\":\"r2\"}");
        String body = new String(resp.payload());
        assertThat(body, containsString("Resource test"));
        assertThat(body, containsString("Some content"));
    }

    @Test
    void resourcesRead_taggedUri_returnsNoteList() throws Exception {
        createNoteWithTags("Tagged note", "content", "demo");
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\","
                + "\"params\":{\"uri\":\"notes://tagged/demo\"},\"id\":\"r3\"}");
        assertThat(new String(resp.payload()), containsString("Tagged note"));
    }

    // --- prompts/list + prompts/get ---

    @Test
    void promptsList_returnsBothPrompts() throws Exception {
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"prompts/list\",\"id\":\"p1\"}");
        String body = new String(resp.payload());
        assertThat(body, containsString("summarize_note"));
        assertThat(body, containsString("compare_notes"));
    }

    @Test
    void promptsGet_summarize_returnsPromptWithContent() throws Exception {
        String id = createNoteAndGetId("Annual report", "Revenue increased 20%");
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"prompts/get\","
                + "\"params\":{\"name\":\"summarize_note\",\"arguments\":{\"note_id\":\"" + id + "\"}},\"id\":\"p2\"}");
        String body = new String(resp.payload());
        assertThat(body, containsString("Annual report"));
        assertThat(body, containsString("Revenue increased"));
    }

    @Test
    void promptsGet_compare_containsBothNotes() throws Exception {
        String id1 = createNoteAndGetId("Note A", "Content of A");
        String id2 = createNoteAndGetId("Note B", "Content of B");
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"prompts/get\","
                + "\"params\":{\"name\":\"compare_notes\",\"arguments\":"
                + "{\"note_id_1\":\"" + id1 + "\",\"note_id_2\":\"" + id2 + "\"}},\"id\":\"p3\"}");
        String body = new String(resp.payload());
        assertThat(body, containsString("Content of A"));
        assertThat(body, containsString("Content of B"));
    }

    // --- error cases ---

    @Test
    void callTool_unknownTool_returnsMethodNotFound() throws Exception {
        var resp = callTool("no_such_tool", "{\"name\":\"no_such_tool\",\"arguments\":{}}");
        assertThat(new String(resp.payload()), containsString("-32601"));
    }

    @Test
    void resourcesRead_unknownUri_returnsMethodNotFound() throws Exception {
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\","
                + "\"params\":{\"uri\":\"unknown://xyz\"},\"id\":\"e1\"}");
        assertThat(new String(resp.payload()), containsString("-32601"));
    }

    @Test
    void promptsGet_unknownPrompt_returnsMethodNotFound() throws Exception {
        var resp = post("{\"jsonrpc\":\"2.0\",\"method\":\"prompts/get\","
                + "\"params\":{\"name\":\"no_such_prompt\"},\"id\":\"e2\"}");
        assertThat(new String(resp.payload()), containsString("-32601"));
    }

    // --- helpers ---

    private String initialize() throws Exception {
        var resp = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .payload(body("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{},\"id\":\"init\"}"))
                        .build(),
                resp);
        return resp.headers().get("Mcp-Session-Id")[0];
    }

    private TestResponseDeleguate post(String json) throws Exception {
        var resp = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.POST, URL)
                        .contentType("application/json")
                        .addHeader("Mcp-Session-Id", sessionId)
                        .payload(body(json))
                        .build(),
                resp);
        return resp;
    }

    private void delete() throws Exception {
        var resp = new TestResponseDeleguate();
        processor.process(
                TestRequestDeleguate.request(RequestDelegate.Method.DELETE, URL)
                        .addHeader("Mcp-Session-Id", sessionId)
                        .build(),
                resp);
    }

    private TestResponseDeleguate callTool(String name, String paramsJson) throws Exception {
        return post("{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":" + paramsJson + ",\"id\":\"" + name + "\"}");
    }

    private String createNoteAndGetId(String title, String content) throws Exception {
        var resp = callTool("create_note",
                "{\"name\":\"create_note\",\"arguments\":{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}}");
        return extractId(new String(resp.payload()));
    }

    private void createNoteWithTags(String title, String content, String tag) throws Exception {
        callTool("create_note",
                "{\"name\":\"create_note\",\"arguments\":{\"title\":\"" + title
                        + "\",\"content\":\"" + content + "\",\"tags\":[\"" + tag + "\"]}}");
    }

    private String extractId(String body) {
        // extracts the UUID from "Note created with id: <uuid>"
        int idx = body.indexOf("id: ");
        if (idx < 0) throw new IllegalStateException("No id in: " + body);
        int start = idx + 4;
        int end = body.indexOf('"', start);
        return end < 0 ? body.substring(start).trim() : body.substring(start, end).trim();
    }

    private ByteArrayInputStream body(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 1: Run all tests**

```bash
mvn test -pl poom-mcp/poom-mcp-demo
```

Expected: all tests pass.

- [ ] **Step 2: Run full reactor to catch regressions**

```bash
mvn install
```

Expected: `BUILD SUCCESS`.

```bash
git add poom-mcp/poom-mcp-demo/src
git commit -m "test: NoteAssistantIntegrationTest — full MCP session lifecycle"
```

---

## Task 10 — Documentation

- [ ] **Step 1: Create `poom-mcp-demo/README.md`**

Content to include:
1. What the Note Assistant demonstrates (table: each MCP feature → which tool/resource/prompt covers it)
2. Running the server
3. Example curl session (initialize → tools/list → create_note → search_notes + SSE → delete)
4. Package structure and extension guide (how to add a new tool in 3 steps)

- [ ] **Step 2: Update `poom-mcp/README.md` Limitations table**

Remove the `❌ not yet dispatched` entries for `resources/read` and `prompts/get` — they are now implemented.

- [ ] **Step 3: Commit**

```bash
git add poom-mcp/poom-mcp-demo/README.md poom-mcp/README.md
git commit -m "docs: poom-mcp-demo README + update poom-mcp limitations"
```

---

## Summary

| Task | Scope | Key output |
|------|-------|-----------|
| 1 | poom-mcp-processor | `resources/read` dispatch |
| 2 | poom-mcp-processor | `prompts/get` dispatch |
| 3 | poom-mcp-demo | Module + `Note` value object |
| 4 | poom-mcp-demo | `NoteRepository` + `NoteService` + `NoteServiceTest` |
| 5 | poom-mcp-demo | 6 tool handlers + 6 × ToolTest |
| 6 | poom-mcp-demo | 2 resource handlers + 2 × ResourceHandlerTest |
| 7 | poom-mcp-demo | 2 prompt handlers + 2 × PromptHandlerTest |
| 8 | poom-mcp-demo | `NoteAssistantDescriptor` + `NoteAssistantServer` |
| 9 | poom-mcp-demo | `NoteAssistantIntegrationTest` (full session, all primitives, async) |
| 10 | docs | `poom-mcp-demo/README.md` + update `poom-mcp/README.md` |

# Design : Support MCP dans poom-services

**Date** : 2026-06-02
**Transport ciblé** : MCP Streamable HTTP (spec 2025-03-26)
**Repos impactés** : `codingmatters-rest`, `poom-services`

---

## Contexte

poom-services dispose déjà d'une couche JSON-RPC (`poom-json-rpc`) qui couvre le transport HTTP POST synchrone. Le protocole MCP (Model Context Protocol) d'Anthropic repose sur JSON-RPC 2.0 mais nécessite en plus :

1. Un mécanisme **SSE (Server-Sent Events)** pour les notifications serveur→client
2. Un **transport Streamable HTTP** : un seul endpoint gérant GET (canal SSE persistant) et POST (réponse JSON synchrone ou SSE asynchrone)

Le transport HTTP+SSE legacy (pré mars 2025) est déprécié. Streamable HTTP est supporté par la Claude API (depuis fév. 2026), les SDK TypeScript/Python MCP, et tous les clients modernes. Claude Desktop reste SSE-only mais peut être bridgé via `mcp-remote` ; c'est un problème transitoire.

---

## Architecture générale

Deux repos touchés avec le même nom de feature branch via `flexio-flow` :

```
codingmatters-rest/
  cdm-rest-api/          ← SseChannel + SseProcessor + ResponseDelegate.openSse()
  cdm-rest-undertow/     ← UndertowSseChannel (ServerSentEventConnection)
  cdm-rest-netty/        ← NettySseChannel (chunked transfer)

poom-services/
  poom-mcp/
    poom-mcp-types/      ← types MCP (value objects générés)
    poom-mcp-descriptors/← McpServerDescriptor, McpToolDescriptor, etc.
    poom-mcp-processor/  ← McpProcessor (Streamable HTTP, routing GET/POST)
```

La logique de routing MCP reste entièrement dans `poom-services`. `codingmatters-rest` fournit uniquement les primitives SSE — aucune connaissance du protocole MCP.

---

## Partie 1 — Primitives SSE dans `codingmatters-rest`

### `cdm-rest-api` — nouvelles interfaces

**`SseChannel`** : abstraction d'une connexion SSE ouverte côté serveur.

```java
public interface SseChannel extends AutoCloseable {
    SseChannel send(String event, String data) throws IOException;
    SseChannel send(String data) throws IOException;
    SseChannel comment(String comment) throws IOException;
    SseChannel id(String id) throws IOException;
    SseChannel retry(long milliseconds) throws IOException;
    void close();
    SseChannel onClose(Runnable handler);
    boolean isOpen();
}
```

**`SseProcessor`** : pendant de `Processor` pour les endpoints purement SSE. Utile en dehors de MCP (notifications, live-queries).

```java
public interface SseProcessor {
    void process(RequestDelegate request, SseChannel channel) throws IOException;
}
```

**Extension de `ResponseDelegate`** :

```java
SseChannel openSse() throws IOException;
```

Quand `openSse()` est appelé, le backend envoie immédiatement les headers SSE (`Content-Type: text/event-stream`, `Cache-Control: no-cache`, `Connection: keep-alive`) et retourne un `SseChannel` actif. Le thread du `Processor` reste bloqué tant que le channel est ouvert — même modèle de concurrence que l'existant.

**Extension de l'interface `Api`** :

```java
default SseProcessor sseProcessor() { return null; }
```

Les backends routent les `GET` avec `Accept: text/event-stream` vers `sseProcessor()` si non null, sinon fallback sur `processor()`.

### Implémentations backend

| Backend | `SseChannel` | `openSse()` |
|---|---|---|
| Undertow | `UndertowSseChannel` via `ServerSentEventConnection` | `ServerSentEventHandler` actif sur le path |
| Netty | `NettySseChannel` via chunked transfer encoding | `HttpChunkedInput` + `ChunkedWriteHandler` |

---

## Partie 2 — Module `poom-mcp` dans `poom-services`

### `poom-mcp-types`

Types MCP générés via `cdm-value-objects-maven-plugin` depuis un `.yaml` :

- `McpRequest` (`jsonrpc`, `method`, `params: ObjectValue`, `id`)
- `McpResponse` (`jsonrpc`, `result: ObjectValue`, `error: McpError`, `id`)
- `McpError` (`code`, `message`, `data: ObjectValue`)
- `McpNotification` (comme `McpRequest` sans `id`)
- `InitializeParams` / `InitializeResult` / `ServerInfo` / `ClientInfo`
- `CallToolParams` / `CallToolResult`
- `ListToolsResult` / `Tool` / `ToolInputSchema`
- `ListResourcesResult` / `Resource` / `ReadResourceParams` / `ReadResourceResult`
- `ListPromptsResult` / `Prompt` / `GetPromptParams` / `GetPromptResult`

### `poom-mcp-descriptors`

Descripteurs Java décrivant un serveur MCP côté implémenteur — même pattern que `RpcEntryPointDescriptor` dans `poom-json-rpc` :

```java
McpToolDescriptor        // name, description, inputSchema, handler: Function<CallToolParams, CallToolResult>
McpResourceDescriptor    // uri, name, mimeType, handler: Function<ReadResourceParams, ReadResourceResult>
McpPromptDescriptor      // name, description, arguments, handler: Function<GetPromptParams, GetPromptResult>
McpServerDescriptor      // name, version, tools[], resources[], prompts[]
```

Le handler de chaque outil est un `Function<P, R>` — même pattern que `RpcMethodHandler`.

### `poom-mcp-processor`

**`McpProcessor`** implémente `Processor`. Il reçoit un `McpServerDescriptor` et un `JsonFactory` à la construction :

```java
new McpProcessor("/mcp", jsonFactory, mcpServerDescriptor)
```

Routing interne :

| Méthode HTTP | Condition | Action |
|---|---|---|
| `POST` | sans `Mcp-Session-Id` + method `initialize` | crée session, retourne `InitializeResult` avec header `Mcp-Session-Id` |
| `POST` | avec `Mcp-Session-Id` | dispatch JSON-RPC → handler, réponse sync ou async SSE |
| `POST` | sans `Mcp-Session-Id`, method ≠ `initialize` | `400 Bad Request` |
| `GET` | `Accept: text/event-stream` + `Mcp-Session-Id` valide | `response.openSse()`, enregistre canal, démarre ping loop |
| `DELETE` | `Mcp-Session-Id` | ferme `SseChannel`, supprime session |

**Session management** : map `sessionId → SseChannel` thread-safe. Le `sessionId` est un UUID généré à `initialize`. Les notifications serveur→client passent par lookup dans cette map.

**Règle sync vs async** : le processor répond en JSON synchrone si le handler retourne avant un timeout configurable (défaut : 500ms). Passé ce délai, il envoie `202 Accepted` et pousse le résultat sur le canal SSE de la session. Si aucun canal SSE n'est ouvert pour la session, il ouvre un SSE inline sur la réponse POST.

---

## Flux du protocole

Voir diagramme PlantUML : [`docs/mcp/streamable-http-flow.puml`](../../mcp/streamable-http-flow.puml)

```
Client                          McpProcessor
  │                                  │
  │── POST /mcp (initialize) ────────►│ crée session, génère sessionId
  │◄── 200 JSON + Mcp-Session-Id ────│
  │                                  │
  │── GET /mcp (SSE) ────────────────►│ response.openSse()
  │◄── 200 text/event-stream ────────│ canal enregistré
  │◄── event: ping (périodique) ─────│
  │                                  │
  │── POST /mcp (tools/call court) ──►│ handler rapide
  │◄── 200 JSON (résultat) ──────────│ réponse synchrone
  │                                  │
  │── POST /mcp (tools/call long) ───►│ handler lent
  │◄── 202 Accepted ─────────────────│
  │◄── event: message (résultat) ────│ poussé via canal SSE
  │                                  │
  │── DELETE /mcp ───────────────────►│ ferme canal, supprime session
  │◄── 200 OK ───────────────────────│
```

---

## Gestion des erreurs

| Cas | Réponse |
|---|---|
| `POST` sans session sur méthode ≠ `initialize` | `400 Bad Request` |
| `Mcp-Session-Id` inconnu | `404 Not Found` |
| Handler lève une exception | `McpError` code `-32603 Internal Error` dans `McpResponse` |
| Parse error JSON-RPC | `McpError` code `-32700 Parse Error` |
| Method inconnue | `McpError` code `-32601 Method Not Found` |

---

## Tests

- `poom-mcp-types` : tests de sérialisation/désérialisation JSON pour chaque type
- `poom-mcp-processor` : tests unitaires de `McpProcessor` avec `poom-services-test-runtime` (backend de test)
- `cdm-rest-api` : tests d'acceptance SSE dans `cdm-rest-server-acceptance` couvrant les deux backends

---

## Contraintes de mise en œuvre

- Feature branch créée via `flexio-flow` avec le **même nom de feature** dans `codingmatters-rest` et `poom-services`
- Génération de sources : `poom-mcp-types` via `cdm-value-objects-maven-plugin`, pas de RAML (pas d'endpoint REST généré, le processor est hand-written)
- Versions : propagées automatiquement via le BOM du root pom — pas de bump manuel

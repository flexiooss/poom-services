# poom-mcp-demo — Note Assistant MCP server

Implémentation de référence d'un serveur MCP avec `poom-mcp`. Le Note Assistant est un serveur de prise de notes en mémoire qui illustre tous les primitives MCP : outils synchrones et asynchrones, ressources, prompts et cycle de vie de session.

---

## Fonctionnel

### Ce que fait le Note Assistant

Le Note Assistant permet à un modèle d'IA de gérer une collection de notes textuelles. Chaque note a un **titre**, un **contenu** libre et des **tags** optionnels. Le serveur expose ces capacités via le protocole MCP :

#### Outils disponibles

| Outil | Description | Comportement |
|-------|-------------|--------------|
| `create_note` | Crée une note avec titre, contenu et tags optionnels | Synchrone — répond immédiatement |
| `get_note` | Récupère une note par son id | Synchrone |
| `update_note` | Mise à jour partielle — seuls les champs fournis changent | Synchrone |
| `delete_note` | Supprime définitivement une note | Synchrone |
| `list_notes` | Liste toutes les notes, avec filtre optionnel par tag | Synchrone |
| `search_notes` | Recherche full-text dans titres et contenus | **Asynchrone** — retourne 202, résultat poussé via SSE |

`search_notes` simule une opération lente (600 ms) pour illustrer le chemin asynchrone : le serveur répond `202 Accepted` immédiatement et pousse le résultat sur le canal SSE de la session quand la recherche se termine.

#### Ressources

| URI | Type | Description |
|-----|------|-------------|
| `note://{id}` | `text/markdown` | Contenu complet d'une note, formaté en markdown |
| `notes://tagged/{tag}` | `text/plain` | Liste des notes portant un tag donné |

#### Prompts

| Nom | Arguments | Génère |
|-----|-----------|--------|
| `summarize_note` | `note_id` | Un prompt demandant au modèle de résumer la note |
| `compare_notes` | `note_id_1`, `note_id_2` | Un prompt demandant au modèle de comparer deux notes |

---

## Lancer le serveur

### Prérequis

Java 25, Maven, accès au dépôt Maven de Flexio (`mvn.ci.flexio.io`).

### Build

```bash
# Depuis la racine du dépôt
mvn install -pl poom-mcp/poom-mcp-demo -am -DskipTests
```

### Démarrage

```bash
SERVICE_HOST=0.0.0.0 SERVICE_PORT=8080 java \
    -cp poom-mcp/poom-mcp-demo/target/dependency/*:poom-mcp/poom-mcp-demo/target/poom-mcp-demo-*.jar \
    org.codingmatters.poom.mcp.demo.NoteAssistantServer
```

| Variable | Défaut | Description |
|----------|--------|-------------|
| `SERVICE_HOST` | `0.0.0.0` | Interface d'écoute |
| `SERVICE_PORT` | `8080` | Port HTTP |

Le serveur expose un unique endpoint : `http://host:port/mcp`.

Le chemin `/mcp` est configurable en premier argument du constructeur `McpProcessor` dans `NoteAssistantServer` — il n'est pas validé par le processor lui-même, donc n'importe quel chemin URL fonctionne (ex. `/notes/assistant/mcp`).

### Vérification

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"initialize","params":{},"id":"1"}'
```

Réponse attendue :
```json
{
  "jsonrpc": "2.0",
  "result": {
    "serverInfo": { "name": "note-assistant", "version": "1.0.0" },
    "protocolVersion": "2024-11-05",
    "capabilities": {}
  },
  "id": "1"
}
```

---

## Exemple d'utilisation depuis Claude

Cette section illustre une session réelle avec le Note Assistant connecté à Claude via le protocole MCP. Le serveur tourne à `http://127.0.0.1:8080/notes/assistant/mcp`.

### Initialisation de la session

```
Utilisateur : connecte-toi au serveur MCP http://127.0.0.1:8080/notes/assistant/mcp
```

Claude initialise une session MCP (POST `initialize`) et reçoit un `Mcp-Session-Id`. Il découvre automatiquement les 6 outils, 2 ressources et 2 prompts disponibles.

---

### Création de notes en masse

```
Utilisateur : crée 18 notes avec du contenu tiré du cycle de Fondation d'Isaac Asimov
```

Claude appelle `create_note` dix-huit fois, en générant un contenu riche pour chaque note. Exemples de notes créées :

- **La Psychohistoire** — *tags : psychohistoire, concept, fondation, seldon*
  > La psychohistoire est la science imaginée par Hari Seldon qui combine mathématiques, histoire et sociologie pour prédire le comportement des grandes masses humaines...

- **Le Mulet — La Variable Imprévue** — *tags : le mulet, personnage, mutant, antagoniste, seconde fondation*
  > Le Mulet est le grand antagoniste de la deuxième partie du cycle, un mutant doté du pouvoir de modifier les émotions humaines...

- **R. Daneel Olivaw — Le Robot Éternel** — *tags : daneel olivaw, robot, personnage, loi zero, fondation secrète*
  > R. Daneel Olivaw est un robot humanoïde vieux de vingt mille ans, personnage clé des romans de robots d'Asimov et révélé comme l'architecte secret du Plan Seldon...

18 appels à `create_note`, tous synchrones, tous acquittés avec un id unique.

---

### Recherche full-text (chemin asynchrone)

```
Utilisateur : montre-moi toutes les notes citant Hari Seldon
```

Claude appelle `search_notes` avec `{"query": "Seldon"}`. Le serveur retourne **202 Accepted** immédiatement (la recherche prend plus de 500 ms). Claude attend le résultat sur le canal SSE — il arrive ~600 ms plus tard sous la forme d'un event `message` :

```
event: message
data: {"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"67485ac7: La Psychohistoire — La psychohistoire est la science imaginée par Hari Seldon...\ncc0952c3: Hari Seldon — Le Fondateur — Hari Seldon est le mathématicien trantorien...\n..."}],"isError":false},"id":"s1"}
```

14 notes sur 18 mentionnent "Seldon". Claude les présente à l'utilisateur sous forme de tableau.

---

### Lecture d'une ressource

```
Utilisateur : montre-moi le contenu de la note sur l'Encyclopédie Galactique
```

Claude appelle `resources/read` avec l'URI `note://237955e5-ee92-4d58-9b43-f6ec55c89216` (l'id obtenu lors de la création). Le serveur retourne le contenu de la note formaté en markdown :

```markdown
# L'Encyclopédie Galactique

*Tags: encyclopédie, prétexte, fondation, encyclopédistes*

L'Encyclopédie Galactique est officiellement la raison d'être de la Fondation de Terminus :
compiler tout le savoir humain en un ouvrage de référence pour préserver la civilisation
pendant la barbarie à venir. En réalité Seldon ne croit pas à ce projet — il sait qu'aucun
livre ne survivra à trente mille ans de chaos...
```

---

### Utilisation d'un prompt

```
Utilisateur : génère un prompt pour résumer la note sur la Psychohistoire
```

Claude appelle `prompts/get` avec `{"name": "summarize_note", "arguments": {"note_id": "67485ac7-..."}}`. Le serveur récupère la note, injecte son contenu dans un template et retourne un message `PromptMessage` prêt à être envoyé au modèle :

```
Please summarize the following note in 2-3 sentences:

Title: La Psychohistoire

La psychohistoire est la science imaginée par Hari Seldon qui combine mathématiques,
histoire et sociologie pour prédire le comportement des grandes masses humaines.
Elle ne peut s'appliquer qu'à des populations de l'ordre de la quadrillions d'individus...
```

Claude envoie ce prompt au modèle et présente le résumé à l'utilisateur.

---

## Implémentation technique

### Architecture générale

```
NoteAssistantServer          — point d'entrée, configure Undertow + McpProcessor
NoteAssistantDescriptor      — assemble le McpServerDescriptor (6 tools, 2 resources, 2 prompts)
│
├── domain/
│   ├── NoteRepository       — fabrique Repository<Note, PropertyQuery> (in-memory)
│   └── NoteService          — façade métier : CRUD, list, search
│       └── Note (généré)    — value object depuis note.yaml
│
├── tools/                   — Function<CallToolParams, CallToolResult>
│   ├── ToolHelper           — success(), error(), arg(), argList()
│   ├── CreateNoteTool
│   ├── GetNoteTool
│   ├── UpdateNoteTool
│   ├── DeleteNoteTool
│   ├── ListNotesTool
│   └── SearchNotesTool
│
├── resources/               — Function<ReadResourceParams, ReadResourceResult>
│   ├── NoteResourceHandler
│   └── TaggedNotesResourceHandler
│
└── prompts/                 — Function<GetPromptParams, GetPromptResult>
    ├── PromptHelper
    ├── SummarizeNotePromptHandler
    └── CompareNotesPromptHandler
```

### Le value object `Note`

`Note` est généré par `cdm-value-objects-maven-plugin` depuis `src/main/resources/note.yaml` :

```yaml
Note:
  title: string
  content: string
  tags:
    $list: string
  created-at: date-time
  updated-at: date-time
```

L'id de la note n'est pas dans le value object — il vient de `Entity<Note>.id()`, géré par le repository. Ce pattern est standard dans poom-services : le repository assigne les identifiants.

### Persistance — Repository<Note, PropertyQuery>

```java
// NoteRepository.java
public static Repository<Note, PropertyQuery> create() {
    return InMemoryRepositoryWithPropertyQuery.validating(Note.class);
}
```

`InMemoryRepositoryWithPropertyQuery.validating()` crée un repository en mémoire qui accepte des requêtes `PropertyQuery` avec validation des noms de propriétés (une `InvalidPropertyException` est levée si un champ inconnu est utilisé dans un filtre).

`NoteService` est la seule classe qui accède au repository. Les handlers (tools, resources, prompts) passent tous par `NoteService` — ils ne connaissent pas le repository.

### Outils — Function\<CallToolParams, CallToolResult\>

Chaque outil est une classe qui implémente `Function<CallToolParams, CallToolResult>` et reçoit un `NoteService` à la construction :

```java
public class CreateNoteTool implements Function<CallToolParams, CallToolResult> {
    private final NoteService noteService;

    @Override
    public CallToolResult apply(CallToolParams params) {
        String title   = ToolHelper.arg(params.arguments(), "title");
        String content = ToolHelper.arg(params.arguments(), "content");
        if (title == null || title.isBlank()) return ToolHelper.error("title is required");
        // ...
        return ToolHelper.success("Note created with id: " + entity.id());
    }
}
```

`ToolHelper.arg(ObjectValue, String)` extrait une valeur scalaire d'un `ObjectValue` de manière null-safe. `ToolHelper.argList()` fait de même pour les listes.

Les résultats d'erreur utilisent `isError: true` dans `CallToolResult` — c'est la convention MCP pour signaler une erreur métier (opposé à une erreur protocole JSON-RPC).

### Outil asynchrone — search_notes

`SearchNotesTool` appelle `noteService.search()` qui contient un `Thread.sleep(600)` intentionnel pour simuler une opération lente. `McpProcessor` attend 500 ms (`syncTimeoutMillis`) avant de switcher sur le chemin asynchrone :

```
POST tools/call search_notes  →  lancé dans toolExecutor
  ↓ après 500ms : TimeoutException
  →  202 Accepted (réponse HTTP libérée)
  →  handleAsyncToolCall() enregistre un callback sur le CompletableFuture
  ↓ après ~100ms : handler termine
  →  résultat sérialisé en JSON-RPC
  →  session.sseChannel().send("message", json)
```

Le canal SSE doit être ouvert (GET) avant d'appeler un outil lent. Sans canal SSE, le résultat est perdu et une erreur est loggée.

### Ressources — Function\<ReadResourceParams, ReadResourceResult\>

Les handlers de ressources reçoivent les `ReadResourceParams` (qui contiennent l'URI complète) et extraient les paramètres eux-mêmes :

```java
public class NoteResourceHandler implements Function<ReadResourceParams, ReadResourceResult> {
    private static final String URI_PREFIX = "note://";

    @Override
    public ReadResourceResult apply(ReadResourceParams params) {
        String id = params.uri().substring(URI_PREFIX.length());
        return noteService.get(id)
                .map(e -> ReadResourceResult.builder()
                        .contents(ResourceContent.builder()
                                .uri(params.uri())
                                .mimeType("text/markdown")
                                .text("# " + e.value().title() + "\n\n" + e.value().content())
                                .build())
                        .build())
                .orElseGet(() -> /* not found */);
    }
}
```

Le routage vers le bon handler se fait dans `McpProcessor` par préfixe d'URI : `note://` → `NoteResourceHandler`, `notes://tagged/` → `TaggedNotesResourceHandler`.

### Prompts — Function\<GetPromptParams, GetPromptResult\>

Les handlers de prompts récupèrent les notes demandées et construisent un `PromptMessage` avec `role: "user"` et le texte du prompt en contenu :

```java
// Résultat typique de SummarizeNotePromptHandler
GetPromptResult.builder()
    .description("Summarize note '" + note.title() + "'")
    .messages(PromptHelper.userMessage(
        "Please summarize the following note in 2-3 sentences:\n\n"
        + "Title: " + note.title() + "\n\n"
        + note.content()))
    .build()
```

Le client MCP (Claude) reçoit ces messages et les injecte dans la conversation comme s'ils venaient de l'utilisateur, avant de demander au modèle de les traiter.

### Assemblage — NoteAssistantDescriptor

`NoteAssistantDescriptor.build(NoteService)` est le point central de câblage. Il crée un `McpServerDescriptor` avec tous les outils, ressources et prompts :

```java
McpServerDescriptor.builder()
    .name("note-assistant").version("1.0.0")
    .tools(createNoteTool(s), getNoteTool(s), /* ... */)
    .resources(
        McpResourceDescriptor.builder()
            .uri("note://{id}").mimeType("text/markdown")
            .handler(new NoteResourceHandler(s)).build(),
        // ...
    )
    .prompts(/* ... */)
    .build()
```

Les `inputSchema` des outils sont des `ObjectValue` JSON Schema minimalistes (type + properties + descriptions). Le tableau `required` est intentionnellement absent — la validation est faite à l'exécution dans chaque handler.

### Tests

```
domain/NoteServiceTest           — 17 tests : CRUD, list, search, cas limites
tools/CreateNoteToolTest         — happy path + arg manquant + tags
tools/GetNoteToolTest            — found + not found + id manquant
tools/UpdateNoteToolTest         — partiel + not found + id manquant
tools/DeleteNoteToolTest         — found + not found + id manquant
tools/ListNotesToolTest          — all + tag filter + empty + tags dans output
tools/SearchNotesToolTest        — title match + content match + no match + arg manquant
resources/NoteResourceHandlerTest        — found (markdown) + tags + not found
resources/TaggedNotesResourceHandlerTest — tagged + empty + mixed tags
prompts/SummarizeNotePromptHandlerTest   — found + arg manquant + not found
prompts/CompareNotesPromptHandlerTest    — two notes + missing id + one not found
NoteAssistantIntegrationTest     — 17 scénarios end-to-end : session lifecycle,
                                   CRUD roundtrip, async search (202+SSE),
                                   resources/list+read, prompts/list+get,
                                   erreurs -32601
```

Total : **66 tests**, tous en JUnit 5, sans serveur HTTP réel (TestRequestDeleguate / TestResponseDeleguate).

### Ajouter un outil

1. **Créer le handler** dans `tools/` — implémenter `Function<CallToolParams, CallToolResult>`, utiliser `ToolHelper.arg()` / `ToolHelper.argList()` pour les arguments, retourner `ToolHelper.success(text)` ou `ToolHelper.error(text)`.

2. **Enregistrer** dans `NoteAssistantDescriptor` — ajouter un `McpToolDescriptor` dans `.tools(...)` avec `name`, `description`, `inputSchema(...)` et `handler(new YourTool(noteService))`.

3. **Tester** dans `test/.../tools/` — au minimum : happy path, argument requis manquant, entité non trouvée.

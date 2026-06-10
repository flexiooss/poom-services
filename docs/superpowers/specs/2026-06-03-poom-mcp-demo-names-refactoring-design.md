# Design : remplacement des littéraux de champs par `_names()` dans poom-mcp-demo

Date : 2026-06-03

## Contexte

Le générateur `cdm-value-objects-maven-plugin` produit pour chaque value object une interface `*Names` accessible via la méthode statique `Xxx.names_()`. Elle expose un accès type-safe aux noms de champs : `Note.names_().title()` retourne `"title"`, `Note.names_().createdAt()` retourne `"created-at"`, etc.

Le code de `poom-mcp-demo` utilise des littéraux de chaînes là où ces méthodes générées devraient être utilisées. Ce refactoring les remplace, ce qui rend les noms de champs refactoring-proof — un renommage dans le YAML de spec sera détecté à la compilation plutôt qu'en runtime.

## Règle de substitution

Tout littéral de chaîne qui nomme un champ d'un value object (pour accéder à une propriété d'un `ObjectValue` ou déclarer le nom d'un argument d'outil qui correspond à ce champ) est remplacé par l'appel `Xxx.names_().field()` correspondant.

**Ne sont pas substituables** :
- Arguments d'outil/prompt sans correspondance dans un value object : `"id"`, `"query"`, `"tag"`, `"note_id"`, `"note_id_1"`, `"note_id_2"`
- Valeurs constantes (pas des noms de champs) : `"user"`, `"text"` (comme valeur de type), `"object"`
- Vocabulaire JSON Schema dans `schema()` : `"type"`, `"description"`, `"properties"`

## Deux familles de substitution

### 1. Champs de `Note` — `Note.names_().*`

`Note.names_()` expose : `title()`, `content()`, `tags()`, `createdAt()`, `updatedAt()`.

| Avant | Après |
|---|---|
| `ToolHelper.arg(args, "title")` | `ToolHelper.arg(args, Note.names_().title())` |
| `ToolHelper.arg(args, "content")` | `ToolHelper.arg(args, Note.names_().content())` |
| `ToolHelper.argList(args, "tags")` | `ToolHelper.argList(args, Note.names_().tags())` |
| `schema("title", "string", "...")` | `schema(Note.names_().title(), "string", "...")` |
| `schema("content", "string", "...")` | `schema(Note.names_().content(), "string", "...")` |
| `schema("tags", "array", "...")` | `schema(Note.names_().tags(), "array", "...")` |

### 2. Champs de `ToolContent` — `ToolContent.names_().*`

`ToolContent.names_()` expose : `type()`, `text()`.

Utilisé dans `PromptHelper` et dans les assertions de tests de prompts, où un `ObjectValue` brut est construit/interrogé avec les clés `"type"` et `"text"` pour représenter le contenu MCP.

| Avant | Après |
|---|---|
| `.property("type", v -> v.stringValue("text"))` | `.property(ToolContent.names_().type(), v -> v.stringValue("text"))` |
| `.property("text", v -> v.stringValue(text))` | `.property(ToolContent.names_().text(), v -> v.stringValue(text))` |
| `.content().property("text")` (assertion) | `.content().property(ToolContent.names_().text())` |

## Fichiers touchés

### `src/main/java/`

| Fichier | Substitutions |
|---|---|
| `tools/CreateNoteTool.java` | 3 — `title`, `content`, `tags` |
| `tools/UpdateNoteTool.java` | 3 — `title`, `content`, `tags` |
| `NoteAssistantDescriptor.java` | 6 — `title`, `content`, `tags` dans `createNoteTool` et `updateNoteTool` |
| `prompts/PromptHelper.java` | 2 — `type`, `text` (construction ObjectValue) |

### `src/test/java/`

| Fichier | Substitutions |
|---|---|
| `prompts/SummarizeNotePromptHandlerTest.java` | 1 — `.property("text")` en assertion |
| `prompts/CompareNotesPromptHandlerTest.java` | 1 — `.property("text")` en assertion |

**Total : ~12 substitutions sur 6 fichiers.**

### Non touchés

- `tools/ToolTestHelper.java` — classe générique qui ne connaît pas `Note`
- `tools/*ToolTest.java` — les clés passées via `ToolTestHelper.params("title", ...)` sont des arguments stringly-typed sans référence directe à un champ de VO dans ce contexte
- `tools/GetNoteTool.java`, `DeleteNoteTool.java`, `ListNotesTool.java`, `SearchNotesTool.java` — aucun champ Note utilisé comme clé

## Imports à ajouter

- `CreateNoteTool`, `UpdateNoteTool` : `Note` déjà importé
- `NoteAssistantDescriptor` : ajouter `import org.codingmatters.poom.mcp.demo.domain.types.Note;`
- `PromptHelper`, `SummarizeNotePromptHandlerTest`, `CompareNotesPromptHandlerTest` : ajouter `import org.codingmatters.poom.mcp.types.ToolContent;`

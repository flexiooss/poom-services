# Paged Collection Browse — Prise en compte des headers `init-ordered`, `since`, `before`

## Contexte

La RAML trait `rfc7233-browse-collection.raml` définit trois headers de request pour la navigation ordonnée par curseur :

- `init-ordered: LATEST | OLDEST` — initialisation du curseur (derniers / premiers éléments)
- `since: <opaque>` — navigation forward depuis un curseur
- `before: <opaque>` — navigation backward depuis un curseur

Et deux headers de response pour propager les curseurs au client :

- `since` — curseur permettant une navigation forward depuis la page courante
- `before` — curseur permettant une navigation backward depuis la page courante

Le `BrowseHandlerGenerator` ignore actuellement ces headers. Cette spec décrit leur intégration.

---

## Périmètre

- **Module domain** : `poom-services-paged-collection-domain` — nouvelles interfaces `OrderedLister` et `OrderedPage` dans `PagedCollectionAdapter`
- **Générateur** : `BrowseHandlerGenerator` dans `poom-services-paged-collection-generation-generators` — branchement curseur vs offset
- **Tests** : `BrowseHandlerGeneratorTest` — nouveaux cas, `TestOrderedLister`

Aucun nouveau module. Pas de modification des specs RAML (déjà à jour).

---

## Section 1 — Interfaces domain

### `PagedCollectionAdapter` — ajouts

Deux nouvelles interfaces imbriquées dans `PagedCollectionAdapter`, et une méthode `default` sur `Pager` :

```java
interface OrderedPage<EntityType> {
    PagedEntityList<EntityType> list();
    Optional<String> since();
    Optional<String> before();
}

interface OrderedLister<EntityType> {
    OrderedPage<EntityType> initLatest(Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;

    OrderedPage<EntityType> initOldest(Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;

    OrderedPage<EntityType> since(String since, Optional<String> before,
                                   Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;

    OrderedPage<EntityType> before(String before,
                                    Optional<PropertyQuery> query, long start, long end)
        throws RepositoryException;
}

interface Pager<EntityType> {
    String unit();
    int maxPageSize();
    default int defaultPageSize() { return this.maxPageSize(); }
    EntityLister<EntityType, PropertyQuery> lister();
    default OrderedLister<EntityType> orderedLister() { return null; } // NEW
}
```

**Aucun breaking change** : `orderedLister()` est `default null`. Les implémentations existantes ne changent pas.

---

## Section 2 — Contrat des méthodes `OrderedLister`

Les valeurs curseur sont des **tokens opaques et immutables** — seule l'implémentation sait les interpréter (timestamp, ID auto-incrémenté, etc.). Le handler généré ne les inspecte jamais : il les lit du request et les écrit dans la response tels quels.

### `initLatest(query, start, end)`

Retourne les **derniers éléments** de la collection, ordonnés du plus ancien au plus récent (oldest-to-newest).

- `start`/`end` : offset dans la queue de la collection (ex: `0-49` = les 50 derniers)
- `query` : filtre/tri optionnel appliqué avant la sélection
- Response `since` : curseur positionné **avant le premier élément retourné** — le client peut paginer forward
- Response `before` : curseur positionné **après le dernier élément retourné** — le client peut paginer backward

### `initOldest(query, start, end)`

Retourne les **premiers éléments** de la collection, ordonnés du plus ancien au plus récent.

- `start`/`end` : offset dans la tête de la collection
- `query` : filtre/tri optionnel
- Response `since` : curseur positionné **avant le premier élément retourné**
- Response `before` : `Optional.empty()` — pas de sens pour une initialisation depuis le début

### `since(since, optionalBefore, query, start, end)`

Retourne les éléments **postérieurs au curseur `since`**, ordonnés du plus ancien au plus récent.

- `since` : borne inférieure exclusive
- `optionalBefore` : si présent, borne supérieure exclusive (filtre) — l'ordre reste oldest-to-newest
- `start`/`end` : offset dans la fenêtre résultante
- `query` : filtre/tri optionnel appliqué en plus des bornes curseur
- Response `since` : curseur positionné **après le dernier élément retourné** — le client peut continuer forward
- Response `before` : valeur de `optionalBefore` si présent, sinon `Optional.empty()`

### `before(before, query, start, end)`

Retourne les éléments **antérieurs au curseur `before`**, ordonnés du plus récent au plus ancien.

- `before` : borne supérieure exclusive
- `start`/`end` : offset dans la fenêtre résultante
- `query` : filtre/tri optionnel
- Response `before` : curseur positionné **avant le dernier élément retourné** (le plus ancien de la page) — le client peut continuer backward
- Response `since` : `Optional.empty()`

---

## Section 2bis — Flow du handler généré (`BrowseHandlerGenerator`)

```
request reçu
    │
    ├─ since / before / init-ordered présent ?
    │       │
    │       ├─ oui → pager.orderedLister() == null ?
    │       │               ├─ oui → 400 ILLEGAL_REQUEST ("ordered browsing not supported")
    │       │               └─ non → parse range → start/end (défaut: 0..defaultPageSize-1)
    │       │                         │
    │       │                         ├─ initOrdered == LATEST  → orderedLister.initLatest(query, start, end)
    │       │                         ├─ initOrdered == OLDEST  → orderedLister.initOldest(query, start, end)
    │       │                         ├─ since présent          → orderedLister.since(since, optBefore, query, start, end)
    │       │                         └─ before seul            → orderedLister.before(before, query, start, end)
    │       │                         │
    │       │                         └─ orderedPage → contentRange/acceptRange depuis list()
    │       │                                          since/before headers depuis orderedPage (omis si Optional.empty())
    │       │                                          → 200 / 206 (isPartial inchangé)
    │       │
    │       └─ non → chemin offset existant (Rfc7233Pager, inchangé)
```

**Détails d'implémentation :**

- Le header `Range` reste actif en mode curseur pour contrôler la taille de page. On utilise `Range.fromRequestedRange()` directement pour extraire `start`/`end` sans instancier le `Rfc7233Pager` complet. Si le range est invalide (`Range.isValid() == false`), on retourne 416 avec `acceptRange` — même comportement que le chemin offset.
- `contentRange` et `acceptRange` sont construits depuis le `PagedEntityList` de `OrderedPage.list()` — même format que le chemin offset.
- Les exceptions `RepositoryException` → 500, `RepositoryAccessDeniedException` → 403, `RepositoryQueryParsingException` → 400 : même gestion que le chemin offset.
- Priorité : `initOrdered` > `since` > `before` si plusieurs sont présents simultanément.

---

## Section 3 — Tests

**Fichier modifié :** `BrowseHandlerGeneratorTest`

### Routing curseur
- `since` présent → `orderedLister.since()` appelé, pas `lister()`
- `before` présent → `orderedLister.before()` appelé
- `initOrdered=LATEST` → `orderedLister.initLatest()` appelé
- `initOrdered=OLDEST` → `orderedLister.initOldest()` appelé
- `since` + `before` → `orderedLister.since()` avec `optBefore` présent
- Aucun curseur → chemin offset inchangé (non-régression)

### orderedLister absent
- Curseur présent mais `orderedLister() == null` → 400 `ILLEGAL_REQUEST`

### Response headers
- 200/206 avec curseur → `since`/`before` présents dans la response quand `OrderedPage` les fournit
- 200/206 avec curseur → `since`/`before` absents quand `OrderedPage` retourne `Optional.empty()`

### Exceptions sur le chemin curseur
- `RepositoryException` → 500
- `RepositoryAccessDeniedException` → 403
- `RepositoryQueryParsingException` → 400

**Nouvelle classe de test :** `TestOrderedLister` dans `src/test/java` — enregistre les appels via `AtomicReference` (pattern existant dans `BrowseHandlerGeneratorTest`).

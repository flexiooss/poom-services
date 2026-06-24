# Advanced Demo — Ordered Browsing sur la collection `/{store}/movies`

## Contexte

La feature cursor-based browsing (`init-ordered`, `since`, `before`) a été intégrée au `BrowseHandlerGenerator` et à `PagedCollectionAdapter`. Ce document spec l'intégration dans `poom-services-advanced-demo` pour valider la feature end-to-end sur un cas concret.

**Collection cible :** `/{store}/movies`  
**Critère d'ordre principal :** `facts.releaseDate` (LocalDate, format ISO `YYYY-MM-DD`)  
**Critère d'ordre secondaire :** `id` (String, pour ordre absolu quand deux films ont la même date)

---

## Périmètre

- **Module domain** : `poom-services-demo-domain` — nouvelle classe `MovieOrderedLister`
- **Module domain** : `MoviePager` — ajout de `orderedLister()` override
- **API spec** : `poom-services-demo-api-spec` — bump `poom-api-specs` `1.36.0` → `1.44.0`
- **Tests** : `MovieOrderedListerTest` (nouvelle classe)

Aucun nouveau module. Pas de modification du schéma Movie.

---

## Section 1 — Format du curseur

### Choix : composite key pipe-delimited

```
cursor = "YYYY-MM-DD|movieId"
```

Exemples :
```
"2003-06-20|movie-42"
"1997-11-21|movie-07"
```

**Propriétés :**
- Opaque pour le handler généré (passé tel quel dans les headers)
- Parsé uniquement par `MovieOrderedLister`
- Immutable — ne contient pas d'état de session
- Trié lexicographiquement par date d'abord, puis par id (ordre stable)

### Helpers de curseur (privés dans `MovieOrderedLister`)

```java
private String cursorFor(Movie movie) {
    return movie.facts().releaseDate().toString() + "|" + movie.id();
}

private String[] parseCursor(String cursor) {
    return cursor.split("\\|", 2);  // [0]=date, [1]=id
}
```

---

## Section 2 — Stratégie de filtrage par curseur

Le filtrage par curseur est effectué **en Java** (pas via PropertyQuery) : `InMemoryRepositoryWithPropertyQuery` normalise les `LocalDate` en `LocalDateTime` avant comparaison, rendant la comparaison contre un string literal (`'YYYY-MM-DD'`) impossible (ClassCastException).

### Approche : fetch-all + filtre Java

1. Fetch tous les films via `repository.search(sortQuery, 0, 9999)` — avec le sort PropertyQuery et le filtre utilisateur éventuel
2. Filtre curseur appliqué en Java sur la liste retournée

### Prédicats Java

```java
// Vrai si movie est strictement après cursor (date > D, ou date == D && id > I)
boolean isAfterCursor(Movie movie, String[] cursorParts) {
    LocalDate cursorDate = LocalDate.parse(cursorParts[0]);
    int dateCmp = movie.facts().releaseDate().compareTo(cursorDate);
    if (dateCmp != 0) return dateCmp > 0;
    return movie.id().compareTo(cursorParts[1]) > 0;
}

// Vrai si movie est strictement avant cursor
boolean isBeforeCursor(Movie movie, String[] cursorParts) {
    LocalDate cursorDate = LocalDate.parse(cursorParts[0]);
    int dateCmp = movie.facts().releaseDate().compareTo(cursorDate);
    if (dateCmp != 0) return dateCmp < 0;
    return movie.id().compareTo(cursorParts[1]) < 0;
}
```

### Sort PropertyQuery (pour repository.search)

- Oldest-to-newest : `"facts.releaseDate asc, id asc"`
- Newest-to-oldest : `"facts.releaseDate desc, id desc"`

### Filtre utilisateur (optionnel)

Si un `PropertyQuery` utilisateur est présent, son `filter()` est passé à `repository.search()` — le sort est toujours celui du curseur.

---

## Section 3 — `MovieOrderedLister`

### Localisation

```
poom-services-demo-domain/src/main/java/
  org/codingmatters/poom/demo/domain/MovieOrderedLister.java
```

### Interface implémentée

```java
public class MovieOrderedLister implements PagedCollectionAdapter.OrderedLister<Movie> {
    private final Repository<Movie, PropertyQuery> repository;

    public MovieOrderedLister(Repository<Movie, PropertyQuery> repository) {
        this.repository = repository;
    }
    // ...
}
```

### Les 4 méthodes

#### `initLatest(query, start, end)`

Retourne les **N plus récents** films, ordonnés oldest-to-newest.

- Sort : `facts.releaseDate desc, id desc`
- `repository.search(descQuery, start, end)` → N films newest-first
- **Reverse la liste** pour obtenir oldest-to-newest
- Reconstruit un `DefaultPagedEntityList` avec indices ajustés :
  - `newStart = total - desc_end - 1`
  - `newEnd = total - desc_start - 1`
- Response `since` : `cursorFor(newest)` — dernier après reverse (le plus récent)
- Response `before` : `cursorFor(oldest)` — premier après reverse (le plus ancien)

#### `initOldest(query, start, end)`

Retourne les **N plus anciens** films, ordonnés oldest-to-newest.

- Sort : `facts.releaseDate asc, id asc`
- `repository.search(ascQuery, start, end)` → N films oldest-first
- Response `since` : `cursorFor(newest)` — dernier de la page (le plus récent)
- Response `before` : `Optional.empty()` — on est au début, pas de navigation backward

#### `since(since, optBefore, query, start, end)`

Retourne les films postérieurs au curseur `since`, oldest-to-newest.

- Filter : `sinceFilter(since)` + si `optBefore` présent : `&& beforePredicate(optBefore.get())`
- Sort : `facts.releaseDate asc, id asc`
- Response `since` : `cursorFor(newest)` — dernier de la page (continuer forward)
- Response `before` : `optBefore` tel quel (conserver la borne supérieure)

#### `before(before, query, start, end)`

Retourne les films antérieurs au curseur `before`, newest-to-oldest.

- Filter : `beforeFilter(before)`
- Sort : `facts.releaseDate desc, id desc`
- Response `since` : `Optional.empty()`
- Response `before` : `cursorFor(oldest)` — dernier en desc (le plus ancien retourné, continuer backward)

### Tableau récapitulatif

| Méthode | Sort | Filter | `since` response | `before` response |
|---|---|---|---|---|
| `initLatest` | desc | aucun | key(newest) | key(oldest) |
| `initOldest` | asc | aucun | key(newest) | empty |
| `since` | asc | sinceFilter [+ optBefore] | key(newest) | optBefore tel quel |
| `before` | desc | beforeFilter | empty | key(oldest) |

---

## Section 4 — Wiring dans `MoviePager`

Ajout d'un override dans `MoviePager` :

```java
@Override
public PagedCollectionAdapter.OrderedLister<Movie> orderedLister() {
    return new MovieOrderedLister(this.repository);
}
```

`MoviePager` possède déjà un champ `repository` — pas de changement de constructeur.

---

## Section 5 — Bump RAML

Le fichier `poom-services-demo-api-spec/src/main/resources/demo.raml` (ou équivalent) référence `poom-api-specs:1.36.0` via `uses:`. Il faut bumper à `1.44.0` pour que les types générés incluent les headers `before`, `init-ordered` dans les request/response.

Vérifier toutes les occurrences de `1.36.0` dans le module `poom-services-demo-api-spec`.

---

## Section 6 — Tests

### Classe : `MovieOrderedListerTest`

**Localisation :**
```
poom-services-demo-domain/src/test/java/
  org/codingmatters/poom/demo/domain/MovieOrderedListerTest.java
```

**Fixture :** 10 films avec des dates variées (certains partageant la même date pour tester l'ordre par id), créés via `InMemoryRepositoryWithPropertyQuery.validating(Movie.class)`.

**Cas de test :**

1. `initLatest` sur repo vide → `list()` vide, `since()` et `before()` tous deux `empty`
2. `initLatest` avec 10 films, page 0-4 → les 5 plus récents retournés oldest-to-newest, `since` = key du plus récent, `before` = key du plus ancien
3. `initOldest` avec 10 films, page 0-4 → les 5 plus anciens oldest-to-newest, `since` = key du plus récent, `before` = empty
4. `since(cursor)` → retourne uniquement les films postérieurs au cursor
5. `since(cursor)` avec `optBefore` → retourne uniquement les films dans la fenêtre `[since, before[`
6. `before(cursor)` → retourne uniquement les films antérieurs au cursor, newest-first, `before` = key du plus ancien
7. **Round-trip** : `initLatest(0-4)` → `before(before_cursor)` → films précédents (5 suivants dans le passé)
8. **Round-trip** : `initLatest(0-4)` → `since(since_cursor)` → `Optional.empty` (on est déjà aux plus récents)
9. **Même date, tri par id** : 3 films à la même date → ordre stable par id, cursors les distinguent correctement

---

## Note sur InMemoryRepositoryWithPropertyQuery et les dates

`facts.releaseDate` est un `LocalDate`. L'in-memory repo accède au champ par réflexion. Pour les comparaisons `>`, `<`, `==`, il appelle probablement `toString()` sur la valeur, donnant `"YYYY-MM-DD"`. Le format ISO est lexicographiquement ordonné, donc les comparaisons string fonctionnent comme des comparaisons de dates.

**À vérifier en premier test** — si ce n'est pas le cas, utiliser un helper de conversion dans `MovieOrderedLister` (ex: stocker la date en millisecondes, ou utiliser un autre field si disponible).

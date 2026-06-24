# MovieOrderedListerTest — comportement de l'ordered browsing

`MovieOrderedLister` implémente `PagedCollectionAdapter.OrderedLister<Movie>`. Il permet de parcourir la collection de films dans l'ordre chronologique (`facts.releaseDate asc/desc, id asc/desc`) via un curseur composite `"YYYY-MM-DD|movieId"`.

## Fixture

10 films insérés **dans le désordre** (pour prouver que le tri vient du sort, pas de l'ordre d'insertion).  
Deux paires partagent délibérément la même date de sortie : **M2/M3** (`1995-06-15`) et **M6/M7** (`2010-07-04`).

```
M1  1990-01-01
M2  1995-06-15
M3  1995-06-15   ← même date que M2
M4  2000-01-01
M5  2005-03-20
M6  2010-07-04
M7  2010-07-04   ← même date que M6
M8  2015-11-30
M9  2020-01-01
M10 2024-06-15
```

Curseur d'un film = `releaseDate.toString() + "|" + id`, ex. `"2010-07-04|m-06"`.

---

## `initLatest` — initialisation sur les films les plus récents

| Test | Ce qu'il vérifie |
|------|-----------------|
| `givenEmptyRepo__whenInitLatest__thenEmptyPageAndNoCursors` | Sur un dépôt vide, la page est vide et les deux curseurs (`since`, `before`) sont absents. |
| `whenInitLatest__thenLastMoviesReturnedOldestFirst` | `initLatest(0, 4)` renvoie une fenêtre de 5 éléments prise **depuis la fin** de la liste triée ascendante, mais **restitués du plus ancien au plus récent** dans la page : `[M6, M7, M8, M9, M10]`. |
| `whenInitLatest__thenSinceCursorIsNewest` | Le curseur `since` pointe sur le film **le plus récent** de la page (`M10`), utilisable pour aller chercher les suivants une fois de nouveaux films ajoutés. |
| `whenInitLatest__thenBeforeCursorIsOldest` | Le curseur `before` pointe sur le film **le plus ancien** de la page (`M6`), utilisable pour naviguer vers les pages antérieures. |
| `whenInitLatest__thenPagedListIndicesAreAdjusted` | Les indices de la `PagedEntityList` reflètent la **position absolue** dans la liste ascendante complète : start=5, end=9, total=10. |

---

## `initOldest` — initialisation sur les films les plus anciens

| Test | Ce qu'il vérifie |
|------|-----------------|
| `whenInitOldest__thenFirstMoviesReturnedOldestFirst` | `initOldest(0, 4)` renvoie les 5 premiers films triés ascendant : `[M1, M2, M3, M4, M5]`. |
| `whenInitOldest__thenSinceCursorIsNewest` | Le curseur `since` pointe sur le film **le plus récent** de la page (`M5`), prêt pour la navigation vers l'avant. |
| `whenInitOldest__thenBeforeCursorIsEmpty` | Le curseur `before` est absent : il n'y a rien avant le premier film. |

---

## `since` — navigation vers l'avant (films plus récents)

| Test | Ce qu'il vérifie |
|------|-----------------|
| `givenSinceCursor__whenSince__thenMoviesAfterCursorReturnedOldestFirst` | `since(cursor(M5))` renvoie tous les films strictement après M5, du plus ancien au plus récent : `[M6, M7, M8, M9, M10]`. |
| `givenSinceCursorOnSharedDate__whenSince__thenIdBoundaryRespected` | Quand deux films partagent la même date (`M2`/`M3`), `since(cursor(M2))` renvoie `M3` mais pas `M2` : la borne est **exclusive** et s'appuie sur l'id pour les ex-æquo. |
| `givenSinceCursorAndBeforeCursor__whenSince__thenWindowedResultReturned` | En passant `since=cursor(M5)` et `before=cursor(M8)`, on obtient la fenêtre `[M6, M7]` — les bornes sont toutes deux **exclusives**. |
| `givenSinceCursorAndBeforeCursor__whenSince__thenOptBeforePreservedInResponse` | Quand un `before` est fourni dans la requête, il est retransmis tel quel dans la réponse (sert au client pour savoir jusqu'où allait la fenêtre). |
| `givenSinceCursor__whenSince__thenSinceCursorIsNewest` | Le curseur `since` de la réponse pointe toujours sur le film **le plus récent retourné** (`M10`), indépendamment du curseur d'entrée. |

---

## `before` — navigation vers l'arrière (films plus anciens)

| Test | Ce qu'il vérifie |
|------|-----------------|
| `givenBeforeCursor__whenBefore__thenMoviesBeforeCursorReturnedNewestFirst` | `before(cursor(M6))` renvoie tous les films strictement avant M6, **du plus récent au plus ancien** : `[M5, M4, M3, M2, M1]`. L'ordre est inversé car on recule dans le temps. |
| `givenBeforeCursor__whenBefore__thenSinceCursorIsEmpty` | Il n'y a pas de curseur `since` en réponse : on ne peut pas aller « encore plus en arrière » depuis une navigation `before`. |
| `givenBeforeCursor__whenBefore__thenBeforeCursorIsOldest` | Le curseur `before` de la réponse pointe sur le film **le plus ancien retourné** (`M1`), utilisable pour une éventuelle page encore plus ancienne. |

---

## Round-trips — enchaînements de pages

| Test | Ce qu'il vérifie |
|------|-----------------|
| `givenInitLatestPage__whenBeforeOnBeforeCursor__thenPreviousPageReturned` | On obtient la page initiale via `initLatest`, on réutilise son curseur `before` pour appeler `before(...)` : on récupère bien la page précédente `[M5, M4, M3, M2, M1]`. |
| `givenInitOldestPage__whenSinceOnSinceCursor__thenNextPageReturned` | On obtient la page initiale via `initOldest`, on réutilise son curseur `since` pour appeler `since(...)` : on récupère bien la page suivante `[M6, M7, M8, M9, M10]`. |

---

## Filtrage par catégorie

| Test | Ce qu'il vérifie |
|------|-----------------|
| `givenCategoryFilter__whenInitLatest__thenOnlyCategoryMoviesReturned` | Un `MovieOrderedLister` construit avec une catégorie (`HORROR`) n'expose que les films de cette catégorie, même si le dépôt en contient d'autres. |
| `givenCategoryFilter__whenSince__thenOnlyCategoryMoviesReturned` | La navigation `since` avec filtre de catégorie franchit correctement les curseurs sans « voir » les films d'autres catégories. |

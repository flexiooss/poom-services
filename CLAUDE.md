# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

**poom-services** ("Poor Old Man's Web Services") is a multi-module Maven library suite (`org.codingmatters.poom`, currently `1.283.0-SNAPSHOT`) that provides building blocks for code-generated REST services on top of the `codingmatters-rest` and `codingmatters-value-objects` frameworks.

The repo is itself a library, not a deployable application — the `poom-services-demo` modules are the canonical examples of how everything fits together.

## Build Commands

```bash
# Full build (everything: compile, generate, test, package)
mvn install

# Build without running tests
mvn install -DskipTests

# Build a single module and its dependencies (run from repo root)
mvn install -pl poom-services-domain -am

# Run tests for a single module
mvn test -pl poom-services-domain

# Run a single test class / method
mvn test -pl poom-services-domain -Dtest=PropertyQueryParserTest
mvn test -pl poom-services-domain -Dtest=PropertyQueryParserTest#testMethodName

# Force regenerate sources (RAML / value-objects / ANTLR / paged-collection / i18n)
mvn generate-sources
```

The reactor declares the module list in the root `pom.xml` (`<modules>`). Module artifact versions are kept in lockstep via the BOM in `<dependencyManagement>` of the root pom — when bumping versions, update both the parent `<version>` and every `1.xxx.x-SNAPSHOT` entry in the BOM.

Builds depend on Codingmatters' private Maven repositories (`mvn.ci.flexio.io`); offline / outside-network builds will fail to resolve `codingmatters-parent`, `codingmatters-rest`, `codingmatters-value-objects`, etc.

## Release Flow

Releases are managed by `flexio-flow` (see `flexio-flow.yml`) — do not bump versions manually for releases. The `dependencyversion-maven-plugin` (`set-version` goal, bound to the default lifecycle) propagates the parent version to all child modules at build time, so child poms typically don't repeat `<version>`.

## Code Generation

Most modules generate Java sources at `generate-sources`. Three generators dominate; understanding which one applies to a module is the fastest way to read it:

| Generator | Input | Plugin | Modules using it |
|---|---|---|---|
| **RAML** REST API | `src/main/resources/*.raml` (api-fragment) | `cdm-rest-maven-plugin` (`generate-api-types`, `generate-server`, `generate-client`) | `*-api-spec`, `*-api-types`, `*-api-processor`, `*-api-client` modules |
| **Value Objects** | `src/main/resources/*.yaml` | `cdm-value-objects-maven-plugin` (`generate`) | Domain modules (`poom-services-domain`, `*-domain`, repository tests) |
| **ANTLR4** grammars | `src/main/antlr4/**/*.g4` | `antlr4-maven-plugin` | `poom-services-domain` (filter & sort grammars) |

Module-specific generators also exist:
- `poom-services-paged-collection-generation-maven-plugin` — generates paged-collection processors from a spec
- `poom-services-i18n-bundle-spec-plugin` — auto-activated by the `i18n-bundle-generation` profile when `src/main/i18n/bundles.yml` exists
- `poom-services-api-test-generation-maven-plugin` — generates API acceptance tests from a descriptor

All generated code lands in `target/generated-sources/` and is added to the source path via `build-helper-maven-plugin`. Never edit files under `target/` — change the spec / grammar / yaml instead and rebuild.

## Module Layout (the big picture)

The repo is organized by responsibility, not by feature. Each "feature" (caches, paged-collection, i18n, json-rpc, demo) tends to be split into the same vertical slices:

- **`*-api-spec`** — RAML/yaml specification (the source of truth)
- **`*-api-types`** — generated value objects
- **`*-api-processor`** — generated server-side request handler
- **`*-api-client`** — generated HTTP client
- **`*-domain` / `*-service`** — hand-written business logic that wires processor → domain → repository

Cross-cutting framework modules (the building blocks the above slices depend on):

| Module | Purpose |
|---|---|
| `poom-services-domain` | `Entity<V>`, `Repository<V,Q>`, `Change<T>`, `PropertyQuery` ANTLR-based filter/sort language. Has its own [CLAUDE.md](poom-services-domain/CLAUDE.md). |
| `poom-services-repositories` | In-memory repo (`*-in-memory`), repository extensions, and an acceptance test suite (`*-acceptance`) that any new repository implementation should pass |
| `poom-services-runtime` | `Service` request lifecycle, request-logging processors / state providers |
| `poom-services-containers` | `ApiContainerRuntime` — the embedded HTTP server abstraction. Backends in `*-undertow-runtime` and `*-netty-runtime`; `*-test-runtime` is for tests. Acceptance tests in `*-runtime-acceptance` cover all backends. |
| `poom-services-support` / `poom-services-production-support` | Misc helpers; production-support also publishes `docker-support` classifiers used by the `docker-image-assembler` profile |
| `poom-services-logging` / `poom-services-logging-json-layout` | SLF4J helpers and a Logback JSON layout used in production images |
| `poom-services-fast-failing` | `FastFailingProcessor` and helpers for failing requests early on container shutdown |
| `poom-services-api-registry` | Service discovery / registry API + client |
| `poom-services-api-test-support` | Generates acceptance tests for any API spec — used by service implementors |
| `poom-services-paged-collection` | Generic pagination over `Repository`, plus generators that produce paged-collection processors |
| `poom-services-i18n` / `poom-l10n` | Bundle-based i18n spec with codegen, and a localized formatter (spec / api / json / client) |
| `poom-caches` | `Cache` API + `CacheManager` (LRU, decision on access). See `poom-caches/README.md`. |
| `poom-json-rpc` | JSON-RPC types, descriptors, processor |
| `poom-services-demo` | Reference apps. `poom-services-simple-demo` is minimal; `poom-services-advanced-demo` ("digital video club") shows a full domain/processor/service stack — read its README for example curl flows. |

## Key Architectural Patterns

**Spec-first**: REST APIs are designed as RAML, value types as yaml. Hand-written code only implements interfaces produced by the generators — never edit the generated processor/client/types directly.

**Repository / Property Query**: Every persistent collection is exposed through `Repository<V, Q>` with a generic query type. Filter/sort criteria are expressed in the property-query DSL (`name == 'John' && age >= 18`, `email =~ /.*@x/i`, `tags IN (...)`, `items CONTAINS_ALL (...)`). Backends translate these by implementing `FilterEvents<T>` / `SortEvents<T>` ANTLR visitors. The `*-acceptance` test suite is run against every new repository backend.

**Container runtime**: A service builds an `ApiContainerRuntime` via `ApiContainerRuntimeBuilder`, registers one or more processors, and chooses a backend (Undertow or Netty). The runtime handles startup/shutdown, fast-failing requests when shutting down, and request logging.

**BOM-driven versions**: Modules never declare versions for sibling modules — they rely on the root `<dependencyManagement>` so a single bump updates the whole reactor.

**Java/runtime target**: The `poom.service.base.image` is `poom-service-base-25-alpine` (Java 25 base image). The CI pipeline (`poom-ci-pipeline.yaml`) currently tracks the Java 25 migration branches; the in-tree `.travis.yml` is legacy (jdk8) and not the active CI.
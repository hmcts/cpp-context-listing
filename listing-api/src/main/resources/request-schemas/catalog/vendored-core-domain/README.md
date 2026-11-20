# Vendored core-domain JSON schemas (D22)

The legacy request schemas (RAML `json/schema`, copied in by `processResources`) and `listing-json`
reference shared definitions by absolute id, for example
`http://justice.gov.uk/core/courts/courtsDefinitions.json#/definitions/uuid`.
`LegacySchemaCatalog` resolves those ids from the classpath (`request-schemas/**/*.json`, indexed by
the draft-04 `id`), never over the network.

This directory holds the core-domain schemas those `$ref`s reach, so the Gradle build no longer
needs the CPP schema jars (decision D22, `docs/MBD_REFACTOR_PROMPT.md` §15.13).

| Directory | Source artifact | Version | Files |
|---|---|---|---|
| `common-core-domain/` | `uk.gov.moj.cpp.core.domain:common-core-domain` | 17.104.6 | 1 |
| `criminal-court-public-model/` | `uk.gov.moj.cpp.core.domain:criminal-court-public-model` | 17.104.6 | 99 |

Vendored on 2026-11-19. 17.104.6 is the `coredomain.version` the Maven (JEE) build resolves from
the CPP parent POM of the (since deleted) Maven build. Each file is a byte-for-byte copy of
`json/schema/<path>` from the jar, kept at its path in the jar under `<artifact>/`.

Only the transitive closure of the `$ref`s is here: start from every schema in
`listing-api/src/raml/command/json/schema`,
`listing-api/src/raml/query/json/schema` and `listing-api/src/listing-json/json/schema` (the former
`listing-command-api`, `listing-query-api` and `listing-json` schemas, moved here at P2.6),
resolve each `$ref` (against the enclosing `id`, fragment removed), and follow it into the schema
with that `id`. Files that are never reached are left out (the jar's duplicate of
`http://justice.gov.uk/listing/courts/list-court-hearing.json` is one; the local
`listing.command.list-court-hearing.json` is the schema that is used).

## Refreshing

Do this when a newer core-domain release must be adopted or a legacy schema gains a
`$ref` into a core-domain schema that is not here.

1. Unpack `json/schema/**` from both jars (same version) into two scratch directories.
2. Recompute the closure described above and copy each reached file to
   `<artifact>/json/schema/<path in the jar>`; delete files no longer reached.
3. Update the version and date in this README.
4. `./gradlew --offline :listing-api:test` — `RequestSchemaRegistry` compiles every mapped schema at
   startup and fails with "JSON schema … is not in the classpath schema catalog" for a missing id.
   Then run the ITs (`./runIntegrationTests.sh`).

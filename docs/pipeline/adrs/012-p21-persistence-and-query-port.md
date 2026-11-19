# ADR 012 — P2.1: persistence and query port choices the prompt does not cover

- **Status:** Proposed (2026-11-19), for review with P2.1
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §5, §6, §8, §10.1 P2.1, §11)

## Context

P2.1 ports the viewstore (13 DeltaSpike entities, their repositories, the JDBC repositories) and
the 17 query paths to Spring Boot. The prompt fixes the stack (Spring Data JPA, Liquibase reused,
RestClient, framework-free services). A few choices it does not cover had to be made to keep the
queries byte-compatible and the Maven build untouched.

## Decision

1. **Module layout.** New Gradle modules sit next to the Maven ones with a `-spring` suffix where
   the §8 directory is still a Maven module (the P2.0 precedent, `listing-integration-test-spring`):
   `listing-domain-spring`, `listing-viewstore-persistence-spring`, `listing-common-spring`. The
   Liquibase changelogs are not copied: Gradle project `:listing-viewstore-liquibase` points at the
   Maven module's directory (resources only, `liquibase.properties` excluded, build output under
   the root `build/`), and listing-api has it as `runtimeOnly`. Legacy package names are kept so
   P2.3 can move command code without renames. The suffix goes at P2.6.
   *Note (D21, 2026-11-19):* reusing the directory unchanged also keeps the changeset identities:
   Spring Boot records the same (id, author, filename) rows as the JEE deployment (compared with a
   JEE-deployed viewstore; `LiquibaseChangelogIdentityTest` guards it), so the cut-over run only
   applies new changesets. It upgrades the stored checksums to Liquibase 5's `9:` format.
2. **Hibernate 7 compatibility without changing behaviour.**
   - `jsonb` columns map to Jackson 2 `JsonNode` through an `AttributeConverter` and
     `@ColumnTransformer(write = "?::jsonb")`, replacing hibernate-types' `JsonNodeBinaryType`.
     `@JdbcTypeCode(SqlTypes.JSON)` was not used because Hibernate's JSON format mapper would not
     yield the Jackson 2 `JsonNode` the code works with under Spring Boot 4 (Jackson 3).
   - Naming strategies are Hibernate's standard/legacy-JPA ones, not Spring Boot's snake_case:
     the entities rely on column names as written (`totalCount`, `caseId`).
   - `hibernate.timezone.default_storage: NORMALIZE_UTC`, matching the UTC WildFly JVM.
   - Two court-list native queries get typed casts for columns they built from text
     (`'d9ea…' as id`, `:courtCentreId as court_centre_id`): Hibernate 7 no longer coerces text to
     UUID when it reads an entity. The SQL is otherwise unchanged.
3. **Outbound identity.** `requestAsAdmin` calls send the configured system user
   (`LISTING_SYSTEM_USER_ID`, §6). Calls the framework made with `Requester#request` (defence
   case lookups, progression application and notes, usersgroups permission check) keep sending
   the **caller's** `CJSCPPUID`, as before: those services authorise the end user.
   Remote context roots are the deployed `<context>-service` roots the legacy ITs stub, not the
   generated clients' compile-time `<context>-query-api`.
4. **One read-only transaction per query.** `HearingQueryApi`, `HearingQueryView` and
   `CourtListDocumentQuery` are `@Transactional(readOnly = true)`: the ported code reads lazy entity
   collections after the repository call, as it could inside the JEE container transaction.
5. **Response-schema checks in the ITs.** The RAML response schemas were never enforced by the
   framework and the legacy responses do not always match them (list in §10.3, P2.1). The ITs
   validate every response against its RAML schema and, where the legacy output differs, strip or
   add only the documented fields before validating. Behaviour and schema files are unchanged.

## Consequences

- The JEE app, its Maven build and `runIntegrationTests.sh` are untouched.
- New `Hearing` rows must be `persist`ed: loading a `Hearing` by id selects the read-only columns
  that only the native queries return (`totalCount`, `hearing_date`, …), so Spring Data `save()`
  (a merge for an assigned id) fails, exactly as a DeltaSpike merge would have. P2.3 has to
  respect this when the commands write hearings.
- The schema defects are reported rather than fixed; fixing them is a contract change.

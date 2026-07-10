# cpp-context-listing — Java 25 / WildFly 40 / Jakarta EE 11 Upgrade

**Prepared by:** Platform Engineering (Java 25 upgrade spike)
**Status:** ✅ Builds green end-to-end (unit + integration). Handover to the listing/hearing team for production decisions.

---

## 1. Executive summary (for planning)
### What this is
Platform Engineering upgraded the **listing** context to **Java 25, WildFly 40 and Jakarta EE 11** as part of the wider platform Java 25 programme. The purpose of this piece of work was to **prove the upgrade is achievable and works end-to-end**, and to hand a working branch to the listing/hearing team.

This demonstrates the Java 25 upgrade is viable for listing and is ready for QA.

### What this is **not**
This is a **spike / proof-of-work**, not a production-ready merge. To get the code to production, the listing team must **review and ratify the technical decisions** described in section 2 — several of them are deliberately pragmatic choices that keep the build green but where the team may prefer a different long-term approach. **The listing team are the ultimate owners of these decisions.**

### What the listing team needs to decide (headline)
See section 2 for full detail. In short:

| # | Decision | Why it matters | Suggested effort |
|---|----------|----------------|------------------|
| D1 | `Hearing` entity "native-load-only" virtual-column design | Root cause behind several of our changes; affects query + test code | Medium–High (design) |
| D2 | Lazy-loading strategy for the hearings query (`enable_lazy_load_no_trans` vs fetch-joins/DTOs) | Performance & correctness of the search endpoints | Medium |
| D3 | Where the MI schema-contract test should live (currently disabled) | Cross-team contract testing; a coupling question | Low–Medium (cross-team) |
| D4 | Keep the query "properties-JSON" approach (Option B) or fetch associations properly | Maintainability of the query view | Low–Medium |
| D5 | `totalCount` pagination mapping under Hibernate 6 | Pagination correctness on search endpoints | Low–Medium |

None of these block QA testing; they are choices for the production hardening pass.

### Temporary items that MUST be resolved before production
See section 4. The most important: the **MI schema-contract test is disabled** (waiting on a downstream release), and a Hibernate lazy-loading setting was enabled globally. Both are clearly marked in the code.

---

## 2. Decisions the listing team must make

Each item below explains **what we found**, **what we did (and why)**, and the **options** the team can choose from.

### D1 — The `Hearing` entity is "native-load-only" (virtual columns)

**Finding.** The `Hearing` entity maps four columns that **do not exist in the database** — they exist only as aliases produced by the hand-written native queries:
`hearing_date`, `totalcount`, `hearing_day_count`, `hearing_day_position`. They are mapped `@Column(insertable=false, updatable=false)`.

Because they are mapped columns, **any Hibernate entity-level `SELECT` of `Hearing`** (JPQL `SELECT h FROM Hearing h`, `em.find`, a lazy association reload, a merge existence-check) generates SQL that references those non-existent columns and **fails** with `column ... does not exist`. Under the previous Hibernate 5 / DeltaSpike stack this was tolerated in more situations; **Hibernate 6 is stricter**.

**What we did (pragmatic, keeps behaviour).** We treated `Hearing` as *native-load-only*:
- `HearingRepository.save()` = persist-if-not-contained (no merge existence-`SELECT`).
- `HearingRepository.remove()` = native reload-if-detached.
- `findBy`/`findByHearingId` = native query, not `em.find`.
- Test cleanup uses native `TRUNCATE ... CASCADE` instead of `findAll()` (which is JPQL and fails).
- The query view no longer walks the entity's lazy collections (see D4).

**Options for production:**
1. **Keep native-load-only** (current). Lowest change, but fragile — every future author must remember not to trigger an entity-level `Hearing` load.
2. **Map the virtual columns as `@Formula` or `@Transient`** so entity `SELECT`s don't reference them. *Care needed:* the native queries alias these names and rely on them mapping back to the entity fields — this must keep working.
3. **Split read models** — a separate lightweight projection/DTO for query responses, keeping `Hearing` as a clean persistable entity.

**Recommendation:** option 2 or 3 for long-term robustness; option 1 is acceptable short-term (it is what ships on this branch).

### D2 — Lazy-loading strategy for the hearings query

**Finding.** The hearings search/query path loads `Hearing` via native queries (which do **not** fetch associations), then the query view / filters walk the `@OneToMany` collections (`listedCases`, `courtApplications`) after the persistence session has closed → `LazyInitializationException` → HTTP 500.

**What we did.** Enabled `hibernate.enable_lazy_load_no_trans=true` in the production `persistence.xml` (`listing-viewstore-persistence`). This restores the pre-upgrade behaviour (lazy loads succeed outside a transaction via a temporary session). Peer contexts (prosecution-casefile, material, …) do **not** use this — their query designs differ — so it is specific to listing's native-query-plus-entity-walk pattern.

**Options for production:**
1. **Keep `enable_lazy_load_no_trans=true`** (current). Simple; the cost is a short-lived temporary session per lazy access (potential N+1).
2. **Fetch-joins / entity graphs** so associations are loaded within the query transaction.
3. **DTO projections** so the query never returns managed entities.

**Recommendation:** acceptable to ship option 1 for the spike; the team should decide whether the temp-session cost is acceptable at production load or move to option 2/3.

### D3 — MI schema-contract test (currently disabled)

**Finding.** `listing-event-listener` unpacked and ran a **consumer-driven contract test** published by **mi-reportdata** (`uk.gov.moj.cpp.mi.reportdata:mireportdata-listing-event-listener:tests`, class `ListingSchemaContractValidationTest`). Its latest *released* version is a Java-17/`javax` artifact and fails under Java 25 (`NoClassDefFoundError: javax/json/JsonValue`). There is no released Jakarta build yet. This was listing's **only** dependency on mi-reportdata, and it is **test-scoped only** (never affected production code).

**What we did.** **Disabled** it — commented out the `maven-dependency-plugin` unpack in `listing-event-listener/pom.xml` with a prominent "LISTING TEAM ACTION REQUIRED" marker (version left at `RELEASE` so it self-heals when the Jakarta build is published). The test source itself is already Jakarta-migrated on the mi-reportdata side; we verified it **passes** against listing's schemas when run with that Jakarta build.

**Options for production:**
1. **Re-enable once mi-reportdata publishes a Jakarta 25.104.x build** (`RELEASE` will resolve to it automatically — uncomment the plugin).
2. **Re-home the contract test** — having listing's build pull a test artifact from a *downstream* consumer is an awkward coupling; the listing/MI teams may prefer to own the contract differently.

**Recommendation:** option 1 as the immediate unblock; consider option 2 as a cross-team improvement.

### D4 — Query view: properties-JSON vs entity associations

**Finding.** `HearingQueryView.removedReViewHearings` walked `hearing.getListedCases()` / `getCourtApplications()`. Those child entities have (default-eager) `@ManyToOne` back-references to `Hearing`, so walking them forced a full-column `Hearing` entity `SELECT` — which fails (see D1).

**What we did (Option B — safest, no entity change).** Rewrote the method to derive the same removal rule (`≥1 listed case AND type == "Review" AND ≥1 court application`) from the `hearing.getProperties()` **JSON blob** instead of the entity collections. The response converter already worked off the properties JSON, so no other change was needed.

**Options for production:** keep the JSON-based approach, or (if D1 option 2/3 is taken) fetch the associations properly and walk the object graph. Either is fine; this was chosen because it is low-risk and touches only query code.

### D5 — `totalCount` pagination under Hibernate 6

**Finding.** The paginated native queries select `count(*) OVER() as totalCount`. Under Hibernate 6 this native alias **does not map** back to the `Hearing.totalCount` field (it returns null); it worked under Hibernate 5 / DeltaSpike. This is used by `HearingQueryView` / `RangeSearchQuery` pagination.

**What we did.** Documented it. It is **not** load-bearing for the tests that currently run (they assert rows/ids, not the total count), so the suite is green — but production pagination that relies on `totalCount` needs a fix.

**Options for production:** add an explicit `@SqlResultSetMapping` for the native result (mapping the `totalCount` alias), or align the entity `@Column` name/case with the alias. The team should confirm which query paths depend on `totalCount` and validate them.

---

## 3. Technical change log (by area)

### 3.1 Build, versions, tooling
- Target: **Java 25**, **WildFly 40**, **Jakarta EE 11**.
- Parent `service-parent-pom:25.104.0-M6`, `coredomain 25.104.0-M6`; project version `25.104.0-M1-SNAPSHOT`.
- Builds under `mvn clean install` with the enforcer **on** and **no SNAPSHOT dependencies**.
- JaCoCo 0.8.14 (Java 25 class files); RAML/messaging generator plugins given `jakarta.xml.bind:jakarta.xml.bind-api:2.3.2` on their plugin classpath (the RAML parser still uses the pre-EE9 `javax.xml.bind.*` namespace).

### 3.2 javax → jakarta
- `javax.*` EE APIs → `jakarta.*` across all modules (Java-SE `javax.*` such as `javax.sql`, `javax.xml.datatype`, `javax.annotation.Nullable` intentionally retained).
- `beans.xml` → Jakarta EE 4.0; `persistence.xml` → Jakarta 3.0.
- `javaee-api` → `jakarta.jakartaee-api`; `jaxb-api` → `jakarta.xml.bind`.

### 3.3 Persistence: DeltaSpike → JPA
- All **6 viewstore repositories** converted from DeltaSpike to plain JPA (`@ApplicationScoped`, `@PersistenceContext EntityManager`, built-in methods implemented explicitly).
- `HearingRepository`: all 17 native `@Query` methods → `entityManager.createNativeQuery(...)`. `save` / `remove` follow the native-load-only model (see D1).
- **jsonb**: removed `com.vladmihalcea:hibernate-types-43` (listing was the only user); `Hearing.properties` / `PublishedCourtList` now use Hibernate 6 native `@JdbcTypeCode(SqlTypes.JSON)`; removed the `pg-uuid` `@TypeDef` (Hibernate 6 maps `UUID` natively).

### 3.4 Hibernate 6 native-query fixes
- **Untyped null in optional filters:** `(?N is null or col = cast(?N ...))` failed under Hibernate 6 when the argument is null (PostgreSQL cannot infer the parameter type). Fixed by casting the null-check itself: `(cast(?N as varchar) is null or ...)`. Applied to every optional-filter parameter check (column null-checks left untouched).
- **`uuid` aliases:** the two jsonb-list synthetic queries (`findHearingsForPublicStandardList`, `findHearingsForAlphabeticalList`) return a string-literal `id` and a varchar-bound `court_centre_id`; Hibernate 6 rejects `String → UUID`. Fixed with `cast(... as uuid)` on those aliases.
- **`totalCount`** — see D5.

### 3.5 Test harnesses (OpenEJB/DeltaSpike → real PostgreSQL)
- Removed OpenEJB / DeltaSpike test-control. Repository ITs and the large `PersistenceTestsIT` (~2,970 lines, 56 tests) now use the framework's `HibernateTestEntityManagerProvider` against **docker PostgreSQL** (`listingviewstore`, production Liquibase schema, `hbm2ddl=none`). Tests roll back per test.
- JUnit 4 → JUnit 5 for the migrated ITs.
- `listing-command-handler` unit tests: expectations aligned to the framework's canonical `ZoneOffset.UTC` (`…Z`) form rather than `ZoneId.of("UTC")` (`…Z[UTC]`) — a JSON round-trip normalisation change, not a behaviour change.

### 3.6 Event modules
- **event-listener:** MI schema-contract test disabled (see D3). Removed a dangling DeltaSpike `BeanManagedUserTransactionStrategy` alternative from `listing-viewstore-persistence`'s `beans.xml` that otherwise broke WAR deployment (`WELD-000123`).
- **event-processor (XHIBIT):** replaced the pre-EE9 `com.sun.xml.bind:jaxb-impl/core` with `org.glassfish.jaxb:jaxb-runtime` (`jaxb.runtime.version=4.0.5`); restored the fixed XHIBIT namespace prefixes (`cs`/`p2`/`apd`/`xsi`) via a marshal-time `NamespacePrefixMapper` (the old `jaxb2-namespace-prefix` codegen add-on is javax-only and was dropped); added `jakarta.xml.bind-api` to the messaging/rest generator plugin classpaths.

### 3.7 Query layer
- `listing-query-api`: `org.drools.core.util.StringUtils.EMPTY` → `org.apache.commons.lang3.StringUtils.EMPTY`; nullness annotations backed by a (BOM-managed) `com.google.code.findbugs:jsr305` dependency (`javax.annotation.Nullable`/`Nonnull` — no Jakarta successor exists); checkerframework `@NonNull` → `javax.annotation.Nonnull`.
- `HearingQueryView` LazyInit + Option B — see D2 / D4.

### 3.8 Test tooling
- `junit-platform-launcher` aligned to `1.14.3` (it must match `junit-platform-engine`; the old hard-coded `1.9.2` broke Surefire/Failsafe test discovery). *Follow-up:* the shared `cp-maven-common-bom` should import `org.junit:junit-bom` so the launcher stays aligned estate-wide (change prepared, to be raised at the next platform milestone).

---

## 4. Temporary items to resolve before production

| Item | Where | Action |
|------|-------|--------|
| MI schema-contract test disabled | `listing-event-listener/pom.xml` (commented block with marker) | Re-enable once mi-reportdata publishes a Jakarta build (see D3) |
| `enable_lazy_load_no_trans=true` | `listing-viewstore-persistence/.../persistence.xml` | Ratify or replace with fetch-joins/DTOs (see D2) |
| Query "properties-JSON" workaround | `HearingQueryView.removedReViewHearings` | Ratify or refactor with D1 (see D4) |
| `totalCount` mapping | native queries / query view | Add `@SqlResultSetMapping` or equivalent (see D5) |
| `Hearing` virtual-column design | `Hearing` entity | Decide long-term approach (see D1) |

All temporary items are commented in the code with the reason.

---

## 5. Environment & prerequisites

- **Java 25** (Temurin) — the pipeline agent for this track is `ubuntu-j25`.
- **WildFly 40** hosts the Jakarta WAR successfully in the local docker stack. *To confirm during hardening:* WildFly 40 must expose the glassfish `NamespacePrefixMapper` class to the deployment (used for XHIBIT XML marshalling) — it worked in the local stack; verify on the target runtime image.
- **PostgreSQL** — the persistence ITs use the stack-provisioned `listingviewstore` DB (schema applied by the stack's Liquibase runner). No manual database setup is required when using `runIntegrationTests.sh`.
- **Docker** — `runIntegrationTests.sh` bootstraps the full stack (`CPP_DOCKER_DIR` must point at a local `cpp-developers-docker` checkout).

---

## 6. Where to look in the code

| Area | Module |
|------|--------|
| Repositories (DeltaSpike→JPA, native queries) | `listing-viewstore/listing-viewstore-persistence` |
| Persistence ITs (`PersistenceTestsIT`) | `listing-integration-test-persistence` |
| Query view / lazy-load / Option B | `listing-query/listing-query-view` |
| Query API (jsr305, drools) | `listing-query/listing-query-api` |
| XHIBIT JAXB / namespace prefixes | `listing-event/listing-event-processor` |
| MI contract test (disabled) | `listing-event/listing-event-listener` |
| Production `persistence.xml` / `beans.xml` | `listing-viewstore/listing-viewstore-persistence/src/main/resources/META-INF` |

---

*Questions on any decision above can go to Platform Engineering; the intent of this document is to give the listing team everything needed to choose the right production approach themselves.*

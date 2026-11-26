# Spring Boot Migration: listing-query → Standalone Service

**Status:** Proposal | **Date:** 2026-03-25

## Problem

The listing-query module is tightly coupled to the CPP framework (Wildfly, CDI, DeltaSpike, RAML, Envelope/JsonEnvelope). This creates:

1. **Slow startup** — Wildfly WAR deployment adds overhead vs embedded Tomcat/Netty
2. **Framework lock-in** — DeltaSpike repositories, `@Handles` annotations, `Requester` pattern, and RAML-generated resource classes are all CPP-framework-specific
3. **Monolithic deployment** — Query and command share the same Wildfly instance despite CQRS separation
4. **Testing friction** — Integration tests require cpp-developers-docker or Tilt for the full Wildfly stack
5. **Developer experience** — No hot-reload, slow feedback loops, no Spring ecosystem tooling

## Proposal

Create a new Spring Boot application (`listing-query-springboot`) that serves all 19 existing query endpoints. The current listing-query module becomes a thin proxy that forwards requests to the new Spring Boot app during the migration period.

### Architecture

```
                  ┌──────────────────────────────────┐
   UI / Other     │       listing-query (Wildfly)     │
   Contexts  ───► │   (proxy — forwards to Spring)    │
                  └──────────────┬───────────────────┘
                                 │ HTTP forward
                                 ▼
                  ┌──────────────────────────────────┐
                  │  listing-query-springboot (new)   │
                  │  ┌────────────────────────────┐  │
                  │  │  REST Controllers           │  │
                  │  │  (Spring MVC)               │  │
                  │  ├────────────────────────────┤  │
                  │  │  Service Layer              │  │
                  │  │  (business logic)           │  │
                  │  ├────────────────────────────┤  │
                  │  │  Spring Data JPA            │  │
                  │  │  Repositories               │  │
                  │  ├────────────────────────────┤  │
                  │  │  WebClient / RestClient     │  │
                  │  │  (cross-context calls)      │  │
                  │  └────────────────────────────┘  │
                  │         PostgreSQL (viewstore)    │
                  └──────────────────────────────────┘
```

## Requirements

### R1 — Functional Parity

All 19 query endpoints must return identical responses. No behavioural changes.

| # | Endpoint | Method | Action |
|---|---|---|---|
| 1 | `/hearings` | GET | `listing.search.hearings` |
| 2 | `/hearings/{id}` | GET | `listing.search.hearing` |
| 3 | `/hearings/range-search` | GET | `listing.range.search.hearings` |
| 4 | `/hearings/range-search` | GET | `listing.range.search.hearings.court.calendar` |
| 5 | `/hearings/available-search` | GET | `listing.available.search.hearings` |
| 6 | `/hearings/allocated-and-unallocated` | GET | `listing.allocated.and.unallocated.hearings` |
| 7 | `/hearings/any-allocation` | GET | `listing.any-allocation.search.hearings` |
| 8 | `/hearings/cotr-search` | GET | `listing.cotr.search.hearings` |
| 9 | `/hearings/unscheduled` | GET | `listing.unscheduled.search.hearings` |
| 10 | `/hearings/range-search` (judge) | GET | `listing.range.search.hearings.for.judge.list` |
| 11 | `/hearingSlots` | GET | `listing.search.hearing.slots` |
| 12 | `/sessionAvailabilityValidation` | POST | `listing.validate.session.availability` |
| 13 | `/courtlist` | GET | `listing.search.court.list` |
| 14 | `/courtlistpayload` | GET | `listing.search.court.list.payload` |
| 15 | `/courtListPublishStatus/{id}` | GET | `listing.court.list.publish.status` |
| 16 | `/cases/by-person-defendant-and-hearingDate` | GET | `listing.get.cases-by-person-defendant` |
| 17 | `/cases/by-organisation-defendant-and-hearingDate` | GET | `listing.get.cases-by-organisation-defendant` |
| 18 | `/cache-refdata-courtrooms/*` | GET/POST | `listing.update.add-courtroom`, `close-courtroom`, `refresh` |
| 19 | `/hearings/download-hearing-csv-report` | GET | `listing.query.download-hearing-csv-report` |

### R2 — Cross-Context Service Calls

The query module calls 5 external services via the CPP `Requester` pattern. These must be replaced with Spring `WebClient` or `RestClient` HTTP calls.

| Service | Current (Requester) | Spring Boot Replacement |
|---|---|---|
| **courtscheduler** | `CourtSchedulerServiceAdapter` (Apache HttpClient) | `WebClient` to courtscheduler REST API |
| **progression** | `ProgressionService` → `Requester` | `WebClient` to progression query API |
| **defence** | `HearingQueryApi` → `Requester` | `WebClient` to defence query API |
| **reference-data** | `ReferenceDataService` → `Requester` | `WebClient` to reference-data query API |
| **users-groups** | `UsersGroupsService` → `Requester` | `WebClient` to users-groups query API |
| **document-generator** | `DocumentGeneratorClient` (platform lib) | `WebClient` to document-generator API |

**Challenge:** The `Requester` pattern resolves service URLs at runtime via JNDI. Spring Boot will need explicit service URL configuration (environment variables or service discovery).

### R3 — Database Access

The viewstore is PostgreSQL with Hibernate/JPA and DeltaSpike Data repositories. Migration path:

| Current | Spring Boot |
|---|---|
| DeltaSpike `@Repository` interfaces | Spring Data JPA `JpaRepository` interfaces |
| Native SQL via `@Query` (DeltaSpike) | `@Query` (Spring Data JPA) — syntax is nearly identical |
| `HearingJdbcRepository` (raw JDBC) | `JdbcTemplate` or `NamedParameterJdbcTemplate` |
| `CourtListPublishStatusJdbcRepository` | `JdbcTemplate` |
| Hibernate JSONB via `JsonNodeBinaryType` | Hypersistence `JsonType` or custom `AttributeConverter` |
| `PersistenceUnit` via `persistence.xml` | Spring auto-configuration via `application.yml` |

**Key entities:** `Hearing`, `HearingDays`, `ListedCases`, `CourtApplications`, `Notes`, `CacheRefDataCourtroom`, `CaseByDefendant`, `PublishedCourtList`

### R4 — Security / Access Control

| Current | Spring Boot |
|---|---|
| DRL (Drools) files for per-endpoint authorization | Spring Security with custom `@PreAuthorize` or filter chain |
| `CJSCPPUID` header for user identity | Preserve same header; extract in Spring filter |
| `capability-manifest.json` | Not needed (Spring Boot manages its own capabilities) |

### R5 — Proxy Mode in listing-query (Wildfly)

During migration, the existing listing-query-api WAR acts as a reverse proxy:

- Each `@Handles` method in `HearingQueryView` and `HearingQueryApi` is modified to forward the request to the Spring Boot app via HTTP
- The proxy adds the `CJSCPPUID` header and passes all query parameters
- If the Spring Boot app is unreachable, fall back to the original implementation (feature-flagged)
- Feature flag: `listing.query.springboot.enabled` (default: `false`)
- Base URL: `listing.query.springboot.baseUrl` (default: `http://localhost:8090`)

### R6 — Observability

| Concern | Implementation |
|---|---|
| Health checks | Spring Actuator `/actuator/health` |
| Metrics | Micrometer → Prometheus (align with cpp-environment-dashboard) |
| Logging | SLF4J + Logback (same as current) |
| Tracing | Micrometer Tracing (propagate existing correlation IDs) |

### R7 — Deployment

| Concern | Implementation |
|---|---|
| Docker image | Multi-stage Dockerfile (build + JRE-slim runtime) |
| Helm chart | New chart in `cpp-helm-chart` or sub-chart of existing listing chart |
| AKS | Deploy alongside listing-service in same namespace |
| Environments | Feature-flagged rollout: dev → sit → ste → nft → prp → prd |

## Migration Steps

### Phase 1 — Scaffold & Core (estimate: 3-5 days)

1. **Create Maven module** `listing-query-springboot` under cpp-context-listing
   - Spring Boot 3.x, Java 17, Spring Web, Spring Data JPA, Spring Actuator
   - PostgreSQL driver, Hypersistence Utils (JSONB support)
   - Add as a module in the root `pom.xml`

2. **Configure datasource** in `application.yml`
   - Point to same PostgreSQL viewstore database
   - Configure connection pool (HikariCP)
   - Hibernate dialect for PostgreSQL

3. **Migrate JPA entities** from `listing-viewstore-persistence`
   - Copy entity classes (`Hearing`, `HearingDays`, `ListedCases`, etc.)
   - Replace `JsonNodeBinaryType` with Hypersistence `JsonType`
   - Replace `PostgresUUIDType` with Hibernate 6 native UUID support
   - Keep same table/column mappings

4. **Create Spring Data JPA repositories**
   - Translate DeltaSpike `@Query` annotations to Spring Data equivalents
   - Migrate `HearingJdbcRepository` to `JdbcTemplate`
   - Migrate `CourtListPublishStatusJdbcRepository` to `JdbcTemplate`
   - Add pagination support via `Pageable`

### Phase 2 — Service Layer (estimate: 3-5 days)

5. **Migrate cross-context service clients**
   - Create `WebClient` beans for each external service
   - `CourtSchedulerClient` — replace `HearingSlotsService` + `CourtSchedulerServiceAdapter`
   - `ProgressionClient` — replace `ProgressionService`
   - `ReferenceDataClient` — replace `ReferenceDataService` + `CacheRefDataCourtroomLoader`
   - `DefenceClient` — replace defence requester calls
   - `UsersGroupsClient` — replace `UsersGroupsService`
   - `DocumentGeneratorClient` — replace platform lib client
   - Configure base URLs via environment variables

6. **Migrate business logic**
   - `RangeSearchQuery` → `RangeSearchService`
   - `CourtListService` → `CourtListService` (same name, Spring `@Service`)
   - `HearingToJsonConverter` and other converters → keep as-is or replace with Jackson serialization
   - `HearingCsvReportService` → `HearingCsvReportService`
   - Template assemblers for document generation → keep as-is

### Phase 3 — REST Controllers (estimate: 2-3 days)

7. **Create Spring MVC controllers**
   - `HearingController` — hearing search endpoints (9 endpoints)
   - `CourtListController` — court list endpoints (3 endpoints)
   - `CaseController` — case search endpoints (2 endpoints)
   - `HearingSlotController` — hearing slots and session availability (2 endpoints)
   - `CacheController` — courtroom cache management (3 endpoints)
   - `ReportController` — CSV report download (1 endpoint)
   - Ensure request/response formats match existing RAML contracts exactly

8. **Add security**
   - Spring Security filter chain
   - Extract `CJSCPPUID` from request header
   - Replicate DRL authorization rules as `@PreAuthorize` expressions or custom voters
   - Add `SecurityContext` holder for user identity

### Phase 4 — Proxy & Testing (estimate: 3-5 days)

9. **Implement proxy in listing-query-api (Wildfly)**
   - Add `HttpClient` dependency to listing-query-api
   - Create `SpringBootQueryProxy` service class
   - Modify each `@Handles` method to check feature flag and forward
   - Preserve all headers, query params, and path variables
   - Add circuit breaker / fallback to original implementation

10. **Contract testing**
    - For each endpoint, create a test that:
      1. Sends a request to the Wildfly proxy
      2. Sends the same request directly to Spring Boot
      3. Asserts both responses are identical (JSON diff)
    - Use existing integration test payloads as seed data
    - Run against a shared PostgreSQL instance

11. **Integration testing**
    - Add Testcontainers-based tests for Spring Boot module
    - PostgreSQL + Liquibase (reuse existing `listing-viewstore-liquibase`)
    - WireMock for cross-context service stubs (courtscheduler, progression, etc.)

### Phase 5 — Deployment & Rollout (estimate: 2-3 days)

12. **Dockerize**
    - Multi-stage Dockerfile
    - Add to existing CI/CD pipeline (`context-validation.yaml`)
    - Push to Azure Container Registry

13. **Helm chart**
    - Create chart for `listing-query-springboot`
    - Configure service, ingress, health probes
    - Environment-specific values (DB URL, service URLs, feature flags)

14. **Rollout**
    - Deploy to dev with feature flag `listing.query.springboot.enabled=false`
    - Enable proxy on dev, run smoke tests
    - Progressive rollout through environments
    - Monitor latency, error rates, response parity
    - Once stable: remove proxy, point consumers directly to Spring Boot

### Phase 6 — Cleanup (estimate: 1-2 days)

15. **Remove proxy code** from listing-query-api
16. **Update RAML** to point to new base URL (or keep reverse proxy in ingress)
17. **Update documentation** — journeys.md, integration-map.mmd
18. **Archive** old listing-query-view handler classes

## Tech Stack (Spring Boot App)

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.x |
| Web | Spring MVC (or WebFlux for reactive) |
| Database | Spring Data JPA + JdbcTemplate |
| Connection Pool | HikariCP |
| JSONB | Hypersistence Utils |
| HTTP Clients | Spring WebClient (reactive) or RestClient (blocking) |
| Security | Spring Security |
| Observability | Spring Actuator + Micrometer |
| Testing | JUnit 5, Testcontainers, WireMock, Spring MockMvc |
| Build | Maven, Spring Boot Maven Plugin |
| Deployment | Docker, Helm, AKS |

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Response format differences between old and new | High | High | Contract tests comparing JSON responses field-by-field |
| Cross-context service URL configuration | Medium | Medium | Use K8s service DNS (`http://listing-courtscheduler:8080`) |
| JSONB handling differences between Hibernate versions | Medium | Medium | Hypersistence Utils handles this; test with production data snapshots |
| DRL authorization rules are complex to translate | Medium | High | Extract rules into a shared config; start with permissive and tighten |
| Performance regression on complex queries | Low | High | Benchmark with production query patterns before rollout |
| Wildfly proxy adds latency during migration | Medium | Low | Proxy period should be short; connection pooling mitigates |
| Two apps reading same viewstore | Low | Low | Query module is read-only; no write conflicts |

## Key Classes to Migrate

### From listing-query-view (31 @Handles methods in HearingQueryView)

Split into focused services:

| Current Handler Method | New Spring Service |
|---|---|
| `searchHearings()`, `searchAvailableHearings()`, `searchAllocatedAndUnallocatedHearings()`, `searchHearingsWithAnyAllocationState()`, `searchUnscheduledHearings()`, `searchHearingsForCotr()` | `HearingSearchService` |
| `rangeSearchHearings()`, `rangeSearchHearingsForCourtCalendar()`, `searchHearingsForJudge()` | `RangeSearchService` |
| `retrieveCourtList()`, `getCourtListContent()`, `getPublishedCourtLists()`, `getCourtListPublishStatus()` | `CourtListService` |
| `getCasesByDefendantAndHearingDate()` | `CaseSearchService` |
| `getHearingById()` | `HearingDetailService` |
| `generateHearingCsvReport()` | `HearingReportService` |

### From listing-query-api

| Current Class | New Spring Class |
|---|---|
| `HearingQueryApi` (RAML-generated) | `HearingController` (@RestController) |
| `DefaultQueryApiHearingSlotsResource` | `HearingSlotController` |
| `DefaultQueryApiSessionAvailabilityValidationResource` | `SessionAvailabilityController` |
| `CacheRefDataCourtroomApi` | `CacheController` |
| `DocumentGeneratorClient` | `DocumentGeneratorWebClient` |
| DRL access control rules | Spring Security config |

## Dependencies on Other Work

- **Testcontainers migration** (see `tilt-testcontainers-migration-analysis/`) — would simplify Spring Boot integration testing
- **Parent POM** — Spring Boot has its own parent; may need to use `spring-boot-dependencies` BOM instead of inheriting from `cpp-platform-maven-service-parent-pom`
- **listing-common** — Shared classes (`CourtSchedulerServiceAdapter`, domain objects, CSV constants) will need to be either:
  - Kept as a dependency (if no CDI annotations)
  - Duplicated into the Spring Boot module (if tightly coupled to CDI)
  - Refactored into a framework-agnostic shared JAR

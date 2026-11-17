# HMCTS SDLC Pipeline — Orchestrator

## Project context
This is an HMCTS engineering project. All work must comply with HMCTS engineering standards,
GDS Service Manual principles, and MOJ security and accessibility requirements.

Always load the following before any pipeline stage:
- `context/tech-stack.md`
- `context/hmcts-standards.md`
- `context/azure-cloud-native.md` — Cloud-Native posture and Shared Responsibility Model on Azure.
- `context/logging-standards.md` — mandatory JSON logging for Spring Boot services.

Load on demand when relevant:
- `context/azure-sdk-guide.md` — when working on any Azure service integration.
- `context/cloud-adoption-rationale.md` — only when lock-in or cloud-cost objections surface, or when an ADR weighs those trade-offs. Do not auto-load.

> **Note:** `context/`, `agents/`, `skills/` and `docs/pipeline/` are **not in this repo**. They come
> from the HMCTS SDLC pipeline marketplace plugins (or the pipeline template repo). If a referenced
> file can't be found, say so and ask; do not invent its contents. Create `docs/pipeline/` when the
> first artefact is written.

---

## This repository — `cpp-context-listing`

The CPP **Listing** context: a WildFly WAR built on the HMCTS/CJS microservice framework
(CQRS + event sourcing, RAML-defined APIs, JMS, DeltaSpike, Liquibase). It is **not** Spring Boot
today. Branch `mbd-refactor` is migrating it to Spring Boot; see `docs/MBD_REFACTOR_PROMPT.md`.
The pipeline above and the Spring Boot rules in the Hard rules apply to that migration work.

**Reference Spring Boot project:** `/Users/hasanshaik/cpp/github/cpp-context-listing-courtscheduler`
(a local checkout with its own `CLAUDE.md`). The migration copies its structure and conventions:
- Spring Boot 4.1.x on Java 25, built with Gradle.
- OpenAPI spec-first: the controller interfaces are generated from the spec.
- `RequestSchemaValidationFilter`, `cp-auth-rules-filter` and Spring Data JPA with Liquibase.

Read it before designing or writing migration code. Wherever courtscheduler differs from the HMCTS
Spring Boot template or the Hard rules, record the difference in an ADR. Courtscheduler has no
domain JMS messaging, so the messaging patterns have to be designed fresh (see
`docs/MBD_REFACTOR_PROMPT.md` §2, §5 and §7).

- **Current build (before migration):** Java 17, Maven. Parent `uk.gov.moj.cpp.common:service-parent-pom`, groupId
  `uk.gov.moj.cpp.listing`. CI is `azure-pipelines.yaml` (templates from `hmcts/cpp-azure-devops-templates`).
- **Modules:**
  - `listing-command` (`-api`, `-handler`): command side.
  - `listing-domain` (`-aggregate`, `-common`, `-event`, `-xhibit`): aggregates and domain events.
  - `listing-event` (`-listener`, `-processor`): event listener (writes the viewstore) and event processor.
  - `listing-query` (`-api`, `-view`): query side.
  - `listing-viewstore` (`-liquibase`, `-persistence`): read model.
  - `listing-service`: the deployable WAR.
  - Also `listing-common`, `listing-json`, `listing-event-sources`, `listing-healthchecks`.
- **API contracts (RAML):**
  - `listing-command/listing-command-api/src/raml/listing-command-api.raml`
  - `listing-command/listing-command-handler/src/raml/listing-command-handler.messaging.raml`
  - `listing-query/listing-query-api/src/raml/listing-query-api.raml`
- **Unit tests:** `mvn clean install`.
- **Integration tests:** `mvn clean && ./runIntegrationTests.sh`. Needs `CPP_DOCKER_DIR` pointing at
  a local checkout of `hmcts/cpp-developers-docker`. Optional arguments: `errorlog` (any unexpected
  server.log ERROR/WARN fails the test), `durations`, `nounit`, `nomidnightguard`. `-D` options are not
  forwarded. The script waits if 00:00 UTC is near, because runs that cross midnight fail.
  The tests live in `listing-integration-test`; also see `listing-integration-test-persistence`.
- **IT conventions:** follow `docs/development-guidelines/integration-test-guide.md`. For example, use
  `uk.gov.justice.services.messaging.JsonObjects`, not `javax.json.Json`. For payload-based tests, see
  `listing-integration-test/README-PAYLOAD-BASED-TESTING.md`.
- **Requests** need a `CJSCPPUID` header (see `README.md`).
- **Other docs:** `docs/development-guidelines/` (journeys, transactions, integration map),
  `docs/big-changes/`, `docs/future-improvements/`.

---

## Pipeline stages

Run stages in order. Do not skip or reorder. Halt at every human gate before proceeding.

| # | Stage          | Agent file                         | Gate   |
|---|----------------|------------------------------------|--------|
| 1 | Requirements   | agents/requirements-analyst.md     | Human  |
| 2 | Architecture & Design | agents/architecture-designer.md | Human|
| 3 | User Story     | agents/story-writer.md             | Human  |
| 4 | Test Specs     | agents/test-engineer.md            | Human  |
| 5 | Code           | agents/implementation.md           | Auto   |
| 6 | Code Review    | agents/code-reviewer.md            | Human  |
| 7 | Build & Test   | agents/ci-orchestrator.md          | Auto   |
| 8 | Deploy Sandbox | agents/deployer.md                 | Human  |

---

## Shared skills (available to all agents)

Skills split across the marketplace and this repo. Install the marketplace plugins once (see the plugin README Prerequisites section) — the file paths below resolve to pointer stubs or HMCTS overlays that reference the installed plugins.

| Skill file                              | Source                            | Use when                                      |
|-----------------------------------------|-----------------------------------|-----------------------------------------------|
| skills/write-acceptance-criteria.md     | marketplace: `bdd-workflow`       | Deriving testable ACs from any requirement    |
| skills/generate-bdd-specs.md            | marketplace: `bdd-workflow`       | Writing Cucumber/Gherkin feature files        |
| skills/accessibility-check.md           | marketplace: `accessibility-check` + HMCTS overlay | WCAG 2.1 AA review + GOV.UK Frontend guidance |
| skills/review-checklist.md              | marketplace: `review-checklist` + HMCTS overlay    | Code review checklist + Spring Boot / Azure / logging |
| skills/adr-template.md                  | marketplace: `adr-template`       | Recording any architecture decision           |
| skills/springboot-service-from-template/| local (HMCTS-specific)            | Standing up a new Spring Boot service from the HMCTS template |
| skills/springboot-api-from-template/    | local (HMCTS-specific)            | Standing up a new HMCTS Marketplace API spec repo |
| skills/cpp-test-authoring/              | local (HMCTS-specific)            | Writing or extending tests in `cpp-ui-e2e-serenity` (Serenity BDD/Cucumber) or `cpp-apitests` (JUnit 5 + REST Assured) |
| skills/export-design-artifact/          | local (HMCTS-specific)            | Exporting a self-contained HTML artifact (plan / decision / design-gap / comparison) from the bundled template gallery — **mandatory** for the implementation plan before Stage 5 |

---

## Artefact output convention

All pipeline artefacts are written to `/docs/pipeline/` in the repo:

```
docs/pipeline/
├── requirements.md
├── user-stories/
│   └── <story-id>.md
├── test-specs/
│   └── <story-id>.feature
├── adrs/
│   └── <NNN>-<title>.md
├── artifacts/
│   └── <NNN>-<slug>.html   # HTML artifacts (plan, decision, design-gap, comparison)
└── deploy-notes.md
```

HTML artifacts are produced via `skills/export-design-artifact/` from the bundled template gallery.
Each keeps its `<!-- claude-artifact: ... -->` marker (line 1 of `<head>`) so it is indexable, and
must be surfaced at the relevant human gate. An **implementation-plan** artifact is mandatory before
Stage 5 — see the Hard rules.

---

## Hard rules

- Never proceed past a human gate without explicit confirmation.
- Never invent requirements, ACs, or test data — flag unknowns as open questions.
- Every story must have a linked Jira ticket before the test stage begins.
- All code must pass the review checklist before CI is triggered.
- Accessibility (WCAG 2.1 AA) is non-negotiable for any user-facing output.
- Do not store PII, case data, or court reference numbers in artefacts or prompts.
- If confidence in a decision is low, write an ADR and surface it for review.
- **Export an HTML artifact** (via `skills/export-design-artifact/`) whenever a decision must be made,
  a plan needs improving, or a design gap is discovered — and **always export an implementation-plan
  artifact before Stage 5 (Code) begins**, even when the design is clean. No plan artifact at
  `docs/pipeline/artifacts/` → do not start coding.
- **Integration tests for new endpoints (backend features & bugfixes).** Any PR that adds or changes
  a REST endpoint / `@Handles` action / message-driven entry point MUST add **at least one integration
  test per new endpoint**, and the integration-test suite MUST be green when run locally — via
  `mvn clean && ./runIntegrationTests.sh` when that script exists at the repo root, otherwise the
  repo's documented IT command. Never weaken or skip an IT to go green.
- For Spring Boot services: the HMCTS templates (`service-hmcts-crime-springboot-template`, `api-hmcts-crime-template`) are the master source. Use `skills/springboot-service-from-template/` or `skills/springboot-api-from-template/` to adopt them — do not scaffold build files, Dockerfile, or logback config from scratch. Deviations require an ADR.
- JSON logging to stdout is mandatory for Spring Boot services. See `context/logging-standards.md`.
- Azure integrations use the Azure SDK via Managed Identity. Connection strings, SAS tokens, and account keys are not permitted in code, config, env vars, or Helm values. See `context/azure-sdk-guide.md`.

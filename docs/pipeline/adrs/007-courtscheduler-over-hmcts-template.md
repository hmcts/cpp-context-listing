# ADR 007 — Follow courtscheduler, not the HMCTS Spring Boot template

- **Status:** Accepted (P2.0, 2026-11-19)
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §1, §2, §6, §7.7, D8)

## Context

`CLAUDE.md` makes `service-hmcts-crime-springboot-template` the master source for Spring Boot build
files, Dockerfile and logback config. For the listing migration, `docs/MBD_REFACTOR_PROMPT.md`
(the only source of instructions for this work) says to follow the reference port
`cpp-context-listing-courtscheduler` and record each difference from the template here. **The
HMCTS template is not available locally** (neither the template repo nor the
`skills/springboot-service-from-template/` skill is in this workspace), so the comparison below is
against what the prompt and `context/logging-standards.md` (read from a sibling repo's copy) say
the template does; it should be re-checked against the template itself.

## Decision

Follow courtscheduler for:

1. **Build layout.** Gradle multi-project; root `build.gradle` (Boot 4.1.0, dependency-management
   1.1.7, openapi-generator 7.24.0, CycloneDX, git-properties, avast docker-compose, ben-manes,
   PMD only when named), Java 25 toolchain, BOMs (Boot, Testcontainers, JUnit 6), Lombok,
   `Europe/London` test timezone, reproducible archives, `bootJar` + `plain` jar, idempotent
   publish guard. Listing specifics: Gradle includes only the new modules (`listing-api`,
   `listing-integration-test-spring`) while Maven still builds the JEE modules (until P2.6).
2. **Dockerfile / `docker/startup.sh`** at the repo root (copied), plus an IT Dockerfile in
   `listing-api/docker/`. Only `startup.sh` is copied into the image because `docker/` also
   holds the WildFly Dockerfile.
3. **Config fragments.** `application.yaml` importing `application-{server-management,authz,audit,messaging}.yml`
   (`-datasource` arrives in P2.1), `application-integration.yaml`, every value `${LISTING_*:default}`.
4. **Logging.** Root `${LOG_LEVEL_ROOT:WARN}`, app `${LOG_LEVEL_LISTING:INFO}` (courtscheduler's
   levels). Courtscheduler has no logback config; listing adds `logback-spring.xml` with
   `LogstashEncoder` (JSON to stdout, banner off), see D12. Deviations from
   `context/logging-standards.md`: it names `logback.xml` with `LoggingEventCompositeJsonEncoder`
   and a fixed provider list, root `INFO`, and MDC `correlationId`/`requestId` from a tracing
   filter. Listing uses `LogstashEncoder` (same library, default field set incl. MDC) and root
   `WARN`; the correlation/request-id filter is not in P2.0.

Listing differs from courtscheduler where the listing legacy contract or the prompt requires it:

5. **Response envelope off by default.** Listing's generated query resources used
   `OkStatusEnvelopePayloadEntityResponseStrategy`: the body is the payload only and the envelope
   id goes in a `CPPID` header. So `listing.envelope.enabled` defaults to `false` and 2xx JSON
   responses get a `CPPID` header; `true` restores courtscheduler's `_metadata` wrapping.
6. **Schema-validation filter before authz** (order +20, not courtscheduler's +100): §5 of the
   prompt and the framework (JAX-RS reader interceptor ran before the access-control interceptor).
   The media type → schema file mapping comes from the spec (`x-json-schema`), and remote `$ref`s
   resolve from a classpath catalog (listing-json + core-domain schema jars). Schema failures add
   `validationErrors` next to `error`, as listing's `BadRequestExceptionMapper` did.
7. **Error shape everywhere.** Added handlers for 404/405/415 (courtscheduler's catch-all would
   give 500), `RequestRoutingGuardFilter` (406/404/405/415 before the auth filter), and
   `LegacyErrorAttributes` so the auth filter's `sendError` 401/403 bodies are `{"error":…}` too.
8. **Integration profile keeps JMS** (§9): Artemis is in the IT stack; `JmsAutoConfiguration` is not excluded.

## Consequences

- Reviewers can diff listing's build/config/plumbing against courtscheduler file by file.
- If the HMCTS template differs further (e.g. base image, logback providers, MDC filter), those
  gaps are tracked here and closed or accepted by P2.5.

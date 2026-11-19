# ADR 008 — Two legacy context roots from one Boot app: one spec, one context path

- **Status:** Accepted (P2.0, 2026-11-19); D11
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §4.1, §4.2, §6, §7.8, §15.10)

## Context

WildFly serves the command API at `/listing-service/command/api/rest/listing` and the query API at
`/listing-service/query/api/rest/listing` (two RAML files, one WAR). Both roots must stay unchanged
(§4). Spring Boot has one servlet context path per app. Options: (a) one context path
`/listing-service` with full paths in one spec; (b) two `DispatcherServlet`s / two specs.

## Decision

- **One OpenAPI spec**, `listing-api/src/main/resources/openapi/listing-api.openapi.yml`, ported 1:1
  from both RAML files (paths, methods, query parameters, vendor media types, status codes; request
  and query-response schemas named per media type in `x-json-schema`).
- **`server.servlet.context-path=/listing-service`**; spec paths start with
  `/command/api/rest/listing/…` or `/query/api/rest/listing/…`; `servers: [/listing-service]`.
- Actuator is therefore at **`/listing-service/actuator/*`** (health:
  `/listing-service/actuator/health`), as courtscheduler serves it under its context path.
- Where one path takes several vendor media types there is one operation and the controller
  dispatches on `Content-Type` (courtscheduler's `/validate` pattern). Tags group operations
  (`hearing-commands`, `hearing-queries`, …), one generated `*OpenApi` interface per tag.

## Consequences

- Clients, ingress and the existing ITs keep their URLs. One `bootJar`, one filter chain, one
  audit spec (`cp-audit-filter-springboot` takes a single spec path).
- Authz exclude prefixes and the DRL `path` attribute are relative to `/listing-service`.
- Bodies are `PassthroughObject` in the spec; the legacy JSON schemas stay the validation source.
  Generating typed models from the draft-04 schemas (remote `$ref`s) is not attempted.
- RAML `type: date` query parameters are plain strings (the framework passed them as strings), so
  no new date-format 400s are introduced.

# ADR 005 — Access control: framework Drools rules → cp-auth-rules-filter

- **Status:** Accepted (P2.0, 2026-11-19)
- **Context:** MBD refactor (`docs/MBD_REFACTOR_PROMPT.md` §4.5, §6, §7.5, §11.3, §15.5)

## Context

On WildFly, access control is the framework's `LocalAccessControlInterceptor`: it evaluates
`listing-command-api.drl` (29 rules) and `listing-query-api.drl` (21 rules) with
`Action.name` = the RAML `(mapping)` name of the request, and the framework's
`UserAndGroupProvider` (groups from usersgroups). No rule fired → 403.

The Spring app uses `cp-auth-rules-filter` 1.0.9, as courtscheduler does. It is a servlet filter
(order `HIGHEST_PRECEDENCE + 30`). It resolves `Action.name` (lower-cased) from the `Content-Type`
vendor token, then the first `Accept` vendor token, then `CPP-ACTION`, then `"<METHOD> <PATH>"`, and
passes attributes `method` and `path` (path within the `/listing-service` context). Its
`UserAndGroupProvider` offers `isMemberOfAnyOfTheSuppliedGroups` and `hasPermission`; groups come
from `usersgroups …/users/logged-in-user/permissions`.

## Decision

1. **Port the two DRL files** to `listing-api/src/main/resources/uk.gov.moj.cpp.listing.api.accesscontrol.drl/`,
   in courtscheduler's format (cp-auth-rules-filter imports and global). **Group lists are not
   changed.** `RuleConstants` is copied unchanged into `uk.gov.moj.cpp.listing.api.accesscontrol`
   (group names exactly as usersgroups returns them, §11.4).
2. **`Action.name` = vendor token** (REVIEW §3.3). Where the RAML mapping name differs from the
   media type's token, the rule matches the token and keeps the legacy name in a comment:
   - commands (Content-Type): `listing.mark-unallocated-hearing-as-duplicate` → `listing.duplicate-unallocated-hearing`,
     `listing.list-next-hearings-v2` → `listing.next-hearings-v2`, `listing.update-related-hearing` →
     `listing.related-hearing`, `listing.command.delete-hearing` → `listing.delete-hearing`;
   - queries (Accept): seven legacy actions share the token `listing.search.hearings` with
     different group lists (`/hearings`, `/hearings/available-search`, `/range-search`,
     `/allocated-and-unallocated`, `/unscheduled`, `/any-allocation`, `/cotr-search`), so those
     rules also match `attributes["path"]`. `listing.range.search.hearings.court.calendar` →
     `listing.search.hearings.court.calendar`.
3. **`isSystemUser($action)`** (8 command rules) does not exist in cp-auth-rules-filter. The
   framework implemented it as `isMemberOfAnyOfTheSuppliedGroups($action, "System Users")`
   (checked in `access-control-common-providers` 17.104.4 bytecode), so the rules call exactly that.
4. **`deny-when-no-rules: true`**, `action-required: false`. Four command media types in the RAML
   (`update-defendants-for-hearing`, `delete-offences-for-hearing`, `add-offences-for-hearing`,
   `update-offences-for-hearing`) have no legacy rule and no command-api handler: they were
   denied on WildFly and stay denied.
5. **Bypasses: exact parity (§15.5 default).** Four query resources were hand-written JAX-RS
   adapters that never ran the interceptor chain, so their DRL rules were never evaluated:
   `/hearingSlots`, `/hearings/download-hearing-csv-report`, `/sessionAvailabilityValidation` and
   `/courtScheduleDraftStatus`. They are in `authz.http.exclude-path-prefixes`. (§15.5 names only
   the first two; the other two bypass for the same reason — see the open question below.) Their
   rules stay in the DRL for reference.
6. **Client-chosen actions.** `AuthzActionGuardFilter` (from courtscheduler) hides `CPP-ACTION`
   and rejects several Accept vendor types with 406. `RequestRoutingGuardFilter` (new, order +15)
   resolves the Spring handler before the auth filter, so an unknown `Accept`/path/method gets
   406/404/405/415 as on JAX-RS, instead of the 403 that `deny-when-no-rules` would give.
7. **Tests.** `AccessControlRulesTest` evaluates the rules with the library's `DroolsAuthzEngine`
   and cross-walks every vendor media type in the OpenAPI spec to a rule (and every rule to an
   endpoint). `AuthorizationIntegrationTest` proves allow/deny end-to-end against WireMock.

## Consequences

- Same allow/deny for the same groups, provided usersgroups' *logged-in-user permissions*
  response carries the same group names as the *groups by user* query the framework used (§15.4).
  WireMock stubs use the production casing ("Listing Officers", "System Users").
- No Spring Security. A new endpoint needs a DRL rule for its vendor token, or it is 403.
- The rule files still read as the legacy action names (rule names unchanged), but matching is by
  token: renaming a media type means changing the rule.
- cp-auth-rules-filter 1.0.9 builds a new KieBase per evaluation (one "Start creation of KieBase"
  log line per request in the ITs) despite `reload-on-each-request: false`: a per-request cost to
  measure before M1 (courtscheduler has the same behaviour). Drools also warns that the DRL
  package does not match its folder; informational, as in courtscheduler.
- **Open:** keep `/sessionAvailabilityValidation` and `/courtScheduleDraftStatus` unprotected
  (parity), or enforce their rules (behaviour change → amend this ADR)?

# listing — J17 → J25 behavioural parity findings

Per the CTP parity guide (Confluence 1990371020) and the users-groups reference (PEG-3336).
J17 (`main`) is the source of truth.

## Context shape

Large JPA context (11 `@Entity`, DeltaSpike→JPA migration). The migration **preserves finder
contracts** and is parity-clean on the persistence BCs:

- The only `getSingleResult()` in the context is `PublishedCourtListRepository.count()` — a
  `SELECT COUNT(...)` that always returns exactly one row, so it never throws `NoResultException`.
  **Not a BC-01/02 finder.**
- Entity finders use `entityManager.find(...)` (null on no-match) and
  `getResultList()` — same null/list contracts as the J17 DeltaSpike repositories.
- No primitive `@Version`; no JPQL `!= null` in queries; no lazy-association access outside a tx.

The ~2.9k-line J17→J25 source diff is the mechanical DeltaSpike→JPA + `javax`→`jakarta` rewrite, not
a behavioural change. The 18 changed test JSON are **`main` advancing past the branch point**
(merge-base 2026-07-02; the changes are post-branch commits on `main`), not upgrade drift.

## BC catalogue disposition

| BC | Present? | Disposition |
|----|----------|-------------|
| BC-01/02 | No | N/A — `find`/`getResultList` finders (null↔null); the sole `getSingleResult` is a COUNT |
| BC-04 | No | N/A — no primitive `@Version` |
| BC-05 | No | N/A — no JPQL `!= null` |
| BC-06 | No | N/A — no lazy-collection access outside a tx |
| BC-07 | **Yes** | **Fixed** — removed `liquibase.hub.mode` from `listing-viewstore-liquibase/…/liquibase.properties`. J25 only. |
| BC-11 | No | N/A |
| BC-20 | **Yes — 2 kbases** | **Guarded** — `AccessControlRuleCountTest` for `COMMAND_API` (command-api) and `QUERY_API` (query-api). Both branches. |
| BC-24 | Runtime | Covered by ITs |

## Changes

- **BC-07:** removed `liquibase.hub.mode: off` (J25 only).
- **BC-20:** two rule-count guards (`COMMAND_API`, `QUERY_API`). Both branches.

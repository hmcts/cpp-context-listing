# HearingView `findHearings` SQL — Optimization Suggestions

Context: comparing `dev/sprdt-1116-latest` against `team/pasthearings`, the only functional
change was a rewrite of the past-hearings `findHearings` query in
`listing-viewstore/listing-viewstore-persistence/src/main/java/uk/gov/moj/cpp/listing/persistence/repository/HearingRepository.java`
(the overload without an `allocated` param, lines ~809-861). This document captures further
optimization opportunities identified on top of that change, split into code-level fixes and
DBA-facing index suggestions.

## 1. Code-level SQL fixes (no DBA involvement needed)

### 1a. Untuned sibling overload still on the old pattern
`HearingRepository.java` lines 740-792 — the overload **with** an `allocated` param — has the
exact same subquery structure the tuned query replaced (nested `OR` branches, redundant
`hearing`/`listed_cases` re-joins, `distinct`). This overload is on the **default, hot path**:
`HearingQueryView.java:439` calls it whenever `returnAllHearings` is `false`. It should get the
same `UNION`-based rewrite applied to the already-tuned overload.

### 1b. `UNION` → `UNION ALL` — ✅ done

The four branches are wrapped in `h.id IN (...)`. Membership is all that matters, so duplicate
`hearing_id` rows across branches don't affect the result. `UNION` forces Postgres to sort/hash
dedupe the combined branch output before the membership check; `UNION ALL` skips that pass
entirely. Applied in `HearingRepository.java` (lines ~830-850), verified passing via
`PersistenceTestsIT`.

### 1c. Redundant `hearingId` exclusion — ✅ done
The outer query already filters `(:hearingId IS NULL OR h.id != cast(...))`. Repeating
`hearing_id != :hearingId` inside three of the four UNION branches was dead weight — the outer
filter already excludes that row regardless. Dropped from the branches in
`HearingRepository.java`, verified passing via `PersistenceTestsIT`.

### 1d. Cast on `defendant.master_defendant_id` defeats indexing — ✅ done (tuned overload only)
The query did `cast(d.master_defendant_id as varchar) IN (:masterDefendantIdSet)`. The column
is `UUID`. Casting it to `varchar` turned the predicate into a per-row function evaluation,
which prevented Postgres from using a plain index on the column even if one exists. Fixed on the
tuned 7-param overload (no `allocated` param): `masterDefendantIdSet` is bound as `Set<UUID>` and
compared directly (`d.master_defendant_id IN (:masterDefendantIdSet)`). `HearingQueryView`
converts via `toMasterDefendantUuidSet`, mapping the `''` no-op placeholder to the nil UUID
(`00000000-0000-0000-0000-000000000000`). The 8-param `allocated` overload was deliberately left
unchanged — apply the same treatment when it gets its UNION rewrite (see 1a).
Note: the param-side `cast(cast(:hearingId as varchar) as uuid)` pattern is intentionally kept —
it is applied to the parameter (evaluated once, no index impact) and is required for null-safe
binding of the nullable `hearingId`.

### 1e. Stray merge-conflict marker (unrelated, but noticed)
`HearingRepository.java:217` has `<<<<<<< Updated upstream` left inside a javadoc comment block
(harmless — doesn't break compilation since it's inside `/** ... */`, but worth cleaning up).

## 2. DBA-facing index suggestions

Verified against the current Liquibase changesets under
`listing-viewstore/listing-viewstore-liquibase/src/main/resources/liquibase/listing-view-store-db-changesets/`.

### Already indexed (no action needed)
- `listed_cases.hearing_id` → `listed_cases_hearing_id_idx` (btree)
- `listed_cases.case_id` → `listed_cases_case_id_idx` (btree)
- `defendant.listed_case_id` → `listed_case_id_idx` (btree)
- `listed_cases.case_reference` → `listed_cases_case_ref_idx` (composite with `hearing_id`,
  plain btree) — **not** functional, so it can't serve `UPPER(case_reference)` predicates below.

### Gap 1 — functional index on `listed_cases.case_reference` (high confidence)
```sql
CREATE INDEX idx_listed_cases_upper_case_reference
  ON listed_cases (UPPER(case_reference));
```
`case_reference` is `TEXT`. The query does `UPPER(lc.case_reference) IN (:caseUrnSet)` /
`IN (:linkedCaseUrn)` in two of the four UNION branches. No `UPPER()`/functional index exists
anywhere in the schema today (confirmed zero hits across all changesets) — today this is a
full scan for those branches.

### Gap 2 — `defendant.master_defendant_id` is fully unindexed
```sql
CREATE INDEX idx_defendant_master_defendant_id ON defendant (master_defendant_id);
```
Column is `UUID`, no index at all currently. Pair with the code fix in **1d** above — the
index alone won't be used while the query still casts the column to `varchar`.

### Gap 3 — `linked_case` table has zero indexes
```sql
CREATE INDEX idx_linked_case_listed_case_id ON linked_case (listed_case_id);
```
`linked_case` (created in `015-CPI-504.xml`) has no indexes whatsoever. `listed_case_id` is the
join key used to resolve linked-case URNs — every lookup today is a full table scan.

### Gap 4 — `hearing.jurisdiction_type` / `end_date` (needs a DBA judgment call, not a blind re-add)
```sql
CREATE INDEX idx_hearing_jurisdiction_enddate ON hearing (jurisdiction_type, end_date)
  WHERE (unscheduled IS NULL OR unscheduled = false);
```
History to flag to the DBA before adding this:
- A composite index covering `jurisdiction_type` existed before —
  `hearing_idx_1 (allocated, unscheduled, is_vacated_trial, type_id, jurisdiction_type)`,
  added in `020-add-new-indexes-hearing.xml`.
- It was dropped twice (`022-drop-hearing-indexes.xml`, `023-drop-indexes-hearing.xml`), with
  no comment explaining why, alongside other `hearing` indexes (`start_date`/`end_date`
  composite, `court_centre_id`/`court_room_id` composite) in the same cleanup passes — reads
  like a broad "not paying for themselves" removal rather than a targeted rejection of indexing
  by jurisdiction.
- The dropped index's column order was poor: `allocated`, `unscheduled`, `is_vacated_trial` are
  all near-boolean/low-cardinality and sit *ahead* of the more selective `jurisdiction_type` and
  `type_id`, which likely made it barely selective while still costing write overhead on a hot,
  frequently-updated table.

Recommendation: ask the DBA to `EXPLAIN ANALYZE` before/after on production-like data given this
table already had indexes pulled once for cost reasons, rather than re-adding blindly.

## Summary for the DBA ticket
1. `CREATE INDEX idx_listed_cases_upper_case_reference ON listed_cases (UPPER(case_reference));`
2. `CREATE INDEX idx_defendant_master_defendant_id ON defendant (master_defendant_id);`
   (dev-side removal of the `cast(... as varchar)` is done — see 1d — so this index will now be used)
3. `CREATE INDEX idx_linked_case_listed_case_id ON linked_case (listed_case_id);`
4. Judgment call — `CREATE INDEX idx_hearing_jurisdiction_enddate ON hearing (jurisdiction_type, end_date) WHERE (unscheduled IS NULL OR unscheduled = false);`
   — verify with `EXPLAIN ANALYZE` given past indexes on this table were dropped for cost reasons.

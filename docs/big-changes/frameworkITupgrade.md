# Framework IT Upgrade - Branch `dev/PEG-Framework-D-perf` vs `main`

## Overview

This branch (3 commits, all titled "PEG-2848 Testing Framework-D improvement") upgrades the parent POM and migrates the entire codebase to use the new Framework-D APIs. The changes span **181 files** across all modules and fall into the following categories:

---

## 1. Parent POM Version Bump

**File:** `pom.xml`

The `service-parent-pom` version was upgraded from `17.103.5` to `17.103.8`.

```xml
<!-- Before -->
<version>17.103.5</version>

<!-- After -->
<version>17.103.8</version>
```

**Impact:** This pulls in the new Framework-D dependencies transitively, which includes the new `JsonObjects` utility class and updated `RestPoller` API.

---

## 2. JSON Builder Migration: `javax.json.Json` -> `JsonObjects`

**Affected:** ~170+ files across all modules (command-api, command-handler, event-listener, event-processor, query-api, query-view, common, integration-test)

All static imports and direct usages of `javax.json.Json` factory methods have been replaced with `uk.gov.justice.services.messaging.JsonObjects`:

| Before | After |
|--------|-------|
| `javax.json.Json.createObjectBuilder()` | `JsonObjects.createObjectBuilder()` |
| `javax.json.Json.createArrayBuilder()` | `JsonObjects.createArrayBuilder()` |
| `javax.json.Json.createReader()` | `JsonObjects.createReader()` |
| `import javax.json.Json;` | `import uk.gov.justice.services.messaging.JsonObjects;` |
| `static import javax.json.Json.createObjectBuilder` | `static import uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder` |

### Examples of affected patterns

**Static imports (most common):**
```java
// Before
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

// After
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
```

**Direct class usage:**
```java
// Before
import javax.json.Json;
final JsonObject obj = Json.createObjectBuilder().add("key", "value").build();

// After
import uk.gov.justice.services.messaging.JsonObjects;
final JsonObject obj = JsonObjects.createObjectBuilder().add("key", "value").build();
```

**Method references in streams:**
```java
// Before
.collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

// After
.collect(JsonObjects::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
```

**Why:** The new `JsonObjects` wrapper from the framework likely provides consistent JSON-P provider resolution and avoids classpath issues with `javax.json.Json` in different runtime environments.

---

## 3. REST Poller API Migration

**Affected files:** `RestPollerHelper.java`, all integration test step classes, endpoint classes, and IT classes (~30+ files)

### 3a. RestPollerHelper Rewritten

The `RestPollerHelper` class was rewritten to use the new `FibonacciPollWithStartAndMax` polling strategy instead of fixed-interval polling:

**Before:**
```java
public static final long INTERVAL_IN_MILLIS = 100L;
public static final long TIMEOUT_IN_MILLIS = 50000L;

public static RestPoller pollWithDefaults(final RequestParams requestParams) {
    return poll(requestParams)
            .timeout(TIMEOUT_IN_MILLIS, MILLISECONDS)
            .pollDelay(DELAY_IN_MILLIS, MILLISECONDS)
            .pollInterval(INTERVAL_IN_MILLIS, MILLISECONDS);
}
```

**After:**
```java
public static final long INTERVAL_IN_MILLIS = 20L;
public static final long TIMEOUT_IN_MILLIS = 10000L;
public static final FibonacciPollWithStartAndMax POLL_INTERVAL =
    new FibonacciPollWithStartAndMax(Duration.ofMillis(INTERVAL_IN_MILLIS), Duration.ofMillis(DELAY_IN_MILLIS));

public static RestPoller pollWithDefaults(final RequestParams requestParams) {
    return poll(requestParams, POLL_INTERVAL, Duration.ofMillis(TIMEOUT_IN_MILLIS));
}
```

**Key changes:**
- Poll interval reduced from `100ms` to `20ms` (Fibonacci start)
- Timeout reduced from `50s` to `10s`
- Uses `FibonacciPollWithStartAndMax` for exponential backoff (starts at 20ms, grows via Fibonacci, capped at 300ms)
- New overloaded `pollWithDefaults(RequestParamsBuilder)` accepting builder directly
- `poll()` method signature changed to accept poll strategy and timeout directly

### 3b. All `RestPoller.poll()` Calls Replaced with `pollWithDefaults()`

Throughout all integration test step classes, direct calls to `poll(requestParams)` were replaced with `pollWithDefaults(requestParams)`:

```java
// Before
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
poll(requestParams(url, mediaType).withHeader(USER_ID, userId))
    .until(status().is(OK), payload().isJson(...));

// After
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
pollWithDefaults(requestParams(url, mediaType).withHeader(USER_ID, userId))
    .until(status().is(OK), payload().isJson(...));
```

**Affected step classes:**
- `ListCourtHearingSteps` (~15 replacements)
- `UpdateHearingSteps`
- `ListNextHearingSteps`
- `CancelHearingSteps`
- `SequenceHearingSteps`
- `DeleteCourtApplicationHearingSteps`
- `PublishCourtListSteps`
- `PayloadBasedListCourtHearingSteps`
- `PayloadBasedListNextHearingSteps`
- `PayloadBasedUpdateHearingSteps`
- `RemoveOffencesFromHearingSteps`
- `EjectCaseApplicationSteps`
- `VacatingTrialSteps`
- `CourtListSteps`
- `ListingNoteIT`
- `HearingSlotsIT`
- `HearingDaysIT`
- `ProsecutionCaseIT`
- `UnallocatedHearingsEndpoint`
- `UnscheduledHearingsEndpoint`
- `ListCourtHearingStepsSpi`

**Why:** Centralizes polling configuration, uses Fibonacci backoff for faster initial checks while avoiding thundering-herd problems, and reduces overall test timeout from 50s to 10s for faster failure detection.

---

## 4. Database Cleanup Optimization in `AbstractIT`

**File:** `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/it/AbstractIT.java`

### Before (setUp):
```java
databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing_days");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "listing_notes");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "cache_refdata_courtroom");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "court_list_publish_status");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "published_court_list");
```

### After (setUp):
```java
databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "stream_status",
    "stream_buffer", "hearing", "hearing_days", "listing_notes",
    "cache_refdata_courtroom", "court_list_publish_status", "published_court_list");
```

### TearDown changes:
The `tearDown` method was simplified - the `reset()` call and all database cleaning operations were **commented out**:

```java
@AfterEach
void tearDown() {
//    reset();
    USER_CONTEXT.remove();
//    databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
//    databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "stream_status", ...);
}
```

**Why:**
- Batching all view-store table truncations into a single call reduces the number of database connections opened/closed (from 8 separate calls to 1).
- Removing tearDown cleanup is a performance optimization — if `setUp` always cleans before each test, the tearDown cleanup is redundant and doubles the DB cleanup time.

---

## 5. New File: `DatabaseCleaner.java` (Local Override)

**File:** `listing-integration-test/src/test/java/uk/gov/justice/services/test/utils/persistence/DatabaseCleaner.java`

A new 199-line `DatabaseCleaner` class was added to the integration test module. This class is placed in the same package as the framework's `DatabaseCleaner` (`uk.gov.justice.services.test.utils.persistence`), effectively **shadowing/overriding** the framework-provided version.

This local copy provides:
- `cleanStreamBufferTable(contextName)`
- `cleanStreamStatusTable(contextName)`
- `cleanProcessedEventTable(contextName)`
- `cleanViewStoreErrorTables(contextName)`
- `cleanEventStoreTables(contextName)` — truncates event_log, event_stream, publish_queue, pre_publish_queue, published_event
- `cleanEventStoreTables(contextName, tableName, additionalTableNames...)` — selective table cleaning
- `cleanViewStoreTables(contextName, tableName, additionalTableNames...)` — varargs interface for batch cleanup
- `cleanSystemTables(contextName)` — truncates stored_command

**Why:** The local copy may be needed to accommodate API differences or provide fixes not yet available in the framework version, or to customize behavior specifically for the listing context's test infrastructure.

---

## 6. `ViewStoreCleaner` Updated to Accept Varargs

**File:** `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/it/util/ViewStoreCleaner.java`

```java
// Before
public void cleanViewStoreTables(String tableName) {
    databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, tableName);
}

// After
public void cleanViewStoreTables(String tableName, String... tableNames) {
    databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, tableName, tableNames);
}
```

**Why:** Allows batch-cleaning multiple tables in a single call, aligning with the `AbstractIT` cleanup optimization.

---

## 7. Temporary Sleep Placeholder Added

**File:** `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/utils/Utilities.java`

A new `sleepToBeRefactored()` method was added:

```java
/**
 * todo this needs to be refactored for each usage with correct polling conditions,
 * it's just placeholder for now
 */
@SuppressWarnings("java:S2925")
public static void sleepToBeRefactored(){
    try {
        Thread.sleep(250);
    } catch (InterruptedException e) {
        // ignore
    }
}
```

This is used in `ListCourtHearingSteps` (and potentially other step classes) as a temporary workaround where proper polling conditions haven't been implemented yet.

**Why:** During the migration, some timing-dependent code couldn't immediately be converted to the new polling API. This method provides a clearly-marked placeholder that should be replaced with proper poll-based assertions in follow-up work.

---

## 8. Awaitility Timeout Reduced in `CourtSchedulerServiceStub`

**File:** `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/utils/CourtSchedulerServiceStub.java`

```java
// Before
Awaitility.await().atMost(30, SECONDS).pollInterval(1, SECONDS).until(() -> { ... });

// After
Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> { ... });
```

**Why:** Aligns with the overall performance optimization theme — halving the maximum wait time and using the shared `POLL_INTERVAL` constant for consistency.

---

## Summary

| Change Category | Scope | Purpose |
|----------------|-------|---------|
| POM version bump | 1 file | Upgrade to Framework-D 17.103.8 |
| `Json` -> `JsonObjects` migration | ~170 files | Adopt framework's JSON builder wrapper |
| REST Poller API migration | ~30 files | Use Fibonacci backoff polling, reduce timeouts |
| DB cleanup optimization | 3 files | Batch table truncations, remove redundant tearDown |
| `DatabaseCleaner` local copy | 1 new file | Override/shadow framework's DB cleaner |
| `sleepToBeRefactored` placeholder | 1 file | Temporary timing workaround (marked for future fix) |
| Awaitility tuning | 1 file | Reduce wait times for faster test feedback |

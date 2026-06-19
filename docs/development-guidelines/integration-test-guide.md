# Integration Test Guide

This document describes the coding standards and patterns to follow when writing or modifying integration tests. These rules are derived from the migration performed on the `dev/PEG-Framework-D-perf` branch relative to `team/sni8349`.

# Latest Integration Test Timing 
[INFO] --- maven-failsafe-plugin:3.1.2:verify (verify) @ listing-integration-test ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  03:45 min
[INFO] Finished at: 2026-02-12T10:48:00Z
[INFO] ------------------------------------------------------------------------
---

## 1. Use `JsonObjects` Instead of `javax.json.Json`

All JSON builder and reader factory methods must come from the framework utility class, **not** from `javax.json.Json`.

### 1.1 `createObjectBuilder`

**Do not use:**
```java
import static javax.json.Json.createObjectBuilder;
// or
javax.json.Json.createObjectBuilder()
```

**Use instead:**
```java
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
// or
uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder()
```

### 1.2 `createArrayBuilder`

**Do not use:**
```java
import static javax.json.Json.createArrayBuilder;
// or
Json.createArrayBuilder()
// or as a method reference in streams
.collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
```

**Use instead:**
```java
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
// or
JsonObjects.createArrayBuilder()
// or as a method reference in streams
.collect(JsonObjects::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
```

### 1.3 `createReader`

**Do not use:**
```java
import static javax.json.Json.createReader;
// or
Json.createReader(new StringReader(json))
```

**Use instead:**
```java
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
// or
JsonObjects.createReader(new StringReader(json))
```

### 1.4 General Import Replacement

Replace **all** `javax.json.Json` static imports and usages:

| Old (javax.json.Json)         | New (uk.gov.justice.services.messaging.JsonObjects) |
|-------------------------------|-----------------------------------------------------|
| `Json.createObjectBuilder()`  | `JsonObjects.createObjectBuilder()`                 |
| `Json.createArrayBuilder()`   | `JsonObjects.createArrayBuilder()`                  |
| `Json.createReader(...)`      | `JsonObjects.createReader(...)`                     |
| `Json::createArrayBuilder`    | `JsonObjects::createArrayBuilder`                   |
| `Json::createObjectBuilder`   | `JsonObjects::createObjectBuilder`                  |

---

## 2. Use `RestPollerHelper.pollWithDefaults()` Instead of `RestPoller.poll()`

All REST polling in integration tests must use the centralized `RestPollerHelper` utility with its pre-configured Fibonacci-based polling strategy, rather than calling `RestPoller.poll()` directly.

### 2.1 Basic Replacement

**Do not use:**
```java
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;

poll(requestParams(url, mediaType).withHeader(USER_ID, getLoggedInUser()))
        .until(status().is(OK));
```

**Use instead:**
```java
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;

pollWithDefaults(requestParams(url, mediaType).withHeader(USER_ID, getLoggedInUser()))
        .until(status().is(OK));
```

### 2.2 Polling Configuration

The centralized `RestPollerHelper` uses a Fibonacci-based poll interval (`FibonacciPollWithStartAndMax`) instead of fixed-interval polling. The default configuration is:

| Parameter        | Old Value   | New Value  |
|------------------|-------------|------------|
| Interval         | 100ms       | 20ms (Fibonacci start) |
| Delay            | 300ms       | 300ms (Fibonacci max)  |
| Timeout          | 50000ms     | 30000ms    |
| Strategy         | Fixed       | Fibonacci (`FibonacciPollWithStartAndMax`) |

**Do not** configure poll intervals manually per test. Use the shared `RestPollerHelper` constants.

### 2.3 JMS Delay Variant

For scenarios that require an additional delay for JMS message processing, use `pollWithDelayForJms`:

```java
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;

pollWithDelayForJms(requestParams)
        .until(status().is(OK), payload().isJson(...));
```

---

## 3. Use Polling-Based Assertions Over Manual Response Parsing

When verifying API responses, prefer using `payload().isJson(withJsonPath(...))` matchers inside the `.until()` block rather than manually parsing the response JSON and asserting on it.

### 3.1 Preferred Pattern

**Do not use:**
```java
final String response = poll(requestParams(...)).until(status().is(OK)).getPayload();
JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
final JsonObject hearingJsonObject = (JsonObject) jsonObject.getJsonArray("hearings").get(0);
assertThat(hearingJsonObject.getString("id"), is(expectedId));
```

**Use instead:**
```java
pollWithDefaults(requestParams(...))
        .until(
                status().is(OK),
                payload().isJson(withJsonPath("$.hearings[0].id", is(expectedId)))
        );
```

This approach is more robust because the payload assertion is part of the polling condition. If the expected data hasn't appeared yet, polling continues instead of failing prematurely.

---

## 4. Use Awaitility with `POLL_INTERVAL` Constant for Database Polling

When using Awaitility to poll databases or WireMock verifications, use the shared `POLL_INTERVAL` constant from `RestPollerHelper` instead of hardcoded values.

**Do not use:**
```java
await().pollInterval(ofSeconds(5)).atMost(30, SECONDS).until(() -> ...);
```

**Use instead:**
```java
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.POLL_INTERVAL;

await().pollInterval(POLL_INTERVAL).atMost(15, SECONDS).until(() -> ...);
```

Similarly, for Awaitility-based waits in ListingNoteIT-style tests:

**Do not use:**
```java
private static final int POLL_INTERVAL = 800;
// ...
with().pollDelay(DELAY, MILLISECONDS)
        .and()
        .pollInterval(POLL_INTERVAL, MILLISECONDS)
        .await().atMost(TIMEOUT, TimeUnit.SECONDS)
        .until(() -> ...);
```

**Use instead:**
```java
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.POLL_INTERVAL;
// ...
with().pollDelay(DELAY, MILLISECONDS)
        .and()
        .pollInterval(POLL_INTERVAL)
        .atMost(TIMEOUT, TimeUnit.SECONDS)
        .until(() -> ...);
```

Note: Timeout values have also been reduced (e.g., from 30s to 15s) as the Fibonacci-based polling is more efficient.

---

## 5. Batch Database Cleanup Calls

When cleaning database tables in `@BeforeEach` / `@AfterEach`, use a single `cleanViewStoreTables` call with varargs instead of multiple separate calls.

**Do not use:**
```java
databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing_days");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "listing_notes");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "cache_refdata_courtroom");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "court_list_publish_status");
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "published_court_list");
```

**Use instead:**
```java
databaseCleaner.cleanViewStoreTables(CONTEXT_NAME,
        "stream_status", "stream_buffer", "hearing", "hearing_days",
        "listing_notes", "cache_refdata_courtroom",
        "court_list_publish_status", "published_court_list");
```

The `cleanViewStoreTables` method now accepts varargs (`String tableName, String... additionalTableNames`), making it possible to clean all tables in a single call. This reduces the number of database connections opened and is more efficient.

Similarly for `ViewStoreCleaner`:

```java
viewStoreCleaner.cleanViewStoreTables("listing_notes", "hearing");
```

---

## 6. Avoid Unnecessary Teardown Cleanup

Consider commenting out or removing `@AfterEach` cleanup if `@BeforeEach` already ensures a clean state. The setup phase should be responsible for a known clean state, removing the need for redundant cleanup after each test.

**Example from AbstractIT:**
```java
@AfterEach
void tearDown() {
//  reset();
    USER_CONTEXT.remove();
//  databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
//  databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, ...);
}
```

The `@BeforeEach` already cleans all tables. Duplicate cleanup in `@AfterEach` doubles test overhead without adding value.

---

## 7. Retry JMS Message Retrieval with Timeout

When retrieving JMS messages from consumers, do **not** call `.get()` directly on the `Optional` result. Instead, retry within a timeout loop to handle timing issues.

**Do not use:**
```java
public static JsonPath retrieveMessage(final JmsMessageConsumerClient consumer) {
    return consumer.retrieveMessageAsJsonPath(RETRIEVE_TIMEOUT).get();
}
```

**Use instead:**
```java
public static JsonPath retrieveMessage(final JmsMessageConsumerClient consumer) {
    final long startTime = System.currentTimeMillis();
    do {
        final Optional<JsonPath> message = consumer.retrieveMessageAsJsonPath(RETRIEVE_TIMEOUT);
        if (message.isPresent()) {
            return message.get();
        }
    } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
    throw new java.util.NoSuchElementException("No JMS message received within " + MESSAGE_RETRIEVE_TRIAL_TIMEOUT + "ms");
}
```

This prevents `NoSuchElementException` ("No value present") from Optional.get() and instead provides a descriptive error message with the timeout duration.

Expected error message change in assertions:

| Old                        | New                             |
|----------------------------|---------------------------------|
| `"No value present"`       | `"No JMS message received"`     |

---

## 8. Clear Stale JMS Messages Before Subsequent Operations

When a test performs multiple iterations of operations (e.g., add-then-delete cycles), clear stale JMS messages between iterations to avoid consuming messages from a previous operation.

**Example pattern:**
```java
// First iteration
listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
listNextHearingSteps.verifyCasesAddedToAllocatedHearingFromApi(existedHearings, oldNextHearings);

// Clear stale messages before second operation
listNextHearingSteps.clearStaleAllocatedHearingMessages();
listNextHearingSteps.whenDeleteNextHearingSubmittedForListing();
listNextHearingSteps.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);
```

---

## 9. Reuse Step Object Instances Across Related Operations

Instead of creating a new Steps object for each operation in a test, reuse the same instance across related sequential operations. This keeps state consistent (e.g., JMS consumers) and avoids resource leaks.

**Do not use:**
```java
final ListNextHearingSteps steps1 = new ListNextHearingSteps(hearingData);
steps1.whenUpdateRelatedHearingSubmittedForListing(...);
steps1.verifyCasesAddedToAllocatedHearingFromApi(...);

final ListNextHearingSteps steps2 = new ListNextHearingSteps(hearingData);
steps2.whenDeleteNextHearingSubmittedForListing();
steps2.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(...);

final ListNextHearingSteps steps3 = new ListNextHearingSteps(hearingData);
steps3.whenUpdateRelatedHearingSubmittedForListing(...);
```

**Use instead:**
```java
final ListNextHearingSteps steps = new ListNextHearingSteps(hearingData);

steps.whenUpdateRelatedHearingSubmittedForListing(...);
steps.verifyCasesAddedToAllocatedHearingFromApi(...);

steps.clearStaleAllocatedHearingMessages();
steps.whenDeleteNextHearingSubmittedForListing();
steps.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(...);

steps.whenUpdateRelatedHearingSubmittedForListing(...);
steps.verifyCasesAddedToAllocatedHearingFromApi(...);
```

---

## 10. Avoid Creating Unnecessary JMS Consumers

When creating step objects that only need to **send** events (produce) and will **verify via REST API polling** rather than consuming JMS messages, use the constructor overload that skips consumer creation.

**Example:**
```java
// Only need to send event and verify via API polling, not via JMS consumers
final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(
        caseId, hearingData, updatedOffenceData, null, false);  // false = don't create consumers
```

This reduces resource usage and avoids creating JMS consumers that will never be used.

---

## 11. Use `RequestParamsBuilder.build()` Consistently

When passing `RequestParams` to `pollWithDefaults`, ensure the builder is finalized with `.build()` where required:

```java
pollWithDefaults(requestParams(url, mediaType).withHeader(USER_ID, getLoggedInUser()).build())
        .until(status().is(OK));
```

Note: `pollWithDefaults` has overloads accepting both `RequestParams` and `RequestParamsBuilder`, so `.build()` is only strictly needed when calling an overload that requires `RequestParams`.

---

## 12. Running Integration Tests with Duration Tracking

The project includes a built-in test duration tracking system that records how long each test takes, ranks them by duration, and generates a CSV report. This is useful for identifying slow tests and monitoring performance regressions.

### 12.1 Maven Profiles

There are three integration test profiles defined in `listing-integration-test/pom.xml`:

| Profile                              | Purpose                                           |
|--------------------------------------|---------------------------------------------------|
| `listing-integration-test`           | Runs all `*IT.java` tests (excludes SPI scenarios) |
| `listing-scenario-integration-test`  | Runs only `*SpiScenario.java` tests               |
| `test-duration-tracking`             | Enables duration tracking for both surefire and failsafe |

### 12.2 Running Integration Tests with Duration Tracking

To run integration tests **with duration tracking enabled**, activate both the integration test profile and the duration tracking profile together:

```bash
mvn verify -pl listing-integration-test -P listing-integration-test,test-duration-tracking
```

For scenario tests with duration tracking:

```bash
mvn verify -pl listing-integration-test -P listing-scenario-integration-test,test-duration-tracking
```

You can also enable duration tracking on any test run by passing the system property directly:

```bash
mvn verify -pl listing-integration-test -P listing-integration-test -Denable.test.duration.tracking=true
```

### 12.3 What Duration Tracking Produces

When enabled, the tracking system:

1. **Console output** — logs the start and completion of each test with its duration in seconds
2. **Summary report** — prints a ranked table at the end of the test run:
   ```
   =====================================================
   ============= TEST EXECUTION SUMMARY =================
   =====================================================
   Total Tests Executed: 42
   Total Execution Time: 325.40 seconds (5.42 minutes)
   Average Test Duration: 7.75 seconds

   === Test Duration Rankings ===
   Rank  | Class Name                     | Test Name                      | Duration (seconds)
   -------------------------------------------------------------------------------------
   1     | ListNextHearingIT              | shouldListAndDeleteNext...      | 45.23
   2     | PublishCourtListIT             | shouldPublishCourtList...       | 32.10
   ...
   ```
3. **CSV report** — writes detailed results to `target/test-results/test-durations.csv` with columns: `Class Name`, `Test Name`, `Duration (seconds)`, sorted by duration (slowest first)

### 12.4 How It Works

The duration tracking is implemented via three JUnit 5 components registered as `TestExecutionListener` services:

| Class                   | Role                                                                 |
|-------------------------|----------------------------------------------------------------------|
| `TestDurationListener`  | Core listener that records start/end times and generates the report  |
| `TestDurationExtension` | JUnit `TestWatcher` + `TestExecutionListener` bridge that delegates to `TestDurationListener` |
| `TestReportGenerator`   | Triggers report generation at test plan completion (via `generate.test.report` property) |

These are auto-discovered via `META-INF/services/org.junit.platform.launcher.TestExecutionListener`. The tracking is gated by the `enable.test.duration.tracking` system property — when `false` (default), all listener methods return immediately with no overhead.

### 12.5 Running Without Duration Tracking

To run integration tests normally (without tracking), simply omit the `test-duration-tracking` profile:

```bash
mvn verify -pl listing-integration-test -P listing-integration-test
```

---

## 13. Deterministic Time: `ItClock` and the Wall-Clock Ban

The suite used to fail in a one-hour band at **00:00–01:00 BST**. British Summer Time is UTC+1, so in
that hour a "today" computed in `Europe/London` and a "today" computed in UTC land on **different calendar
days**. With many files independently calling `LocalDate.now()` / `ZonedDateTime.now()`, a test could build
a hearing date in one zone and assert against another — flaky only during that hour, and "green local /
red CI" because a UK laptop runs `Europe/London` while CI runs UTC.

### 13.1 The rule: one anchor, one zone, derive everything

All test time goes through **`ItClock`** (`listing-integration-test/.../it/util/ItClock.java`):

| Instead of | Use | Returns |
|---|---|---|
| `LocalDate.now()` | `ItClock.today()` | `LocalDate` (anchored once per run, UTC) |
| `ZonedDateTime.now()`, `ZonedDateTime.now(ZoneOffset.UTC)` | `ItClock.nowUtc()` | `ZonedDateTime` (UTC) |
| `ZonedDateTime.now(ZoneId.of("Europe/London"))` | `ItClock.nowLondon()` | `ZonedDateTime` (London) |
| `LocalDateTime.now()` | `ItClock.nowLocalDateTime()` | `LocalDateTime` |
| `Instant.now()` (as a data date) | `ItClock.nowInstant()` | `Instant` |
| bespoke London→UTC conversion | `ItClock.utc(date, londonTime)` | UTC instant `String` |

`today()` is captured **once per JVM** and is **UTC-internal**, so (a) a test can never straddle midnight
between building a date and asserting on it, and (b) a UK-laptop run and a UTC CI run agree on "today"
without needing a JVM timezone pin — `ItClock` *is* the canonical UTC source. Chained arithmetic is
unchanged: `ItClock.today().plusDays(7)`, `ItClock.nowUtc().truncatedTo(ChronoUnit.HOURS)`, etc.

Elapsed-time measurement is **not** a date and stays as-is: `System.currentTimeMillis()` in `QueueUtil`
timing loops and `Instant.now()` inside duration/log-marker infra.

### 13.2 The guard rail

A `forbidden-apis` check (in the IT module's default `build` profile, so it runs on `mvn install`) fails
the build if any IT class calls a no-arg wall-clock `now()` or `new Date()` outside `ItClock`. Only the
no-arg overloads are banned — `ItClock` itself uses the `Clock`-argument overloads (`LocalDate.now(CLOCK)`),
so it is not flagged. The command-line `-P listing-integration-test` profile deactivates the `build`
profile, so the guard adds zero overhead to the IT run itself.

### 13.3 Midnight simulation (no waiting for real midnight)

`ItClock` reads an optional `-Dit.clock` anchor (forwarded into the forked IT JVM by the failsafe
`systemPropertyVariables`). `run-it-midnight.sh` at the repo root freezes the clock across the
00:00–01:00 BST band and weekend boundaries and runs the date-sensitive subset:

```bash
./run-it-midnight.sh   # green at every anchor == midnight-safe
```

### 13.4 Day-of-week safety: working-day anchors for multi-day spans

`ItClock.today()` removes *time-of-day* non-determinism (the midnight band) but it is still the **live
calendar date**, so it can be any day of the week. **Courts do not sit at weekends.** A test that builds a
**split / multi-day / extend** hearing span with raw `plusDays(...)` and then asserts per-day listing events
(e.g. `verifyHearingRequestedForListingEvent`) silently straddles a Sat/Sun on some days of the week — the
weekend day emits no listing request, the awaited JMS message never arrives, and the test times out. This is
non-determinism by **day-of-week** rather than time-of-day, and it only surfaces on the days the run lands on.

Anchor such spans on working days:

| Use | Returns |
|---|---|
| `ItClock.nextWorkingDay()` | `today()`, or the next Mon-Fri if today is a weekend |
| `ItClock.nextWorkingDay(date)` | `date`, or the next Mon-Fri if `date` is a weekend |
| `ItClock.plusWorkingDays(date, n)` | `date` advanced by `n` working days, skipping weekends; result is always Mon-Fri |

`plusWorkingDays` is identical to `plusDays` on a run with no weekend in range, so it does not perturb the
passing weekday case. Example (`HearingDaysIT.testHearingDaysWithCourtCentreForSplit`):

```java
// before — span [start+1, start+3] hits a weekend whenever the suite runs Wed-Sat:
hearingData.getHearingStartDate().plusDays(1), hearingData.getHearingEndDate().plusDays(2)
// after — span endpoints are always working days:
final LocalDate splitStartDate = ItClock.plusWorkingDays(hearingData.getHearingStartDate(), 1);
final LocalDate splitEndDate   = ItClock.plusWorkingDays(splitStartDate, 2);
```

Single-day-on-`today()` tests are weekend-safe and need no change; only multi-day spans that assert
sitting-day events do.

---

## Summary Checklist

When writing or reviewing integration tests, verify:
- [ ] No raw wall-clock reads (`LocalDate.now()`, `ZonedDateTime.now()`, `new Date()`, …) — use `ItClock` (§13; the `forbidden-apis` guard enforces this on `mvn install`)
- [ ] Multi-day / split / extend spans that assert sitting-day events use `ItClock.plusWorkingDays`/`nextWorkingDay`, never raw `plusDays` — so they never straddle a weekend (§13.4)

- [ ] No usage of `javax.json.Json` — use `uk.gov.justice.services.messaging.JsonObjects` instead
- [ ] No direct `RestPoller.poll()` calls — use `RestPollerHelper.pollWithDefaults()` or `pollWithDelayForJms()`
- [ ] Polling-based assertions using `payload().isJson(withJsonPath(...))` inside `.until()` blocks
- [ ] Database cleanup uses batched varargs `cleanViewStoreTables()` calls
- [ ] No hardcoded poll intervals — use shared `POLL_INTERVAL` constant
- [ ] JMS message retrieval uses retry loops with descriptive timeout errors
- [ ] Stale JMS messages are cleared between test iterations
- [ ] Step objects are reused across related sequential operations
- [ ] Unnecessary JMS consumers are not created
- [ ] Awaitility/WireMock timeouts use reduced, reasonable values (e.g., 15s instead of 30s)
- [ ] Run with `-P test-duration-tracking` to check for slow tests when making changes to integration tests

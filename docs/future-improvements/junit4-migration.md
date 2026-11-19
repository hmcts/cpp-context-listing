# JUnit 4 to JUnit 5 Migration Notes

## Current State

This project uses **both JUnit 4.13.1 and JUnit 5 (Jupiter)**. The JUnit Vintage Engine is included to run legacy JUnit 4 tests on the JUnit 5 platform. Most modules have already migrated to JUnit 5.

## Files Still Using JUnit 4

### 1. CourtServicesMapperTest.java — Full JUnit 4 test class (Medium effort)

**Path:** `listing-event/listing-event-processor/src/test/java/uk/gov/moj/cpp/listing/event/processor/xhibit/courtlist/mapper/CourtServicesMapperTest.java`

**JUnit 4 usage:**
- `org.junit.Test`
- `org.junit.Before` (3 separate `@Before` methods)
- `@Test(expected = InvalidDataException.class)`
- `MockitoAnnotations.initMocks(this)` (deprecated Mockito pattern)

**Migration steps:**
| JUnit 4 | JUnit 5 |
|----------|---------|
| `import org.junit.Test` | `import org.junit.jupiter.api.Test` |
| `import org.junit.Before` | `import org.junit.jupiter.api.BeforeEach` |
| `@Before` | `@BeforeEach` |
| `@Test(expected = X.class)` | `assertThrows(X.class, () -> { ... })` |
| `MockitoAnnotations.initMocks(this)` | `@ExtendWith(MockitoExtension.class)` on the class |

---

### 2. PersistenceTestsIT.java — Full JUnit 4 test class (Hard — blocked by CdiTestRunner)

**Path:** `listing-integration-test-persistence/src/test/java/uk/gov/moj/cpp/listing/persistence/repository/PersistenceTestsIT.java`

**JUnit 4 usage:**
- `@RunWith(CdiTestRunner.class)` (DeltaSpike JUnit 4 runner)
- `org.junit.Test`, `org.junit.Before`, `org.junit.Assert`

**Why it's hard:**
DeltaSpike's `CdiTestRunner` is a JUnit 4 runner. Migration requires either:
1. A DeltaSpike version that provides a JUnit 5 extension (DeltaSpike 2.x+)
2. Switching to an alternative like `@EnableAutoWeld` from Weld JUnit 5 extensions
3. Keeping the JUnit Vintage Engine for this module (current strategy)

**Note:** The `listing-integration-test-persistence/pom.xml` explicitly includes `junit-vintage-engine` for this reason.

---

### 3. EventAggregateConverterTest.java — Mixed: JUnit 5 test with JUnit 4 assertion (Easy fix)

**Path:** `listing-domain/listing-domain-aggregate/src/test/java/uk/gov/moj/cpp/listing/domain/aggregate/EventAggregateConverterTest.java`

**Issue:** Uses `org.junit.jupiter.api.Test` (JUnit 5) but calls `org.junit.Assert.assertThat()` (JUnit 4, deprecated).

**Fix:** Replace the one `Assert.assertThat()` call on line 44:
```java
// Before
import org.junit.Assert;
Assert.assertThat(result, is(expected));

// After (already used on lines 54 and 63 of this file)
import org.hamcrest.MatcherAssert;
MatcherAssert.assertThat(result, is(expected));
```

Then remove the `import org.junit.Assert;` line.

---

### 4. ListCourtHearingSteps.java — Cucumber steps using JUnit 4 assertion (Easy fix)

**Path:** `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/steps/ListCourtHearingSteps.java`

**Issue:** Uses `org.junit.Assert.assertFalse` in 2 places (lines 2241, 2281).

**Fix:**
```java
// Before
import org.junit.Assert;
Assert.assertFalse(newCaseIds.contains(allocatedHearingCaseId));

// After
import static org.junit.jupiter.api.Assertions.assertFalse;
assertFalse(newCaseIds.contains(allocatedHearingCaseId));
```

---

## Suggested Migration Order

1. **EventAggregateConverterTest.java** — trivial one-line fix
2. **ListCourtHearingSteps.java** — trivial two-line fix
3. **CourtServicesMapperTest.java** — moderate refactor, no external blockers
4. **PersistenceTestsIT.java** — blocked by DeltaSpike/CdiTestRunner dependency; defer or keep Vintage Engine

## Post-Migration Cleanup

Once files 1-3 are migrated:
- Remove `junit:junit:4.13.1` from root `pom.xml` `<dependencyManagement>` (if PersistenceTestsIT still needs it, scope it to that module only)
- Remove `junit-vintage-engine` from modules that no longer need it
- Keep `junit-vintage-engine` only in `listing-integration-test-persistence/pom.xml` until CdiTestRunner is replaced

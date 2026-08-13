# LPT-2405 Inherit Tier & List Type Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When listing creates a next hearing from a seeding hearing, carry the seeding hearing's finalised tier, list type and key reason onto the new hearing and store them in the listing view store.

**Architecture:** A new command-API enrichment service queries the hearing context (`hearing.get-ptph-detail`) for the seeding hearing, but only when the next hearing is a trial type. The values are stamped onto the command payload before the aggregate sees them, so they enter the listing event stream, ride `listing.events.hearing-listed`, and land in the `hearing.properties` jsonb column via the existing listener. No coredomain schema change and no Liquibase changeset.

**Tech Stack:** Java 17, Maven, WildFly, HMCTS Justice Services Framework (CQRS/event sourcing), JUnit 5, Mockito, JSON Schema draft-04 + pojo-plugin code generation.

**Spec:** `docs/superpowers/specs/2026-07-29-lpt-2405-inherit-tier-listtype-design.md`

## Global Constraints

- **Maven only. NEVER Gradle.**
- **Commit each task on branch `feature/LPT-2405-inherit-tier-listtype`, inside this worktree only.** These are throwaway WIP commits: **never push**, never touch any other branch or repository. The developer owns squashing, rebasing, the PR and the merge. End each commit message with `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.
- **No changes to `criminal-court-public-model` (coredomain).** `core/courts/*.json` schemas are external and read-only here. Where a coredomain type is the carrier, add a sibling field to the in-repo wrapper schema instead.
- **No Liquibase changeset.** Values reach the view store through `hearing.properties` (jsonb).
- Tier and list type are carried as **plain strings**, never enums, in listing. The hearing context is the single validator.
- Tests: JUnit 5 (`org.junit.jupiter`), Mockito via `@ExtendWith(MockitoExtension.class)`, `@Mock` / `@InjectMocks`, package-private test classes — matching `HearingDurationEnrichmentServiceTest`.
- All new schema fields are **optional**; every touched schema is `additionalProperties: false`, so each field must be declared explicitly.
- Worktree: `/home/arcad/devenv/project/msjs/cpp-context-listing-lpt-2405`, branch `feature/LPT-2405-inherit-tier-listtype`, based on `main`.
- Build one module with: `mvn -B -pl <module> -am -DskipTests install`, then `mvn -B -pl <module> test`.

## File Structure

**Create:**
- `listing-domain/listing-domain-common/src/main/java/uk/gov/moj/cpp/listing/domain/PtphDetail.java` — immutable value object (`tier`, `listType`, `keyReason`); the single type used by query result, enrichment, and aggregate.
- `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/PtphDetailService.java` — the hearing-context query, nothing else.
- `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/PtphDetailEnrichmentService.java` — the trial gate + finalised gate + stamping rules.
- Tests mirroring both services.

**Modify:**
- `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/courtcentre/HearingTypeFactory.java` — add trial-type-id lookup.
- `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/ListingCommandApi.java:139-155` (scheduled) and `:200-223` (unscheduled) — call the enrichment service.
- `listing-json/src/main/resources/json/schema/commands/listing.command.hearingListingNeeds.json` — carrier fields for the scheduled flow.
- `listing-json/src/main/resources/json/schema/hearing.json` — the listing-events hearing, so values reach `properties`.
- `listing-command/listing-command-handler/.../CommandToDomainConverter.java`, `listing-domain/listing-domain-common/.../Hearing.java` (domain), `listing-domain/listing-domain-aggregate/.../Hearing.java:406` (`list`) and `:566` (`listUnscheduled`).
- Unscheduled wrapper schemas (Task 6).

---

### Task 1: `PtphDetail` value object and `PtphDetailService`

**Files:**
- Create: `listing-domain/listing-domain-common/src/main/java/uk/gov/moj/cpp/listing/domain/PtphDetail.java`
- Create: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/PtphDetailService.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/PtphDetailServiceTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `PtphDetail(String tier, String listType, String keyReason)` with getters `getTier()`, `getListType()`, `getKeyReason()`; and `PtphDetailService.getFinalisedPtphDetail(UUID seedingHearingId, JsonEnvelope envelope) → Optional<PtphDetail>`.

The hearing context returns `{ "tier": …, "listType": …, "keyReason": …, "finalised": bool }`, and returns `finalised: false` with null fields when no record exists — so "missing record" is a successful response, not an error.

- [ ] **Step 1: Write the failing test**

`PtphDetailServiceTest.java`:

```java
package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PtphDetailServiceTest {

    private static final UUID SEEDING_HEARING_ID = randomUUID();

    @Mock
    private Requester requester;

    @InjectMocks
    private PtphDetailService ptphDetailService;

    private JsonEnvelope incoming() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.list-next-hearings-v2").build(),
                createObjectBuilder().build());
    }

    private JsonEnvelope response(final JsonObject payload) {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("hearing.get-ptph-detail").build(), payload);
    }

    @Test
    void shouldReturnDetailWhenFinalised() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("tier", "TIER_3")
                .add("listType", "TYPE_1_FIXED")
                .add("keyReason", "Vulnerable witness")
                .add("finalised", true)
                .build()));

        final Optional<PtphDetail> result = ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming());

        assertTrue(result.isPresent());
        assertEquals("TIER_3", result.get().getTier());
        assertEquals("TYPE_1_FIXED", result.get().getListType());
        assertEquals("Vulnerable witness", result.get().getKeyReason());
    }

    @Test
    void shouldQueryHearingContextWithSeedingHearingId() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("finalised", false).build()));

        ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming());

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        org.mockito.Mockito.verify(requester).requestAsAdmin(captor.capture());
        assertEquals("hearing.get-ptph-detail", captor.getValue().metadata().name());
        assertEquals(SEEDING_HEARING_ID.toString(),
                captor.getValue().payloadAsJsonObject().getString("hearingId"));
    }

    @Test
    void shouldReturnEmptyWhenNotFinalised() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("tier", "TIER_3")
                .add("listType", "TYPE_2_FLEXIBLE")
                .add("finalised", false)
                .build()));

        assertFalse(ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()).isPresent());
    }

    @Test
    void shouldReturnEmptyWhenNoRecordExists() {
        // hearing context returns finalised=false with null fields when there is no row
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("finalised", false).build()));

        assertFalse(ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()).isPresent());
    }

    @Test
    void shouldPropagateQueryFailure() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class)))
                .thenThrow(new RuntimeException("hearing context unavailable"));

        assertThrows(RuntimeException.class,
                () -> ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl listing-command/listing-command-api test -Dtest=PtphDetailServiceTest`
Expected: FAIL — compilation error, `PtphDetailService` and `PtphDetail` do not exist.

- [ ] **Step 3: Write `PtphDetail`**

```java
package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.Objects;

public class PtphDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String tier;
    private final String listType;
    private final String keyReason;

    public PtphDetail(final String tier, final String listType, final String keyReason) {
        this.tier = tier;
        this.listType = listType;
        this.keyReason = keyReason;
    }

    public String getTier() {
        return tier;
    }

    public String getListType() {
        return listType;
    }

    public String getKeyReason() {
        return keyReason;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final PtphDetail that = (PtphDetail) obj;
        return Objects.equals(tier, that.tier)
                && Objects.equals(listType, that.listType)
                && Objects.equals(keyReason, that.keyReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier, listType, keyReason);
    }

    @Override
    public String toString() {
        return "PtphDetail{tier='" + tier + "', listType='" + listType + "', keyReason='" + keyReason + "'}";
    }
}
```

- [ ] **Step 4: Write `PtphDetailService`**

Follows `ReferenceDataService` exactly: `@ServiceComponent(COMMAND_API) Requester`, `Enveloper.envelop(...).withName(...).withMetadataFrom(envelope)`, `requestAsAdmin`. No `try`/`catch` — failures propagate, matching every other enrichment service in this package.

```java
package uk.gov.moj.cpp.listing.command.api.service;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PtphDetailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtphDetailService.class);

    private static final String HEARING_QUERY_PTPH_DETAIL = "hearing.get-ptph-detail";
    private static final String HEARING_ID = "hearingId";
    private static final String TIER = "tier";
    private static final String LIST_TYPE = "listType";
    private static final String KEY_REASON = "keyReason";
    private static final String FINALISED = "finalised";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    /**
     * Returns the seeding hearing's tier / list type / key reason, but only when the
     * hearing context reports the record as finalised. A missing record arrives as a
     * successful response with finalised=false, so it is covered by the same check.
     * Query failures are deliberately not caught: a swallowed failure would produce a
     * blank trial hearing indistinguishable from a legitimately blank one.
     */
    public Optional<PtphDetail> getFinalisedPtphDetail(final UUID seedingHearingId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(HEARING_ID, seedingHearingId.toString()).build();

        final Envelope<JsonObject> request = Enveloper.envelop(payload)
                .withName(HEARING_QUERY_PTPH_DETAIL)
                .withMetadataFrom(envelope);

        final JsonEnvelope response = requester.requestAsAdmin(envelopeFrom(request.metadata(), request.payload()));
        final JsonObject responsePayload = response.payloadAsJsonObject();

        if (!responsePayload.getBoolean(FINALISED, false)) {
            LOGGER.info("No finalised tier/list type for seeding hearing {}", seedingHearingId);
            return Optional.empty();
        }

        return Optional.of(new PtphDetail(
                stringOrNull(responsePayload, TIER),
                stringOrNull(responsePayload, LIST_TYPE),
                stringOrNull(responsePayload, KEY_REASON)));
    }

    private String stringOrNull(final JsonObject payload, final String field) {
        return payload.containsKey(field) && !payload.isNull(field) ? payload.getString(field) : null;
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -B -pl listing-domain/listing-domain-common -am -DskipTests install && mvn -B -pl listing-command/listing-command-api test -Dtest=PtphDetailServiceTest`
Expected: PASS, 5 tests.

- [ ] **Step 6: Commit**

Report the files changed and the test result. The developer commits.

---

### Task 2: Trial hearing type lookup on `HearingTypeFactory`

**Files:**
- Modify: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/courtcentre/HearingTypeFactory.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/courtcentre/HearingTypeFactoryTest.java` (create if absent)

**Interfaces:**
- Consumes: existing `ReferenceDataService.getHearingTypes(JsonEnvelope) → JsonEnvelope`.
- Produces: `HearingTypeFactory.getTrialHearingTypeIds(JsonEnvelope) → Set<String>` — hearing type ids (as strings, matching the existing `getHearingTypesIdDurationMap` key style) where `trialTypeFlag` is `true`.

`referencedata.query.all-hearing-types` already returns `trialTypeFlag` per hearing type, and this class already calls that query for durations — so this adds **no** new reference-data call.

- [ ] **Step 1: Write the failing test**

```java
package uk.gov.moj.cpp.listing.command.api.courtcentre;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.service.ReferenceDataService;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingTypeFactoryTest {

    private static final String TRIAL_ID = randomUUID().toString();
    private static final String PTPH_ID = randomUUID().toString();
    private static final String NO_FLAG_ID = randomUUID().toString();

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private HearingTypeFactory hearingTypeFactory;

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.list-next-hearings-v2").build(),
                createObjectBuilder().build());
    }

    private void givenHearingTypes() {
        when(referenceDataService.getHearingTypes(any(JsonEnvelope.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.all-hearing-types").build(),
                createObjectBuilder().add("hearingTypes", createArrayBuilder()
                        .add(createObjectBuilder().add("id", TRIAL_ID).add("defaultDurationMin", 360)
                                .add("hearingCode", "TRL").add("trialTypeFlag", true))
                        .add(createObjectBuilder().add("id", PTPH_ID).add("defaultDurationMin", 20)
                                .add("hearingCode", "PTP").add("trialTypeFlag", false))
                        .add(createObjectBuilder().add("id", NO_FLAG_ID).add("defaultDurationMin", 30)
                                .add("hearingCode", "APN"))
                        .build()).build()));
    }

    @Test
    void shouldReturnOnlyHearingTypeIdsFlaggedAsTrial() {
        givenHearingTypes();

        final Set<String> trialIds = hearingTypeFactory.getTrialHearingTypeIds(envelope());

        assertEquals(1, trialIds.size());
        assertTrue(trialIds.contains(TRIAL_ID));
    }

    @Test
    void shouldTreatAbsentTrialTypeFlagAsNotTrial() {
        givenHearingTypes();

        assertFalse(hearingTypeFactory.getTrialHearingTypeIds(envelope()).contains(NO_FLAG_ID));
    }

    @Test
    void shouldTreatFalseTrialTypeFlagAsNotTrial() {
        givenHearingTypes();

        assertFalse(hearingTypeFactory.getTrialHearingTypeIds(envelope()).contains(PTPH_ID));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl listing-command/listing-command-api test -Dtest=HearingTypeFactoryTest`
Expected: FAIL — `getTrialHearingTypeIds` does not exist.

- [ ] **Step 3: Add the method**

Add to `HearingTypeFactory`, alongside `getHearingTypesIdDurationMap`, reusing its raw-JSON style:

```java
    private static final String HEARING_TYPE_TRIAL_FLAG = "trialTypeFlag";

    public Set<String> getTrialHearingTypeIds(final JsonEnvelope envelope) {
        final JsonObject jsonObject = referenceDataService.getHearingTypes(envelope).payloadAsJsonObject();
        final Set<String> trialHearingTypeIds = new HashSet<>();
        jsonObject.getJsonArray(HEARING_TYPES_ARRAY).getValuesAs(JsonObject.class).forEach(hearingType -> {
            if (hearingType.getBoolean(HEARING_TYPE_TRIAL_FLAG, false)) {
                trialHearingTypeIds.add(hearingType.getString(HEARING_TYPE_ID));
            }
        });
        return trialHearingTypeIds;
    }
```

Add imports `java.util.HashSet` and `java.util.Set`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -B -pl listing-command/listing-command-api test -Dtest=HearingTypeFactoryTest`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

---

### Task 3: Carrier schema fields for the scheduled flow

**Files:**
- Modify: `listing-json/src/main/resources/json/schema/commands/listing.command.hearingListingNeeds.json`
- Modify: `listing-json/src/main/resources/json/schema/hearing.json`

**Interfaces:**
- Produces: `uk.gov.justice.listing.commands.HearingListingNeeds` gains `getTier()`, `getListType()`, `getKeyReason()` and builder methods `withTier(String)`, `withListType(String)`, `withKeyReason(String)`. `uk.gov.justice.listing.events.Hearing` gains the same three.

`listing.events.next-hearing-requested` and `listing.command.list-next-hearing` both `$ref` `hearingListingNeeds.json`, so they inherit the fields with no edit. Both files are `additionalProperties: false`, so the fields must be declared. All three are optional — existing callers are unaffected.

- [ ] **Step 1: Add the fields to the command carrier**

In `listing.command.hearingListingNeeds.json`, inside `properties` (do **not** add to `required`):

```json
    "tier": {
      "description": "Tier inherited from the seeding hearing's finalised PTPH detail. Opaque string owned by the hearing context.",
      "type": "string"
    },
    "listType": {
      "description": "List type inherited from the seeding hearing's finalised PTPH detail. Opaque string owned by the hearing context.",
      "type": "string"
    },
    "keyReason": {
      "description": "Key reason for a fixed-date list type, inherited from the seeding hearing.",
      "type": "string"
    },
```

- [ ] **Step 2: Add the same fields to the listing events hearing**

Apply the identical three properties to `listing-json/src/main/resources/json/schema/hearing.json` (id `http://justice.gov.uk/listing/events/hearing.json`), again outside `required`. This is what puts the values into `hearing.properties`.

- [ ] **Step 3: Regenerate and confirm the generated types carry the fields**

Run: `mvn -B -pl listing-json -am -DskipTests install && mvn -B -pl listing-command/listing-command-api -am -DskipTests install`
Then confirm generation:

```bash
grep -n "withTier\|getTier" listing-command/listing-command-api/target/generated-sources/uk/gov/justice/listing/commands/HearingListingNeeds.java | head
```
Expected: `getTier`, `withTier` present. If the generated sources land in a different module path, search: `find . -path '*generated-sources*' -name 'HearingListingNeeds.java'`.

- [ ] **Step 4: Commit**

---

### Task 4: `PtphDetailEnrichmentService` — the rules

**Files:**
- Create: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/service/PtphDetailEnrichmentService.java`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/service/PtphDetailEnrichmentServiceTest.java`

**Interfaces:**
- Consumes: `PtphDetailService.getFinalisedPtphDetail(UUID, JsonEnvelope) → Optional<PtphDetail>`; `HearingTypeFactory.getTrialHearingTypeIds(JsonEnvelope) → Set<String>`.
- Produces: `enrichWithPtphDetail(List<HearingListingNeeds> hearings, SeedingHearing seedingHearing, JsonEnvelope envelope) → List<HearingListingNeeds>`.

`SeedingHearing` is `uk.gov.justice.core.courts.SeedingHearing`. The critical rule: **when no hearing in the command is a trial type, the hearing context must not be called at all.**

- [ ] **Step 1: Write the failing test**

```java
package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.HearingTypeFactory;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PtphDetailEnrichmentServiceTest {

    private static final UUID SEEDING_HEARING_ID = randomUUID();
    private static final UUID TRIAL_TYPE_ID = randomUUID();
    private static final UUID PTPH_TYPE_ID = randomUUID();

    @Mock
    private HearingTypeFactory hearingTypeFactory;

    @Mock
    private PtphDetailService ptphDetailService;

    @InjectMocks
    private PtphDetailEnrichmentService ptphDetailEnrichmentService;

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.list-next-hearings-v2").build(),
                createObjectBuilder().build());
    }

    private SeedingHearing seedingHearing() {
        return SeedingHearing.seedingHearing().withSeedingHearingId(SEEDING_HEARING_ID).build();
    }

    private HearingListingNeeds hearingOfType(final UUID typeId) {
        return hearingListingNeeds()
                .withId(randomUUID())
                .withType(HearingType.hearingType().withId(typeId).withDescription("desc").build())
                .build();
    }

    @Test
    void shouldNotCallHearingContextWhenNoHearingIsATrial() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(hearingOfType(PTPH_TYPE_ID)), seedingHearing(), envelope());

        verifyNoInteractions(ptphDetailService);
        assertNull(result.get(0).getTier());
        assertNull(result.get(0).getListType());
    }

    @Test
    void shouldStampTrialHearingWhenSeedingRecordFinalised() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_3", "TYPE_1_FIXED", "Vulnerable witness")));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertEquals("TIER_3", result.get(0).getTier());
        assertEquals("TYPE_1_FIXED", result.get(0).getListType());
        assertEquals("Vulnerable witness", result.get(0).getKeyReason());
    }

    @Test
    void shouldLeaveHearingUntouchedWhenSeedingRecordNotFinalised() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.empty());

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertNull(result.get(0).getTier());
        assertNull(result.get(0).getListType());
        assertNull(result.get(0).getKeyReason());
    }

    @Test
    void shouldStampOnlyTrialHearingsInAMixedCommand() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_1", "TYPE_2_FLEXIBLE", null)));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                Arrays.asList(hearingOfType(PTPH_TYPE_ID), hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertNull(result.get(0).getTier());
        assertEquals("TIER_1", result.get(1).getTier());
    }

    @Test
    void shouldFetchOncePerCommandForMultipleTrials() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_2", "TYPE_2_FLEXIBLE", null)));

        ptphDetailEnrichmentService.enrichWithPtphDetail(
                Arrays.asList(hearingOfType(TRIAL_TYPE_ID), hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        verify(ptphDetailService).getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class));
    }

    @Test
    void shouldOverwriteAnyInboundValues() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_5", "TYPE_2_FLEXIBLE", null)));

        final HearingListingNeeds spoofed = hearingListingNeeds()
                .withId(randomUUID())
                .withType(HearingType.hearingType().withId(TRIAL_TYPE_ID).withDescription("desc").build())
                .withTier("TIER_1")
                .withListType("TYPE_1_FIXED")
                .withKeyReason("spoofed")
                .build();

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(spoofed), seedingHearing(), envelope());

        assertEquals("TIER_5", result.get(0).getTier());
        assertEquals("TYPE_2_FLEXIBLE", result.get(0).getListType());
        assertNull(result.get(0).getKeyReason());
    }

    @Test
    void shouldReturnHearingsUnchangedWhenNoSeedingHearingId() {
        final List<HearingListingNeeds> hearings = singletonList(hearingOfType(TRIAL_TYPE_ID));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                hearings, SeedingHearing.seedingHearing().build(), envelope());

        assertEquals(hearings, result);
        verifyNoInteractions(ptphDetailService);
        verify(hearingTypeFactory, never()).getTrialHearingTypeIds(any(JsonEnvelope.class));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl listing-command/listing-command-api test -Dtest=PtphDetailEnrichmentServiceTest`
Expected: FAIL — `PtphDetailEnrichmentService` does not exist.

- [ ] **Step 3: Write the service**

```java
package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;

import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.HearingTypeFactory;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the seeding hearing's finalised tier / list type / key reason onto the next
 * hearings being listed from it. The hearing context is queried only when at least one
 * of those next hearings is a trial type.
 */
@ApplicationScoped
public class PtphDetailEnrichmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtphDetailEnrichmentService.class);

    @Inject
    private HearingTypeFactory hearingTypeFactory;

    @Inject
    private PtphDetailService ptphDetailService;

    public List<HearingListingNeeds> enrichWithPtphDetail(final List<HearingListingNeeds> hearings,
                                                          final SeedingHearing seedingHearing,
                                                          final JsonEnvelope envelope) {
        final UUID seedingHearingId = isNull(seedingHearing) ? null : seedingHearing.getSeedingHearingId();
        if (isNull(seedingHearingId) || isNull(hearings) || hearings.isEmpty()) {
            return hearings;
        }

        final Set<String> trialHearingTypeIds = hearingTypeFactory.getTrialHearingTypeIds(envelope);
        if (hearings.stream().noneMatch(hearing -> isTrial(hearing, trialHearingTypeIds))) {
            LOGGER.info("No trial hearing listed from seeding hearing {}; not querying the hearing context", seedingHearingId);
            return hearings;
        }

        final Optional<PtphDetail> ptphDetail = ptphDetailService.getFinalisedPtphDetail(seedingHearingId, envelope);
        if (ptphDetail.isEmpty()) {
            return hearings;
        }

        final List<HearingListingNeeds> enriched = new ArrayList<>();
        hearings.forEach(hearing -> enriched.add(isTrial(hearing, trialHearingTypeIds)
                ? stamp(hearing, ptphDetail.get())
                : hearing));
        return enriched;
    }

    private boolean isTrial(final HearingListingNeeds hearing, final Set<String> trialHearingTypeIds) {
        return nonNull(hearing.getType())
                && nonNull(hearing.getType().getId())
                && trialHearingTypeIds.contains(hearing.getType().getId().toString());
    }

    /**
     * Always overwrites all three fields, so values already present on the inbound
     * command cannot masquerade as hearing-context data.
     */
    private HearingListingNeeds stamp(final HearingListingNeeds hearing, final PtphDetail ptphDetail) {
        LOGGER.info("Inheriting tier {} and list type {} onto trial hearing {}",
                ptphDetail.getTier(), ptphDetail.getListType(), hearing.getId());
        return hearingListingNeeds()
                .withValuesFrom(hearing)
                .withTier(ptphDetail.getTier())
                .withListType(ptphDetail.getListType())
                .withKeyReason(ptphDetail.getKeyReason())
                .build();
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -B -pl listing-command/listing-command-api test -Dtest=PtphDetailEnrichmentServiceTest`
Expected: PASS, 7 tests. If `withValuesFrom` does not copy the new fields, the overwrite test still passes because all three are set explicitly.

- [ ] **Step 5: Commit**

---

### Task 5: Wire the scheduled flow into `ListingCommandApi`

**Files:**
- Modify: `listing-command/listing-command-api/src/main/java/uk/gov/moj/cpp/listing/command/api/ListingCommandApi.java:132-155`
- Test: `listing-command/listing-command-api/src/test/java/uk/gov/moj/cpp/listing/command/api/ListingCommandApiTest.java` (extend if present; create the single test below if absent)

**Interfaces:**
- Consumes: `PtphDetailEnrichmentService.enrichWithPtphDetail(...)`.
- Produces: `listing.command.list-next-hearings-enriched-v2` whose `listNextHearings.hearings[]` carry tier / listType / keyReason for trial hearings.

- [ ] **Step 1: Add the injection point and the call**

In `ListingCommandApi`, add:

```java
    @Inject
    private PtphDetailEnrichmentService ptphDetailEnrichmentService;
```

In `listNextHearings` (currently line ~140), change the enrichment line from:

```java
        final List<HearingListingNeeds> enrichedHearings = hearingEnrichmentOrchestrator.enrichListCourtHearing(listNextHearings.getHearings(), envelope);
```

to:

```java
        final List<HearingListingNeeds> enrichedHearings = ptphDetailEnrichmentService.enrichWithPtphDetail(
                hearingEnrichmentOrchestrator.enrichListCourtHearing(listNextHearings.getHearings(), envelope),
                listNextHearings.getSeedingHearing(),
                envelope);
```

Leave the rest of the method (court centres, `ListNextHearingsEnrichedV2` build, `sender.send`) untouched — `withHearings(enrichedHearings)` already carries the stamped list.

- [ ] **Step 2: Verify the module still compiles and all its tests pass**

Run: `mvn -B -pl listing-command/listing-command-api test`
Expected: PASS with no regressions. If `ListingCommandApiTest` mocks collaborators strictly, add `@Mock private PtphDetailEnrichmentService ptphDetailEnrichmentService;` and stub `enrichWithPtphDetail` to return its first argument:

```java
        lenient().when(ptphDetailEnrichmentService.enrichWithPtphDetail(anyList(), any(), any(JsonEnvelope.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
```

- [ ] **Step 3: Commit**

---

### Task 6: Carry the values to the view store (domain, aggregate, event)

**Files:**
- Modify: `listing-domain/listing-domain-common/src/main/java/uk/gov/moj/cpp/listing/domain/Hearing.java` — add a `PtphDetail ptphDetail` field, builder method `withPtphDetail`, getter `getPtphDetail()`.
- Modify: `listing-command/listing-command-handler/src/main/java/uk/gov/moj/cpp/listing/command/utils/CommandToDomainConverter.java` — map the three command fields into a `PtphDetail` on the domain hearing.
- Modify: `listing-domain/listing-domain-aggregate/src/main/java/uk/gov/moj/cpp/listing/domain/aggregate/Hearing.java:406` (`list`) — add a single trailing `final PtphDetail ptphDetail` parameter and set the three fields on the `uk.gov.justice.listing.events.Hearing.Builder builder` declared at ~line 434.
- Modify: `listing-command/listing-command-handler/src/main/java/uk/gov/moj/cpp/listing/command/handler/ListNextHearingCommandHandler.java:101` and `ListingCommandHandler.java:278` — pass `domainHearing.getPtphDetail()` into `hearing.list(...)`.
- Test: `listing-domain/listing-domain-aggregate/src/test/java/.../HearingPtphDetailTest.java` (new), plus `listing-event/listing-event-listener/src/test/java/.../HearingEventListenerTest.java` (extend).

**Interfaces:**
- Consumes: `PtphDetail` (Task 1); `HearingListingNeeds.getTier()/getListType()/getKeyReason()` (Task 3).
- Produces: `listing.events.hearing-listed` whose `hearing` object carries `tier`, `listType`, `keyReason`; these then appear in `hearing.properties`.

`Hearing.list(...)` already takes ~28 parameters, so add **one** `PtphDetail` parameter rather than three strings.

- [ ] **Step 1: Write the failing aggregate test**

Add these two methods to the **existing** `HearingAggregateTest`
(`listing-domain/listing-domain-aggregate/src/test/java/uk/gov/moj/cpp/listing/domain/aggregate/HearingAggregateTest.java`)
so they reuse its fixture fields. It is JUnit 5 (`@ExtendWith(MockitoExtension.class)`,
package-private class) and uses hamcrest `assertThat`. The `list(...)` argument list below
is copied verbatim from the existing test at line 177, with the new `PtphDetail` parameter
appended last:

```java
    @Test
    void shouldCarryPtphDetailOntoHearingListedEvent() {
        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingDays, nonDefaultDays, nonSittingDays, isSlotsBooked,
                "", "'", null, of(Boolean.FALSE), of(false), empty(),
                new uk.gov.moj.cpp.listing.domain.PtphDetail("TIER_3", "TYPE_1_FIXED", "Vulnerable witness"));

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing listedEventHearing = hearingListed.getHearing();

        assertThat(listedEventHearing.getTier(), is("TIER_3"));
        assertThat(listedEventHearing.getListType(), is("TYPE_1_FIXED"));
        assertThat(listedEventHearing.getKeyReason(), is("Vulnerable witness"));
    }

    @Test
    void shouldLeavePtphDetailFieldsNullWhenNotSupplied() {
        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingDays, nonDefaultDays, nonSittingDays, isSlotsBooked,
                "", "'", null, of(Boolean.FALSE), of(false), empty(),
                null);

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing listedEventHearing = hearingListed.getHearing();

        assertThat(listedEventHearing.getTier(), is(nullValue()));
        assertThat(listedEventHearing.getListType(), is(nullValue()));
        assertThat(listedEventHearing.getKeyReason(), is(nullValue()));
    }
```

Add `import static org.hamcrest.Matchers.nullValue;` if absent. Note the local variable is
named `listedEventHearing`, because the existing tests shadow the field `hearing` with a
local of the same name — do not repeat that shadowing here.

- [ ] **Step 2: Run it to verify it fails**

Run: `mvn -B -pl listing-domain/listing-domain-aggregate test -Dtest=HearingAggregateTest`
Expected: FAIL — `list(...)` does not accept a `PtphDetail` argument, and
`uk.gov.justice.listing.events.Hearing` has no `getTier()`.

- [ ] **Step 3: Thread the value through**

1. Domain `Hearing`: add the field, builder method and getter.
2. `CommandToDomainConverter`: where it builds the domain hearing from `HearingListingNeeds`, add
   `.withPtphDetail(new PtphDetail(hearing.getTier(), hearing.getListType(), hearing.getKeyReason()))`,
   passing `null` when all three are null.
3. Aggregate `Hearing.list(...)`: add the trailing parameter and, next to the other `builder.withX(...)` calls, set:

```java
            if (nonNull(ptphDetail)) {
                builder.withTier(ptphDetail.getTier())
                        .withListType(ptphDetail.getListType())
                        .withKeyReason(ptphDetail.getKeyReason());
            }
```

4. Update both call sites (`ListNextHearingCommandHandler.listNextCourtHearing`, `ListingCommandHandler.listCourtHearing`) to pass `domainHearing.getPtphDetail()`.

- [ ] **Step 4: Run the tests**

Run: `mvn -B -pl listing-domain/listing-domain-aggregate -am -DskipTests install && mvn -B -pl listing-domain/listing-domain-aggregate,listing-command/listing-command-handler test`
Expected: PASS, including the pre-existing aggregate tests.

- [ ] **Step 5: Add the view-store serialisation assertion**

`HearingEventListener.hearingListed` needs **no change** — it already serialises the whole
event hearing into `properties` via `mapper.valueToTree(...)`. What must be proven is that
the generated event type actually serialises the three new fields, because that is what
lands in the jsonb column.

The existing `HearingEventListenerTest` mocks `ObjectMapper`
(`given(mapper.valueToTree(hearingEvent)).willReturn(jsonNode)`), so it cannot assert real
JSON. Add a separate focused test using a **real** mapper:

`listing-event/listing-event-listener/src/test/java/uk/gov/moj/cpp/listing/event/listener/HearingEventPtphDetailSerialisationTest.java`

```java
package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class HearingEventPtphDetailSerialisationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerialisePtphDetailFieldsIntoHearingProperties() {
        final UUID hearingId = randomUUID();
        final uk.gov.justice.listing.events.Hearing hearingEvent = uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withTier("TIER_3")
                .withListType("TYPE_1_FIXED")
                .withKeyReason("Vulnerable witness")
                .build();

        final JsonNode properties = mapper.valueToTree(hearingEvent);

        assertThat(properties.get("tier").asText(), is("TIER_3"));
        assertThat(properties.get("listType").asText(), is("TYPE_1_FIXED"));
        assertThat(properties.get("keyReason").asText(), is("Vulnerable witness"));
    }

    @Test
    void shouldOmitPtphDetailFieldsWhenNotSet() {
        final uk.gov.justice.listing.events.Hearing hearingEvent = uk.gov.justice.listing.events.Hearing.hearing()
                .withId(randomUUID())
                .build();

        final JsonNode properties = mapper.valueToTree(hearingEvent);

        assertThat(properties.get("tier"), is(nullValue()));
        assertThat(properties.get("listType"), is(nullValue()));
    }
}
```

If the generated POJO serialises unset fields as explicit `null` rather than omitting them,
change the second test to assert `properties.get("tier").isNull()` — check the generated
class's Jackson annotations to see which applies before choosing.

Run: `mvn -B -pl listing-event/listing-event-listener test -Dtest=HearingEventPtphDetailSerialisationTest,HearingEventListenerTest`
Expected: PASS, and `HearingEventListenerTest` unchanged and still green.

- [ ] **Step 6: Commit**

---

### Task 7: Unscheduled flow

**Files:**
- Modify: `listing-command/listing-command-api/src/raml/json/schema/listing.command.list-unscheduled-next-hearings-enriched.json`
- Modify: `listing-command/listing-command-handler/src/raml/json/schema/listing.command.list-unscheduled-next-hearings-enriched.json`
- Modify: `listing-event/listing-event-processor/src/yaml/json/schema/listing.events.unscheduled-next-hearing-requested.json`
- Modify: `listing-event/listing-event-listener/src/yaml/json/schema/listing.events.unscheduled-next-hearing-requested.json`
- Modify: `listing-command/listing-command-handler/src/raml/json/schema/listing.command.list-unscheduled-next-hearing.json`
- Modify: `PtphDetailEnrichmentService`, `ListingCommandApi:200-223`, `UnscheduledListingCommandHandler`, aggregate `Hearing.listUnscheduled(...)` (~line 566)
- Test: extend `PtphDetailEnrichmentServiceTest`

**Interfaces:**
- Produces: `PtphDetailEnrichmentService.resolvePtphDetails(List<HearingUnscheduledListingNeeds> hearings, SeedingHearing seedingHearing, JsonEnvelope envelope) → List<<generated item type>>`, empty when nothing applies.

**The item type is generated, not hand-written.** Adding the `ptphDetails` array to the
wrapper schema in Step 1 makes pojo-plugin generate the item class. **Determine its actual
name after Step 1 and before writing any code that references it:**

```bash
mvn -B -pl listing-command/listing-command-api -am -DskipTests install
find . -path '*generated-sources*' -name '*PtphDetail*.java' | head
```

Use whatever name generation produced (it derives from the schema property, so expect
something like `uk.gov.justice.listing.commands.PtphDetails`) consistently in the service
signature, the handler, and the tests. Do **not** create a second hand-written type for
this — the `uk.gov.moj.cpp.listing.domain.PtphDetail` value object from Task 1 stays the
type used at the domain and aggregate boundary, and the generated type is used only on the
command/event payloads.

The unscheduled carrier is the coredomain `core/courts/hearingUnscheduledListingNeeds.json`, which cannot be modified here, so the values travel as a **sibling list keyed by hearing id** on the in-repo wrapper schemas.

- [ ] **Step 1: Add the sibling field to all five schemas**

Add to `properties` in each of the five files above (not in `required`):

```json
    "ptphDetails": {
      "description": "Tier / list type inherited from the seeding hearing, per next hearing. Sibling of hearings[] because the hearing carrier is owned by coredomain.",
      "type": "array",
      "minItems": 0,
      "items": {
        "type": "object",
        "properties": {
          "hearingId": { "$ref": "http://justice.gov.uk/domain/core/common/definitions.json#/definitions/uuid" },
          "tier": { "type": "string" },
          "listType": { "type": "string" },
          "keyReason": { "type": "string" }
        },
        "required": ["hearingId"],
        "additionalProperties": false
      }
    },
```

Check the `$ref` for uuid used by each file's neighbours and match it — `listing.command.*` files use `http://justice.gov.uk/domain/core/common/definitions.json#/definitions/uuid`, while some listing event schemas use `http://justice.gov.uk/listing/events/listing-definitions.json#/definitions/uuid`. Use whichever that file already uses.

- [ ] **Step 2: Write the failing test for `resolvePtphDetails`**

Add to `PtphDetailEnrichmentServiceTest`, mirroring the scheduled cases: no trial → `verifyNoInteractions(ptphDetailService)` and empty list; trial + finalised → one entry whose `hearingId` matches the trial hearing and whose tier/listType match; trial + not finalised → empty list; mixed → exactly one entry, for the trial hearing only.

Build inputs with `HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()` — read the generated builder first to confirm the method names for id and type.

- [ ] **Step 3: Run it to verify it fails**

Run: `mvn -B -pl listing-command/listing-command-api test -Dtest=PtphDetailEnrichmentServiceTest`
Expected: FAIL — `resolvePtphDetails` does not exist.

- [ ] **Step 4: Implement `resolvePtphDetails` and wire it**

Extract the shared rule (seeding id present → trial present → fetch once → apply) into one private method used by both public entry points, so the trial gate and finalised gate exist in exactly one place. Then:
- `ListingCommandApi.handleListUnscheduledNextCourtHearings`: add `.withPtphDetails(ptphDetailEnrichmentService.resolvePtphDetails(unscheduledNextHearings.getHearings(), unscheduledNextHearings.getSeedingHearing(), envelope))` to the `ListUnscheduledNextHearingsEnriched` builder.
- `UnscheduledListingCommandHandler`: resolve each hearing's entry from `ptphDetails` by hearing id and pass a single `PtphDetail` into the aggregate, so the aggregate signature matches the scheduled path.
- Aggregate `listUnscheduled(...)`: same one-parameter addition and the same `builder.withTier/withListType/withKeyReason` block as Task 6.

- [ ] **Step 5: Run the tests**

Run: `mvn -B -pl listing-command/listing-command-api,listing-command/listing-command-handler,listing-domain/listing-domain-aggregate test`
Expected: PASS.

- [ ] **Step 6: Commit**

---

### Task 8: Resolve the flow-3 re-entry question

**Files:**
- Investigate only; then modify `PtphDetailEnrichmentService` and `ListingCommandApi.handleListCourtHearing` **only if** the finding requires it.

`listing.delete-previous-hearings-and-create-next-hearing` does not create the hearing inside listing: `DeletePreviousHearingsAndCreateNextHearingHandler` emits `CreateNextHearingRequested`, `CreateNextHearingRequestedEventProcessor` publishes `public.listing.create-next-hearing-requested`, and progression's `CreateNextHearingEventProcessor` turns it into `progression.command.create-next-hearing`. The hearing therefore re-enters listing later as an ordinary listing request.

**FINDING (verified 2026-08-11): re-entry is `listing.list-next-hearings-v2`, so no code
change is required — Tasks 4–6 already cover flow 3.** Traced in
`/home/arcad/devenv/project/msjs/cpp-context-progression`:

```
progression.command.create-next-hearing
  → CreateNextHearingCommandHandler.processCreateNextHearing
  → HearingAggregate.processCreateNextHearing  (HearingAggregate.java:1855)
  → progression.event.next-hearings-requested  (NextHearingsRequested, carries seedingHearing)
  → HearingResultedEventProcessor.handleNextHearingsRequested  (:163)
        builds ListNextHearingsV3 .withSeedingHearing(seedingHearing)
  → ProgressionService.updateHearingListingStatusToSentForListing  (:1042)
        ├─ ListingService.listNextCourtHearings  (:1081)
        └─ or progression.update-defendant-listing-status-v3 → ProsecutionCaseDefendantListingStatusChangedProcessor:78
             → ListingService.listNextCourtHearings
  → LISTING_COMMAND_SEND_LIST_NEXT_HEARINGS = "listing.list-next-hearings-v2"  (ListingService.java:50)
```

Both branches send `listing.list-next-hearings-v2` with a **top-level `seedingHearing`**,
which is exactly what `PtphDetailEnrichmentService.enrichWithPtphDetail` reads. The
offence-level fallback contemplated below is therefore not needed and was not built.

- [ ] **Step 1: Determine which listing command progression re-enters with**

Trace `progression.command.create-next-hearing` → `CreateNextHearingCommandHandler` → `HearingAggregate.processCreateNextHearing` → emitted events → the processor that sends the listing command, in `/home/arcad/devenv/project/msjs/cpp-context-progression`. Record the command name.

- [ ] **Step 2: Act on the finding**

- If re-entry is `listing.list-next-hearings-v2` (or any command with a top-level `seedingHearing`): **no code change**. Note the finding in the plan and move on — Tasks 4–6 already cover it.
- If re-entry is `listing.command.list-court-hearing`: that payload has no top-level `seedingHearing` — seeding data sits per-offence (see `CommandToDomainConverter.buildSeedingHearing` and `CourtsOffenceToDomainOffenceConverter:118`). Add an overload that derives the seeding hearing id from the offences' `seedingHearing.seedingHearingId`, requiring all resolved ids to agree (log and skip enrichment if they disagree), and call it from `handleListCourtHearing`. Cover it with the same test matrix as Task 4.

- [ ] **Step 3: Commit**

---

### Task 9: Integration test

**Files:**
- Create: a next-hearings integration test in `listing-integration-test/src/test/java/...`, modelled on the existing `list-next-hearings-v2` coverage
- Reference fixtures: `listing-integration-test/src/test/resources/test-data/CROWN/list-next-hearings-v2/adjorunment_crown_fixed_date.json`

- [ ] **Step 1: Read an existing next-hearings integration test and its fixture**

Locate the test that drives `listing.list-next-hearings-v2` and note how it stubs other contexts' queries — the hearing-context stub for `hearing.get-ptph-detail` must follow the same mechanism.

- [ ] **Step 2: Add the scenario**

Given a seeding hearing whose `hearing.get-ptph-detail` returns `{tier: "TIER_3", listType: "TYPE_1_FIXED", keyReason: "Vulnerable witness", finalised: true}` and a next hearing of a `trialTypeFlag` hearing type, when the command is processed, then the listing view store `hearing` row for the new hearing has `tier`, `listType` and `keyReason` in `properties`.

Add a second scenario with `finalised: false` asserting the fields are absent.

- [ ] **Step 3: Run the integration test**

Run: `mvn -B -pl listing-integration-test verify -Pintegration-test` (requires Docker/DB per the repo's integration-test setup)
Expected: PASS.

- [ ] **Step 4: Commit**

---

## Definition of done

- [ ] Trial next hearing from a seeding hearing with a finalised record shows tier / list type / key reason in the listing view store.
- [ ] Non-trial next hearing triggers **no** call to the hearing context (asserted by test, not inspection).
- [ ] Non-finalised or absent seeding record leaves the new hearing blank.
- [ ] A hearing-context error or timeout fails the command rather than silently producing a blank trial hearing.
- [ ] No coredomain change, no Liquibase changeset.
- [ ] `mvn -B clean install` green across the modules touched.
- [ ] All work committed on `feature/LPT-2405-inherit-tier-listtype` in the worktree, nothing pushed, no other branch or repo touched.

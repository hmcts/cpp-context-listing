package uk.gov.moj.cpp.listing.command.api.service;

import static javax.json.Json.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.listing.command.api.service.SplitHearingPayloadConverter.toProgressionSplitRequest;

import java.io.StringReader;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

/**
 * The conversion table from the SPRDT-1289 design (section 4) — listing's split payload to
 * progression's {@code {listNewHearing, sendNotificationToParties}} shape.
 */
class SplitHearingPayloadConverterTest {

    private static final String COURT_CENTRE_NAME = "Croydon Crown Court";
    private static final String COURT_ROOM_NAME = "Courtroom 01";

    private static JsonObject json(final String raw) {
        try (var reader = createReader(new StringReader(raw))) {
            return reader.readObject();
        }
    }

    // One 1080-minute block on a single virtual descriptor — the CROWN court-calendar split.
    private static JsonObject crownSplitPayload() {
        return json("""
                {
                  "courtCentreId": "07e45c88-9e5d-3e44-b664-d5345bb13be2",
                  "courtRoomId": "731816c1-5ee4-373a-9bda-840e13a5bcb0",
                  "startDate": "2026-09-10",
                  "endDate": "2026-09-10",
                  "jurisdictionType": "CROWN",
                  "type": { "id": "4a0e892d-c0c5-3c51-95b8-704d8c781776", "description": "First hearing" },
                  "judiciary": [],
                  "nonDefaultDays": [{
                    "virtual": true,
                    "duration": 1080,
                    "courtScheduleId": "c585703f-1b0e-4a5c-8f2c-6b4d9a1e3f77",
                    "session": "AD",
                    "oucode": "C01CY00",
                    "startTime": "2026-09-10T09:00:00.000Z"
                  }],
                  "prosecutionCases": [{
                    "caseId": "b14ba162-3f21-4c8e-9a77-1d2e5c8b4a90",
                    "defendants": [{
                      "defendantId": "7ba20d5f-5c44-4a1b-8e33-9f6d2c7a5b18",
                      "offences": [
                        { "offenceId": "79d8699d-2a31-4c55-b7e8-3f1a9d6c2e44" },
                        { "offenceId": "6dffce40-8b12-4d67-a9c3-5e2f8a1b7d90" }
                      ]
                    }]
                  }],
                  "sendNotificationToParties": false
                }
                """);
    }

    // Three 360-minute slots on non-consecutive days — the MAGS shape.
    private static JsonObject magsSplitPayload() {
        return json("""
                {
                  "courtCentreId": "9689207b-a9d2-4c2e-bd38-269b78a132a8",
                  "courtRoomId": "e0e04581-8f66-4bbf-b5b4-2d41fcc2ae7c",
                  "startDate": "2026-09-10",
                  "jurisdictionType": "MAGISTRATES",
                  "type": { "id": "52edf232-3c09-4c74-a6ad-737985c2e662", "description": "PTP" },
                  "nonDefaultDays": [
                    { "virtual": true, "duration": 360, "startTime": "2026-09-10T09:00:00.000Z", "courtScheduleId": "aaa" },
                    { "virtual": true, "duration": 360, "startTime": "2026-09-14T09:00:00.000Z", "courtScheduleId": "bbb" },
                    { "virtual": true, "duration": 360, "startTime": "2026-09-17T09:00:00.000Z", "courtScheduleId": "ccc" }
                  ],
                  "prosecutionCases": [{
                    "caseId": "case-1",
                    "defendants": [{ "defendantId": "def-1", "offences": [{ "offenceId": "off-1" }] }]
                  }]
                }
                """);
    }

    private static JsonObject listNewHearing(final JsonObject request) {
        return request.getJsonObject("listNewHearing");
    }

    @Test
    void shouldConvertVirtualNonDefaultDaysIntoBookedSlotsAndDropTheVirtualMarker() {
        final JsonObject hearing = listNewHearing(
                toProgressionSplitRequest(crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null));

        final JsonArray bookedSlots = hearing.getJsonArray("bookedSlots");
        assertThat(bookedSlots, hasSize(1));

        final JsonObject slot = bookedSlots.getJsonObject(0);
        assertThat(slot.containsKey("virtual"), is(false));
        assertThat(slot.getInt("duration"), is(1080));
        assertThat(slot.getString("courtScheduleId"), is("c585703f-1b0e-4a5c-8f2c-6b4d9a1e3f77"));
        assertThat(slot.getString("session"), is("AD"));
        assertThat(slot.getString("oucode"), is("C01CY00"));
        assertThat(slot.getString("startTime"), is("2026-09-10T09:00:00.000Z"));
    }

    @Test
    void shouldSumBookedSlotDurationsIntoEstimatedMinutes() {
        assertThat(listNewHearing(toProgressionSplitRequest(crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .getInt("estimatedMinutes"), is(1080));

        assertThat(listNewHearing(toProgressionSplitRequest(magsSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .getInt("estimatedMinutes"), is(1080));
    }

    @Test
    void shouldCarryEveryMagsSlotSeparatelyIncludingNonConsecutiveDays() {
        final JsonArray bookedSlots = listNewHearing(
                toProgressionSplitRequest(magsSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .getJsonArray("bookedSlots");

        assertThat(bookedSlots, hasSize(3));
        assertThat(bookedSlots.getValuesAs(JsonObject.class).stream()
                        .map(slot -> slot.getString("courtScheduleId")).collect(Collectors.toList()),
                contains("aaa", "bbb", "ccc"));
    }

    @Test
    void shouldFallBackToTheHearingTypeDefaultWhenNoSlotsArePresent() {
        final JsonObject noSlots = json("""
                { "courtCentreId": "cc-1", "jurisdictionType": "CROWN", "nonDefaultDays": [],
                  "prosecutionCases": [{ "caseId": "c", "defendants": [{ "defendantId": "d", "offences": [] }] }] }
                """);

        assertThat(listNewHearing(toProgressionSplitRequest(noSlots, COURT_CENTRE_NAME, null, 60))
                .getInt("estimatedMinutes"), is(60));
    }

    @Test
    void shouldTakeEarliestStartDateTimeFromTheFirstBookedSlot() {
        assertThat(listNewHearing(toProgressionSplitRequest(crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .getString("earliestStartDateTime"), is("2026-09-10T09:00:00.000Z"));
    }

    @Test
    void shouldRenameTypeToHearingTypeKeepingIdAndDescription() {
        final JsonObject hearingType = listNewHearing(
                toProgressionSplitRequest(crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .getJsonObject("hearingType");

        assertThat(hearingType.getString("id"), is("4a0e892d-c0c5-3c51-95b8-704d8c781776"));
        assertThat(hearingType.getString("description"), is("First hearing"));
    }

    @Test
    void shouldResolveCourtCentreAndRoomNamesFromRefdata() {
        final JsonObject courtCentre = listNewHearing(
                toProgressionSplitRequest(crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .getJsonObject("courtCentre");

        assertThat(courtCentre.getString("id"), is("07e45c88-9e5d-3e44-b664-d5345bb13be2"));
        assertThat(courtCentre.getString("name"), is(COURT_CENTRE_NAME));
        assertThat(courtCentre.getString("roomId"), is("731816c1-5ee4-373a-9bda-840e13a5bcb0"));
        assertThat(courtCentre.getString("roomName"), is(COURT_ROOM_NAME));
    }

    @Test
    void shouldFlattenProsecutionCasesIntoOneListDefendantRequestPerDefendant() {
        final JsonArray requests = listNewHearing(
                toProgressionSplitRequest(crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .getJsonArray("listDefendantRequests");

        assertThat(requests, hasSize(1));
        final JsonObject request = requests.getJsonObject(0);
        assertThat(request.getString("prosecutionCaseId"), is("b14ba162-3f21-4c8e-9a77-1d2e5c8b4a90"));
        assertThat(request.getString("defendantId"), is("7ba20d5f-5c44-4a1b-8e33-9f6d2c7a5b18"));
        assertThat(request.getJsonArray("defendantOffences").getValuesAs(javax.json.JsonString.class).stream()
                        .map(javax.json.JsonString::getString).collect(Collectors.toList()),
                contains("79d8699d-2a31-4c55-b7e8-3f1a9d6c2e44", "6dffce40-8b12-4d67-a9c3-5e2f8a1b7d90"));
    }

    @Test
    void shouldPassRealNonDefaultDaysThroughAndKeepThemOutOfBookedSlots() {
        final JsonObject mixed = json("""
                {
                  "courtCentreId": "cc-1", "jurisdictionType": "CROWN",
                  "nonDefaultDays": [
                    { "virtual": true, "duration": 720, "startTime": "2026-09-10T09:00:00.000Z" },
                    { "startTime": "2026-09-11T10:30:00.000Z", "duration": 120 }
                  ],
                  "prosecutionCases": [{ "caseId": "c", "defendants": [{ "defendantId": "d", "offences": [] }] }]
                }
                """);

        final JsonObject hearing = listNewHearing(toProgressionSplitRequest(mixed, COURT_CENTRE_NAME, null, null));

        assertThat(hearing.getJsonArray("bookedSlots"), hasSize(1));
        final JsonArray nonDefaultDays = hearing.getJsonArray("nonDefaultDays");
        assertThat(nonDefaultDays, hasSize(1));
        assertThat(nonDefaultDays.getJsonObject(0).getString("startTime"), is("2026-09-11T10:30:00.000Z"));
        assertThat(hearing.getInt("estimatedMinutes"), is(720));
    }

    @Test
    void shouldCarryWeekCommencingDateOnlyWhenPresent() {
        assertThat(listNewHearing(toProgressionSplitRequest(crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null))
                .containsKey("weekCommencingDate"), is(false));

        final JsonObject weekCommencing = json("""
                {
                  "courtCentreId": "cc-1", "jurisdictionType": "CROWN",
                  "weekCommencingStartDate": "2026-09-14", "weekCommencingDurationInWeeks": 2,
                  "nonDefaultDays": [],
                  "prosecutionCases": [{ "caseId": "c", "defendants": [{ "defendantId": "d", "offences": [] }] }]
                }
                """);

        final JsonObject carried = listNewHearing(
                toProgressionSplitRequest(weekCommencing, COURT_CENTRE_NAME, null, null))
                .getJsonObject("weekCommencingDate");
        assertThat(carried.getString("startDate"), is("2026-09-14"));
        assertThat(carried.getInt("duration"), is(2));
    }

    @Test
    void shouldPassThroughSendNotificationToPartiesAtTopLevel() {
        final JsonObject request = toProgressionSplitRequest(
                crownSplitPayload(), COURT_CENTRE_NAME, COURT_ROOM_NAME, null);
        assertThat(request.getBoolean("sendNotificationToParties"), is(false));
        assertThat(listNewHearing(request).containsKey("sendNotificationToParties"), is(false));
    }

    @Test
    void shouldNotCarryFieldsThatCourtHearingRequestDoesNotDefine() {
        final JsonObject withUnsupported = json("""
                {
                  "courtCentreId": "cc-1", "jurisdictionType": "CROWN",
                  "hearingLanguage": "ENGLISH", "hasVideoLink": true, "publicListNote": "note",
                  "nonSittingDays": ["2026-09-12"], "panel": "ADULT",
                  "nonDefaultDays": [],
                  "prosecutionCases": [{ "caseId": "c", "defendants": [{ "defendantId": "d", "offences": [] }] }]
                }
                """);

        final JsonObject hearing = listNewHearing(
                toProgressionSplitRequest(withUnsupported, COURT_CENTRE_NAME, null, null));

        for (final String unsupported : new String[]{"hearingLanguage", "hasVideoLink", "publicListNote", "nonSittingDays", "panel"}) {
            assertThat(unsupported + " must not be forwarded", hearing.containsKey(unsupported), is(false));
        }
    }

    @Test
    void shouldPassThroughJurisdictionAndSchedulingFieldsUnchanged() {
        final JsonObject withPassThroughs = json("""
                {
                  "courtCentreId": "cc-1", "jurisdictionType": "CROWN",
                  "judiciary": [{ "judicialId": "j-1" }],
                  "priority": "HIGH", "bookingType": "FIXED", "specialRequirements": "step-free access",
                  "nonDefaultDays": [],
                  "prosecutionCases": [{ "caseId": "c", "defendants": [{ "defendantId": "d", "offences": [] }] }]
                }
                """);

        final JsonObject hearing = listNewHearing(
                toProgressionSplitRequest(withPassThroughs, COURT_CENTRE_NAME, null, null));

        assertThat(hearing.getString("jurisdictionType"), is("CROWN"));
        assertThat(hearing.getJsonArray("judiciary"), hasSize(1));
        assertThat(hearing.getString("priority"), is("HIGH"));
        assertThat(hearing.getString("bookingType"), is("FIXED"));
        assertThat(hearing.getString("specialRequirements"), is("step-free access"));
    }

    @Test
    void shouldOmitCourtRoomNameWhenNoRoomWasRequested() {
        final JsonObject noRoom = json("""
                { "courtCentreId": "cc-1", "jurisdictionType": "CROWN", "nonDefaultDays": [],
                  "prosecutionCases": [{ "caseId": "c", "defendants": [{ "defendantId": "d", "offences": [] }] }] }
                """);

        final JsonObject courtCentre = listNewHearing(
                toProgressionSplitRequest(noRoom, COURT_CENTRE_NAME, null, null))
                .getJsonObject("courtCentre");

        assertThat(courtCentre.getString("name"), is(COURT_CENTRE_NAME));
        assertThat(courtCentre.containsKey("roomId"), is(false));
        assertThat(courtCentre.containsKey("roomName"), is(false));
        assertThat(courtCentre.getString("id"), is(not(nullValue())));
    }
}

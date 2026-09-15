package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.utils.ProgressionServiceStub.splitHearingCallCount;
import static uk.gov.moj.cpp.listing.utils.ProgressionServiceStub.splitHearingRequestBody;
import static uk.gov.moj.cpp.listing.utils.ProgressionServiceStub.stubSplitHearing;

import java.text.MessageFormat;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * SPRDT-1363 — listing's split-hearing proxy.
 *
 * <p>The proxy converts the court-calendar split payload into progression's list-new-hearing shape
 * and forwards it. It performs no enrichment, changes no aggregate and emits no events, which is
 * what stops the SPRDT-1227 leak: the courtscheduler booking can no longer be written under the
 * source hearing's id before anyone has classified the request as a split.
 *
 * <p>The assertions below therefore split into two halves — the recorded progression request must
 * match the conversion table, and courtscheduler must see nothing at all.
 */
public class SplitHearingProxyIT extends AbstractIT {

    private static final String MEDIA_TYPE_SPLIT_HEARING =
            "application/vnd.listing.command.split-hearing+json";
    private static final String SPLIT_HEARING_ENDPOINT_KEY = "listing.command.update-hearing-for-listing";

    private static final String COURT_CENTRE_ID = "07e45c88-9e5d-3e44-b664-d5345bb13be2";
    private static final String COURT_ROOM_ID = "731816c1-5ee4-373a-9bda-840e13a5bcb0";

    private String buildSplitHearingUrl(final UUID hearingId) {
        return String.format("%s/%s", uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri(),
                MessageFormat.format(uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig()
                        .getProperty(SPLIT_HEARING_ENDPOINT_KEY), hearingId.toString()));
    }

    // One 1080-minute block on a single virtual descriptor — the CROWN court-calendar split.
    private static String crownSplitPayload(final UUID hearingId) {
        return """
                {
                  "hearingId": "%s",
                  "courtCentreId": "%s",
                  "courtRoomId": "%s",
                  "startDate": "2026-09-10",
                  "endDate": "2026-09-10",
                  "jurisdictionType": "CROWN",
                  "hearingLanguage": "ENGLISH",
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
                """.formatted(hearingId, COURT_CENTRE_ID, COURT_ROOM_ID);
    }

    // Three 360-minute slots on non-consecutive days, plus a real (non-virtual) per-day override.
    private static String magsSplitPayload(final UUID hearingId) {
        return """
                {
                  "hearingId": "%s",
                  "courtCentreId": "%s",
                  "startDate": "2026-09-10",
                  "jurisdictionType": "MAGISTRATES",
                  "hearingLanguage": "ENGLISH",
                  "type": { "id": "52edf232-3c09-4c74-a6ad-737985c2e662", "description": "PTP" },
                  "judiciary": [],
                  "weekCommencingStartDate": "2026-09-14",
                  "weekCommencingDurationInWeeks": 2,
                  "nonDefaultDays": [
                    { "virtual": true, "duration": 360, "startTime": "2026-09-10T09:00:00.000Z", "courtScheduleId": "aaaaaaaa-0000-0000-0000-000000000001" },
                    { "virtual": true, "duration": 360, "startTime": "2026-09-14T09:00:00.000Z", "courtScheduleId": "aaaaaaaa-0000-0000-0000-000000000002" },
                    { "virtual": true, "duration": 360, "startTime": "2026-09-17T09:00:00.000Z", "courtScheduleId": "aaaaaaaa-0000-0000-0000-000000000003" },
                    { "startTime": "2026-09-18T10:30:00.000Z", "duration": 120 }
                  ],
                  "prosecutionCases": [{
                    "caseId": "b14ba162-3f21-4c8e-9a77-1d2e5c8b4a90",
                    "defendants": [{
                      "defendantId": "7ba20d5f-5c44-4a1b-8e33-9f6d2c7a5b18",
                      "offences": [ { "offenceId": "79d8699d-2a31-4c55-b7e8-3f1a9d6c2e44" } ]
                    }]
                  }],
                  "sendNotificationToParties": true
                }
                """.formatted(hearingId, COURT_CENTRE_ID);
    }

    private Response postSplit(final UUID hearingId, final String payload) {
        return AbstractIT.restClient.postCommand(
                buildSplitHearingUrl(hearingId),
                MEDIA_TYPE_SPLIT_HEARING,
                payload,
                getLoggedInHeader());
    }

    private static JsonObject listNewHearing(final UUID hearingId) {
        return splitHearingRequestBody(hearingId.toString()).getJsonObject("listNewHearing");
    }

    @Test
    void shouldConvertCrownSplitPayloadIntoProgressionListNewHearingShape() throws Exception {
        final UUID hearingId = randomUUID();
        stubSplitHearing(hearingId.toString(), HttpStatus.SC_ACCEPTED);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        final Response response = postSplit(hearingId, crownSplitPayload(hearingId));
        assertThat(response.getStatus(), is(HttpStatus.SC_ACCEPTED));

        final JsonObject hearing = listNewHearing(hearingId);

        final JsonArray bookedSlots = hearing.getJsonArray("bookedSlots");
        assertThat(bookedSlots, hasSize(1));
        assertThat(bookedSlots.getJsonObject(0).containsKey("virtual"), is(false));
        assertThat(bookedSlots.getJsonObject(0).getInt("duration"), is(1080));
        assertThat(bookedSlots.getJsonObject(0).getString("courtScheduleId"),
                is("c585703f-1b0e-4a5c-8f2c-6b4d9a1e3f77"));
        assertThat(bookedSlots.getJsonObject(0).getString("session"), is("AD"));
        assertThat(bookedSlots.getJsonObject(0).getString("oucode"), is("C01CY00"));

        assertThat(hearing.getInt("estimatedMinutes"), is(1080));
        assertThat(hearing.getString("earliestStartDateTime"), is("2026-09-10T09:00:00.000Z"));
        assertThat(hearing.getJsonObject("hearingType").getString("description"), is("First hearing"));
        assertThat(hearing.getString("jurisdictionType"), is("CROWN"));

        final JsonArray defendantRequests = hearing.getJsonArray("listDefendantRequests");
        assertThat(defendantRequests, hasSize(1));
        assertThat(defendantRequests.getJsonObject(0).getString("prosecutionCaseId"),
                is("b14ba162-3f21-4c8e-9a77-1d2e5c8b4a90"));
        assertThat(defendantRequests.getJsonObject(0).getJsonArray("defendantOffences")
                        .getValuesAs(JsonString.class).stream()
                        .map(JsonString::getString).collect(Collectors.toList()),
                contains("79d8699d-2a31-4c55-b7e8-3f1a9d6c2e44", "6dffce40-8b12-4d67-a9c3-5e2f8a1b7d90"));

        // Fields CourtHearingRequest does not define must not be forwarded.
        assertThat(hearing.containsKey("hearingLanguage"), is(false));
        assertThat(hearing.containsKey("nonSittingDays"), is(false));
    }

    @Test
    void shouldCarryEveryMagsSlotAndPassRealNonDefaultDaysThrough() throws Exception {
        final UUID hearingId = randomUUID();
        stubSplitHearing(hearingId.toString(), HttpStatus.SC_ACCEPTED);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        assertThat(postSplit(hearingId, magsSplitPayload(hearingId)).getStatus(), is(HttpStatus.SC_ACCEPTED));

        final JsonObject hearing = listNewHearing(hearingId);

        assertThat(hearing.getJsonArray("bookedSlots"), hasSize(3));
        assertThat(hearing.getInt("estimatedMinutes"), is(1080));

        // The non-virtual entry is a real per-day override, not a booked session.
        final JsonArray nonDefaultDays = hearing.getJsonArray("nonDefaultDays");
        assertThat(nonDefaultDays, hasSize(1));
        assertThat(nonDefaultDays.getJsonObject(0).getString("startTime"), is("2026-09-18T10:30:00.000Z"));

        final JsonObject weekCommencing = hearing.getJsonObject("weekCommencingDate");
        assertThat(weekCommencing.getString("startDate"), is("2026-09-14"));
        assertThat(weekCommencing.getInt("duration"), is(2));

        assertThat(splitHearingRequestBody(hearingId.toString()).getBoolean("sendNotificationToParties"), is(true));
    }

    @Test
    void shouldNotTouchCourtSchedulerForTheSourceHearing() throws Exception {
        final UUID hearingId = randomUUID();
        stubSplitHearing(hearingId.toString(), HttpStatus.SC_ACCEPTED);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        assertThat(postSplit(hearingId, crownSplitPayload(hearingId)).getStatus(), is(HttpStatus.SC_ACCEPTED));
        assertThat(splitHearingCallCount(hearingId.toString()), is(1));

        // The SPRDT-1227 regression guard: the proxy must reach progression without any
        // courtscheduler write under the source hearing id.
        assertThat("courtscheduler must see no call for the source hearing during a split",
                uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub
                        .courtSchedulerCallCountForHearing(hearingId.toString()), is(0));
    }

    @Test
    void shouldReturnProgressionRejectionsToTheCallerUnchanged() throws Exception {
        for (final int status : new int[]{HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_CONFLICT}) {
            final UUID hearingId = randomUUID();
            stubSplitHearing(hearingId.toString(), status, "{\"error\":\"rejected\"}");
            givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

            assertThat("progression " + status + " must surface unchanged",
                    postSplit(hearingId, crownSplitPayload(hearingId)).getStatus(), is(status));
        }
    }
}

package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataOrganisationUnitById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.utils.ProgressionServiceStub;

import java.io.StringReader;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SPRDT-1363 — the split-hearing endpoint's contract, for the front end to integrate against.
 *
 * <p>Listing accepts the front end's CROWN and MAGISTRATES payloads, converts them to
 * progression's list-new-hearing shape and forwards them through a client generated from
 * progression's RAML. It performs no enrichment, changes no aggregate and emits no events of its
 * own — the SPRDT-1227 guard, which is why courtscheduler must see nothing for the source hearing.
 *
 * <p>What the payload converts into is asserted directly in {@code SplitHearingPayloadConverterTest}.
 *
 * <p>On the way in, hearingId is carried only as the URI parameter — the framework merges it into
 * the payload after schema validation, so an inbound body that also declares it fails
 * {@code additionalProperties}. On the way out it is put into the payload so the generated
 * client can fill progression's URI template, and the framework then drops it from the body.
 */
public class SplitHearingIT extends AbstractIT {

    private static final String MEDIA_TYPE_PROGRESSION_SPLIT_HEARING =
            "application/vnd.progression.split-hearing+json";
    private static final String MEDIA_TYPE_SPLIT_HEARING =
            "application/vnd.listing.command.split-hearing+json";
    private static final String SPLIT_HEARING_ENDPOINT_KEY = "listing.command.update-hearing-for-listing";

    private static final String COURT_CENTRE_ID = "07e45c88-9e5d-3e44-b664-d5345bb13be2";
    private static final String COURT_ROOM_ID = "731816c1-5ee4-373a-9bda-840e13a5bcb0";
    private static final String SPLIT_CASE_ID = "b14ba162-3f21-4c8e-9a77-1d2e5c8b4a90";
    private static final String SPLIT_DEFENDANT_ID = "7ba20d5f-5c44-4a1b-8e33-9f6d2c7a5b18";
    private static final String SPLIT_OFFENCE_ID = "79d8699d-2a31-4c55-b7e8-3f1a9d6c2e44";

    /**
     * The handler resolves the court centre through reference data before converting, so without
     * these stubs the lookup returns a null-payload envelope and the request 500s.
     */
    @BeforeEach
    public void givenReferenceDataForTheSplitCourtCentre() {
        final CourtCentreData courtCentreData = new CourtCentreData(
                fromString(COURT_CENTRE_ID),
                LocalTime.of(10, 30),
                "6:30",
                fromString(COURT_ROOM_ID),
                "Test Court Centre");
        stubGetReferenceDataCourtCentre(courtCentreData);
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataCourtMappings(courtCentreData);
        stubGetReferenceDataHearingTypes(randomUUID());
        stubGetReferenceDataOrganisationUnitById(fromString(COURT_CENTRE_ID));
        stubOrganisationUnit(fromString(COURT_CENTRE_ID));
        ProgressionServiceStub.stubSplitHearing();
    }

    private String buildSplitHearingUrl(final UUID hearingId) {
        return String.format("%s/%s", uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri(),
                MessageFormat.format(uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig()
                        .getProperty(SPLIT_HEARING_ENDPOINT_KEY), hearingId.toString()));
    }

    // One 1080-minute block on a single virtual descriptor — the CROWN court-calendar split.
    private static String crownSplitPayload() {
        return """
                {
                  "courtCentreId": "%s",
                  "courtRoomId": "%s",
                  "startDate": "2026-09-10",
                  "endDate": "2026-09-10",
                  "nonSittingDays": [],
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
                """.formatted(COURT_CENTRE_ID, COURT_ROOM_ID);
    }

    // Three 360-minute slots on non-consecutive days, plus a real (non-virtual) per-day override.
    private static String magsSplitPayload() {
        return """
                {
                  "courtCentreId": "%s",
                  "startDate": "2026-09-10",
                  "jurisdictionType": "MAGISTRATES",
                  "hearingLanguage": "ENGLISH",
                  "type": { "id": "52edf232-3c09-4c74-a6ad-737985c2e662", "description": "PTP" },
                  "judiciary": [],
                  "weekCommencingStartDate": "2026-09-14",
                  "weekCommencingEndDate": "2026-09-25",
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
                """.formatted(COURT_CENTRE_ID);
    }

    /**
     * SPRDT-1367. A MAGS split spreads the new hearing over three non-consecutive sitting days.
     * The CROWN case converts one all-day block, which cannot show whether several days survive the
     * conversion intact - a converter that kept only the first, summed the wrong total or lost the
     * real (non-virtual) day would pass every CROWN assertion.
     */
    @Test
    void shouldConvertEveryVirtualDayOfAMagistratesSplit() throws Exception {
        final UUID hearingId = randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        assertThat(postSplit(hearingId, magsSplitPayload()).getStatus(), is(HttpStatus.SC_ACCEPTED));

        final List<LoggedRequest> forwarded = ProgressionServiceStub.splitRequestsForHearing(hearingId.toString());
        assertThat("listing must forward the mags split to progression", forwarded, hasSize(1));

        try (final JsonReader reader = Json.createReader(new StringReader(forwarded.get(0).getBodyAsString()))) {
            final JsonObject listNewHearing = reader.readObject().getJsonObject("listNewHearing");

            final JsonArray slots = listNewHearing.getJsonArray("bookedSlots");
            assertThat("every virtual day becomes a booked slot", slots, hasSize(3));
            for (int i = 0; i < slots.size(); i++) {
                final JsonObject slot = slots.getJsonObject(i);
                assertThat(slot.getInt("duration"), is(360));
                assertThat("each slot keeps its own session", slot.containsKey("courtScheduleId"), is(true));
                assertThat("listing reads courtCentreId off every slot unguarded",
                        slot.containsKey(COURT_CENTRE_ID_KEY), is(true));
                assertThat(slot.containsKey("virtual"), is(false));
            }

            assertThat("the estimate is the sum of the days, not one of them",
                    listNewHearing.getInt("estimatedMinutes"), is(1080));
            assertThat("the hearing starts on the earliest day",
                    listNewHearing.getString("earliestStartDateTime"), startsWith("2026-09-10"));
            assertThat("a real (non-virtual) day is carried as a nonDefaultDay, not a booked slot",
                    listNewHearing.getJsonArray("nonDefaultDays"), hasSize(1));
            assertThat("the week-commencing window survives the conversion",
                    listNewHearing.containsKey("weekCommencingDate"), is(true));
        }
    }

    private static final String COURT_CENTRE_ID_KEY = "courtCentreId";

    private Response postSplit(final UUID hearingId, final String payload) {
        return AbstractIT.restClient.postCommand(
                buildSplitHearingUrl(hearingId),
                MEDIA_TYPE_SPLIT_HEARING,
                payload,
                getLoggedInHeader());
    }

    /**
     * SPRDT-1411. Progression owns the split, so its verdict is the caller's verdict. The generated
     * command client turned every rejection into a bare RuntimeException, which reached the front
     * end as 500 and made a stale request indistinguishable from progression being down. Each status
     * progression can answer with is pinned here, because only the status tells the two apart.
     */
    @ParameterizedTest(name = "progression {0} is returned to the caller unchanged")
    @ValueSource(ints = {HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_CONFLICT})
    void shouldReturnProgressionsRejectionStatusUnchanged(final int rejectionStatus) throws Exception {
        final UUID hearingId = randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
        ProgressionServiceStub.stubSplitHearingRejectedWith(rejectionStatus, "HEARING_ALREADY_CHANGED",
                "the hearing has moved on since this request was built");

        final Response response = postSplit(hearingId, crownSplitPayload());

        assertThat("progression's status must reach the caller, not a generic 500",
                response.getStatus(), is(rejectionStatus));
        assertThat("listing must still have forwarded the split before rejecting it",
                ProgressionServiceStub.splitRequestsForHearing(hearingId.toString()), hasSize(1));
    }

    /**
     * The rejection body carries the errorCode the front end needs to explain itself, so it has to
     * survive the hop back through listing rather than being replaced with listing's own message.
     */
    @Test
    void shouldPassProgressionsErrorCodeBackToTheCaller() throws Exception {
        final UUID hearingId = randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
        ProgressionServiceStub.stubSplitHearingRejectedWith(HttpStatus.SC_CONFLICT, "HEARING_ALREADY_CHANGED",
                "the hearing has moved on since this request was built");

        final Response response = postSplit(hearingId, crownSplitPayload());

        assertThat(response.getStatus(), is(HttpStatus.SC_CONFLICT));
        try (final JsonReader reader = Json.createReader(new StringReader(response.readEntity(String.class)))) {
            final JsonObject body = reader.readObject();
            assertThat("the front end distinguishes a stale request by progression's errorCode",
                    body.getString("errorCode"), is("HEARING_ALREADY_CHANGED"));
        }
    }

    @Test
    void shouldAcceptTheCrownSplitPayload() throws Exception {
        final UUID hearingId = randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        assertThat(postSplit(hearingId, crownSplitPayload()).getStatus(), is(HttpStatus.SC_ACCEPTED));
    }

    @Test
    void shouldAcceptTheMagsSplitPayloadWithItsWeekCommencingWindow() throws Exception {
        final UUID hearingId = randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        assertThat(postSplit(hearingId, magsSplitPayload()).getStatus(), is(HttpStatus.SC_ACCEPTED));
    }

    /**
     * The proxy's whole purpose: a split accepted by listing has to reach progression, which owns
     * the operation. Asserting the 202 alone would pass just as well if the forward were missing —
     * so this reads the request progression actually received, and checks it carries the converted
     * shape rather than the front end's.
     */
    @Test
    void shouldForwardTheConvertedSplitToProgression() throws Exception {
        final UUID hearingId = randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        assertThat(postSplit(hearingId, crownSplitPayload()).getStatus(), is(HttpStatus.SC_ACCEPTED));

        final List<LoggedRequest> forwarded = ProgressionServiceStub.splitRequestsForHearing(hearingId.toString());
        assertThat("listing must forward the split to progression", forwarded, hasSize(1));

        final LoggedRequest request = forwarded.get(0);
        assertThat("the source hearing id addresses progression's endpoint",
                request.getUrl(), containsString(hearingId.toString()));

        assertThat("progression's own media type must be used, not listing's",
                request.getHeader(CONTENT_TYPE), containsString(MEDIA_TYPE_PROGRESSION_SPLIT_HEARING));

        try (final JsonReader reader = Json.createReader(new StringReader(request.getBodyAsString()))) {
            final JsonObject body = reader.readObject();
            assertThat("the hearing id is spent filling the URI template, so the framework drops it from the body",
                    body.containsKey("hearingId"), is(false));

            final JsonObject listNewHearing = body.getJsonObject("listNewHearing");
            assertThat("progression receives the converted list-new-hearing shape, not listing's payload",
                    listNewHearing, is(notNullValue()));

            // the offences the front end nests under prosecutionCases arrive as a flat id list
            final JsonObject defendantRequest = listNewHearing
                    .getJsonArray("listDefendantRequests").getJsonObject(0);
            assertThat(defendantRequest.getString("prosecutionCaseId"), is(SPLIT_CASE_ID));
            assertThat(defendantRequest.getString("defendantId"), is(SPLIT_DEFENDANT_ID));
            assertThat(defendantRequest.getJsonArray("defendantOffences").getString(0), is(SPLIT_OFFENCE_ID));

            // virtual nonDefaultDays become bookedSlots, and every slot carries a court centre
            final JsonObject bookedSlot = listNewHearing.getJsonArray("bookedSlots").getJsonObject(0);
            assertThat(bookedSlot.containsKey("virtual"), is(false));
            assertThat(bookedSlot.getInt("duration"), is(1080));
            assertThat("a slot without a court centre fails the onward listing of the new hearing",
                    bookedSlot.getString("courtCentreId"), is(COURT_CENTRE_ID));

            // the request carries only a courtCentreId, so a name at all proves listing resolved it
            // through reference data rather than echoing what the front end sent
            assertThat(listNewHearing.getJsonObject("courtCentre").getString("id"), is(COURT_CENTRE_ID));
            assertThat("the court centre name is resolved, not echoed",
                    listNewHearing.getJsonObject("courtCentre").getString("name", "").isEmpty(), is(false));

            assertThat("a split carries no judiciary yet, and progression rejects an empty one",
                    listNewHearing.containsKey("judiciary"), is(false));
        }
    }

    /**
     * The SPRDT-1227 guard: classifying a request as a split must not write a courtscheduler
     * booking under the source hearing's id.
     */
    @Test
    void shouldNotTouchCourtSchedulerForTheSourceHearing() throws Exception {
        final UUID hearingId = randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        assertThat(postSplit(hearingId, crownSplitPayload()).getStatus(), is(HttpStatus.SC_ACCEPTED));

        assertThat("courtscheduler must see no call for the source hearing during a split",
                uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub
                        .courtSchedulerCallCountForHearing(hearingId.toString()), is(0));
    }
}

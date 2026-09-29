package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
public class SplitHearingContractIT extends AbstractIT {

    private static final String MEDIA_TYPE_SPLIT_HEARING =
            "application/vnd.listing.command.split-hearing+json";
    private static final String SPLIT_HEARING_ENDPOINT_KEY = "listing.command.update-hearing-for-listing";

    private static final String COURT_CENTRE_ID = "07e45c88-9e5d-3e44-b664-d5345bb13be2";
    private static final String COURT_ROOM_ID = "731816c1-5ee4-373a-9bda-840e13a5bcb0";

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

    private Response postSplit(final UUID hearingId, final String payload) {
        return AbstractIT.restClient.postCommand(
                buildSplitHearingUrl(hearingId),
                MEDIA_TYPE_SPLIT_HEARING,
                payload,
                getLoggedInHeader());
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

        try (final JsonReader reader = Json.createReader(new StringReader(request.getBodyAsString()))) {
            final JsonObject body = reader.readObject();
            assertThat("progression receives the converted list-new-hearing shape, not listing's payload",
                    body.containsKey("listNewHearing"), is(true));
            assertThat("the hearing id is spent filling the URI template, so the framework drops it from the body",
                    body.containsKey("hearingId"), is(false));
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

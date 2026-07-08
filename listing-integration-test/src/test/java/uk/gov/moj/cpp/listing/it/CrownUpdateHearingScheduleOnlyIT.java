package uk.gov.moj.cpp.listing.it;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsForCourtSchedule;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubSearchCourtSchedulesByIdSession;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataOrganisationUnitById;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.it.util.RestPollerHelper;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.InputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher;
import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;

/**
 * Crown Phase 2: update-hearing-for-listing with a SCHEDULE-ONLY payload — the nonDefaultDay
 * carries a courtScheduleId but NO room fields (no top-level courtRoomId, no selectedCourtCentre,
 * no roomId on the nonDefaultDay).
 *
 * <p>When the courtScheduleId resolves (via courtscheduler search.court-schedules-by-id) to a
 * FINAL (isDraft=false) session, enrichment must take the courtroom from the resolved court
 * schedule — not from the payload only — populate it on the hearing and its hearing days, and
 * let allocation proceed: {@code canAllocateForCrown()} opens, hearing-allocated-for-listing-v2
 * fires, and the query view shows {@code allocated=true} with the hearing-level courtRoomId
 * taken from the resolved schedule. Without the derivation the handler reads a null command-level
 * courtroom, calls removeCourtRoom, and the hearing silently stays unallocated even though
 * courtscheduler firmly booked the session.
 *
 * <p>Mirror case (ADR-005 "Strip courtroom information from unallocated Crown hearings"): when
 * the courtScheduleId resolves to a DRAFT session, the hearing must stay UNALLOCATED and
 * roomless. Courtscheduler's by-id responses carry a room ONLY for non-draft sessions (the
 * CourtScheduleRoomSanitiser nulls rooms on draft sessions), which is exactly the signal the
 * enrichment keys off.
 *
 * <p>Sibling {@link CrownUpdateHearingSingleDayIT} covers the room-bearing (UI-parity) payload;
 * this class locks the schedule-only shape sent by API callers.
 */
public class CrownUpdateHearingScheduleOnlyIT extends AbstractIT {

    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING =
            "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY =
            "listing.command.update-hearing-for-listing";
    private static final String SCHEDULE_ONLY_PAYLOAD =
            "test-data/CROWN/update-hearing-for-listing/update-hearing-for-listing-crown-singleday-schedule-only.json";

    @Test
    void shouldAllocateAndDeriveRoomFromSchedule_whenScheduleOnlyPayloadResolvesToFinalSession() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID scheduleCourtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID finalCourtScheduleId = UUID.randomUUID();

        final LocalDate targetDate = ItClock.plusWorkingDays(ItClock.today(), 10);
        final ZonedDateTime sessionStart = targetDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        // FINAL session: by-id resolution returns isDraft=false AND the session's courtroom —
        // the room exists ONLY here, never in the payload.
        stubSearchCourtSchedulesByIdSession(
                finalCourtScheduleId.toString(),
                courtHouseId,
                scheduleCourtRoomId,
                targetDate,
                sessionStart,
                false);
        stubListHearingInCourtSessionsForCourtSchedule(
                hearingId.toString(),
                finalCourtScheduleId.toString(),
                sessionStart);

        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
        final ListCourtHearingSteps seedSteps = givenARealHearingExists(hearingId);
        givenReferenceDataStubsForUpdateHearing(courtCentreId, scheduleCourtRoomId);

        AbstractIT.restClient.postCommand(
                buildUpdateHearingUrl(hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                scheduleOnlyPayload(courtCentreId, targetDate, finalCourtScheduleId),
                getLoggedInHeader());

        // The hearing must end ALLOCATED with the hearing-level courtroom DERIVED from the
        // resolved schedule. Day-level assertions prove the schedule id/room landed on the day.
        pollHearingView(hearingId)
                .until(
                        ResponseStatusMatcher.status().is(Status.OK),
                        ResponsePayloadMatcher.payload()
                                .isJson(allOf(
                                        JsonPathMatchers.withJsonPath("$.startDate",
                                                is(targetDate.toString())),
                                        JsonPathMatchers.withJsonPath("$.allocated",
                                                is(true)),
                                        JsonPathMatchers.withJsonPath("$.courtRoomId",
                                                is(scheduleCourtRoomId.toString())),
                                        JsonPathMatchers.withJsonPath("$.hearingDays[0].courtScheduleId",
                                                is(finalCourtScheduleId.toString())),
                                        JsonPathMatchers.withJsonPath("$.hearingDays[0].courtRoomId",
                                                is(scheduleCourtRoomId.toString())))));
        seedSteps.verifyPublicEVentHearingChangesSaved(hearingId);
    }

    @Test
    void shouldStayUnallocatedAndRoomless_whenScheduleOnlyPayloadResolvesToDraftSession() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID draftCourtScheduleId = UUID.randomUUID();

        final LocalDate targetDate = ItClock.plusWorkingDays(ItClock.today(), 10);
        final ZonedDateTime sessionStart = targetDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        // DRAFT session: by-id resolution returns isDraft=true and NO courtroom (draft sessions
        // are room-sanitised on every courtscheduler query path — ADR-005 corollary).
        stubSearchCourtSchedulesByIdSession(
                draftCourtScheduleId.toString(),
                courtHouseId,
                null,
                targetDate,
                sessionStart,
                true);
        stubListHearingInCourtSessionsForCourtSchedule(
                hearingId.toString(),
                draftCourtScheduleId.toString(),
                sessionStart);

        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
        givenARealHearingExists(hearingId);
        givenReferenceDataStubsForUpdateHearing(courtCentreId, UUID.randomUUID());

        AbstractIT.restClient.postCommand(
                buildUpdateHearingUrl(hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                scheduleOnlyPayload(courtCentreId, targetDate, draftCourtScheduleId),
                getLoggedInHeader());

        // The update must project (startDate moves) with the hearing UNALLOCATED and ROOMLESS —
        // the draft schedule id lands on the day, but no courtroom may appear at any level.
        pollHearingView(hearingId)
                .until(
                        ResponseStatusMatcher.status().is(Status.OK),
                        ResponsePayloadMatcher.payload()
                                .isJson(allOf(
                                        JsonPathMatchers.withJsonPath("$.startDate",
                                                is(targetDate.toString())),
                                        JsonPathMatchers.withJsonPath("$.allocated",
                                                is(false)),
                                        JsonPathMatchers.hasNoJsonPath("$.courtRoomId"),
                                        JsonPathMatchers.withJsonPath("$.hearingDays[0].courtScheduleId",
                                                is(draftCourtScheduleId.toString())),
                                        JsonPathMatchers.hasNoJsonPath("$.hearingDays[0].courtRoomId"))));
    }

    private String scheduleOnlyPayload(final UUID courtCentreId,
                                       final LocalDate targetDate,
                                       final UUID courtScheduleId) throws Exception {
        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%%COURT_CENTRE_ID%%", courtCentreId.toString());
        placeholders.put("%%TARGET_DATE%%", targetDate.toString());
        placeholders.put("%%COURT_SCHEDULE_ID%%", courtScheduleId.toString());
        return loadAndSubstitute(SCHEDULE_ONLY_PAYLOAD, placeholders);
    }

    private uk.gov.justice.services.test.utils.core.http.RestPoller pollHearingView(final UUID hearingId) {
        final String url = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearing"), hearingId.toString()));
        return RestPollerHelper.pollWithDefaults(
                RequestParamsBuilder
                        .requestParams(url, "application/vnd.listing.search.hearing+json")
                        .withHeader(HeaderConstants.USER_ID, getLoggedInUser())
                        .build());
    }

    private ListCourtHearingSteps givenARealHearingExists(final UUID hearingId) {
        final ListCourtHearingSteps seedSteps =
                new ListCourtHearingSteps(HearingsData.hearingsData(hearingId));
        seedSteps.whenCaseIsSubmittedForListing();
        seedSteps.verifyHearingIsCreated(hearingId, 2);
        return seedSteps;
    }

    private static void givenReferenceDataStubsForUpdateHearing(final UUID courtCentreId, final UUID courtRoomId) {
        final CourtCentreData courtCentreData = new CourtCentreData(
                courtCentreId,
                LocalTime.of(10, 30),
                "6:30",
                courtRoomId,
                "Test Court Centre");
        stubGetReferenceDataCourtCentre(courtCentreData);
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataCourtMappings(courtCentreData);
        stubGetReferenceDataHearingTypes(UUID.randomUUID());
        stubGetReferenceDataOrganisationUnitById(courtCentreId);
    }

    private static String loadAndSubstitute(final String classpathResource,
                                            final Map<String, String> placeholders) throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Test payload not found on classpath: " + classpathResource);
            }
            String body = new String(in.readAllBytes());
            for (final Map.Entry<String, String> e : placeholders.entrySet()) {
                body = body.replace(e.getKey(), e.getValue());
            }
            return body;
        }
    }

    private static String buildUpdateHearingUrl(final UUID hearingId) {
        final String path = MessageFormat.format(
                readConfig().getProperty(UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY),
                hearingId.toString());
        return String.format("%s/%s", getBaseUri(), path);
    }
}

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

import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.it.util.RestPollerHelper;

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
 * Regression lock for SPRDT-1011 / J05: a CROWN single-day hearing that was UNALLOCATED is
 * updated via update-hearing-for-listing with a non-draft courtScheduleId on nonDefaultDays
 * and must end ALLOCATED.
 *
 * <p>The bug (fixed in commit 8b405b1c1): when {@code hearingDays} carried a stale OLD (draft)
 * courtScheduleId and {@code nonDefaultDays} carried the NEW (non-draft) courtScheduleId for the
 * same target date, {@code mergeCourtScheduleIdsFromNonDefaultDays} did NOT overwrite the stale id
 * (the old code had an early-return guard that fired when all hearingDays already had an id).
 * Downstream {@code fetchCourtSchedulesByIds} then resolved the draft id, {@code isDraft=true}
 * propagated, {@code canAllocateForCrown()} was closed, and the hearing stayed UNALLOCATED.
 *
 * <p>The fix: remove the early-return guard so nonDefaultDays always wins over a same-date
 * hearingDay when the ids differ. After the fix the NEW non-draft id is fetched,
 * {@code isDraft=false} propagates, {@code canAllocateForCrown()} opens, and
 * {@code hearing-allocated-for-listing-v2} fires.
 *
 * <p>Integration test perspective: the JSON schema for update-hearing-for-listing does not expose
 * {@code hearingDays} as a submittable field (additionalProperties:false), so the exact stale-id
 * scenario from the unit test cannot be reproduced at IT level. This test instead locks the
 * end-to-end allocation outcome for the single-day CROWN path: nonDefaultDays carries the
 * non-draft courtScheduleId; courtscheduler is stubbed to confirm non-draft; the hearing must
 * end ALLOCATED. Reverting the fix causes the enrichment path to break for hearingDays-populated
 * flows and the companion unit tests to fail; this IT locks the integration layer.
 */
public class CrownUpdateHearingSingleDayIT extends AbstractIT {

    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING =
            "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY =
            "listing.command.update-hearing-for-listing";

    @Test
    void shouldAllocateHearing_whenCrownSingleDayUpdatedWithNonDraftCourtScheduleId() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        // Non-draft courtScheduleId supplied on nonDefaultDays — simulates the reschedule target
        // session (a final, non-draft session assigned by courtscheduler).
        final UUID nonDraftCourtScheduleId = UUID.randomUUID();

        final LocalDate targetDate = ItClock.plusWorkingDays(ItClock.today(), 10);
        final ZonedDateTime sessionStart = targetDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        // Stub GET /sessions for the non-draft courtScheduleId.
        // fetchCourtSchedulesByIds in the single-day CROWN path calls this after the hearingDay
        // has been seeded from nonDefaultDays. isDraft=false must propagate to canAllocateForCrown().
        stubSearchCourtSchedulesByIdSession(
                nonDraftCourtScheduleId.toString(),
                courtHouseId,
                courtRoomId,
                targetDate,
                sessionStart,
                false);

        // Stub POST /hearings (list.hearings-in-sessions) to record the hearing against the session.
        stubListHearingInCourtSessionsForCourtSchedule(
                hearingId.toString(),
                nonDraftCourtScheduleId.toString(),
                sessionStart);

        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
        final uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps seedSteps = givenARealHearingExists(hearingId);
        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);

        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%%COURT_CENTRE_ID%%", courtCentreId.toString());
        placeholders.put("%%COURT_ROOM_ID%%", courtRoomId.toString());
        placeholders.put("%%TARGET_DATE%%", targetDate.toString());
        placeholders.put("%%NEW_COURT_SCHEDULE_ID%%", nonDraftCourtScheduleId.toString());

        final String payload = loadAndSubstitute(
                "test-data/CROWN/update-hearing-for-listing/update-hearing-for-listing-crown-singleday-reschedule.json",
                placeholders);

        AbstractIT.restClient.postCommand(
                buildUpdateHearingUrl(hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                payload,
                getLoggedInHeader());

        // Regression lock: the hearing must be ALLOCATED after the update.
        // The single-day CROWN enrichment path resolves the nonDefaultDays courtScheduleId via
        // fetchCourtSchedulesByIds; if isDraft=false is returned, canAllocateForCrown() opens and
        // hearing-allocated-for-listing-v2 fires. A broken enrichment (stale draft id surviving)
        // would leave allocated=false and this poll would time out.
        awaitAllocatedProjection(hearingId, targetDate);
        seedSteps.verifyPublicEVentHearingChangesSaved(hearingId);
    }

    private void awaitAllocatedProjection(final UUID hearingId, final LocalDate expectedStartDate) {
        final String url = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearing"), hearingId.toString()));
        RestPollerHelper.pollWithDefaults(
                RequestParamsBuilder
                        .requestParams(url, "application/vnd.listing.search.hearing+json")
                        .withHeader(HeaderConstants.USER_ID, getLoggedInUser())
                        .build())
                .until(
                        ResponseStatusMatcher.status().is(Status.OK),
                        ResponsePayloadMatcher.payload()
                                .isJson(allOf(
                                        JsonPathMatchers.withJsonPath("$.startDate",
                                                is(expectedStartDate.toString())),
                                        JsonPathMatchers.withJsonPath("$.allocated",
                                                is(true)))));
    }

    private uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps givenARealHearingExists(final UUID hearingId) {
        final uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps seedSteps =
                new uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps(
                        uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData(hearingId));
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

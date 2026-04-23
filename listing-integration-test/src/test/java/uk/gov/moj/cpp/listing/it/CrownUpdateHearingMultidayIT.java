package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubMultiDaySearchAndBook;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyMultiDaySearchAndBookCalled;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;

import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;

import java.io.InputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Integration coverage for the CROWN update-hearing-for-listing CourtSchedule-first routing.
 *
 * <p>Regression guard: a raw multi-day Crown update (hearingDays empty, single nonDefaultDay
 * carrying a CROWN courtScheduleId + duration > MINUTES_IN_DAY) must hit courtscheduler's
 * {@code /multidaysearchandbook/hearingslots} with the full requested duration —
 * the bug this protects against silently expanded startDate→endDate into 50+ calendar days.
 *
 * <p>Assertion scope: we verify the WireMock call to courtscheduler was made with the correct
 * parameters. The HTTP response code isn't asserted because this IT posts to a synthetic hearingId
 * (no pre-existing aggregate) — the async command handler will roll back, but the enrichment path
 * (where the fix lives) has already executed and hit the stub by that point. Unit tests in
 * {@code CourtScheduleEnrichmentServiceTest} and {@code HearingEnrichmentOrchestratorTest} cover
 * the request/response behaviour in isolation.
 */
public class CrownUpdateHearingMultidayIT extends AbstractIT {

    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING =
            "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY =
            "listing.command.update-hearing-for-listing";
    private static final int MULTI_DAY_TOTAL_DURATION_MINUTES = 1080;

    @Test
    void shouldCallMultiDaySearchAndBookOnListingCourtScheduler_whenCrownUpdateCarriesCourtScheduleIdOnNonDefaultDayWithMultiDayDuration() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID startingCourtScheduleId = UUID.randomUUID();
        final LocalDate startDate = LocalDate.now().plusDays(30);
        // endDate spans ~2 months — deliberately much wider than the 3-day session window to prove
        // the fix prevents startDate→endDate expansion when courtscheduler is the authority.
        final LocalDate endDate = startDate.plusDays(57);
        final ZonedDateTime sessionStart = startDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        final List<String> sessionScheduleIds = new ArrayList<>();
        sessionScheduleIds.add(startingCourtScheduleId.toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());

        stubMultiDaySearchAndBook(sessionScheduleIds, courtHouseId, courtRoomId, startDate, false);
        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        final String payload = loadAndSubstitute(
                "test-data/CROWN/update-hearing-for-listing/update-hearing-for-listing-crown-multiday-courtscheduleid.json",
                basePlaceholders(hearingId, courtCentreId, courtRoomId, startingCourtScheduleId, startDate, endDate, sessionStart));

        AbstractIT.restClient.postCommand(
                buildUpdateHearingUrl(hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                payload,
                getLoggedInHeader());

        // Core assertion: the CROWN update was routed through CourtSchedule-first and hit
        // /multidaysearchandbook with the full 1080-minute duration — proving the bug fix is wired.
        verifyMultiDaySearchAndBookCalled(startingCourtScheduleId.toString(), MULTI_DAY_TOTAL_DURATION_MINUTES);
    }

    @Test
    void shouldCallMultiDaySearchAndBookForFullDuration_whenNonSittingDaysAreSubmitted() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID startingCourtScheduleId = UUID.randomUUID();
        final LocalDate startDate = LocalDate.now().plusDays(30);
        final LocalDate endDate = startDate.plusDays(57);
        final LocalDate nonSittingDay = startDate.plusDays(1);
        final ZonedDateTime sessionStart = startDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        final List<String> sessionScheduleIds = new ArrayList<>();
        sessionScheduleIds.add(startingCourtScheduleId.toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());

        stubMultiDaySearchAndBook(sessionScheduleIds, courtHouseId, courtRoomId, startDate, false);
        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        final Map<String, String> placeholders = basePlaceholders(hearingId, courtCentreId, courtRoomId,
                startingCourtScheduleId, startDate, endDate, sessionStart);
        placeholders.put("%%NON_SITTING_DAY%%", nonSittingDay.toString());
        final String payload = loadAndSubstitute(
                "test-data/CROWN/update-hearing-for-listing/update-hearing-for-listing-crown-multiday-with-nonsitting.json",
                placeholders);

        AbstractIT.restClient.postCommand(
                buildUpdateHearingUrl(hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                payload,
                getLoggedInHeader());

        // NonSittingDays must NOT reduce the duration sent to courtscheduler — slot accounting requires
        // all N sessions be deducted. Filtering of nonSittingDays happens post-enrichment on hearingDays.
        verifyMultiDaySearchAndBookCalled(startingCourtScheduleId.toString(), MULTI_DAY_TOTAL_DURATION_MINUTES);
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
    }

    private static Map<String, String> basePlaceholders(final UUID hearingId,
                                                         final UUID courtCentreId,
                                                         final UUID courtRoomId,
                                                         final UUID startingCourtScheduleId,
                                                         final LocalDate startDate,
                                                         final LocalDate endDate,
                                                         final ZonedDateTime sessionStart) {
        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%%HEARING_ID_TO_UPDATE%%", hearingId.toString());
        placeholders.put("%%COURT_CENTRE_ID%%", courtCentreId.toString());
        placeholders.put("%%COURT_ROOM_ID%%", courtRoomId.toString());
        placeholders.put("%%STARTING_COURT_SCHEDULE_ID%%", startingCourtScheduleId.toString());
        placeholders.put("%%START_DATE%%", startDate.toString());
        placeholders.put("%%END_DATE%%", endDate.toString());
        placeholders.put("%%START_TIME%%", sessionStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return placeholders;
    }

    private static String loadAndSubstitute(final String classpathResource, final Map<String, String> placeholders) throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Test payload not found on classpath: " + classpathResource);
            }
            String body = new String(in.readAllBytes());
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
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

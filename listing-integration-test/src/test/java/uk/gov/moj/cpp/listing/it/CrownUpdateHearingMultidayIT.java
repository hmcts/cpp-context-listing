package uk.gov.moj.cpp.listing.it;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubExtendMultiDayHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubExtendMultiDayHearingFailure;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyExtendMultiDayHearingCalled;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;

import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

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
 * Integration coverage for the CROWN update-hearing-for-listing multi-day extension routing (SPRDT-901).
 *
 * <p>A raw multi-day Crown update with NO courtScheduleId submitted (hearingDays empty, single
 * nonDefaultDay with duration > MINUTES_IN_DAY and no courtScheduleId) must hit courtscheduler's
 * {@code /extendmultidayhearing/hearingslots} POST endpoint with the full requested duration —
 * the listing officer has not yet picked sessions, so courtscheduler is asked to extend/book them.
 * When a courtScheduleId IS submitted the update reverts to {@code enrichCrownCourtScheduleFirst}
 * (multiDaySearchAndBook) — the pre-d62d3446 behaviour — and extend-multiday is NOT called; that
 * courtScheduleId-wins routing is locked by HearingEnrichmentOrchestratorTest. The single-day CROWN
 * path (duration ≤ MINUTES_IN_DAY) and fresh allocations via list-court-hearing are likewise unchanged.
 *
 * <p>Assertion scope: we verify the WireMock call to courtscheduler was made with the correct
 * hearingId and duration. The success-path tests seed a REAL hearing first (see
 * {@code givenARealHearingExists}) so the async command handler has an aggregate to update — the
 * original synthetic-hearingId design caused a 10-attempt redelivery storm of
 * "There is no Hearing for this ID" ERRORs plus DLQ poison once enrichment stopped failing first.
 * The 422 test stays synthetic: its command is rejected synchronously in COMMAND_API and never
 * reaches the handler. Unit tests in {@code CourtScheduleEnrichmentServiceTest} and
 * {@code HearingEnrichmentOrchestratorTest} cover the request/response behaviour in isolation.
 */
public class CrownUpdateHearingMultidayIT extends AbstractIT {

    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING =
            "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String UPDATE_HEARING_FOR_LISTING_ENDPOINT_KEY =
            "listing.command.update-hearing-for-listing";
    private static final int MULTI_DAY_TOTAL_DURATION_MINUTES = 1080;

    @Test
    void shouldCallExtendMultiDayHearingOnListingCourtScheduler_whenCrownMultiDayUpdateHasNoCourtScheduleIdOnNonDefaultDay() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID startingCourtScheduleId = UUID.randomUUID();
        final LocalDate startDate = ItClock.today().plusDays(30);
        // endDate spans ~2 months — deliberately much wider than the 3-day session window to prove
        // courtscheduler (not startDate→endDate iteration) is the authority on session count.
        final LocalDate endDate = startDate.plusDays(57);
        final ZonedDateTime sessionStart = startDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        final List<String> sessionScheduleIds = new ArrayList<>();
        sessionScheduleIds.add(startingCourtScheduleId.toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());

        stubExtendMultiDayHearing(hearingId.toString(), sessionScheduleIds, courtHouseId, courtRoomId, startDate, false);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
        final uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps seedSteps = givenARealHearingExists(hearingId);
        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);

        final String payload = loadAndSubstitute(
                "test-data/CROWN/update-hearing-for-listing/update-hearing-for-listing-crown-multiday-no-courtscheduleid.json",
                basePlaceholders(hearingId, courtCentreId, courtRoomId, startingCourtScheduleId, startDate, endDate, sessionStart));

        AbstractIT.restClient.postCommand(
                buildUpdateHearingUrl(hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                payload,
                getLoggedInHeader());

        // Core assertion: the CROWN update was routed through handleCrownMultiDayExtension and hit
        // POST /extendmultidayhearing/hearingslots with hearingId + full 1080-minute duration.
        verifyExtendMultiDayHearingCalled(hearingId.toString(), MULTI_DAY_TOTAL_DURATION_MINUTES);
        // Drain our own async aftermath before the test ends: viewstore projection (event listener)
        // AND the public hearing-changes-saved (event processor) — otherwise the next test's cleanup
        // races our in-flight events into JsonValue.NULL redelivery storms.
        awaitUpdateProjection(hearingId, startDate);
        seedSteps.verifyPublicEVentHearingChangesSaved(hearingId);
    }

    @Test
    void shouldCallExtendMultiDayHearingForFullDuration_whenNonSittingDaysAreSubmitted() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID startingCourtScheduleId = UUID.randomUUID();
        final LocalDate startDate = ItClock.today().plusDays(30);
        final LocalDate endDate = startDate.plusDays(57);
        final LocalDate nonSittingDay = startDate.plusDays(1);
        final ZonedDateTime sessionStart = startDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        final List<String> sessionScheduleIds = new ArrayList<>();
        sessionScheduleIds.add(startingCourtScheduleId.toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());
        sessionScheduleIds.add(UUID.randomUUID().toString());

        stubExtendMultiDayHearing(hearingId.toString(), sessionScheduleIds, courtHouseId, courtRoomId, startDate, false);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
        final uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps seedSteps = givenARealHearingExists(hearingId);
        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);

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
        verifyExtendMultiDayHearingCalled(hearingId.toString(), MULTI_DAY_TOTAL_DURATION_MINUTES);
        // Drain our own async aftermath — see shouldCallExtendMultiDayHearingOnListingCourtScheduler test.
        awaitUpdateProjection(hearingId, startDate);
        seedSteps.verifyPublicEVentHearingChangesSaved(hearingId);
    }

    @Test
    @ExpectedServerErrors("courtscheduler extend stub returns 422 -> ERROR 'Retrieve ...extend.multiday.hearing+json failed with status code:422' + ERROR 'extendMultiDayHearing from courtscheduler returned an error: NO_AVAILABILITY'")
    void shouldReturn422WithErrorCodeAndUnavailableDates_whenCourtschedulerRejectsExtendMultiDay() throws Exception {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID startingCourtScheduleId = UUID.randomUUID();
        final LocalDate startDate = ItClock.today().plusDays(30);
        final LocalDate endDate = startDate.plusDays(57);
        final ZonedDateTime sessionStart = startDate.atTime(9, 0).atZone(ZoneOffset.UTC);

        stubExtendMultiDayHearingFailure(
                hearingId.toString(), 422, "NO_AVAILABILITY",
                asList(startDate.plusDays(1).toString(), startDate.plusDays(2).toString()));
        givenReferenceDataStubsForUpdateHearing(courtCentreId, courtRoomId);
        givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);

        final String payload = loadAndSubstitute(
                "test-data/CROWN/update-hearing-for-listing/update-hearing-for-listing-crown-multiday-no-courtscheduleid.json",
                basePlaceholders(hearingId, courtCentreId, courtRoomId, startingCourtScheduleId, startDate, endDate, sessionStart));

        final javax.ws.rs.core.Response response = AbstractIT.restClient.postCommand(
                buildUpdateHearingUrl(hearingId),
                MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                payload,
                getLoggedInHeader());

        assertThat(response.getStatus(), is(422));
        final String body = response.readEntity(String.class);
        assertThat(body, containsString("\"errorCode\":\"NO_AVAILABILITY\""));
        assertThat(body, containsString("\"unavailableDates\""));
        verifyExtendMultiDayHearingCalled(hearingId.toString(), MULTI_DAY_TOTAL_DURATION_MINUTES);
    }

    /**
     * Seeds a real CROWN hearing (list-court-hearing + await its query-view projection) so the async
     * {@code update-hearing-for-listing-enriched} command finds an aggregate to update. The earlier
     * synthetic-hearingId design relied on the handler "just rolling back" — in practice the rollback
     * redelivers up to maxDeliveryAttempts, spraying 10× "There is no Hearing for this ID" ERROR pairs
     * per test into server.log and dead-lettering the command. The enrichment routing under test runs
     * BEFORE the handler either way; seeding only silences the aftermath.
     */
    private uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps givenARealHearingExists(final UUID hearingId) {
        final uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps seedSteps =
                new uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps(
                        uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData(hearingId));
        seedSteps.whenCaseIsSubmittedForListing();
        seedSteps.verifyHearingIsCreated(hearingId, 2);
        return seedSteps;
    }

    /**
     * Awaits the seeded hearing's async update aftermath (start-date-changed / v2 allocation events)
     * reaching the viewstore before the test ends. Without this the NEXT test's event-store cleanup
     * races our still-in-flight events: the event processor redelivers them with JsonValue.NULL
     * payloads (the known teardown-race signature) ~10 times each and dead-letters them.
     */
    private void awaitUpdateProjection(final UUID hearingId, final LocalDate expectedStartDate) {
        final String url = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearing"), hearingId.toString()));
        uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults(
                uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder
                        .requestParams(url, "application/vnd.listing.search.hearing+json")
                        .withHeader(uk.gov.justice.services.common.http.HeaderConstants.USER_ID, getLoggedInUser())
                        .build())
                .until(
                        uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status().is(javax.ws.rs.core.Response.Status.OK),
                        uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload()
                                .isJson(com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath(
                                        "$.startDate", org.hamcrest.CoreMatchers.is(expectedStartDate.toString()))));
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
        // The event processor's public hearing-confirmed V2 factory resolves the hearing's court centre
        // via referencedata organisation-units/{id} (ReferenceDataService.getOrganizationUnitById).
        // Without this stub the requester returns a NULL-payload envelope and the processor rollback-
        // redelivers the v2 allocation events 10x into the DLQ ("JsonValue.NULL" storm).
        uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataOrganisationUnitById(courtCentreId);
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

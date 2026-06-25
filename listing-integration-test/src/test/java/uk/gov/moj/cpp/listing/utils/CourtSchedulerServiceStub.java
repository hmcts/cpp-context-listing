package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.POLL_INTERVAL;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import org.awaitility.Awaitility;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

public class CourtSchedulerServiceStub {

    private static final String COURT_SCHEDULER_ENDPOINT = "/listingcourtscheduler-api/rest/courtscheduler";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";
    private static final String HEARING_SLOTS = "/hearingslots";
    private static final String VALIDATE_SESSION_AVAILABILITY = "/validate-session-availability";
    private static final String SEARCH_COURT_SCHEDULES_BY_ID = "/sessions";
    private static final String SESSIONS_PATH = "/sessions";
    private static final String HEARINGS_PATH = "/hearings";
    /** Content-type used by the new POST /hearings/{id} crown search-and-book endpoint. */
    private static final String CROWN_SEARCH_AND_BOOK_TYPE = "application/vnd.courtscheduler.crown.search.and.book+json";
    /** Content-type used by the new POST /hearings/{id} mags search-and-book endpoint. */
    private static final String MAGS_SEARCH_AND_BOOK_TYPE = "application/vnd.courtscheduler.mags.search.and.book+json";
    /** Kept for backward-compat constant name; body of crown-fallback stub now uses CROWN_SEARCH_AND_BOOK_TYPE. */
    private static final String CROWN_FALLBACK_SEARCH_BOOK_TYPE = CROWN_SEARCH_AND_BOOK_TYPE;
    private static final String COURTSCHEDULER_GET_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.get.hearing.slots+json";
    private static final String COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE = "application/vnd.courtscheduler.validate.session.availability+json";
    public static final String COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE = "application/vnd.courtscheduler.get.provisional.booking+json";
    public static final String ROTASL_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES = "stub-data/rotasl.get.hearing.slots.with-judiciaries.json";
    public static final String LISTING_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES_AND_SLOTTIMES = "stub-data/listing.get.hearing.slots.with-judiciaries-and-slotstarttimes.json";
    public static final String LISTING_SEARCH_HEARING_SLOTS_JSON = "stub-data/listing.search.hearing.slots.json";
    public static final String LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON = "stub-data/listing.search.hearing.slots.empty.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON = "stub-data/provisionalBookingSampleDataSingleCourtScheduleCountBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_JSON = "stub-data/provisionalBookingSampleDataMultipleCourtSchedulesCountBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_DURATION_BASED_JSON = "stub-data/provisionalBookingSampleDataSingleCourtScheduleDurationBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_DURATION_BASED_JSON = "stub-data/provisionalBookingSampleDataMultipleCourtSchedulesDurationBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_WITH_SESSION_DATE_JSON = "stub-data/provisionalBookingSampleDataSingleCourtScheduleCountBasedWithSessionDate.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULES_WITH_CUSTOM_PARAMS_JSON = "stub-data/provisionalBookingSampleDataCustomParams.json";
    private static final LocalTime DEFAULT_MORNING_START = LocalTime.of(10, 0, 0, 0);
    public static final String EXACT_HEARING_START_DATETIME = "exactHearingStartDateTime";

    static {
        WireMock.configureFor(CourtSchedulerServiceStub.HOST, 8080);
    }

    public static void stubUpdateAvailableHearingSlotsService() {
        stubFor(WireMock.post(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .willReturn(aResponse().withStatus(NO_CONTENT.getStatusCode())));
    }

    /** Stub DELETE /sessions/{hearingId} — release a booked session (was DELETE /hearingslots/{id}). */
    public static void stubDeleteAvailableHearingSlotsService(final String hearingId) {
        stubFor(delete(urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH + "/" + hearingId))
                .withHeader(CONTENT_TYPE, containing("application/vnd.courtscheduler.release.sessions+json"))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }

    /** Stub DELETE /sessions/.* for any hearingId — release a booked session (was DELETE /hearingslots/.*). */
    public static void stubDeleteAvailableHearingSlotsServiceForAnyHearing() {
        stubFor(delete(urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH + "/[0-9a-fA-F-]+"))
                .withHeader(CONTENT_TYPE, containing("application/vnd.courtscheduler.release.sessions+json"))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }

    public static void verifyDeleteAvailableHearingSlotsStubCommandInvoked(final String hearingId) {
        verifyDeleteAvailableHearingSlotsStubCommandInvokedNTimes(hearingId, 1);
    }

    public static void verifyDeleteAvailableHearingSlotsStubCommandIsNeverInvoked(final String hearingId) {
        verifyDeleteAvailableHearingSlotsStubCommandInvokedNTimes(hearingId, 0);
    }

    private static void verifyDeleteAvailableHearingSlotsStubCommandInvokedNTimes(final String hearingId, final int invocationCount) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = WireMock.deleteRequestedFor(urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH + "/" + hearingId));
            try {
                WireMock.verify(WireMock.exactly(invocationCount), requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }

    public static void verifyHearingSlotsSearchCalledWithJurisdiction(final String jurisdiction) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = WireMock.getRequestedFor(urlPathMatching(COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS))
                    .withQueryParam("jurisdiction", WireMock.equalTo(jurisdiction));
            try {
                WireMock.verify(WireMock.moreThanOrExactly(1), requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }

    public static void stubValidateSessionAvailability() {
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + VALIDATE_SESSION_AVAILABILITY)))
                .withHeader("Content-Type", containing(COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE))
                .withRequestBody(containing("courtScheduleIdList"))
                .withRequestBody(containing("duration"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub a successful response from GET /sessions (was /courtschedule/search.court-schedules-by-id)
     * for the given courtScheduleId. Returned wire shape mirrors what the real courtscheduler emits via
     * {@code CourtSchedulerApi.searchCourtSchedulesById} - FLAT: each courtSchedules[] element
     * is a single CourtSchedule with isDraft at the top level.
     *
     * @param courtScheduleId the id under query
     * @param isDraft         draft state to report - drives whether
     *                        {@code listing.query.court.schedule.draft.status} returns
     *                        {@code anyDraft=true} (strip) or {@code anyDraft=false} (preserve)
     */
    /**
     * Stub courtscheduler's GET /sessions (search-by-id) response with an explicit choice of
     * draft-field name. Real-world Jackson serialisation of CourtSchedule emits one of:
     *   - {@code "isDraft": <bool>} (from the setter convention)
     *   - {@code "draft": <bool>}   (from the boolean-getter "is" prefix stripping)
     * The parser must accept either, so tests assert both.
     *
     * @param courtScheduleId the id under query
     * @param draftKey        wire field name to emit - either "isDraft" or "draft"
     * @param draft           value for that field
     */
    public static void stubSearchCourtSchedulesByIdWithKey(final String courtScheduleId,
                                                            final String draftKey,
                                                            final boolean draft) {
        final String body = "{\"courtSchedules\":[{\"courtScheduleId\":\"" + courtScheduleId
                + "\",\"" + draftKey + "\":" + draft + "}]}";
        stubFor(get(urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub GET /sessions (was /courtschedule/search.court-schedules-by-id) to return a 500. Exercises the listing
     * adapter's fail-closed path (anyDraft=true on courtscheduler error).
     */
    public static void stubSearchCourtSchedulesByIdServerError() {
        stubFor(get(urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH)))
                .willReturn(aResponse().withStatus(500)
                        .withBody("internal server error")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub GET /sessions (was /courtschedule/search.court-schedules-by-id) so a CROWN bookingReference
     * (which IS the courtScheduleId) resolves to a single session echoing the supplied courtHouse / room / date.
     * The listing command resolves the bookingReference here
     * (see {@code CourtScheduleEnrichmentService.promoteCrownBookingReferenceToBookedSlot}).
     * Scoped by the {@code ids} query param so it answers only for this hearing's bookingReference
     * and never pollutes other tests (WireMock stubs persist across IT classes in a suite).
     */
    public static void stubSearchCourtSchedulesByIdSession(final String courtScheduleId,
                                                           final UUID courtHouseId,
                                                           final UUID courtRoomId,
                                                           final LocalDate sessionDate,
                                                           final ZonedDateTime hearingStartTime,
                                                           final boolean isDraft) {
        final StringBuilder session = new StringBuilder();
        session.append("{\"courtScheduleId\":\"").append(courtScheduleId).append("\"");
        if (courtHouseId != null) {
            session.append(",\"courtHouseId\":\"").append(courtHouseId).append("\"");
        }
        if (courtRoomId != null) {
            session.append(",\"courtRoomId\":\"").append(courtRoomId).append("\"");
        }
        if (sessionDate != null) {
            session.append(",\"sessionDate\":\"").append(sessionDate).append("\"");
        }
        if (hearingStartTime != null) {
            session.append(",\"hearingStartTime\":\"").append(hearingStartTime).append("\"");
        }
        session.append(",\"isDraft\":").append(isDraft).append("}");
        final String body = "{\"courtSchedules\":[" + session + "]}";

        stubFor(get(urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH)))
                .atPriority(2)
                .withQueryParam("ids", containing(courtScheduleId))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    // --- Crown fallback search-and-book stubs (POST /hearings/{hearingId}, crown.search.and.book, durationInMinutes <= 360) ---

    /**
     * Stub a successful 200 response from POST /hearings/{hearingId} with content-type
     * {@code crown.search.and.book} for the single-day (fallback) path.
     * Discriminated from the multi-day stub by {@code durationInMinutes <= 360} body matcher.
     * Response carries flat top-level fields (hearingId, courtScheduleId, source,
     * courtRoomId, sessionDate, sessionStartTime, sessionEndTime, durationInMinutes, isDraft,
     * businessType, overbooked) plus {@code "sessions":[]} (empty, single-day shape).
     *
     * @param hearingId       hearingId echoed back in the response
     * @param courtScheduleId the booked session id to return (as if courtscheduler picked it)
     * @param isDraft         draft state of the picked session — drives ALLOCATED/UNALLOCATED downstream
     * @param source          expected source label on the incoming request; verified via request-body match
     */
    public static void stubCrownFallbackSearchAndBookSuccess(final String hearingId,
                                                              final String courtScheduleId,
                                                              final boolean isDraft,
                                                              final String source) {
        final String sessionDate = ItClock.today().plusDays(1).toString();
        final String startTime = sessionDate + "T09:00:00Z";
        final String endTime = sessionDate + "T17:00:00Z";
        final String body = format(
                "{\"hearingId\":\"%s\",\"courtScheduleId\":\"%s\",\"courtRoomId\":731816," +
                        "\"sessionDate\":\"%s\",\"sessionStartTime\":\"%s\",\"sessionEndTime\":\"%s\"," +
                        "\"durationInMinutes\":10,\"isDraft\":%s,\"businessType\":\"CR\"," +
                        "\"source\":\"%s\",\"overbooked\":false,\"sessions\":[]}",
                hearingId, courtScheduleId, sessionDate, startTime, endTime, isDraft, source);

        // durationInMinutes <= 360 distinguishes single-day (crown fallback) from multi-day
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(containing("\"source\":\"" + source + "\""))
                .withRequestBody(matchingJsonPath("$.durationInMinutes", matching("[0-9]|[1-9][0-9]|[12][0-9]{2}|3[0-5][0-9]|360")))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /** Stub 404 "no session found" on POST /hearings/{id} (crown.search.and.book, single-day)
     * — listing-side translates to CrownFallbackNoSessionException. */
    public static void stubCrownFallbackSearchAndBookNotFound() {
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(matchingJsonPath("$.durationInMinutes", matching("[0-9]|[1-9][0-9]|[12][0-9]{2}|3[0-5][0-9]|360")))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /** Stub 400 "invalid request" on POST /hearings/{id} (crown.search.and.book, single-day)
     * — used for defensive coverage; the listing-side multi-day guard fires first. */
    public static void stubCrownFallbackSearchAndBookBadRequest() {
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(matchingJsonPath("$.durationInMinutes", matching("[0-9]|[1-9][0-9]|[12][0-9]{2}|3[0-5][0-9]|360")))
                .willReturn(aResponse()
                        .withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("{\"error\":\"durationInMinutes exceeds single-day cap\"}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /** Verify POST /hearings/{id} (crown.search.and.book) was called with the expected source label in the body. */
    public static void verifyCrownFallbackSearchAndBookCalledWithSource(final String source) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            try {
                WireMock.verify(WireMock.postRequestedFor(urlPathMatching(
                        COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+"))
                        .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                        .withRequestBody(containing("\"source\":\"" + source + "\"")));
                return true;
            } catch (VerificationException e) {
                return false;
            }
        });
    }

    /** Verify POST /hearings/{id} (crown.search.and.book) was NEVER called
     * (regression guard for MAGS / already-allocated CROWN). */
    public static void verifyCrownFallbackSearchAndBookNeverCalled() {
        WireMock.verify(0, WireMock.postRequestedFor(urlPathMatching(
                COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+"))
                .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE)));
    }

    // --- Multi-day search-and-book stubs (POST /hearings/{hearingId}, crown.search.and.book, durationInMinutes > 360) ---

    /**
     * Stub a successful response from POST /hearings/{hearingId} (crown.search.and.book) returning the
     * supplied court schedule sessions under the key {@code "sessions"} (was {@code "courtSchedules"}
     * on the old GET /multidaysearchandbook/hearingslots endpoint). Used to drive the CROWN multi-day
     * update path — the listing service passes the starting courtScheduleId + total duration, courtscheduler
     * returns N consecutive sessions that together cover the duration.
     * Discriminated from the crown-fallback single-day stub by {@code durationInMinutes > 360}.
     */
    public static void stubMultiDaySearchAndBook(final List<String> courtScheduleIds,
                                                  final UUID courtHouseId,
                                                  final UUID courtRoomId,
                                                  final LocalDate firstSessionDate,
                                                  final boolean isDraft) {
        final StringBuilder body = new StringBuilder();
        body.append("{\"sessions\":[");
        for (int i = 0; i < courtScheduleIds.size(); i++) {
            if (i > 0) {
                body.append(",");
            }
            final LocalDate sessionDate = firstSessionDate.plusDays(i);
            body.append("{")
                    .append("\"courtScheduleId\":\"").append(courtScheduleIds.get(i)).append("\",")
                    .append("\"courtHouseId\":\"").append(courtHouseId).append("\",")
                    .append("\"courtRoomId\":\"").append(courtRoomId).append("\",")
                    .append("\"sessionDate\":\"").append(sessionDate).append("\",")
                    .append("\"hearingStartTime\":\"").append(sessionDate).append("T09:00:00Z\",")
                    .append("\"isDraft\":").append(isDraft)
                    .append("}");
        }
        body.append("]}");

        // durationInMinutes > 360 distinguishes multi-day from single-day (crown fallback)
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(matchingJsonPath("$.durationInMinutes", matching("3[6-9][1-9]|[4-9][0-9]{2}|[1-9][0-9]{3,}")))
                .atPriority(1)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /**
     * Verify that POST /hearings/{id} (crown.search.and.book) was called with the expected courtScheduleId
     * + total duration in the request body. Proves the CROWN update path correctly routed multi-day through
     * the CourtSchedule-first flow and didn't regress to the startDate→endDate expansion.
     */
    public static void verifyMultiDaySearchAndBookCalled(final String courtScheduleId, final int durationInMinutes) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            try {
                WireMock.verify(WireMock.postRequestedFor(urlPathMatching(
                        COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+"))
                        .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                        .withRequestBody(containing("\"courtScheduleId\":\"" + courtScheduleId + "\""))
                        .withRequestBody(containing("\"durationInMinutes\":" + durationInMinutes)));
                return true;
            } catch (VerificationException e) {
                return false;
            }
        });
    }

    /** Regression guard: CROWN update without a courtScheduleId must NOT trigger multi-day search-and-book
     * via POST /hearings/{id} (crown.search.and.book). */
    public static void verifyMultiDaySearchAndBookNeverCalled() {
        WireMock.verify(0, WireMock.postRequestedFor(urlPathMatching(
                COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+"))
                .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(matchingJsonPath("$.durationInMinutes", matching("3[6-9][1-9]|[4-9][0-9]{2}|[1-9][0-9]{3,}"))));
    }

    // --- Extend multi-day hearing stubs (SPRDT-901: CROWN update-hearing-for-listing multi-day path) ---
    // Endpoint reshaped: was POST /extendmultidayhearing/hearingslots, now PATCH /hearings/{hearingId}.

    private static final String COURTSCHEDULER_EXTEND_MULTIDAY_TYPE =
            "application/vnd.courtscheduler.extend.multiday.hearing+json";

    /**
     * Stub a successful PATCH to /hearings/{hearingId} (extend.multiday.hearing) returning the supplied
     * court schedule sessions under key {@code "courtSchedules"}, scoped to the supplied hearingId.
     * SPRDT-901 routes CROWN multi-day updates here instead of the old GET-based endpoint.
     * Courtscheduler receives the full duration and returns N sessions to use as the rebuilt hearingDays.
     *
     * <p><b>Scoping:</b> WireMock stubs persist across IT classes in the same suite. Without a body
     * matcher, this stub would intercept every other IT that extends a CROWN hearing into multi-day
     * (e.g. HearingCsvReportIT) and return these synthetic courtSchedules — corrupting their hearingDays.
     * The hearingId in the path makes the stub apply only to the test's own hearing.
     */
    public static void stubExtendMultiDayHearing(final String hearingId,
                                                  final List<String> courtScheduleIds,
                                                  final UUID courtHouseId,
                                                  final UUID courtRoomId,
                                                  final LocalDate firstSessionDate,
                                                  final boolean isDraft) {
        final StringBuilder body = new StringBuilder();
        body.append("{\"courtSchedules\":[");
        for (int i = 0; i < courtScheduleIds.size(); i++) {
            if (i > 0) {
                body.append(",");
            }
            final LocalDate sessionDate = firstSessionDate.plusDays(i);
            // Wire-shape note: the courtscheduler endpoint serialises the domain CourtSchedule, which
            // only has "sessionStartTime". Listing's buildHearingDaysFromMultiDaySessions reads
            // getSessionStartTime() — emitting "hearingStartTime" leaves HearingDay.startTime null.
            // Match the wire, not the old RAML example. Response key stays "courtSchedules" (unchanged).
            body.append("{")
                    .append("\"courtScheduleId\":\"").append(courtScheduleIds.get(i)).append("\",")
                    .append("\"courtHouseId\":\"").append(courtHouseId).append("\",")
                    .append("\"courtRoomId\":\"").append(courtRoomId).append("\",")
                    .append("\"sessionDate\":\"").append(sessionDate).append("\",")
                    .append("\"sessionStartTime\":\"").append(sessionDate).append("T09:00:00.000Z\",")
                    .append("\"isDraft\":").append(isDraft)
                    .append("}");
        }
        body.append("]}");

        stubFor(patch(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(COURTSCHEDULER_EXTEND_MULTIDAY_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /**
     * Verify that PATCH /hearings/{id} (extend.multiday.hearing) was called with a body containing the
     * supplied hearingId and durationInMinutes. Proves SPRDT-901 routing: the CROWN multi-day update
     * was sent to courtscheduler's reshape endpoint with the full requested duration.
     */
    public static void verifyExtendMultiDayHearingCalled(final String hearingId, final int durationInMinutes) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            try {
                WireMock.verify(WireMock.patchRequestedFor(urlPathMatching(
                        COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+"))
                        .withHeader(CONTENT_TYPE, containing(COURTSCHEDULER_EXTEND_MULTIDAY_TYPE))
                        .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                        .withRequestBody(containing("\"durationInMinutes\":" + durationInMinutes)));
                return true;
            } catch (VerificationException e) {
                return false;
            }
        });
    }

    /** Regression guard: single-day CROWN updates / non-CROWN updates must NOT call PATCH /hearings/{id}
     * (extend.multiday.hearing). */
    public static void verifyExtendMultiDayHearingNeverCalled() {
        WireMock.verify(0, WireMock.patchRequestedFor(urlPathMatching(
                COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")));
    }

    /**
     * SPRDT-902: stub a 422 typed-failure response from PATCH /hearings/{hearingId} (extend.multiday.hearing),
     * scoped to a specific hearingId. Body shape mirrors the courtscheduler RAML error contract.
     */
    public static void stubExtendMultiDayHearingFailure(final String hearingId,
                                                         final int statusCode,
                                                         final String errorCode,
                                                         final List<String> unavailableDates) {
        final StringBuilder body = new StringBuilder("{\"errorCode\":\"").append(errorCode).append("\"");
        if (unavailableDates != null && !unavailableDates.isEmpty()) {
            body.append(",\"unavailableDates\":[");
            for (int i = 0; i < unavailableDates.size(); i++) {
                if (i > 0) {
                    body.append(",");
                }
                body.append("\"").append(unavailableDates.get(i)).append("\"");
            }
            body.append("]");
        }
        body.append("}");

        stubFor(patch(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(COURTSCHEDULER_EXTEND_MULTIDAY_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .willReturn(aResponse().withStatus(statusCode)
                        .withBody(body.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubValidateSessionAvailabilityFailure() {
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + VALIDATE_SESSION_AVAILABILITY)))
                .withHeader("Content-Type", containing(COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("{\"validationResult\":{\"status\":\"FAILURE\",\"validationError\":\"duration is required\"}}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetAvailableHearingSlots(boolean isEmpty) {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .withQueryParam("sessionEndDate", matching("2020-10-11"))
                .withQueryParam("jurisdiction", matching("MAGISTRATES"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(isEmpty ? CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON : CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_SLOTS_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetAvailableHearingSlots() {
        stubFor(get(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_SLOTS_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetAvailableHearingSlotsWithOverbookedSlots(boolean showOverbookedSlots) {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .withQueryParam("sessionEndDate", matching("2020-10-11"))
                .withQueryParam("showOverbookedSlots", matching(String.valueOf(showOverbookedSlots)))
                .withQueryParam("jurisdiction", matching("MAGISTRATES"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(showOverbookedSlots ? LISTING_SEARCH_HEARING_SLOTS_JSON : LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetHearingIds(boolean isEmpty) {
        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("1900-01-01"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("panel", matching("ADULT,YOUTH"))
                .withQueryParam("ouCode", matching("B01LY00"))
                .withQueryParam("sessionEndDate", matching("9999-01-01"))
                .withHeader("Accept", containing("application/vnd.courtscheduler.get.hearing.ids+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(isEmpty ? "stub-data/listing.search.hearing.ids.empty.json" : "stub-data/listing.search.hearing.ids.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetHearingIds(final Instant exactHearingStartDateTime) {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("1900-01-01"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("panel", matching("ADULT,YOUTH"))
                .withQueryParam("ouCode", matching("B01LY00"))
                .withQueryParam("sessionEndDate", matching("9999-01-01"))
                .withQueryParam(EXACT_HEARING_START_DATETIME, matching(exactHearingStartDateTime.toString()))
                .withHeader("Accept", containing("application/vnd.courtscheduler.get.hearing.ids+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload( "stub-data/listing.search.hearing.ids-single.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate sessionDate, final Map<String, String> values) {
        final String courtRoomId = Optional.ofNullable(values.get("courtRoomId")).orElse("fce80cd4-0c00-3c30-9471-2c2ee7a52453");
        final String hearingStartTime = sessionDate.atTime(LocalTime.of(10, 0,0,0)).atZone(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneOffset.UTC).toString();

        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_WITH_SESSION_DATE_JSON)
                                .replace("%SESSION_DATE%", sessionDate.toString())
                                .replace("%COURT_ROOM_ID%", courtRoomId)
                                .replace("%HEARING_START_TIME%", hearingStartTime))
                        .withHeader("Content-Type", "application/json")
                ));
    }

    public static void stubProvisionalBookingWithCustomParams(final Map<String, String> values) {
        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULES_WITH_CUSTOM_PARAMS_JSON)
                                .replace("%SESSION_DATE%", Optional.of(values.get("SESSION_DATE")).orElse(ItClock.today().toString()))
                                .replace("%COURT_ROOM_ID%", Optional.of(values.get("COURT_ROOM_ID")).orElse(UUID.randomUUID().toString()))
                                .replace("%COURT_SCHEDULE_ID%", Optional.of(values.get("COURT_SCHEDULE_ID")).orElse(UUID.randomUUID().toString()))
                                .replace("%BOOKING_ID%", Optional.of(values.get("BOOKING_ID")).orElse(UUID.randomUUID().toString()))
                                .replace("%COURT_CENTRE_ID%", Optional.of(values.get("COURT_CENTRE_ID")).orElse(UUID.randomUUID().toString()))
                                .replace("%HEARING_START_TIME%", Optional.of(values.get("HEARING_START_TIME")).orElse(LocalDate.parse(values.get("SESSION_DATE")).atTime(LocalTime.of(10, 0, 0, 0)).atZone(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneOffset.UTC).toString()))
                        )
                        .withHeader("Content-Type", "application/json")
                ));
    }




    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased() {
        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased() {
        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased() {
        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_DURATION_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(final Map<String, String> courtRoomScedules, final String courtCentreId) {
        String payload = getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_DURATION_BASED_JSON);
        String dateStr;
        ZonedDateTime hearingStartTime;
        int idx = 0;
        for (Map.Entry<String, String> schedule : courtRoomScedules.entrySet()) {
            ++idx;
            dateStr = schedule.getKey();
            hearingStartTime = LocalDate.parse(dateStr)
                    .atTime(LocalTime.of(10, 0))         // 10:00:00
                    .atZone(ZoneId.of("Europe/London")) // Europe/London zone
                    .withZoneSameInstant(ZoneOffset.UTC); // convert to UTC (Z)

            payload = payload.replace("SESSION_DATE_" + (idx), schedule.getKey());
            payload = payload.replace("COURT_ROOM_ID_" + (idx), schedule.getValue());
            payload = payload.replace("HEARING_START_TIME_" + (idx), hearingStartTime.format(DateTimeFormatter.ISO_INSTANT));
            payload = payload.replace("COURT_CENTRE_ID", courtCentreId);
        }

        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static JsonObject stubGetAvailableHearingSlotsWithQueryParams(boolean isEmpty,
                                                                         final String courtRoomId,
                                                                         final String ouCode,
                                                                         final String sessionStartDate,
                                                                         final String sessionEndDate) throws IOException {
        final String getHearingSlotsPayload = getPayload(isEmpty ? CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON :
                CourtSchedulerServiceStub.ROTASL_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES)
                .replaceAll("COURT_ROOM_ID", courtRoomId)
                .replaceAll("OU_CODE", ouCode)
                .replaceAll("SESSION_DATE", sessionStartDate);

        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching(sessionStartDate))
                .withQueryParam("sessionEndDate", matching(sessionEndDate))
                .withQueryParam("ouCode", matching(ouCode))
                .withQueryParam("pageSize", matching("1"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("courtRoomId", matching(courtRoomId))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getHearingSlotsPayload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));

        return FileUtil.payloadToObject(getHearingSlotsPayload);
    }

    public static JsonObject stubGetAvailableHearingSlotsWithQueryParams(final UpdatedHearingData updatedHearingData) throws IOException {
        final StringBuilder hearingSlotsJson = new StringBuilder();
        hearingSlotsJson.append("{\n");
        hearingSlotsJson.append("  \"results\": ").append(updatedHearingData.getNonDefaultDays().size()).append(",\n");
        hearingSlotsJson.append("  \"pageCount\": 1,\n");
        hearingSlotsJson.append("  \"hearingSlots\": [\n");

        boolean isFirst = true;
        for (NonDefaultDayData nonDefaultDay : updatedHearingData.getNonDefaultDays()) {
            if (!isFirst) {
                hearingSlotsJson.append(",\n");
            }

            final String courtScheduleId = nonDefaultDay.getCourtScheduleId().orElse("8e837de0-743a-4a2c-9db3-b2e678c48729");
            final String courtRoomId = nonDefaultDay.getRoomId().orElse(updatedHearingData.getCourtRoomId() != null ? updatedHearingData.getCourtRoomId().toString() : UUID.randomUUID().toString());
            final String ouCode = nonDefaultDay.getOucode().orElse("B01LY00");
            final String sessionDate = ZonedDateTime.parse(nonDefaultDay.getStartTime()).toLocalDate().toString();
            final String session = nonDefaultDay.getSession().orElse("AM");

            hearingSlotsJson.append("    {\n");
            hearingSlotsJson.append("      \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
            hearingSlotsJson.append("      \"listingProfileId\": \"CS2339681\",\n");
            hearingSlotsJson.append("      \"ouCode\": \"").append(ouCode).append("\",\n");
            hearingSlotsJson.append("      \"courtRoomId\": \"").append(courtRoomId).append("\",\n");
            hearingSlotsJson.append("      \"courtRoomNumber\": 2564,\n");
            hearingSlotsJson.append("      \"courtHouseId\": \"").append(updatedHearingData.getCourtCentreId().toString()).append("\",\n");
            hearingSlotsJson.append("      \"courtHouseName\": \"").append(updatedHearingData.getName()).append("\",\n");
            hearingSlotsJson.append("      \"courtRoomName\": \"Courtroom 10\",\n");
            hearingSlotsJson.append("      \"operationalUnit\": \"6\",\n");
            hearingSlotsJson.append("      \"businessType\": \"TRL\",\n");
            hearingSlotsJson.append("      \"panel\": \"").append(updatedHearingData.getPanel()).append("\",\n");
            hearingSlotsJson.append("      \"courtSession\": \"").append(session).append("\",\n");
            hearingSlotsJson.append("      \"slotBased\": false,\n");
            hearingSlotsJson.append("      \"sessionDate\": \"").append(sessionDate).append("\",\n");
            hearingSlotsJson.append("      \"maxSlots\": 0,\n");
            hearingSlotsJson.append("      \"maxDuration\": 0,\n");
            hearingSlotsJson.append("      \"availableSlots\": 0,\n");
            hearingSlotsJson.append("      \"availableDuration\": 0,\n");
            hearingSlotsJson.append("      \"judiciaries\": [\n");

            // Add judiciary information if available
            if (updatedHearingData.getJudiciary() != null && !updatedHearingData.getJudiciary().isEmpty()) {
                boolean isFirstJudiciary = true;
                for (int i = 0; i < updatedHearingData.getJudiciary().size(); i++) {
                    if (!isFirstJudiciary) {
                        hearingSlotsJson.append(",\n");
                    }
                    hearingSlotsJson.append("        {\n");
                    hearingSlotsJson.append("          \"judiciaryId\": \"").append(UUID.randomUUID().toString()).append("\",\n");
                    hearingSlotsJson.append("          \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
                    hearingSlotsJson.append("          \"courtListingProfileId\": \"CS2339681\",\n");
                    hearingSlotsJson.append("          \"judiciaryType\": \"MAGISTRATE\",\n");
                    hearingSlotsJson.append("          \"deputy\": ").append(i > 0 ? "true" : "false").append(",\n");
                    hearingSlotsJson.append("          \"benchChairman\": ").append(i == 0 ? "true" : "false").append("\n");
                    hearingSlotsJson.append("        }");
                    isFirstJudiciary = false;
                }
            }

            hearingSlotsJson.append("\n      ],\n");
            hearingSlotsJson.append("      \"slotStartTimes\": [\n");
            hearingSlotsJson.append("        {\n");
            hearingSlotsJson.append("          \"hearingStartTime\": \"").append(nonDefaultDay.getStartTime()).append("\",\n");
            hearingSlotsJson.append("          \"count\": 1\n");
            hearingSlotsJson.append("        }\n");
            hearingSlotsJson.append("      ],\n");
            hearingSlotsJson.append("      \"sessionStartTime\": \"").append(nonDefaultDay.getStartTime()).append("\"\n");
            hearingSlotsJson.append("    }");

            isFirst = false;
        }

        hearingSlotsJson.append("\n  ]\n");
        hearingSlotsJson.append("}");

        final String payloadString = hearingSlotsJson.toString();

        // Create WireMock stub that matches any query parameters
        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching(".*"))
                .withQueryParam("sessionEndDate", matching(".*"))
                .withQueryParam("ouCode", matching(".*"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("pageNumber", matching(".*"))
                .withQueryParam("courtRoomId", matching(".*"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payloadString)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));

        return FileUtil.payloadToObject(payloadString);
    }

    public static JsonObject stubGetAvailableHearingSlotsWithQueryParamsForPayloadIT(boolean isEmpty,
                                                                         final String courtRoomId,
                                                                         final String courtScheduleId,
                                                                         final String ouCode,
                                                                         final String sessionStartDate,
                                                                         final ZonedDateTime hearingStartTime) throws IOException {
        final String getHearingSlotsPayload = getPayload(isEmpty ? CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON :
                        CourtSchedulerServiceStub.LISTING_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES_AND_SLOTTIMES)
                .replaceAll("%%COURT_ROOM_ID%%", courtRoomId)
                .replaceAll("%%COURT_SCHEDULE_ID%%", courtScheduleId)
                .replaceAll("%%OU_CODE%%", ouCode)
                .replaceAll("%%SESSION_DATE%%", sessionStartDate)
                .replaceAll("%%HEARING_START_TIME%%", hearingStartTime.toString());

        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
//                .withQueryParam("sessionStartDate", WireMock.matching(sessionStartDate))
                .withQueryParam("sessionStartDate", matching(".*"))
//                .withQueryParam("sessionEndDate", WireMock.matching(sessionEndDate))
                .withQueryParam("sessionEndDate", matching(".*"))
//                .withQueryParam("ouCode", WireMock.matching(ouCode))
                .withQueryParam("ouCode", matching(".*"))
                .withQueryParam("pageSize", matching("1"))
                .withQueryParam("pageNumber", matching("1"))
//                .withQueryParam("courtRoomId", WireMock.matching(courtRoomId))
                .withQueryParam("courtRoomId", matching(".*"))
//                .withQueryParam("panel", WireMock.matching("ADULT,YOUTH"))
                .withQueryParam("panel", matching(".*"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getHearingSlotsPayload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));

        return FileUtil.payloadToObject(getHearingSlotsPayload);
    }

    public static void stubSessionEndDateEmptyRequest() {
        stubFor(get(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .withQueryParam("jurisdiction", matching("MAGISTRATES"))
                .withHeader("Accept", containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria sessionEndDate cannot be null")
                ));
    }

    public static void stubListHearingInCourtSessions(final String hearingId, final String courtScheduleId, final ZonedDateTime hearingStartTime) {
        final String payload = "{\n" +
                "  \"hearings\": [\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime.toString() + "\",\n" +
                "      \"duration\": 20\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing("hearings"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Scoped variant of {@link #stubListHearingInCourtSessions(String, String, ZonedDateTime)}: matches only the
     * PUT whose request body carries this {@code courtScheduleId}. Needed when one command lists several CROWN
     * hearings (e.g. list-next-hearings-v2 with two next hearings) — each hearing's list call must resolve to its
     * own session rather than the last broad stub registered winning for all of them.
     */
    public static void stubListHearingInCourtSessionsForCourtSchedule(final String hearingId, final String courtScheduleId, final ZonedDateTime hearingStartTime) {
        final String payload = "{\n" +
                "  \"hearings\": [\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime.toString() + "\",\n" +
                "      \"duration\": 20\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .atPriority(2)
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing(courtScheduleId))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubListHearingInCourtSessions(final String hearingId, final String courtScheduleId, final String hearingStartTime) {
        final String payload = "{\n" +
                "  \"hearings\": [\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime + "\",\n" +
                "      \"duration\": 20\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing("hearings"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubListHearingInCourtSessionsWithJudiciary(final String hearingId, final String courtScheduleId, final ZonedDateTime hearingStartTime, final Integer duration, final List<JudicialRoleData> judiciaries) {
        StringBuilder payload = new StringBuilder();
        payload.append("{\n");
        payload.append("  \"hearings\": [\n");
        payload.append("    {\n");
        payload.append("      \"hearingId\": \"").append(hearingId).append("\",\n");
        payload.append("      \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
        payload.append("      \"hearingStartTime\": \"").append(hearingStartTime.toString()).append("\",\n");
        payload.append("      \"duration\": ").append(duration).append(",\n");
        payload.append("      \"judiciaries\": [\n");

        if (judiciaries != null && !judiciaries.isEmpty()) {
            for (int i = 0; i < judiciaries.size(); i++) {
                if (i > 0) {
                    payload.append(",\n");
                }
                JudicialRoleData judiciary = judiciaries.get(i);
                payload.append("        {\n");
                payload.append("          \"judiciaryId\": \"").append(judiciary.getJudicialId()).append("\",\n");
                payload.append("          \"rotaJudiciaryId\": \"MA").append(format("%04d", 2000 + i)).append("\",\n");
                payload.append("          \"title\": \"Mr\",\n");
                payload.append("          \"surname\": \"DefaultSurname").append(i + 1).append("\",\n");
                payload.append("          \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
                payload.append("          \"judiciaryType\": \"").append(judiciary.getJudicialRoleType().getJudiciaryType()).append("\",\n");

                // Determine position based on chairman status and index
                String position;
                if (judiciary.getIsBenchChairman().orElse(false)) {
                    position = "CHAIR";
                } else if (i == 1) {
                    position = "LEFT_WINGER";
                } else if (i == 2) {
                    position = "RIGHT_WINGER";
                } else {
                    position = "CHAIR"; // Default fallback
                }
                payload.append("          \"position\": \"").append(position).append("\",\n");

                payload.append("          \"active\": false,\n");
                payload.append("          \"createdOn\": \"2025-03-12T20:27:00.724+00:00\",\n");
                payload.append("          \"updatedOn\": \"2025-03-12T20:27:00.724+00:00\",\n");
                payload.append("          \"deputy\": ").append(judiciary.getIsDeputy().orElse(false)).append(",\n");
                payload.append("          \"benchChairman\": ").append(judiciary.getIsBenchChairman().orElse(false)).append("\n");
                payload.append("        }");
            }
        }

        payload.append("\n      ]\n");
        payload.append("    }\n");
        payload.append("  ]\n");
        payload.append("}");

        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing("hearings"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubListHearingInCourtSessionsWithMultipleSchedules(final String hearingId, final String courtScheduleId1, final String courtScheduleId2, final ZonedDateTime hearingStartTime , final Integer duration) {
        final String payload = "{\n" +
                "  \"hearings\": [\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId1 + "\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime.toString() + "\",\n" +
                "      \"duration\": " + duration + "\n" +
                "    },\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId2 + "\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime.toString() + "\",\n" +
                "      \"duration\": " + duration + "\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing("hearings"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubListHearingInCourtSessionsWithMultipleSchedules(final UpdatedHearingData updatedHearingData) {
        final StringBuilder hearingsJson = new StringBuilder();
        hearingsJson.append("{\n");
        hearingsJson.append("  \"hearings\": [\n");

        boolean isFirst = true;
        for (NonDefaultDayData nonDefaultDay : updatedHearingData.getNonDefaultDays()) {
            if (!isFirst) {
                hearingsJson.append(",\n");
            }

            final Integer duration = nonDefaultDay.getDuration().orElse(20);
            final String courtScheduleId = nonDefaultDay.getCourtScheduleId().orElse("8e837de0-743a-4a2c-9db3-b2e678c48729");

            hearingsJson.append("    {\n");
            hearingsJson.append("      \"hearingId\": \"").append(updatedHearingData.getHearingId()).append("\",\n");
            hearingsJson.append("      \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
            hearingsJson.append("      \"hearingStartTime\": \"").append(nonDefaultDay.getStartTime()).append("\",\n");
            hearingsJson.append("      \"duration\": ").append(duration);

            // Add judiciary information if available
            List<JudicialRoleData> judiciaries = updatedHearingData.getJudiciary();
            if (isNotEmpty(judiciaries)) {
                hearingsJson.append(",\n");
                hearingsJson.append("      \"judiciaries\": [\n");

                for (int i = 0; i < judiciaries.size(); i++) {
                    if (i > 0) {
                        hearingsJson.append(",\n");
                    }
                    JudicialRoleData judiciary = judiciaries.get(i);
                    hearingsJson.append("        {\n");
                    hearingsJson.append("          \"judiciaryId\": \"").append(judiciary.getJudicialId()).append("\",\n");
                    hearingsJson.append("          \"rotaJudiciaryId\": \"MA").append(format("%04d", 2000 + i)).append("\",\n");
                    hearingsJson.append("          \"title\": \"Mr\",\n");
                    hearingsJson.append("          \"surname\": \"DefaultSurname").append(i + 1).append("\",\n");
                    hearingsJson.append("          \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
                    hearingsJson.append("          \"judiciaryType\": \"").append(judiciary.getJudicialRoleType().getJudiciaryType()).append("\",\n");

                    // Determine position based on chairman status and index
                    String position;
                    if (judiciary.getIsBenchChairman().orElse(false)) {
                        position = "CHAIR";
                    } else if (i == 1) {
                        position = "LEFT_WINGER";
                    } else if (i == 2) {
                        position = "RIGHT_WINGER";
                    } else {
                        position = "CHAIR"; // Default fallback
                    }
                    hearingsJson.append("          \"position\": \"").append(position).append("\",\n");

                    hearingsJson.append("          \"active\": false,\n");
                    hearingsJson.append("          \"createdOn\": \"2025-03-12T20:27:00.724+00:00\",\n");
                    hearingsJson.append("          \"updatedOn\": \"2025-03-12T20:27:00.724+00:00\",\n");
                    hearingsJson.append("          \"deputy\": ").append(judiciary.getIsDeputy().orElse(false)).append(",\n");
                    hearingsJson.append("          \"benchChairman\": ").append(judiciary.getIsBenchChairman().orElse(false)).append("\n");
                    hearingsJson.append("        }");
                }

                hearingsJson.append("\n      ]");
            }

            hearingsJson.append("\n");
            hearingsJson.append("    }");

            isFirst = false;
        }

        hearingsJson.append("\n  ]\n");
        hearingsJson.append("}");

        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing("hearings"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(hearingsJson.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubListHearingInCourtSessionsWithMultipleSchedulesWithJudiciaries(final UpdatedHearingData updatedHearingData,final List<JudicialRoleData> judiciaries) {
        final StringBuilder hearingsJson = new StringBuilder();
        hearingsJson.append("{\n");
        hearingsJson.append("  \"hearings\": [\n");

        boolean isFirst = true;
        for (NonDefaultDayData nonDefaultDay : updatedHearingData.getNonDefaultDays()) {
            if (!isFirst) {
                hearingsJson.append(",\n");
            }

            final Integer duration = nonDefaultDay.getDuration().orElse(20);
            final String courtScheduleId = nonDefaultDay.getCourtScheduleId().orElse("8e837de0-743a-4a2c-9db3-b2e678c48729");

            hearingsJson.append("    {\n");
            hearingsJson.append("      \"hearingId\": \"").append(updatedHearingData.getHearingId()).append("\",\n");
            hearingsJson.append("      \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
            hearingsJson.append("      \"hearingStartTime\": \"").append(nonDefaultDay.getStartTime()).append("\",\n");
            hearingsJson.append("      \"duration\": ").append(duration);

            // Add judiciary information if available
            if (isNotEmpty(judiciaries)) {
                hearingsJson.append(",\n");
                hearingsJson.append("      \"judiciaries\": [\n");

                for (int i = 0; i < judiciaries.size(); i++) {
                    if (i > 0) {
                        hearingsJson.append(",\n");
                    }
                    JudicialRoleData judiciary = judiciaries.get(i);
                    hearingsJson.append("        {\n");
                    hearingsJson.append("          \"judiciaryId\": \"").append(judiciary.getJudicialId()).append("\",\n");
                    hearingsJson.append("          \"rotaJudiciaryId\": \"MA").append(format("%04d", 2000 + i)).append("\",\n");
                    hearingsJson.append("          \"title\": \"Mr\",\n");
                    hearingsJson.append("          \"surname\": \"DefaultSurname").append(i + 1).append("\",\n");
                    hearingsJson.append("          \"courtScheduleId\": \"").append(courtScheduleId).append("\",\n");
                    hearingsJson.append("          \"judiciaryType\": \"").append(judiciary.getJudicialRoleType().getJudiciaryType()).append("\",\n");

                    // Determine position based on chairman status and index
                    String position;
                    if (judiciary.getIsBenchChairman().orElse(false)) {
                        position = "CHAIR";
                    } else if (i == 1) {
                        position = "LEFT_WINGER";
                    } else if (i == 2) {
                        position = "RIGHT_WINGER";
                    } else {
                        position = "CHAIR"; // Default fallback
                    }
                    hearingsJson.append("          \"position\": \"").append(position).append("\",\n");

                    hearingsJson.append("          \"active\": false,\n");
                    hearingsJson.append("          \"createdOn\": \"2025-03-12T20:27:00.724+00:00\",\n");
                    hearingsJson.append("          \"updatedOn\": \"2025-03-12T20:27:00.724+00:00\",\n");
                    hearingsJson.append("          \"deputy\": ").append(judiciary.getIsDeputy().orElse(false)).append(",\n");
                    hearingsJson.append("          \"benchChairman\": ").append(judiciary.getIsBenchChairman().orElse(false)).append("\n");
                    hearingsJson.append("        }");
                }

                hearingsJson.append("\n      ]");
            }

            hearingsJson.append("\n");
            hearingsJson.append("    }");

            isFirst = false;
        }

        hearingsJson.append("\n  ]\n");
        hearingsJson.append("}");

        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing("hearings"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(hearingsJson.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubListHearingInCourtSessionsForProvisionalBooking(final String hearingId, final String sittingDay) {
        //set the values
        //convert the sitting day into hearingtimes
        String hearingStartTime1 = LocalDate.parse(sittingDay).atTime(LocalTime.of(10, 0,0)).atZone(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneOffset.UTC).toString();
        String hearingStartTime2 = LocalDate.parse(sittingDay).plusDays(1).atTime(LocalTime.of(10, 0,0)).atZone(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneOffset.UTC).toString();
        String hearingStartTime3 = LocalDate.parse(sittingDay).plusDays(2).atTime(LocalTime.of(10, 0,0)).atZone(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneOffset.UTC).toString();
        final String payload = "{\n" +
                "  \"hearings\": [\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"1e837de0-743a-4a2c-9db3-b2e678c48729\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime1 + "\",\n" +
                "      \"duration\": 30\n" +
                "    },\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"2e837de0-743a-4a2c-9db3-b2e678c48729\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime2 + "\",\n" +
                "      \"duration\": 30\n" +
                "    },\n" +
                "    {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"3e837de0-743a-4a2c-9db3-b2e678c48729\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime3 + "\",\n" +
                "      \"duration\": 30\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-sessions+json"))
                .withRequestBody(containing("hearings"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub POST /hearings/{hearingId} (mags.search.and.book) — was GET /searchlist/hearingslots.
     * Response reshaped: old {@code hearingSlots{}} object replaced by
     * {@code {hearingId, sessions:[{courtScheduleId, courtRoomId, sessionStartTime, draft, businessType, judiciaries:[]}]}}.
     * Fields from the old hearingSlots object are moved into sessions[0].
     */
    public static void stubSearchBookHearingSlots(final String hearingId, final String courtCentreId, final String hearingDate, final ZonedDateTime hearingStartTime) {
        final String courtScheduleId = UUID.randomUUID().toString();
        final String sessionStartTime = hearingStartTime.toString();
        final String payload = "{\n" +
                "  \"hearingId\": \"" + hearingId + "\",\n" +
                "  \"sessions\": [\n" +
                "    {\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"courtRoomId\": \"" + courtCentreId + "\",\n" +
                "      \"sessionStartTime\": \"" + sessionStartTime + "\",\n" +
                "      \"draft\": false,\n" +
                "      \"businessType\": \"MC\",\n" +
                "      \"judiciaries\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Request body now carries the params (hearingId, courtCentreId, hearingDate, etc.)
        // Match on hearingId in request body + content-type; stub scoped to this hearing
        stubFor(post(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(MAGS_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub POST /hearings/{hearingId} (mags.search.and.book) with businessType variant.
     * Response reshaped to sessions[] shape; courtRoomId and sessionStartTime carried in sessions[0].
     */
    public static void stubSearchBookHearingSlotsWithBusinessType(final String hearingId, final String courtCentreId,
                                                                  final String hearingDate, final ZonedDateTime hearingStartTime,
                                                                  final String businessType, final String courtRoomId,
                                                                  final Integer durationInMinutes) {
        final String resolvedRoomId = courtRoomId != null ? courtRoomId : courtCentreId;
        final String sessionStartTime = hearingStartTime != null ? hearingStartTime.toString() : "";
        final String courtScheduleId = UUID.randomUUID().toString();
        final String payload = "{\n" +
                "  \"hearingId\": \"" + hearingId + "\",\n" +
                "  \"sessions\": [\n" +
                "    {\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"courtRoomId\": \"" + resolvedRoomId + "\",\n" +
                "      \"sessionStartTime\": \"" + sessionStartTime + "\",\n" +
                "      \"draft\": false,\n" +
                "      \"businessType\": \"" + businessType + "\",\n" +
                "      \"judiciaries\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(post(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(MAGS_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .withRequestBody(containing("\"businessType\":\"" + businessType + "\""))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub POST /hearings/{hearingId} (mags.search.and.book) for draft sessions.
     * Response uses sessions[] shape with {@code "draft":true} in sessions[0].
     */
    public static void stubSearchBookHearingSlotsForDraftSessions(final String hearingId, final String courtCentreId,
                                                                  final String hearingDate, final ZonedDateTime hearingStartTime,
                                                                  final String courtRoomId, final Integer durationInMinutes) {
        final String resolvedRoomId = courtRoomId != null ? courtRoomId : courtCentreId;
        final String sessionStartTime = hearingStartTime != null ? hearingStartTime.toString() : "";
        final String courtScheduleId = UUID.randomUUID().toString();
        final String payload = "{\n" +
                "  \"hearingId\": \"" + hearingId + "\",\n" +
                "  \"sessions\": [\n" +
                "    {\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"courtRoomId\": \"" + resolvedRoomId + "\",\n" +
                "      \"sessionStartTime\": \"" + sessionStartTime + "\",\n" +
                "      \"draft\": true,\n" +
                "      \"businessType\": \"MC\",\n" +
                "      \"judiciaries\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(post(urlPathMatching(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(MAGS_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub POST /hearings/{hearingId} (mags.search.and.book) for CROWN path
     * (was GET /searchlist/hearingslots without Accept header restriction).
     * Response uses sessions[] shape.
     */
    public static void stubSearchBookHearingSlotsForCrown(final String hearingId, final String courtCentreId,
                                                            final String courtRoomId) {
        final String courtScheduleId = UUID.randomUUID().toString();
        final String sessionStartTime = ItClock.nowUtc().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0).toString();
        final String payload = "{\n" +
                "  \"hearingId\": \"" + hearingId + "\",\n" +
                "  \"sessions\": [\n" +
                "    {\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"courtRoomId\": \"" + courtRoomId + "\",\n" +
                "      \"sessionStartTime\": \"" + sessionStartTime + "\",\n" +
                "      \"draft\": false,\n" +
                "      \"businessType\": \"CR\",\n" +
                "      \"judiciaries\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(MAGS_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub POST /hearings/{hearingId} (mags.search.and.book) for CROWN draft scenario
     * (was GET /searchlist/hearingslots). Reports booked session as {@code "draft":true} in sessions[0].
     * The update path ({@code handleCrownUpdateSearchAndBook}) takes only courtScheduleId+draft from
     * this response and lets the aggregate unallocate — clearing the previously-allocated court room.
     */
    public static void stubSearchBookHearingSlotsForCrownDraft(final String hearingId, final String courtCentreId) {
        final String courtScheduleId = UUID.randomUUID().toString();
        final String sessionStartTime = ItClock.nowUtc().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0).toString();
        final String payload = "{\n" +
                "  \"hearingId\": \"" + hearingId + "\",\n" +
                "  \"sessions\": [\n" +
                "    {\n" +
                "      \"courtScheduleId\": \"" + courtScheduleId + "\",\n" +
                "      \"courtRoomId\": \"" + courtCentreId + "\",\n" +
                "      \"sessionStartTime\": \"" + sessionStartTime + "\",\n" +
                "      \"draft\": true,\n" +
                "      \"businessType\": \"CR\",\n" +
                "      \"judiciaries\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(MAGS_SEARCH_AND_BOOK_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Registers low-priority catch-all stubs for all court-scheduler endpoints.
     * These prevent 60s timeouts when the enrichment service makes calls that
     * don't match any specific stub. Individual test stubs (default priority 5)
     * take precedence over these (priority 10).
     */
    public static void stubCourtSchedulerCatchAll() {
        // POST /hearings/{id} — mags.search.and.book (was GET /searchlist/hearingslots)
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(MAGS_SEARCH_AND_BOOK_TYPE))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearingId\":\"\",\"sessions\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // GET /hearingslots — search (available hearing slots) — NO CHANGE
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearingSlots\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // GET /sessions — search-court-schedules-by-id (was /courtschedule/search.court-schedules-by-id)
        stubFor(get(urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH)))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"courtSchedules\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // POST /hearings/{id} — crown.search.and.book (was GET /multidaysearchandbook/hearingslots and GET /crownfallbacksearchandbook/hearingslots)
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH + "/[0-9a-fA-F-]+")))
                .withHeader(CONTENT_TYPE, containing(CROWN_SEARCH_AND_BOOK_TYPE))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"sessions\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // POST /hearings — listHearingInCourtSessions (was PUT /list/hearingslots)
        stubFor(WireMock.post(WireMock.urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + HEARINGS_PATH)))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearings\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // POST /hearingslots — updateAvailableHearingSlots — NO CHANGE
        stubFor(WireMock.post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .atPriority(10)
                .willReturn(aResponse().withStatus(NO_CONTENT.getStatusCode())));
    }

    /**
     * Minimal draft-status stub: each session carries ONLY courtScheduleId + isDraft.
     * Sufficient for LIST-path consumers (allocation decision reads just the flag).
     * For UPDATE-path consumers use the overload below — the single-day update's
     * sanityCheckAndEnrichCrown overwrites hearingDate with the session's sessionDate,
     * so a session without one would null the date and log a CROWN sanity ERROR.
     *
     * <p>HISTORY: until 2026-06-07 this emitted the envelope key "hearingSlots" — the real
     * courtscheduler wire (and listing's fetchCourtSchedulesByIds) uses "courtSchedules",
     * so every caller silently parsed an EMPTY list and enrichment degraded with
     * "CROWN single-day update: failed to fetch court schedules" WARNs.</p>
     */
    public static void stubGetCourtSchedulesByIdWithDraftStatus(final List<String> courtScheduleIds, final boolean isDraft) {
        final StringBuilder schedulesJson = new StringBuilder();
        for (int i = 0; i < courtScheduleIds.size(); i++) {
            if (i > 0) {
                schedulesJson.append(",");
            }
            schedulesJson.append("{\"courtScheduleId\":\"").append(courtScheduleIds.get(i)).append("\",")
                    .append("\"isDraft\":").append(isDraft).append("}");
        }
        stubCourtSchedulesByIdResponse("{\"courtSchedules\":[" + schedulesJson + "]}");
    }

    /**
     * Full draft-status stub for UPDATE-path flows: sessions carry sessionDate, courtHouseId,
     * courtRoomId and hearingStartTime so sanityCheckAndEnrichCrown can re-derive the hearing
     * day without nulling its date. Values MUST agree with the update payload's nonDefaultDays
     * (same date/centre/room) or the enrichment will log a CROWN sanity date-mismatch ERROR
     * and shift the projected hearing day.
     */
    public static void stubGetCourtSchedulesByIdWithDraftStatus(final List<String> courtScheduleIds,
                                                                final boolean isDraft,
                                                                final LocalDate sessionDate,
                                                                final UUID courtHouseId,
                                                                final UUID courtRoomId,
                                                                final ZonedDateTime hearingStartTime) {
        final StringBuilder schedulesJson = new StringBuilder();
        for (int i = 0; i < courtScheduleIds.size(); i++) {
            if (i > 0) {
                schedulesJson.append(",");
            }
            schedulesJson.append("{\"courtScheduleId\":\"").append(courtScheduleIds.get(i)).append("\"")
                    .append(",\"courtHouseId\":\"").append(courtHouseId).append("\"");
            if (courtRoomId != null) {
                schedulesJson.append(",\"courtRoomId\":\"").append(courtRoomId).append("\"");
            }
            schedulesJson.append(",\"sessionDate\":\"").append(sessionDate).append("\"")
                    .append(",\"hearingStartTime\":\"").append(hearingStartTime).append("\"")
                    .append(",\"isDraft\":").append(isDraft).append("}");
        }
        stubCourtSchedulesByIdResponse("{\"courtSchedules\":[" + schedulesJson + "]}");
    }

    private static void stubCourtSchedulesByIdResponse(final String body) {
        // Endpoint reshaped: was GET /courtschedule/search.court-schedules-by-id?courtScheduleIds=...
        // Now GET /sessions?ids=...  (query param key changed from courtScheduleIds to ids)
        stubFor(get(urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + SESSIONS_PATH)))
                .withQueryParam("ids", matching(".*"))
                .withHeader("Accept", containing("application/vnd.courtscheduler.search.court-schedules-by-id+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }
}

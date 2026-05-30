package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
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

public class CourtSchedulerServiceStub {

    private static final String COURT_SCHEDULER_ENDPOINT = "/listingcourtscheduler-api/rest/courtscheduler";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";
    private static final String HEARING_SLOTS = "/hearingslots";
    private static final String VALIDATE_SESSION_AVAILABILITY = "/validate-session-availability";
    private static final String SEARCH_COURT_SCHEDULES_BY_ID = "/courtschedule/search.court-schedules-by-id";
    private static final String CROWN_FALLBACK_SEARCH_BOOK = "/crownfallbacksearchandbook/hearingslots";
    private static final String CROWN_FALLBACK_SEARCH_BOOK_TYPE = "application/vnd.courtscheduler.crown.fallback.search.book.hearing.slots+json";
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

    public static void stubDeleteAvailableHearingSlotsService(final String hearingId) {
        stubFor(WireMock.delete(urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS + "/" + hearingId))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }

    public static void stubDeleteAvailableHearingSlotsServiceForAnyHearing() {
        stubFor(WireMock.delete(urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS + "/.*"))
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
            final RequestPatternBuilder requestPatternBuilder = WireMock.deleteRequestedFor(urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS + "/" + hearingId));
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
     * Stub a successful response from /courtschedule/search.court-schedules-by-id for the given
     * courtScheduleId. Returned wire shape mirrors what the real courtscheduler emits via
     * {@code CourtSchedulerApi.searchCourtSchedulesById} - FLAT: each courtSchedules[] element
     * is a single CourtSchedule with isDraft at the top level. The schema example in
     * courtscheduler-api shows a misleading nested "sessions" structure copied from a different
     * endpoint; never match the schema shape, match the wire shape.
     *
     * @param courtScheduleId the id under query
     * @param isDraft         draft state to report - drives whether
     *                        {@code listing.query.court.schedule.draft.status} returns
     *                        {@code anyDraft=true} (strip) or {@code anyDraft=false} (preserve)
     */
    /**
     * Stub courtscheduler's search-court-schedules-by-id response with an explicit choice of
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
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + SEARCH_COURT_SCHEDULES_BY_ID)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub /courtschedule/search.court-schedules-by-id to return a 500. Exercises the listing
     * adapter's fail-closed path (anyDraft=true on courtscheduler error).
     */
    public static void stubSearchCourtSchedulesByIdServerError() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + SEARCH_COURT_SCHEDULES_BY_ID)))
                .willReturn(aResponse().withStatus(500)
                        .withBody("internal server error")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Stub {@code search.court-schedules-by-id} so a CROWN bookingReference (which IS the courtScheduleId)
     * resolves to a single session echoing the supplied courtHouse / room / date. The listing command resolves
     * the bookingReference here (see {@code CourtScheduleEnrichmentService.promoteCrownBookingReferenceToBookedSlot}).
     * Scoped by the {@code courtScheduleIds} query param so it answers only for this hearing's bookingReference
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

        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + SEARCH_COURT_SCHEDULES_BY_ID)))
                .atPriority(2)
                .withQueryParam("courtScheduleIds", containing(courtScheduleId))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    // --- Crown fallback search-and-book stubs (Option C: courtCentreId-only wire) ---

    /**
     * Stub a successful 200 response from /crownfallbacksearchandbook/hearingslots.
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
        final String sessionDate = LocalDate.now().plusDays(1).toString();
        final String startTime = sessionDate + "T09:00:00Z";
        final String endTime = sessionDate + "T17:00:00Z";
        final String body = format(
                "{\"hearingId\":\"%s\",\"courtScheduleId\":\"%s\",\"courtRoomId\":731816," +
                        "\"sessionDate\":\"%s\",\"sessionStartTime\":\"%s\",\"sessionEndTime\":\"%s\"," +
                        "\"durationInMinutes\":10,\"isDraft\":%s,\"businessType\":\"CR\"," +
                        "\"source\":\"%s\",\"overbooked\":false}",
                hearingId, courtScheduleId, sessionDate, startTime, endTime, isDraft, source);

        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + CROWN_FALLBACK_SEARCH_BOOK)))
                .withQueryParam("source", matching(source))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body)
                        .withHeader(CONTENT_TYPE, CROWN_FALLBACK_SEARCH_BOOK_TYPE)));
    }

    /** Stub 404 "no session found" — listing-side translates to CrownFallbackNoSessionException. */
    public static void stubCrownFallbackSearchAndBookNotFound() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + CROWN_FALLBACK_SEARCH_BOOK)))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /** Stub 400 "invalid request" — used for defensive coverage; the listing-side multi-day guard fires first. */
    public static void stubCrownFallbackSearchAndBookBadRequest() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + CROWN_FALLBACK_SEARCH_BOOK)))
                .willReturn(aResponse()
                        .withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("{\"error\":\"durationInMinutes exceeds single-day cap\"}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /** Verify the Crown fallback endpoint was called with the expected source label. */
    public static void verifyCrownFallbackSearchAndBookCalledWithSource(final String source) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            try {
                WireMock.verify(WireMock.getRequestedFor(urlPathMatching(
                        COURT_SCHEDULER_ENDPOINT + CROWN_FALLBACK_SEARCH_BOOK))
                        .withQueryParam("source", matching(source)));
                return true;
            } catch (VerificationException e) {
                return false;
            }
        });
    }

    /** Verify the Crown fallback endpoint was NEVER called (regression guard for MAGS / already-allocated CROWN). */
    public static void verifyCrownFallbackSearchAndBookNeverCalled() {
        WireMock.verify(0, WireMock.getRequestedFor(urlPathMatching(
                COURT_SCHEDULER_ENDPOINT + CROWN_FALLBACK_SEARCH_BOOK)));
    }

    // --- Multi-day search-and-book stubs (CROWN update multi-day path) ---

    /**
     * Stub a successful response from GET /multidaysearchandbook/hearingslots returning the supplied
     * court schedule sessions. Used to drive the CROWN multi-day update path — the listing service
     * passes the starting courtScheduleId + total duration, courtscheduler returns N consecutive
     * sessions that together cover the duration.
     */
    public static void stubMultiDaySearchAndBook(final List<String> courtScheduleIds,
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

        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + "/multidaysearchandbook/hearingslots")))
                .atPriority(1)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /**
     * Verify that GET /multidaysearchandbook/hearingslots was called with the expected courtScheduleId
     * + total duration. Proves the CROWN update path correctly routed multi-day through the CourtSchedule-first
     * flow and didn't regress to the startDate→endDate expansion.
     */
    public static void verifyMultiDaySearchAndBookCalled(final String courtScheduleId, final int durationInMinutes) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            try {
                WireMock.verify(WireMock.getRequestedFor(urlPathMatching(
                        COURT_SCHEDULER_ENDPOINT + "/multidaysearchandbook/hearingslots"))
                        .withQueryParam("courtScheduleId", WireMock.equalTo(courtScheduleId))
                        .withQueryParam("durationInMinutes", WireMock.equalTo(String.valueOf(durationInMinutes))));
                return true;
            } catch (VerificationException e) {
                return false;
            }
        });
    }

    /** Regression guard: CROWN update without a courtScheduleId must NOT trigger multi-day search-and-book. */
    public static void verifyMultiDaySearchAndBookNeverCalled() {
        WireMock.verify(0, WireMock.getRequestedFor(urlPathMatching(
                COURT_SCHEDULER_ENDPOINT + "/multidaysearchandbook/hearingslots")));
    }

    // --- Extend multi-day hearing stubs (SPRDT-901: CROWN update-hearing-for-listing multi-day path) ---

    private static final String EXTEND_MULTIDAY = "/extendmultidayhearing/hearingslots";
    private static final String COURTSCHEDULER_EXTEND_MULTIDAY_TYPE =
            "application/vnd.courtscheduler.extend.multiday.hearing+json";

    /**
     * Stub a successful POST to /extendmultidayhearing/hearingslots returning the supplied court schedule
     * sessions, scoped to the supplied hearingId. SPRDT-901 routes CROWN multi-day updates here instead of
     * the GET-based /multidaysearchandbook — courtscheduler receives the full duration and returns N
     * sessions to use as the rebuilt hearingDays.
     *
     * <p><b>Scoping:</b> WireMock stubs persist across IT classes in the same suite. Without a body
     * matcher, this stub would intercept every other IT that extends a CROWN hearing into multi-day
     * (e.g. HearingCsvReportIT) and return these synthetic courtSchedules — corrupting their hearingDays.
     * The hearingId body match makes the stub apply only to the test's own hearing.
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

        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + EXTEND_MULTIDAY)))
                .withHeader(CONTENT_TYPE, containing(COURTSCHEDULER_EXTEND_MULTIDAY_TYPE))
                .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(body.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    /**
     * Verify that POST /extendmultidayhearing/hearingslots was called with a body containing the
     * supplied hearingId and durationInMinutes. Proves SPRDT-901 routing: the CROWN multi-day update
     * was sent to courtscheduler's new extension endpoint with the full requested duration.
     */
    public static void verifyExtendMultiDayHearingCalled(final String hearingId, final int durationInMinutes) {
        Awaitility.await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            try {
                WireMock.verify(WireMock.postRequestedFor(urlPathMatching(
                        COURT_SCHEDULER_ENDPOINT + EXTEND_MULTIDAY))
                        .withHeader(CONTENT_TYPE, containing(COURTSCHEDULER_EXTEND_MULTIDAY_TYPE))
                        .withRequestBody(containing("\"hearingId\":\"" + hearingId + "\""))
                        .withRequestBody(containing("\"durationInMinutes\":" + durationInMinutes)));
                return true;
            } catch (VerificationException e) {
                return false;
            }
        });
    }

    /** Regression guard: single-day CROWN updates / non-CROWN updates must NOT call /extendmultidayhearing. */
    public static void verifyExtendMultiDayHearingNeverCalled() {
        WireMock.verify(0, WireMock.postRequestedFor(urlPathMatching(
                COURT_SCHEDULER_ENDPOINT + EXTEND_MULTIDAY)));
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
                                .replace("%SESSION_DATE%", Optional.of(values.get("SESSION_DATE")).orElse(LocalDate.now().toString()))
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

        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(containing("hearingSlots"))
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

        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .atPriority(2)
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
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

        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(containing("hearingSlots"))
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

        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(containing("hearingSlots"))
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

        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(containing("hearingSlots"))
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

        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(containing("hearingSlots"))
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

        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(containing("hearingSlots"))
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


        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(containing("hearingSlots"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubSearchBookHearingSlots(final String hearingId, final String courtCentreId, final String hearingDate,final ZonedDateTime hearingStartTime) {
        final String payload = "{\n" +
                "  \"hearingSlots\": {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + UUID.randomUUID() + "\",\n" +
                "      \"courtRoomId\": \"" + courtCentreId + "\",\n" +
                "      \"hearingDate\": \"" + hearingDate + "\",\n" +
                "      \"hearingSessionDateSearchCutOff\": \"" + hearingDate + "\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime.toString() + "\",\n" +
                "      \"duration\": 20\n" +
                "  }\n" +
                "}";

        stubFor(get(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/searchlist/hearingslots")))
                .withHeader("Accept", containing("application/vnd.courtscheduler.search.book.hearing.slots+json"))
//                .withQueryParam("hearingId", WireMock.matching(".*"))
                .withQueryParam("hearingId", matching(hearingId))
//                .withQueryParam("courtCentreId", WireMock.matching(".*"))
                .withQueryParam("courtCentreId", matching(courtCentreId))
//                .withQueryParam("hearingDate", WireMock.matching(".*"))
                .withQueryParam("hearingDate", matching(hearingDate))
                .withQueryParam("hearingSessionDateSearchCutOff", matching(hearingDate))
//                .withQueryParam("hearingStartTime", WireMock.matching(hearingStartTime.toString()))
                .withQueryParam("durationInMinutes", matching("20"))
                .withQueryParam("isPolice", matching("true|false"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubSearchBookHearingSlotsWithBusinessType(final String hearingId, final String courtCentreId,
                                                                  final String hearingDate, final ZonedDateTime hearingStartTime,
                                                                  final String businessType, final String courtRoomId,
                                                                  final Integer durationInMinutes) {
        final String payload = "{\n" +
                "  \"hearingSlots\": {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + UUID.randomUUID() + "\",\n" +
                "      \"courtRoomId\": \"" + (courtRoomId != null ? courtRoomId : courtCentreId) + "\",\n" +
                "      \"hearingDate\": \"" + hearingDate + "\",\n" +
                "      \"hearingSessionDateSearchCutOff\": \"" + hearingDate + "\",\n" +
                "      \"hearingStartTime\": \"" + hearingStartTime.toString() + "\",\n" +
                "      \"duration\": " + (durationInMinutes != null ? durationInMinutes : 20) + "\n" +
                "  }\n" +
                "}";

        com.github.tomakehurst.wiremock.client.MappingBuilder mappingBuilder = get(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/searchlist/hearingslots")))
                .withHeader("Accept", containing("application/vnd.courtscheduler.search.book.hearing.slots+json"))
                .withQueryParam("hearingId", matching(hearingId))
                .withQueryParam("courtCentreId", matching(courtCentreId))
                .withQueryParam("hearingDate", matching(hearingDate))
                .withQueryParam("businessType", matching(businessType))
                .withQueryParam("durationInMinutes", matching(String.valueOf(durationInMinutes != null ? durationInMinutes : 20)))
                .withQueryParam("isPolice", matching("true|false"));

        if (courtRoomId != null) {
            mappingBuilder = mappingBuilder.withQueryParam("courtRoomId", matching(courtRoomId));
        }
        if (hearingStartTime != null) {
            mappingBuilder = mappingBuilder.withQueryParam("hearingStartTime", matching(hearingStartTime.toString()));
        }

        stubFor(mappingBuilder
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubSearchBookHearingSlotsForDraftSessions(final String hearingId, final String courtCentreId,
                                                                  final String hearingDate, final ZonedDateTime hearingStartTime,
                                                                  final String courtRoomId, final Integer durationInMinutes) {
        final String payload = "{\n" +
                "  \"hearingSlots\": {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + UUID.randomUUID() + "\",\n" +
                "      \"courtRoomId\": \"" + (courtRoomId != null ? courtRoomId : courtCentreId) + "\",\n" +
                "      \"hearingDate\": \"" + hearingDate + "\",\n" +
                "      \"hearingSessionDateSearchCutOff\": \"" + hearingDate + "\",\n" +
                "      \"hearingStartTime\": \"" + (hearingStartTime != null ? hearingStartTime.toString() : "") + "\",\n" +
                "      \"duration\": " + (durationInMinutes != null ? durationInMinutes : 20) + ",\n" +
                "      \"isDraft\": true\n" +
                "  }\n" +
                "}";

        com.github.tomakehurst.wiremock.client.MappingBuilder mappingBuilder = get(WireMock.urlPathEqualTo(format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/searchlist/hearingslots")))
                .withHeader("Accept", containing("application/vnd.courtscheduler.search.book.hearing.slots+json"))
                .withQueryParam("hearingId", matching(hearingId))
                .withQueryParam("courtCentreId", matching(courtCentreId))
                .withQueryParam("hearingDate", matching(hearingDate))
                .withQueryParam("durationInMinutes", matching(String.valueOf(durationInMinutes != null ? durationInMinutes : 20)))
                .withQueryParam("isPolice", matching("true|false"));

        if (courtRoomId != null) {
            mappingBuilder = mappingBuilder.withQueryParam("courtRoomId", matching(courtRoomId));
        }
        if (hearingStartTime != null) {
            mappingBuilder = mappingBuilder.withQueryParam("hearingStartTime", matching(hearingStartTime.toString()));
        }

        stubFor(mappingBuilder
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubSearchBookHearingSlotsForCrown(final String hearingId, final String courtCentreId,
                                                            final String courtRoomId) {
        final String payload = "{\n" +
                "  \"hearingSlots\": {\n" +
                "      \"hearingId\": \"" + hearingId + "\",\n" +
                "      \"courtScheduleId\": \"" + UUID.randomUUID() + "\",\n" +
                "      \"courtRoomId\": \"" + courtRoomId + "\",\n" +
                "      \"hearingDate\": \"" + LocalDate.now().plusDays(5) + "\",\n" +
                "      \"hearingStartTime\": \"" + ZonedDateTime.now(java.time.ZoneOffset.UTC).plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0) + "\",\n" +
                "      \"duration\": 30\n" +
                "  }\n" +
                "}";

        stubFor(get(WireMock.urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + "/searchlist/hearingslots")))
                .withQueryParam("hearingId", matching(hearingId))
                .withQueryParam("courtCentreId", matching(courtCentreId))
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
        // GET /searchlist/hearingslots — searchBookSlots
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + "/searchlist/hearingslots")))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearingSlots\":{}}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // GET /hearingslots — search (available hearing slots)
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearingSlots\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // GET /courtschedule/search.court-schedules-by-id
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + "/courtschedule/search.court-schedules-by-id")))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearingSlots\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // GET /multidaysearchandbook/hearingslots
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + "/multidaysearchandbook/hearingslots")))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearingSlots\":{}}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // PUT /list/hearingslots — listHearingInCourtSessions
        stubFor(WireMock.put(WireMock.urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .atPriority(10)
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"hearings\":[]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        // POST /hearingslots — updateAvailableHearingSlots
        stubFor(WireMock.post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .atPriority(10)
                .willReturn(aResponse().withStatus(NO_CONTENT.getStatusCode())));
    }

    public static void stubGetCourtSchedulesByIdWithDraftStatus(final List<String> courtScheduleIds, final boolean isDraft) {
        final StringBuilder hearingSlotsJson = new StringBuilder();
        hearingSlotsJson.append("{\n");
        hearingSlotsJson.append("  \"hearingSlots\": [\n");

        for (int i = 0; i < courtScheduleIds.size(); i++) {
            if (i > 0) {
                hearingSlotsJson.append(",\n");
            }
            hearingSlotsJson.append("    {\n");
            hearingSlotsJson.append("      \"courtScheduleId\": \"").append(courtScheduleIds.get(i)).append("\",\n");
            hearingSlotsJson.append("      \"isDraft\": ").append(isDraft).append("\n");
            hearingSlotsJson.append("    }");
        }

        hearingSlotsJson.append("\n  ]\n");
        hearingSlotsJson.append("}");

        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + "/courtschedule/search.court-schedules-by-id")))
                .withQueryParam("courtScheduleIds", matching(".*"))
                .withHeader("Accept", containing("application/vnd.courtscheduler.search.court-schedules-by-id+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(hearingSlotsJson.toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }
}

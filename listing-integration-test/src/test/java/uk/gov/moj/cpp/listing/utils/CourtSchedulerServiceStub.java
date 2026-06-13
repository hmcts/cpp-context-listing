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
import uk.gov.moj.cpp.listing.it.util.ItClock;

public class CourtSchedulerServiceStub {

    private static final String COURT_SCHEDULER_ENDPOINT = "/listingcourtscheduler-api/rest/courtscheduler";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";
    private static final String HEARING_SLOTS = "/hearingslots";
    private static final String COURTSCHEDULER_GET_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.get.hearing.slots+json";
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

    public static void stubGetAvailableHearingSlots(boolean isEmpty) {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .withQueryParam("sessionEndDate", matching("2020-10-11"))
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
}

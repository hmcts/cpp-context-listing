package uk.gov.moj.cpp.listing.utils;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.awaitility.Awaitility;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

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

    static {
        WireMock.configureFor(CourtSchedulerServiceStub.HOST, 8080);
    }

    public static void stubUpdateAvailableHearingSlotsService() {
        WireMock.stubFor(WireMock.post(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .willReturn(WireMock.aResponse().withStatus(NO_CONTENT.getStatusCode())));
    }

    public static void stubDeleteAvailableHearingSlotsService(final String hearingId) {
        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS + "/" + hearingId))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())));
    }

    public static void verifyDeleteAvailableHearingSlotsStubCommandInvoked(final String hearingId) {
        Awaitility.await().atMost(30, SECONDS).pollInterval(1, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = WireMock.deleteRequestedFor(WireMock.urlPathMatching(CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS + "/" + hearingId));
            try {
                WireMock.verify(WireMock.exactly(1), requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }

    public static void stubGetAvailableHearingSlots(boolean isEmpty) {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", WireMock.matching("2017-10-11"))
                .withQueryParam("pageNumber", WireMock.matching("1"))
                .withQueryParam("pageSize", WireMock.matching("20"))
                .withQueryParam("panel", WireMock.matching("ADULT"))
                .withQueryParam("oucodeL2Code", WireMock.matching("Z01KR05"))
                .withQueryParam("sessionEndDate", WireMock.matching("2020-10-11"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(isEmpty ? CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON : CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_SLOTS_JSON))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }

    public static void stubGetAvailableHearingSlots() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_SLOTS_JSON))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }

    public static void stubGetHearingIds(boolean isEmpty) {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", WireMock.matching("1900-01-01"))
                .withQueryParam("pageNumber", WireMock.matching("1"))
                .withQueryParam("pageSize", WireMock.matching("20"))
                .withQueryParam("panel", WireMock.matching("ADULT,YOUTH"))
                .withQueryParam("ouCode", WireMock.matching("B01LY00"))
                .withQueryParam("sessionEndDate", WireMock.matching("9999-01-01"))
                .withHeader("Accept", WireMock.containing("application/vnd.courtscheduler.get.hearing.ids+json"))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(isEmpty ? "stub-data/listing.search.hearing.ids.empty.json" : "stub-data/listing.search.hearing.ids.json"))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate sessionDate, final Map<String, String> values) {
        final String courtRoomId = Optional.ofNullable(values.get("courtRoomId")).orElse("fce80cd4-0c00-3c30-9471-2c2ee7a52453");
        final String hearingStartTime = sessionDate.atTime(LocalTime.of(10, 0,0,0)).atZone(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneOffset.UTC).toString();

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_WITH_SESSION_DATE_JSON)
                                .replace("%SESSION_DATE%", sessionDate.toString())
                                .replace("%COURT_ROOM_ID%", courtRoomId)
                                .replace("%HEARING_START_TIME%", hearingStartTime))
                        .withHeader("Content-Type", "application/json")
                ));
    }

    public static void stubProvisionalBookingWithCustomParams(final Map<String, String> values) {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULES_WITH_CUSTOM_PARAMS_JSON)
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
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_JSON))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(FileUtil.getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_DURATION_BASED_JSON))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(final Map<String, String> courtRoomScedules, final String courtCentreId) {
        String payload = FileUtil.getPayload(CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_DURATION_BASED_JSON);
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

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", WireMock.notMatching("null"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }

    public static JsonObject stubGetAvailableHearingSlotsWithQueryParams(boolean isEmpty,
                                                                         final String courtRoomId,
                                                                         final String ouCode,
                                                                         final String sessionStartDate,
                                                                         final String sessionEndDate) throws IOException {
        final String getHearingSlotsPayload = FileUtil.getPayload(isEmpty ? CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON :
                CourtSchedulerServiceStub.ROTASL_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES)
                .replaceAll("COURT_ROOM_ID", courtRoomId)
                .replaceAll("OU_CODE", ouCode)
                .replaceAll("SESSION_DATE", sessionStartDate);

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", WireMock.matching(sessionStartDate))
                .withQueryParam("sessionEndDate", WireMock.matching(sessionEndDate))
                .withQueryParam("ouCode", WireMock.matching(ouCode))
                .withQueryParam("pageSize", WireMock.matching("1"))
                .withQueryParam("pageNumber", WireMock.matching("1"))
                .withQueryParam("courtRoomId", WireMock.matching(courtRoomId))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(getHearingSlotsPayload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
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
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", WireMock.matching(".*"))
                .withQueryParam("sessionEndDate", WireMock.matching(".*"))
                .withQueryParam("ouCode", WireMock.matching(".*"))
                .withQueryParam("pageSize", WireMock.matching(".*"))
                .withQueryParam("pageNumber", WireMock.matching(".*"))
                .withQueryParam("courtRoomId", WireMock.matching(".*"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(payloadString)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));

        return FileUtil.payloadToObject(payloadString);
    }

    public static JsonObject stubGetAvailableHearingSlotsWithQueryParamsForPayloadIT(boolean isEmpty,
                                                                         final String courtRoomId,
                                                                         final String courtScheduleId,
                                                                         final String ouCode,
                                                                         final String sessionStartDate,
                                                                         final ZonedDateTime hearingStartTime) throws IOException {
        final String getHearingSlotsPayload = FileUtil.getPayload(isEmpty ? CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON :
                        CourtSchedulerServiceStub.LISTING_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES_AND_SLOTTIMES)
                .replaceAll("%%COURT_ROOM_ID%%", courtRoomId)
                .replaceAll("%%COURT_SCHEDULE_ID%%", courtScheduleId)
                .replaceAll("%%OU_CODE%%", ouCode)
                .replaceAll("%%SESSION_DATE%%", sessionStartDate)
                .replaceAll("%%HEARING_START_TIME%%", hearingStartTime.toString());

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
//                .withQueryParam("sessionStartDate", WireMock.matching(sessionStartDate))
                .withQueryParam("sessionStartDate", WireMock.matching(".*"))
//                .withQueryParam("sessionEndDate", WireMock.matching(sessionEndDate))
                .withQueryParam("sessionEndDate", WireMock.matching(".*"))
//                .withQueryParam("ouCode", WireMock.matching(ouCode))
                .withQueryParam("ouCode", WireMock.matching(".*"))
                .withQueryParam("pageSize", WireMock.matching("1"))
                .withQueryParam("pageNumber", WireMock.matching("1"))
//                .withQueryParam("courtRoomId", WireMock.matching(courtRoomId))
                .withQueryParam("courtRoomId", WireMock.matching(".*"))
//                .withQueryParam("panel", WireMock.matching("ADULT,YOUTH"))
                .withQueryParam("panel", WireMock.matching(".*"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(getHearingSlotsPayload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));

        return FileUtil.payloadToObject(getHearingSlotsPayload);
    }

    public static void stubSessionEndDateEmptyRequest() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + CourtSchedulerServiceStub.HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", WireMock.matching("2017-10-11"))
                .withQueryParam("pageNumber", WireMock.matching("1"))
                .withQueryParam("pageSize", WireMock.matching("20"))
                .withQueryParam("panel", WireMock.matching("ADULT"))
                .withQueryParam("oucodeL2Code", WireMock.matching("Z01KR05"))
                .withHeader("Accept", WireMock.containing(CourtSchedulerServiceStub.COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(WireMock.aResponse().withStatus(BAD_REQUEST.getStatusCode())
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

        WireMock.stubFor(WireMock.put(WireMock.urlPathEqualTo(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", WireMock.containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(WireMock.containing("hearingSlots"))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
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

        WireMock.stubFor(WireMock.put(WireMock.urlPathEqualTo(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", WireMock.containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(WireMock.containing("hearingSlots"))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
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

        WireMock.stubFor(WireMock.put(WireMock.urlPathEqualTo(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", WireMock.containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(WireMock.containing("hearingSlots"))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
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
            hearingsJson.append("      \"duration\": ").append(duration).append("\n");
            hearingsJson.append("    }");
            
            isFirst = false;
        }
        
        hearingsJson.append("\n  ]\n");
        hearingsJson.append("}");

        WireMock.stubFor(WireMock.put(WireMock.urlPathEqualTo(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", WireMock.containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(WireMock.containing("hearingSlots"))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(hearingsJson.toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
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


        WireMock.stubFor(WireMock.put(WireMock.urlPathEqualTo(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/list/hearingslots")))
                .withHeader("content-type", WireMock.containing("application/vnd.courtscheduler.list.hearings-in-court-sessions+json"))
                .withRequestBody(WireMock.containing("hearingSlots"))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
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

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(String.format("%s", CourtSchedulerServiceStub.COURT_SCHEDULER_ENDPOINT + "/searchlist/hearingslots")))
                .withHeader("Accept", WireMock.containing("application/vnd.courtscheduler.search.book.hearing.slots+json"))
//                .withQueryParam("hearingId", WireMock.matching(".*"))
                .withQueryParam("hearingId", WireMock.matching(hearingId))
//                .withQueryParam("courtCentreId", WireMock.matching(".*"))
                .withQueryParam("courtCentreId", WireMock.matching(courtCentreId))
//                .withQueryParam("hearingDate", WireMock.matching(".*"))
                .withQueryParam("hearingDate", WireMock.matching(hearingDate))
                .withQueryParam("hearingSessionDateSearchCutOff", WireMock.matching(hearingDate))
//                .withQueryParam("hearingStartTime", WireMock.matching(hearingStartTime.toString()))
                .withQueryParam("durationInMinutes", WireMock.matching("20"))
                .withQueryParam("isPolice", WireMock.matching("true|false"))
                .willReturn(WireMock.aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ));
    }
}

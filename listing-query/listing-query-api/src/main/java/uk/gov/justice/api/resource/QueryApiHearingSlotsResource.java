package uk.gov.justice.api.resource;

import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.AVAILABLE_DURATION_MINS;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.BUSINESS_TYPE;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.CONSECUTIVE_DAYS;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.COURT_ROOM_ID;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.COURT_ROOM_NUMBER;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.COURT_SESSION;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.HEARING_START_TIME;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.IS_SLOT_BASED;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.IS_WEEK_COMMENCING;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.JURISDICTION;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.OUCODE;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.OU_L2_CODE;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.PAGE_NUMBER;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.PAGE_SIZE;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.PANEL;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.SESSION_END_DATE;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.SESSION_START_DATE;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.SHOW_OVERBOOKED_SLOTS;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.STATUS;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("hearingSlots")
public interface QueryApiHearingSlotsResource {

    @GET
    @Produces("application/vnd.listing.search.hearing.slots+json")
    Response getHearingSlots(@QueryParam(PANEL) String panel,
                             @QueryParam(SESSION_START_DATE) String sessionStartDate,
                             @QueryParam(SESSION_END_DATE) String sessionEndDate,
                             @QueryParam(HEARING_START_TIME) String hearingStartTime,
                             @QueryParam(OU_L2_CODE) String oucodeL2Code,
                             @QueryParam(OUCODE) String ouCode,
                             @QueryParam(COURT_ROOM_ID) String courtRoomId,
                             @QueryParam(COURT_ROOM_NUMBER) String courtRoomNumber,
                             @QueryParam(BUSINESS_TYPE) String businessType,
                             @QueryParam(COURT_SESSION) String courtSession,
                             @QueryParam(IS_SLOT_BASED) Boolean isSlotBased,
                             @QueryParam(SHOW_OVERBOOKED_SLOTS) Boolean showOverbookedSlots,
                             @QueryParam(PAGE_SIZE) String pageSize,
                             @QueryParam(PAGE_NUMBER) String pageNumber,
                             @QueryParam(AVAILABLE_DURATION_MINS) Integer availableDurationMins,
                             @QueryParam(STATUS) String status,
                             @QueryParam(CONSECUTIVE_DAYS) Integer consecutiveDays,
                             @QueryParam(IS_WEEK_COMMENCING) Boolean isWeekCommencing,
                             @QueryParam(JURISDICTION) String jurisdiction);
}

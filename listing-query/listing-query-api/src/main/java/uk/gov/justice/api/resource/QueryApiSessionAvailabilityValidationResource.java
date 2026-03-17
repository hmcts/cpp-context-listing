package uk.gov.justice.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("sessionAvailabilityValidation")
public interface QueryApiSessionAvailabilityValidationResource {

    @GET
    @Produces("application/vnd.listing.validate.session.availability+json")
    Response validateSessionAvailability(@QueryParam(SessionAvailabilityValidationQueryParamConstants.PANEL) String panel,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.SESSION_START_DATE) String sessionStartDate,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.SESSION_END_DATE) String sessionEndDate,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.HEARING_START_TIME) String hearingStartTime,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.OU_L2_CODE) String oucodeL2Code,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.OUCODE) String ouCode,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.COURT_ROOM_ID) String courtRoomId,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.COURT_ROOM_NUMBER) String courtRoomNumber,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.BUSINESS_TYPE) String businessType,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.COURT_SESSION) String courtSession,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.IS_SLOT_BASED) Boolean isSlotBased,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.SHOW_OVERBOOKED_SLOTS) Boolean showOverbookedSlots,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.PAGE_SIZE) String pageSize,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.PAGE_NUMBER) String pageNumber,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.AVAILABLE_DURATION_MINS) Integer availableDurationMins,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.STATUS) String status,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.CONSECUTIVE_DAYS) Integer consecutiveDays,
                                         @QueryParam(SessionAvailabilityValidationQueryParamConstants.IS_WEEK_COMMENCING) Boolean isWeekCommencing);
}

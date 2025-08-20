package uk.gov.justice.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("hearingSlots")
public interface QueryApiHearingSlotsResource {
    String PANEL = "panel";
    String SESSION_START_DATE = "sessionStartDate";
    String SESSION_END_DATE = "sessionEndDate";
    String HEARING_START_TIME = "hearingStartTime";
    String OU_L2_CODE = "oucodeL2Code";
    String OUCODE = "ouCode";
    String COURT_ROOM_ID = "courtRoomId";
    String COURT_ROOM_NUMBER = "courtRoomNumber";
    String BUSINESS_TYPE = "businessType";
    String COURT_SESSION = "courtSession";
    String IS_SLOT_BASED = "isSlotBased";
    String PAGE_SIZE = "pageSize";
    String PAGE_NUMBER = "pageNumber";

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
                             @QueryParam(PAGE_SIZE) String pageSize,
                             @QueryParam(PAGE_NUMBER) String pageNumber);
}
package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.CourtListIT.STANDARD;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.utils.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;


import org.hamcrest.Matcher;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import javax.ws.rs.core.Response;

import java.util.UUID;

public class CourtListSteps extends AbstractIT {
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final String COURT_LIST_DATA = "test";
    private  UpdatedHearingData updatedHearingData;
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST = "application/vnd.listing.search.court.list+json";
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD = "application/vnd.listing.search.court.list.payload+json";

    public CourtListSteps() { }

    public CourtListSteps(final UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        stubDocumentCreate(COURT_LIST_DATA);
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataJudiciaries(updatedHearingData.getJudiciary().get(0).getJudicialId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
    }

    public void verifyCourtListRequestedAndIsCorrect(final String listId) {
        final Response response = getResponseData(listId);
        final String responseData = response.readEntity(String.class);
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(COURT_LIST_DATA, responseData);
    }

    public void verifyCourtListRequestedAndIsCorrectJson(final String listId, final String templateName, final Matcher[] allocatedMatchers) {
        final String endDate = listId.equals(STANDARD) ? updatedHearingData.getStartDate() : updatedHearingData.getEndDate();
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.court.list.payload-court-room-id"), updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(), listId, endDate, updatedHearingData.getCourtRoomId()));
        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(allOf(
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].panel", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].caseId", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[0].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[1].id", notNullValue()),
                                withoutJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[1].listingNumber"),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[2].id", notNullValue()),
                                withoutJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[2].listingNumber"),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[1].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[1].offences[0].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[1].offences[1].id", notNullValue()),
                                withoutJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[1].offences[1].listingNumber"),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[1].offences[2].id", notNullValue()),
                                withoutJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[1].offences[2].listingNumber"),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].panel", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].caseId", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[0].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[0].offences[0].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[0].offences[1].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[0].offences[2].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[1].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[1].offences[0].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[1].offences[1].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[1].defendants[1].offences[2].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[2].id", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[2].panel", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[2].courtApplicationId", notNullValue()),
                                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[2].applicationOffences[0].id", notNullValue()),
                                withoutJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[2].applicationOffences[0].listingNumber"),
                                withJsonPath("$.templateName", is(templateName))),
                                allOf(allocatedMatchers)
                        )));
    }

    private Response getResponseData(final String listId) {
        final String endDate = listId.equals(STANDARD) ? updatedHearingData.getStartDate() : updatedHearingData.getEndDate();
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.court.list-court-room-id"), updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(), listId, endDate, updatedHearingData.getCourtRoomId()));
        final RequestParams requestParams = requestParams(searchHearingUrl, "application/vnd.listing.search.court.list+json")
                                                    .withHeader(USER_ID, USER_ID_VALUE)
                                                    .build();
        return new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
    }

    public void verifyCourtListGenerated(final UUID courtCentreId, final String listId, final String startDate, final String endDate) {
        stubDocumentCreate(COURT_LIST_DATA);
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.query.court-list"), listId, courtCentreId, startDate, endDate));
        final RequestParams requestParams = requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_COURT_LIST)
                .withHeader(USER_ID, USER_ID_VALUE)
                .build();

        final Response response = new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
        final String responseData = response.readEntity(String.class);
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(COURT_LIST_DATA, responseData);
    }

}

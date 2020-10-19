package uk.gov.moj.cpp.listing.steps;

import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.listing.it.CourtListIT.STANDARD;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.utils.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import javax.ws.rs.core.Response;

import java.util.UUID;

import javax.ws.rs.core.Response;

public class CourtListSteps extends AbstractIT {
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final String COURT_LIST_DATA = "test";
    private  UpdatedHearingData updatedHearingData;
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST = "application/vnd.listing.search.court.list+json";

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

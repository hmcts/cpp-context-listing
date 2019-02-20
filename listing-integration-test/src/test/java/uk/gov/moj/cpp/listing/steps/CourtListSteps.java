package uk.gov.moj.cpp.listing.steps;

import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.listing.it.CourtListIT.STANDARD;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.utils.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import javax.ws.rs.core.Response;


public class CourtListSteps  extends AbstractIT{
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final String COURT_LIST_DATA = "test";
    private final UpdatedHearingData updatedHearingData;


    public CourtListSteps(UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        stubDocumentCreate(COURT_LIST_DATA);
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId()));
        stubGetReferenceDataJudiciaries(updatedHearingData.getJudiciary().get(0).getJudicialId());
    }
    public void verifyCourtListRequestedAndIsCorrect(String listId) {
        Response response = getResponseData(listId);
        String responseData = response.readEntity(String.class);
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(COURT_LIST_DATA, responseData);
    }


    private Response getResponseData(final String listId) {
        String endDate = listId.equals(STANDARD) ? updatedHearingData.getStartDate() : updatedHearingData.getEndDate() ;
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.create.court.list"), updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(),listId, endDate));
        final RequestParams requestParams = requestParams(searchHearingUrl, "application/vnd.listing.search.court.list+json")
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
        return new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
    }

}

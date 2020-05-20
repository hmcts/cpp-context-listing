package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubSessionEndDateEmptyRequest;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ListStubMappingsResult;
import org.apache.http.HttpStatus;
import org.junit.Test;

public class HearingSlotsIT extends AbstractIT {

    private static final String QUERY_API_PATH = "/listing-query-api/query/api/rest/listing/hearingSlots";
    private static final String QUERY_MEDIA_TYPE = "application/vnd.listing.search.hearing.slots+json";

    @Test
    public void shouldGetHearingSlots() {
        final String queryString = getQueryString(getParams());

        stubGetAvailableHearingSlots();
        final ListStubMappingsResult listStubMappingsResult = WireMock.listAllStubMappings();
        final RequestParams requestParams = getRequestParams(queryString);
        poll(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(446)),
                        withJsonPath("$.pageCount", is(23)),
                        withJsonPath("$.hearingSlots.size()", is(10)),
                        withJsonPath("$.hearingSlots[0].courtScheduleId", is("0205eb29-5d01-4779-a8c1-3038bc39dc09")),
                        withJsonPath("$.hearingSlots[0].sessionDate", is("2020-06-01")),
                        withJsonPath("$.hearingSlots[0].ouCode", is("B01LY00")),
                        withJsonPath("$.hearingSlots[0].courtHouseName", is("Lavender Hill Magistrates' Court")),
                        withJsonPath("$.hearingSlots[0].courtRoomNumber", is(2331)),
                        withJsonPath("$.hearingSlots[0].courtRoomName", is("Courtroom 02")),
                        withJsonPath("$.hearingSlots[0].availableSlots", is(5)),
                        withJsonPath("$.hearingSlots[0].availableDuration", is(0)),
                        withJsonPath("$.hearingSlots[0].maxSlots", is(10)),
                        withJsonPath("$.hearingSlots[0].maxDuration", is(0))
                ))
        );
    }


    @Test
    public void shouldReturnErrorWhenSessionEndDateIsEmpty() throws IOException {
        final Map<String, String> params = getParams();
        params.remove("sessionEndDate");

        final String queryString = getQueryString(params);
        stubSessionEndDateEmptyRequest();

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria sessionEndDate cannot be null"));
    }


    private RequestParams getRequestParams(final String queryString) {
        final String url = format("%s%s%s%s", getBaseUri(), QUERY_API_PATH, "?", queryString);
        return requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
    }

    private ResponseData queryService(final RequestParams requestParams) {
        final Response response = new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
        return new ResponseData(Response.Status.fromStatusCode(response.getStatus()), response.readEntity(String.class), response.getHeaders());
    }


}

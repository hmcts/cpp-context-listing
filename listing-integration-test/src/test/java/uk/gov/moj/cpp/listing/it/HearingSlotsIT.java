package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
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
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubOuCodeAndL2CodeEmptyRequest;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubPageNumberEmptyRequest;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubPageSizeEmptyRequest;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubPanelEmptyRequest;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubSessionEndDateEmptyRequest;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubSessionStartDateEmptyRequest;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Ignoring for now, until we find an alternative way to replicate 3rd party service")
public class HearingSlotsIT extends AbstractIT {

    private static final String QUERY_API_PATH = "/listing-query-api/query/api/rest/listing/hearingSlots";
    private static final String QUERY_MEDIA_TYPE = "application/vnd.listing.search.hearing.slots+json";

    @Test
    public void shouldGetHearingSlots() throws IOException {
        final String queryString = getQueryString(getParams());

        stubGetAvailableHearingSlots(queryString);

        final RequestParams requestParams = getRequestParams(queryString);
        poll(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(446)),
                        withJsonPath("$.pageCount", is(23)),
                        withJsonPath("$.hearingSlots.size()", is(20)),
                        withJsonPath("$.hearingSlots[0].courtScheduleId", is("880cf889-02b5-4887-ac6b-86cdea380476")),
                        withJsonPath("$.hearingSlots[0].date", is("01-Oct-2019")),
                        withJsonPath("$.hearingSlots[0].ouCode", is("B01KR35")),
                        withJsonPath("$.hearingSlots[0].courtHouseName", is("Cheltenham MC TD")),
                        withJsonPath("$.hearingSlots[0].courtRoomId", is(1057)),
                        withJsonPath("$.hearingSlots[0].courtRoomName", is("Court 1 Cheltenham")),
                        withJsonPath("$.hearingSlots[0].businessType", is("DVB")),
                        withJsonPath("$.hearingSlots[0].courtSession", is("AM")),
                        withJsonPath("$.hearingSlots[0].availableSlot", is(0)),
                        withJsonPath("$.hearingSlots[0].availableDuration", is(3)),
                        withJsonPath("$.hearingSlots[0].maxSlot", is(0)),
                        withJsonPath("$.hearingSlots[0].maxDuration", is(3))
                )));
    }

    @Test
    public void shouldReturnErrorWhenSessionStartDateIsEmpty() throws IOException {
        final Map<String, String> params = getParams();
        params.remove("sessionStartDate");

        final String queryString = getQueryString(params);
        stubSessionStartDateEmptyRequest(queryString);

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria sessionStartDate cannot be null"));
    }

    @Test
    public void shouldReturnErrorWhenSessionEndDateIsEmpty() throws IOException {
        final Map<String, String> params = getParams();
        params.remove("sessionEndDate");

        final String queryString = getQueryString(params);
        stubSessionEndDateEmptyRequest(queryString);

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria sessionEndDate cannot be null"));
    }

    @Test
    public void shouldReturnErrorWhenOucodeAndOperationalUnitIsEmpty() throws IOException {
        final Map<String, String> params = getParams();
        params.remove("oucodeL2Code");

        final String queryString = getQueryString(params);
        stubOuCodeAndL2CodeEmptyRequest(queryString);

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Either oucodeL2Code or ouCode should be entered"));
    }

    @Test
    public void shouldReturnErrorWhenPanelIsEmpty() throws IOException {
        final Map<String, String> params = getParams();
        params.remove("panel");

        final String queryString = getQueryString(params);
        stubPanelEmptyRequest(queryString);

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria panel cannot be null"));
    }

    @Test
    public void shouldReturnErrorWhenPageNumberIsEmpty() throws IOException {
        final Map<String, String> params = getParams();
        params.remove("pageNumber");

        final String queryString = getQueryString(params);
        stubPageNumberEmptyRequest(queryString);

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria pageNumber cannot be null"));
    }

    @Test
    public void shouldReturnErrorWhenPageSizeIsEmpty() throws IOException {
        final Map<String, String> params = getParams();
        params.remove("pageSize");

        final String queryString = getQueryString(params);
        stubPageSizeEmptyRequest(queryString);

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria pageSize cannot be null"));
    }

    private RequestParams getRequestParams(final String queryString) {
        final String url = format("%s%s%s%s", getBaseUri(), QUERY_API_PATH, "?", queryString);
        return requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
    }

    private ResponseData queryService(final RequestParams requestParams) {
        Response response = new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
        return new ResponseData(Response.Status.fromStatusCode(response.getStatus()), response.readEntity(String.class), response.getHeaders());
    }

    private String getQueryString(final Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("&"));
    }

    private Map<String, String> getParams() {
        final Map<String, String> params = new HashMap<>();
        params.put("panel", "ADULT");
        params.put("oucodeL2Code", "Z01KR05");
        params.put("sessionStartDate", "2017-10-11");
        params.put("sessionEndDate", "2020-10-11");
        params.put("pageSize", "20");
        params.put("pageNumber", "1");

        return params;
    }
}

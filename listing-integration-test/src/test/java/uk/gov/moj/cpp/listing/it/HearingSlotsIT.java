package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.listing.utils.CSVFileReader;
import uk.gov.moj.cpp.listing.utils.DBUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class HearingSlotsIT extends AbstractIT {

    private static final String SLOTS_TABLE = "COURT_SCHEDULE";
    private static final String QUERY_API_PATH = "/listing-query-api/query/api/rest/listing/hearingSlots";
    private static final String QUERY_MEDIA_TYPE = "application/vnd.listing.search.hearing.slots+json";

    private String hearingSlotsServiceUrl;

    private String hearingServiceSubscriptionKey;

    private DBUtil dbUtil = new DBUtil();

    private CSVFileReader csvReader = new CSVFileReader();

    @Before
    public void setup() {
        hearingSlotsServiceUrl = Optional.ofNullable(getProperty("search-hearing-slots.url")).orElse("https://restapilatency-spike-function.azure-api.net/sandlblobeventprocessor/getApiForSchedulingAndListingTesting");
        hearingServiceSubscriptionKey = Optional.ofNullable(getProperty("search-hearing-slots.subscription.key")).orElse("93411ae22b514c12ade724e880c135dc");
    }

    @Test
    public void shouldGetHearingSlots() throws IOException {

        assertThat(checkPortalIsUpAndFunctionAppDeployed(), is(HttpStatus.SC_OK));

        cleanTestData();
        populateTestData();

        final RequestParams requestParams = getRequestParams(getParams());
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
    public void shouldReturnErrorWhenPanelIsEmpty() throws IOException {

        assertThat(checkPortalIsUpAndFunctionAppDeployed(), is(HttpStatus.SC_OK));

        final Map<String, String> params = getParams();
        params.remove("panel");

        final ResponseData responseData = queryService(getRequestParams(params));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria panel cannot  be null "));
    }

    @Test
    public void shouldReturnErrorWhenSessionStartDateIsEmpty() throws IOException {

        assertThat(checkPortalIsUpAndFunctionAppDeployed(), is(HttpStatus.SC_OK));

        final Map<String, String> params = getParams();
        params.remove("sessionStartDate");

        final ResponseData responseData = queryService(getRequestParams(params));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria sessionStartDate cannot  be null "));
    }

    @Test
    public void shouldReturnErrorWhenSessionEndDateIsEmpty() throws IOException {

        assertThat(checkPortalIsUpAndFunctionAppDeployed(), is(HttpStatus.SC_OK));

        final Map<String, String> params = getParams();
        params.remove("sessionEndDate");

        final ResponseData responseData = queryService(getRequestParams(params));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria sessionEndDate cannot  be null "));
    }

    @Test
    public void shouldReturnErrorWhenPageSizeIsEmpty() throws IOException {

        assertThat(checkPortalIsUpAndFunctionAppDeployed(), is(HttpStatus.SC_OK));

        final Map<String, String> params = getParams();
        params.remove("pageSize");

        final ResponseData responseData = queryService(getRequestParams(params));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria pageSize cannot  be null "));
    }

    @Test
    public void shouldReturnErrorWhenPageNumberIsEmpty() throws IOException {

        assertThat(checkPortalIsUpAndFunctionAppDeployed(), is(HttpStatus.SC_OK));

        final Map<String, String> params = getParams();
        params.remove("pageNumber");

        final ResponseData responseData = queryService(getRequestParams(params));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria pageNumber cannot  be null "));
    }


    @Test
    public void shouldReturnErrorWhenOucodeAndOperationalUnitIsEmpty() throws IOException {

        assertThat(checkPortalIsUpAndFunctionAppDeployed(), is(HttpStatus.SC_OK));

        final Map<String, String> params = getParams();
        params.remove("oucodeL2Code");

        final ResponseData responseData = queryService(getRequestParams(params));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Either oucodeL2Code or ouCode should be entered "));
    }

    private RequestParams getRequestParams(final Map<String, String> params) {
        final String queryString = params.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));

        final String url = format("%s%s%s%s", getBaseUri(), QUERY_API_PATH, "?", queryString);
        return requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
    }


    private ResponseData queryService(final RequestParams requestParams) {
        Response response = new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
        return new ResponseData(Response.Status.fromStatusCode(response.getStatus()), response.readEntity(String.class), response.getHeaders());
    }

    private int checkPortalIsUpAndFunctionAppDeployed() throws IOException {

        HttpURLConnection httpConn = (HttpURLConnection) new URL(format("%s%s%s", hearingSlotsServiceUrl, "?", getQueryString(getParams()))).openConnection();
        httpConn.setRequestProperty("Accept", "application/json");
        httpConn.setRequestProperty("Ocp-Apim-Subscription-Key", hearingServiceSubscriptionKey);
        httpConn.setRequestProperty("Ocp-Apim-Trace", "true");

        return httpConn.getResponseCode();
    }

    private void populateTestData() {
        final Map<String, Class> dataTypes = new HashMap<>();
        dataTypes.put("session_start", Date.class);
        dataTypes.put("court_room_id", Integer.class);
        dataTypes.put("max_slot", Integer.class);
        dataTypes.put("max_duration_mins", Integer.class);
        dataTypes.put("available_slot", Integer.class);
        dataTypes.put("available_duration_mins", Integer.class);

        final List<CSVRecord> csvRecords = csvReader.readData("testdata/scsl-248-court-schedule.csv");

        dbUtil.insert(SLOTS_TABLE, csvRecords, dataTypes);
    }

    private void cleanTestData() {
        dbUtil.delete(SLOTS_TABLE, "court_house_name like '%TD' and operational_unit like 'Z%'");
    }

    private String getQueryString(final Map<String, String> params) {

        return params.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private Map<String, String> getParams() {
        final Map<String, String> params = new HashMap<>();
        params.put("panel", "ADULT");
        params.put("sessionStartDate", "2017-10-11");
        params.put("sessionEndDate", "2020-10-11");
        params.put("pageSize", "20");
        params.put("pageNumber", "1");
        params.put("oucodeL2Code", "Z01KR05");

        return params;
    }
}

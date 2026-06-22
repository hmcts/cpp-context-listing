package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubSearchAvailableJudiciaries;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubSearchAvailableJudiciariesBadRequest;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

class SearchAvailableJudiciariesIT extends AbstractIT {

    private static final String QUERY_API_PATH = "/listing-query-api/query/api/rest/listing/judiciaries/search-available";
    private static final String QUERY_MEDIA_TYPE = "application/vnd.listing.search.available.judiciaries+json";

    @Test
    void shouldProxySearchAvailableJudiciariesFromCourtScheduler() {
        final Map<String, String> params = new HashMap<>();
        params.put("search", "ai");
        final String queryString = getQueryString(params);

        stubSearchAvailableJudiciaries();

        final RequestParams requestParams = getRequestParams(queryString);
        final ResponseData responseData = pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.judiciaries.size()", is(1)),
                        withJsonPath("$.judiciaries[0].id", is("9f39f876-3ff6-32b5-926e-c588e36a87b8")),
                        withJsonPath("$.judiciaries[0].surname", is("Ainsworth")),
                        withJsonPath("$.judiciaries[0].specialisms.size()", is(2))
                ))
        );

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_OK));
    }

    @Test
    void shouldPropagateBadRequestFromCourtScheduler() {
        final Map<String, String> params = new HashMap<>();
        params.put("search", "x");
        final String queryString = getQueryString(params);

        stubSearchAvailableJudiciariesBadRequest();

        final RequestParams requestParams = getRequestParams(queryString);
        final ResponseData responseData = queryService(requestParams);

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("search text too short"));
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

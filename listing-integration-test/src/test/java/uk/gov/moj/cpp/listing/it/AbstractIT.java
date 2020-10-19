package uk.gov.moj.cpp.listing.it;

import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Header;
import org.junit.Before;

@SuppressWarnings("WeakerAccess")
public class AbstractIT {
    public static final UUID USER_ID_VALUE = randomUUID();

    protected static final Header CPP_UID_HEADER = new Header(USER_ID, USER_ID_VALUE.toString());

    protected static RestClient restClient = new RestClient();
    protected static final DateTimeFormatter ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    protected static final boolean ALLOCATED = true;
    protected static final boolean UNALLOCATED = false;

    private final static ThreadLocal<UUID> USER_CONTEXT = new ThreadLocal<>();

    @Before
    public void setUp() {
        reset();
        setupAsAuthorisedUser(USER_ID_VALUE);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased();
    }

    protected void givenAUserHasLoggedInAsAListingOfficer(final UUID validUserId) {
        setLoggedInUser(validUserId);
    }

    protected void setLoggedInUser(final UUID userId) {
        USER_CONTEXT.set(userId);
    }

    protected UUID getLoggedInUser() {
        return USER_CONTEXT.get();
    }

    protected MultivaluedMap<String, Object> getLoggedInHeader() {
        return getLoggedInHeader(getLoggedInUser());
    }

    protected MultivaluedMap<String, Object> getLoggedInHeader(final UUID userId) {
        final MultivaluedMap<String, Object> header = new MultivaluedHashMap<>();
        header.add(USER_ID, userId);
        return header;
    }

    protected String getQueryString(final Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("&"));
    }

    protected Map<String, String> getParams() {
        final Map<String, String> params = new HashMap<>();
        params.put("panel", "ADULT");
        params.put("oucodeL2Code", "Z01KR05");
        params.put("sessionStartDate", "2017-10-11");
        params.put("sessionEndDate", "2020-10-11");
        params.put("pageSize", "20");
        params.put("pageNumber", "1");

        return params;
    }

    protected static String getStringFromResource(final String path) throws IOException {
        return Resources.toString(getResource(path), defaultCharset());
    }
}

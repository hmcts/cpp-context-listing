package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.listing.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetProvisionalBookedSlots;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.jayway.restassured.response.Header;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class AbstractIT {
        public static final UUID USER_ID_VALUE = randomUUID();

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIT.class);
    protected static final Header CPP_UID_HEADER = new Header(USER_ID, USER_ID_VALUE.toString());

    protected static RestClient restClient = new RestClient();
    protected static final DateTimeFormatter ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    protected static final boolean ALLOCATED = true;
    protected static final boolean UNALLOCATED = false;

    private final static ThreadLocal<UUID> USER_CONTEXT = new ThreadLocal<>();

    public static void givenAUserHasLoggedInAsAListingOfficers(final UUID validUserId) {
        setLoggedInUser(validUserId);
    }

    @Before
    public void setUp() {
        setupAsAuthorisedUser(USER_ID_VALUE);
        stubEnableAllCapabilities();
        stubGetProvisionalBookedSlots();
    }

    protected static void setLoggedInUser(final UUID userId) {
        USER_CONTEXT.set(userId);
    }

    protected static UUID getLoggedInUser() {
        return USER_CONTEXT.get();
    }

    protected static MultivaluedMap<String, Object> getLoggedInHeader() {
        final MultivaluedMap<String, Object> header = new MultivaluedHashMap<>();
        header.add(USER_ID, getLoggedInUser().toString());
        return header;
    }

    public String getQueryString(final Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("&"));
    }

    public Map<String, String> getParams() {
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

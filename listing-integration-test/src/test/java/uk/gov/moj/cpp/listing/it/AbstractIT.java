package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.listing.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;

@SuppressWarnings("WeakerAccess")
public class AbstractIT {

    public static final UUID USER_ID_VALUE = randomUUID();

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

}

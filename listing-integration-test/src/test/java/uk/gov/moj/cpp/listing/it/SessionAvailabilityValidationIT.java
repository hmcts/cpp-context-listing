package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubValidateSessionAvailability;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubValidateSessionAvailabilityFailure;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

class SessionAvailabilityValidationIT extends AbstractIT {

    private static final String VALIDATE_SESSION_AVAILABILITY_URL = "listing.query.validate-session-availability";
    private static final String CONTENT_TYPE = "application/vnd.listing.validate.session.availability+json";

    private static final String VALID_PAYLOAD =
            "{\"courtScheduleIdList\":[{\"courtScheduleId\":\"f8254db1-1683-483e-afb3-b87fde5a0a26\"}],\"duration\":30}";
    private static final String PAYLOAD_WITHOUT_DURATION =
            "{\"courtScheduleIdList\":[{\"courtScheduleId\":\"f8254db1-1683-483e-afb3-b87fde5a0a26\"}]}";

    @Test
    void shouldReturnOkWhenCourtSchedulerValidatesSuccessfully() {
        stubValidateSessionAvailability();

        final Response response = postToValidateSessionAvailability(VALID_PAYLOAD);

        assertThat(response.getStatus(), is(OK.getStatusCode()));
    }

    @Test
    void shouldPassThroughCourtSchedulerErrorResponse() {
        stubValidateSessionAvailabilityFailure();

        final Response response = postToValidateSessionAvailability(PAYLOAD_WITHOUT_DURATION);

        assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
    }

    private Response postToValidateSessionAvailability(final String payload) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(VALIDATE_SESSION_AVAILABILITY_URL)));
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());
        return restClient.postCommand(url, CONTENT_TYPE, payload, headers);
    }
}

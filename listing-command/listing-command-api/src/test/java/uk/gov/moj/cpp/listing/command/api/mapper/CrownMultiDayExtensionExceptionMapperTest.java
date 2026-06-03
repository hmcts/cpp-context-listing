package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import uk.gov.moj.cpp.listing.common.crownfallback.CrownMultiDayExtensionException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class CrownMultiDayExtensionExceptionMapperTest {

    private CrownMultiDayExtensionExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CrownMultiDayExtensionExceptionMapper();
        mapper.logger = LoggerFactory.getLogger(CrownMultiDayExtensionExceptionMapperTest.class);
    }

    @Test
    void noAvailability_returns422_withErrorCodeAndUnavailableDates() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_AVAILABILITY")
                .add("message", "One or more requested session dates are not bookable")
                .add("unavailableDates", createArrayBuilder().add("2026-03-04").add("2026-03-05"))
                .build();

        final Response response = mapper.toResponse(
                new CrownMultiDayExtensionException(422, body, "rejected"));

        assertThat(response.getStatus(), is(422));
        final String entity = response.getEntity().toString();
        assertThat(entity, containsString("\"errorCode\":\"NO_AVAILABILITY\""));
        assertThat(entity, containsString("\"unavailableDates\":[\"2026-03-04\",\"2026-03-05\"]"));
    }

    @Test
    void startDateChangeNotAllowed_returns422_withErrorCodeOnly() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "START_DATE_CHANGE_NOT_ALLOWED")
                .add("message", "Start date cannot change for an existing multi-day hearing")
                .build();

        final Response response = mapper.toResponse(
                new CrownMultiDayExtensionException(422, body, "rejected"));

        assertThat(response.getStatus(), is(422));
        final String entity = response.getEntity().toString();
        assertThat(entity, containsString("\"errorCode\":\"START_DATE_CHANGE_NOT_ALLOWED\""));
        assertThat(entity, not(containsString("unavailableDates")));
    }

    @Test
    void noExistingAllocation_returns422_withErrorCodeOnly() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_EXISTING_ALLOCATION")
                .build();

        final Response response = mapper.toResponse(
                new CrownMultiDayExtensionException(422, body, "rejected"));

        assertThat(response.getStatus(), is(422));
        final String entity = response.getEntity().toString();
        assertThat(entity, containsString("\"errorCode\":\"NO_EXISTING_ALLOCATION\""));
        assertThat(entity, not(containsString("unavailableDates")));
    }

    @Test
    void preservesUnderlyingHttpStatus_for500() {
        final JsonObject body = createObjectBuilder().build();

        final Response response = mapper.toResponse(
                new CrownMultiDayExtensionException(500, body, "courtscheduler unavailable"));

        assertThat(response.getStatus(), is(500));
    }
}

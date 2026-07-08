package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.listing.common.courtroomchange.ChangeCourtRoomForMultidayException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ChangeCourtRoomForMultidayExceptionMapperTest {

    private ChangeCourtRoomForMultidayExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ChangeCourtRoomForMultidayExceptionMapper();
        mapper.logger = LoggerFactory.getLogger(ChangeCourtRoomForMultidayExceptionMapperTest.class);
    }

    @Test
    void noSessionFound_returns422_withErrorCodeAndMessage() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_SESSION_FOUND")
                .add("message", "No court-schedule session found")
                .build();

        final Response response = mapper.toResponse(new ChangeCourtRoomForMultidayException(422, body, "rejected"));

        assertThat(response.getStatus(), is(422));
        final String entity = response.getEntity().toString();
        assertThat(entity, containsString("\"errorCode\":\"NO_SESSION_FOUND\""));
        assertThat(entity, containsString("\"message\":\"No court-schedule session found\""));
    }

    @Test
    void noAllocationOnDate_returns422_withErrorCode() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_ALLOCATION_ON_DATE")
                .add("message", "No allocation for the requested date")
                .build();

        final Response response = mapper.toResponse(new ChangeCourtRoomForMultidayException(422, body, "rejected"));

        assertThat(response.getStatus(), is(422));
        assertThat(response.getEntity().toString(), containsString("\"errorCode\":\"NO_ALLOCATION_ON_DATE\""));
    }

    @Test
    void nullBody_fallsBackToExceptionMessage() {
        final Response response = mapper.toResponse(new ChangeCourtRoomForMultidayException(500, null, "unexpected failure"));

        assertThat(response.getStatus(), is(500));
        assertThat(response.getEntity().toString(), containsString("\"message\":\"unexpected failure\""));
    }
}

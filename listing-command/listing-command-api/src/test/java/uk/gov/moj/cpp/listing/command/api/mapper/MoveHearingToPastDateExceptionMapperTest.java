package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class MoveHearingToPastDateExceptionMapperTest {

    private MoveHearingToPastDateExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MoveHearingToPastDateExceptionMapper();
        mapper.logger = LoggerFactory.getLogger(MoveHearingToPastDateExceptionMapperTest.class);
    }

    @Test
    void futureDate_returns422_withErrorCodeAndMessage() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "FUTURE_DATE_NOT_ALLOWED")
                .add("message", "Hearings can only be moved to today or an earlier date")
                .build();

        final Response response = mapper.toResponse(new MoveHearingToPastDateException(422, body, "rejected"));

        assertThat(response.getStatus(), is(422));
        final String entity = response.getEntity().toString();
        assertThat(entity, containsString("\"errorCode\":\"FUTURE_DATE_NOT_ALLOWED\""));
        assertThat(entity, containsString("\"message\":\"Hearings can only be moved to today or an earlier date\""));
    }

    @Test
    void unknownHearing_returns422_withHearingIdNotFound() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "HEARING_ID_NOT_FOUND")
                .add("message", "No hearing found")
                .build();

        final Response response = mapper.toResponse(new MoveHearingToPastDateException(422, body, "rejected"));

        assertThat(response.getStatus(), is(422));
        assertThat(response.getEntity().toString(), containsString("\"errorCode\":\"HEARING_ID_NOT_FOUND\""));
    }

    @Test
    void noSession_propagates404_withMessage() {
        final JsonObject body = createObjectBuilder()
                .add("message", "No court-schedule session found")
                .build();

        final Response response = mapper.toResponse(new MoveHearingToPastDateException(404, body, "not found"));

        assertThat(response.getStatus(), is(404));
        assertThat(response.getEntity().toString(), containsString("\"message\":\"No court-schedule session found\""));
    }

    @Test
    void nullBody_fallsBackToExceptionMessage() {
        final Response response = mapper.toResponse(new MoveHearingToPastDateException(500, null, "unexpected failure"));

        assertThat(response.getStatus(), is(500));
        assertThat(response.getEntity().toString(), containsString("\"message\":\"unexpected failure\""));
    }
}

package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.listing.common.split.SplitHearingRejectedException;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SplitHearingRejectedExceptionMapperTest {

    private SplitHearingRejectedExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SplitHearingRejectedExceptionMapper();
        mapper.logger = LoggerFactory.getLogger(SplitHearingRejectedExceptionMapperTest.class);
    }

    @Test
    void shouldReturnProgressionStatusAndBodyUnchanged() {
        final JsonObject body = createObjectBuilder()
                .add("error", "Offences are not on hearing")
                .build();

        final Response response = mapper.toResponse(
                new SplitHearingRejectedException(400, body, "rejected"));

        assertThat(response.getStatus(), is(400));
        assertThat(response.getEntity().toString(), containsString("\"error\":\"Offences are not on hearing\""));
        assertThat(response.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    void shouldCarryA404Through() {
        final Response response = mapper.toResponse(
                new SplitHearingRejectedException(404, createObjectBuilder().build(), "unknown hearing"));

        assertThat(response.getStatus(), is(404));
    }

    @Test
    void shouldCarryA409Through() {
        final Response response = mapper.toResponse(
                new SplitHearingRejectedException(409, createObjectBuilder().add("error", "resulted").build(), "resulted"));

        assertThat(response.getStatus(), is(409));
        assertThat(response.getEntity().toString(), containsString("resulted"));
    }

    // Progression can reject with an empty body; the mapper must still produce valid JSON rather
    // than a null entity the container would render as a 500.
    @Test
    void shouldRenderAnEmptyObjectWhenProgressionSentNoBody() {
        final Response response = mapper.toResponse(
                new SplitHearingRejectedException(400, null, "no body"));

        assertThat(response.getStatus(), is(400));
        assertThat(response.getEntity().toString(), is("{}"));
    }

    @Test
    void shouldExposeStatusAndBodyOnTheException() {
        final JsonObject body = createObjectBuilder().add("errorCode", "NOT_A_SUBSET").build();
        final SplitHearingRejectedException exception = new SplitHearingRejectedException(400, body, "rejected");

        assertThat(exception.getHttpStatus(), is(400));
        assertThat(exception.getResponseBody(), is(body));
        assertThat(exception.getMessage(), is("rejected"));
    }
}

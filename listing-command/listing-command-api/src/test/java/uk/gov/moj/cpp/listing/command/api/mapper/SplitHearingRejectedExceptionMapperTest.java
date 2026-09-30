package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.listing.common.splithearing.SplitHearingRejectedException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class SplitHearingRejectedExceptionMapperTest {

    @Mock
    private Logger logger;

    @InjectMocks
    private SplitHearingRejectedExceptionMapper mapper;

    @Test
    void shouldReturnProgressionStatusWithErrorCodeAndBodyMessage() {
        final JsonObject body = createObjectBuilder().add("errorCode", "STALE").add("message", "hearing changed").build();

        final Response response = mapper.toResponse(new SplitHearingRejectedException(409, body, "fallback"));

        assertThat(response.getStatus(), is(409));
        assertThat(response.getMediaType().toString(), is("application/json"));
        assertThat(response.getEntity().toString(), is("{\"errorCode\":\"STALE\",\"message\":\"hearing changed\"}"));
    }

    @Test
    void shouldFallBackToExceptionMessageWhenBodyHasNoMessage() {
        final JsonObject body = createObjectBuilder().add("errorCode", "NOT_FOUND").build();

        final Response response = mapper.toResponse(new SplitHearingRejectedException(404, body, "fallback"));

        assertThat(response.getStatus(), is(404));
        assertThat(response.getEntity().toString(), is("{\"errorCode\":\"NOT_FOUND\",\"message\":\"fallback\"}"));
    }

    @Test
    void shouldUseExceptionMessageAndOmitErrorCodeWhenBodyIsNull() {
        final Response response = mapper.toResponse(new SplitHearingRejectedException(500, null, "progression down"));

        assertThat(response.getStatus(), is(500));
        assertThat(response.getEntity().toString(), is("{\"message\":\"progression down\"}"));
    }

    @Test
    void shouldOmitMessageWhenNoneAvailable() {
        final Response response = mapper.toResponse(new SplitHearingRejectedException(400, null, null));

        assertThat(response.getStatus(), is(400));
        assertThat(response.getEntity().toString(), is("{}"));
    }
}

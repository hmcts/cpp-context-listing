package uk.gov.moj.cpp.listing.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Ported from listing-common's ProgressionSplitHearingServiceTest (MBD refactor P2.3.3). */
class ProgressionSplitHearingServiceTest {

    private static final String GATEWAY = "http://gw";
    private static final String SPLIT_URL_PREFIX = GATEWAY + "/progression-service/command/api/rest/progression/hearing/";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ProgressionSplitHearingService service = new ProgressionSplitHearingService(builder.build(),
            new ListingClientsProperties(GATEWAY, null, UUID.randomUUID(), null, null));

    private final JsonObject payload = Json.createObjectBuilder().add("listNewHearing", Json.createObjectBuilder()).build();

    @Test
    void shouldForwardPayloadAndAllMetadataHeadersAndReturnProgressionBody() {
        final String hearingId = UUID.randomUUID().toString();
        final UUID causation1 = UUID.randomUUID();
        final UUID causation2 = UUID.randomUUID();
        server.expect(requestTo(SPLIT_URL_PREFIX + hearingId + "/split"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/vnd.progression.split-hearing+json"))
                .andExpect(header("CJSCPPUID", "user"))
                .andExpect(header("CPPSID", "session"))
                .andExpect(header("CPPCLIENTCORRELATIONID", "correlation"))
                .andExpect(header("CPPCAUSATION", causation1 + "," + causation2))
                .andExpect(content().json(payload.toString(), true))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"bad\"}"));

        final HttpResult result = service.splitHearing(hearingId, payload, Optional.of("user"), Optional.of("session"),
                Optional.of("correlation"), List.of(causation1, causation2));

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.jsonEntity().getString("message")).isEqualTo("bad");
        server.verify();
    }

    @Test
    void shouldOmitOptionalHeadersWhenMetadataHasNone() {
        final String hearingId = UUID.randomUUID().toString();
        server.expect(requestTo(SPLIT_URL_PREFIX + hearingId + "/split"))
                .andExpect(headerDoesNotExist("CJSCPPUID"))
                .andExpect(headerDoesNotExist("CPPSID"))
                .andExpect(headerDoesNotExist("CPPCLIENTCORRELATIONID"))
                .andExpect(headerDoesNotExist("CPPCAUSATION"))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        final HttpResult result = service.splitHearing(hearingId, payload, Optional.empty(), Optional.empty(), Optional.empty(), List.of());

        assertThat(result.getStatus()).isEqualTo(202);
        server.verify();
    }

    @Test
    void shouldReturnProgressionStatusUnchangedWithNullEntityWhenBodyBlank() {
        final String hearingId = UUID.randomUUID().toString();
        server.expect(requestTo(SPLIT_URL_PREFIX + hearingId + "/split")).andRespond(withStatus(HttpStatus.CONFLICT).body("  "));

        final HttpResult result = service.splitHearing(hearingId, payload, Optional.empty(), Optional.empty(), Optional.empty(), List.of());

        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.hasEntity()).isFalse();
    }

    @Test
    void shouldReturn500WhenProgressionCannotBeReached() {
        final String hearingId = UUID.randomUUID().toString();
        server.expect(requestTo(SPLIT_URL_PREFIX + hearingId + "/split")).andRespond(withException(new IOException("down")));

        final HttpResult result = service.splitHearing(hearingId, payload, Optional.empty(), Optional.empty(), Optional.empty(), List.of());

        assertThat(result.getStatus()).isEqualTo(500);
        assertThat(result.hasEntity()).isFalse();
    }
}

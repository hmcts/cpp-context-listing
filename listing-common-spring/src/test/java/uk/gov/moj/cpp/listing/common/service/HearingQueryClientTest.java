package uk.gov.moj.cpp.listing.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** {@link HearingQueryClient} (MBD refactor P2.3.3): URL, Accept, CJSCPPUID and status handling as the JEE client. */
class HearingQueryClientTest {

    private static final String GATEWAY = "http://gw";
    private static final UUID SYSTEM_USER = UUID.randomUUID();

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final HearingQueryClient client = new HearingQueryClient(builder.build(),
            new ListingClientsProperties(GATEWAY, null, SYSTEM_USER, null, null));

    @Test
    void getsThePtphDetailAsTheSystemUser() {
        final UUID hearingId = UUID.randomUUID();
        server.expect(requestTo(GATEWAY + "/hearing-service/query/api/rest/hearing/hearings/" + hearingId + "/ptph-detail"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "application/vnd.hearing.get-ptph-detail+json"))
                .andExpect(header("CJSCPPUID", SYSTEM_USER.toString()))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"tier\":\"A\",\"finalised\":true}"));

        final Optional<JsonObject> detail = client.getPtphDetail(hearingId);

        assertThat(detail).isPresent();
        assertThat(detail.get().getString("tier")).isEqualTo("A");
        server.verify();
    }

    @Test
    void aNotFoundIsEmpty() {
        server.expect(requestTo(GATEWAY + "/hearing-service/query/api/rest/hearing/x")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.get("/x", "application/vnd.x+json")).isEmpty();
    }

    @Test
    void anyOtherStatusThrows() {
        server.expect(requestTo(GATEWAY + "/hearing-service/query/api/rest/hearing/x")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.get("/x", "application/vnd.x+json"))
                .isInstanceOf(HearingQueryException.class)
                .hasMessage("Hearing query /x failed with status 500");
    }

    @Test
    void anIoFailureThrows() {
        server.expect(requestTo(GATEWAY + "/hearing-service/query/api/rest/hearing/x")).andRespond(withException(new IOException("down")));

        assertThatThrownBy(() -> client.get("/x", "application/vnd.x+json")).isInstanceOf(HearingQueryException.class);
    }

    @Test
    void theConfiguredRootIsUsed() {
        final RestClient.Builder otherBuilder = RestClient.builder();
        final MockRestServiceServer otherServer = MockRestServiceServer.bindTo(otherBuilder).build();
        final HearingQueryClient other = new HearingQueryClient(otherBuilder.build(),
                new ListingClientsProperties(GATEWAY, null, SYSTEM_USER, null, null, "http://hq/", null));
        otherServer.expect(requestTo("http://hq/x")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(other.get("/x", "application/vnd.x+json")).isEmpty();
        otherServer.verify();
    }
}

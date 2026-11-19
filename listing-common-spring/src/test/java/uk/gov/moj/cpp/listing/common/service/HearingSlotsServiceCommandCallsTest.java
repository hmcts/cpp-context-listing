package uk.gov.moj.cpp.listing.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The command-side courtscheduler calls of {@link HearingSlotsService} and
 * {@link ProvisionalBookingService} (MBD refactor P2.3.2): URL, media type, CJSCPPUID and body as
 * the JEE Apache HttpClient calls, and their status handling.
 */
class HearingSlotsServiceCommandCallsTest {

    private static final String BASE = "http://cs/courtscheduler";
    private static final UUID SYSTEM_USER = UUID.randomUUID();

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ListingClientsProperties properties = new ListingClientsProperties("http://gw", BASE, SYSTEM_USER, null, null);
    private final RestClient restClient = builder.build();
    private final HearingSlotsService service = new HearingSlotsService(restClient, properties);

    @Test
    void searchAndBookPostsTheParamsAsATypedBodyWithTheHearingIdInThePath() {
        final UUID hearingId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/hearings/" + hearingId))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/vnd.courtscheduler.mags.search.and.book+json"))
                .andExpect(header("CJSCPPUID", SYSTEM_USER.toString()))
                .andExpect(content().json("{\"durationInMinutes\":30,\"isPolice\":true,\"ouCode\":\"B01\"}", true))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"courtScheduleId\":\"x\"}"));

        final Map<String, String> params = new LinkedHashMap<>();
        params.put("hearingId", hearingId.toString());
        params.put("durationInMinutes", "30");
        params.put("isPolice", "true");
        params.put("ouCode", "B01");
        final HttpResult result = service.searchBookSlots(params);

        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(result.jsonEntity().getString("courtScheduleId")).isEqualTo("x");
        server.verify();
    }

    @Test
    void searchAndBookKeepsTheStatusAndJsonBodyOfARejection() {
        final UUID hearingId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/hearings/" + hearingId))
                .andExpect(header("Content-Type", "application/vnd.courtscheduler.crown.search.and.book+json"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON).body("{\"errorCode\":\"NO_AVAILABILITY\"}"));

        final HttpResult result = service.multiDaySearchAndBook(Map.of("hearingId", hearingId.toString()));

        assertThat(result.getStatus()).isEqualTo(422);
        assertThat(result.jsonEntity().getString("errorCode")).isEqualTo("NO_AVAILABILITY");
    }

    @Test
    void moveHearingToPastDateHasNoEntityForABlankBody() {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject payload = Json.createObjectBuilder().add("sessionDate", "2026-01-01").build();
        server.expect(requestTo(BASE + "/hearings/" + hearingId))
                .andExpect(header("Content-Type", "application/vnd.courtscheduler.move-hearing-to-past-date+json"))
                .andExpect(content().json(payload.toString(), true))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        final HttpResult result = service.moveHearingToPastDate(hearingId, payload);

        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.hasEntity()).isFalse();
    }

    @Test
    void deleteReleasesTheSessionsAndSwallowsAFailure() {
        final UUID hearingId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/sessions/" + hearingId))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Content-Type", "application/vnd.courtscheduler.release.sessions+json"))
                .andExpect(header("CJSCPPUID", SYSTEM_USER.toString()))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        service.delete(hearingId);

        server.verify();
    }

    @Test
    void provisionalSlotsAreReadAndANon200IsA500() {
        final ProvisionalBookingService provisional = new ProvisionalBookingService(restClient, properties);
        server.expect(requestTo(BASE + "/unconfirmedBooking?bookingIds=bk-1"))
                .andExpect(header("Accept", "application/vnd.courtscheduler.get.unconfirmed.booking+json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"provisionalSlots\":[]}"));
        server.expect(requestTo(BASE + "/unconfirmedBooking?bookingIds=bk-2"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThat(provisional.getSlots(Map.of("bookingIds", "bk-1")).jsonEntity().getJsonArray("provisionalSlots")).isEmpty();
        assertThat(provisional.getSlots(Map.of("bookingIds", "bk-2")).getStatus()).isEqualTo(500);
    }
}

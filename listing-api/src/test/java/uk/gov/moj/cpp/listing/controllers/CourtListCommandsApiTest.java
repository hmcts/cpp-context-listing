package uk.gov.moj.cpp.listing.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.listing.command.courtlist.CourtListCommands;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.Map;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class CourtListCommandsApiTest {

    private final CourtListCommands commands = mock(CourtListCommands.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final CourtListCommandsApi api = new CourtListCommandsApi(commands, request);

    private final ArgumentCaptor<Metadata> metadata = ArgumentCaptor.forClass(Metadata.class);
    private final ArgumentCaptor<JsonObject> payload = ArgumentCaptor.forClass(JsonObject.class);

    @Test
    void publishCourtListTakesTheCourtCentreFromThePathAndReturns202() {
        request.addHeader("CJSCPPUID", "user-1");

        final var response = api.postPublishCourtList("centre-1", Map.of("courtCentreId", "other", "startDate", "2026-11-23",
                "endDate", "2026-11-23", "publishCourtListType", "DRAFT"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(commands).publishCourtList(metadata.capture(), payload.capture());
        assertThat(metadata.getValue().name()).isEqualTo("listing.command.publish-court-list");
        assertThat(metadata.getValue().userId()).contains("user-1");
        assertThat(payload.getValue().getString("courtCentreId")).isEqualTo("centre-1");
        assertThat(payload.getValue().getString("publishCourtListType")).isEqualTo("DRAFT");
    }

    @Test
    void publishCourtListsForCrownCourtsReturns202() {
        assertThat(api.postPublishCourtListsForCrownCourts(Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        verify(commands).publishCourtListsForCrownCourts(metadata.capture(), payload.capture());
        assertThat(metadata.getValue().name()).isEqualTo("listing.command.publish-court-lists-for-crown-courts");
    }

    @Test
    void requestExportMergesThePathParameters() {
        assertThat(api.postCourtListRequestExport("centre-1", "FINAL", "2026-11-20", Map.of("courtListJson", "{}")).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);

        verify(commands).courtListRequestExport(metadata.capture(), payload.capture());
        assertThat(metadata.getValue().name()).isEqualTo("listing.command.court-list-request-export");
        assertThat(payload.getValue().getString("courtCentreId")).isEqualTo("centre-1");
        assertThat(payload.getValue().getString("publishCourtListType")).isEqualTo("FINAL");
        assertThat(payload.getValue().getString("startDate")).isEqualTo("2026-11-20");
        assertThat(payload.getValue().getString("courtListJson")).isEqualTo("{}");
    }
}

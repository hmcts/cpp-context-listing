package uk.gov.moj.cpp.listing.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerSearchService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchAvailableJudiciariesQueryHandlerTest {

    @Mock
    private CourtSchedulerSearchService courtSchedulerSearchService;

    @InjectMocks
    private SearchAvailableJudiciariesQueryHandler handler;

    @Test
    void searchAvailableJudiciaries_returnsEnvelopeWhenCourtSchedulerOk() {
        final JsonObject payload = Json.createObjectBuilder().add("search", "ai").build();
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("listing.search.available.judiciaries"),
                payload);
        final JsonObject courtSchedulerBody = Json.createObjectBuilder()
                .add("judiciaries", Json.createArrayBuilder().build())
                .build();
        when(courtSchedulerSearchService.searchAvailableJudiciaries(any()))
                .thenReturn(Response.ok(courtSchedulerBody).build());

        final JsonEnvelope result = handler.searchAvailableJudiciaries(query);

        assertThat(result, jsonEnvelope(
                metadata().withName("listing.search.available.judiciaries"),
                payloadIsJson(allOf(withJsonPath("$.judiciaries.length()", equalTo(0))))));
        verify(courtSchedulerSearchService).searchAvailableJudiciaries(argThat(m ->
                "ai".equals(m.get("search"))));
    }

    @Test
    void searchAvailableJudiciaries_propagatesNonOkStatus() {
        final JsonObject payload = Json.createObjectBuilder().add("search", "x").build();
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("listing.search.available.judiciaries"),
                payload);
        when(courtSchedulerSearchService.searchAvailableJudiciaries(any()))
                .thenReturn(Response.status(Response.Status.BAD_REQUEST).entity("bad").build());

        final WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> handler.searchAvailableJudiciaries(query));

        assertThat(ex.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }
}

package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PtphDetailServiceTest {

    private static final UUID SEEDING_HEARING_ID = randomUUID();

    @Mock
    private Requester requester;

    @InjectMocks
    private PtphDetailService ptphDetailService;

    private JsonEnvelope incoming() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.list-next-hearings-v2").build(),
                createObjectBuilder().build());
    }

    private JsonEnvelope response(final JsonObject payload) {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("hearing.get-ptph-detail").build(), payload);
    }

    @Test
    void shouldReturnDetailWhenFinalised() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("tier", "TIER_3")
                .add("listType", "TYPE_1_FIXED")
                .add("keyReason", "Vulnerable witness")
                .add("finalised", true)
                .build()));

        final Optional<PtphDetail> result = ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming());

        assertTrue(result.isPresent());
        assertEquals("TIER_3", result.get().getTier());
        assertEquals("TYPE_1_FIXED", result.get().getListType());
        assertEquals("Vulnerable witness", result.get().getKeyReason());
    }

    @Test
    void shouldQueryHearingContextWithSeedingHearingId() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("finalised", false).build()));

        ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming());

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        org.mockito.Mockito.verify(requester).requestAsAdmin(captor.capture());
        assertEquals("hearing.get-ptph-detail", captor.getValue().metadata().name());
        assertEquals(SEEDING_HEARING_ID.toString(),
                captor.getValue().payloadAsJsonObject().getString("hearingId"));
    }

    @Test
    void shouldReturnEmptyWhenNotFinalised() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("tier", "TIER_3")
                .add("listType", "TYPE_2_FLEXIBLE")
                .add("finalised", false)
                .build()));

        assertFalse(ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()).isPresent());
    }

    @Test
    void shouldReturnEmptyWhenNoRecordExists() {
        // hearing context returns finalised=false with null fields when there is no row
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("finalised", false).build()));

        assertFalse(ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()).isPresent());
    }

    @Test
    void shouldPropagateQueryFailure() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class)))
                .thenThrow(new RuntimeException("hearing context unavailable"));

        assertThrows(RuntimeException.class,
                () -> ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()));
    }
}

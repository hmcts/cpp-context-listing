package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.HearingQueryClient;
import uk.gov.moj.cpp.listing.common.service.HearingQueryException;
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
    private HearingQueryClient hearingQueryClient;

    @InjectMocks
    private PtphDetailService ptphDetailService;

    private JsonEnvelope incoming() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.list-next-hearings-v2").build(),
                createObjectBuilder().build());
    }

    private Optional<JsonObject> response(final JsonObject payload) {
        return Optional.of(payload);
    }

    @Test
    void shouldReturnDetailWhenFinalised() {
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID)).thenReturn(response(createObjectBuilder()
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
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID)).thenReturn(response(createObjectBuilder()
                .add("finalised", false).build()));

        ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming());

        org.mockito.Mockito.verify(hearingQueryClient).getPtphDetail(SEEDING_HEARING_ID);
    }

    @Test
    void shouldReturnEmptyWhenNotFinalised() {
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID)).thenReturn(response(createObjectBuilder()
                .add("tier", "TIER_3")
                .add("listType", "TYPE_2_FLEXIBLE")
                .add("finalised", false)
                .build()));

        assertFalse(ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()).isPresent());
    }

    @Test
    void shouldReturnEmptyWhenNoRecordExists() {
        // hearing context returns finalised=false with null fields when there is no row
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID)).thenReturn(response(createObjectBuilder()
                .add("finalised", false).build()));

        assertFalse(ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()).isPresent());
    }

    @Test
    void shouldPropagateQueryFailure() {
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID))
                .thenThrow(new HearingQueryException("hearing context unavailable"));

        assertThrows(HearingQueryException.class,
                () -> ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()));
    }

    /** A 404 from the hearing context reaches us as an empty Optional, not an exception. */
    @Test
    void shouldReturnEmptyWhenTheHearingContextHasNoRecord() {
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID)).thenReturn(Optional.empty());

        assertFalse(ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming()).isPresent());
    }

    /**
     * A finalised record needs both a tier and a list type, so this shape should not occur —
     * but the reader must not blow up on a field the hearing context omitted, because that
     * would fail the whole listing command over a missing optional value.
     */
    @Test
    void shouldReadAbsentFieldsAsNullRatherThanFailing() {
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID)).thenReturn(response(createObjectBuilder()
                .add("finalised", true)
                .build()));

        final Optional<PtphDetail> result = ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming());

        assertTrue(result.isPresent());
        assertNull(result.get().getTier());
        assertNull(result.get().getListType());
        assertNull(result.get().getKeyReason());
    }

    /**
     * `keyReason` is only ever set for a fixed-date list type, and the hearing context sends it
     * as an explicit JSON null for a flexible one — which is distinct from omitting the key.
     */
    @Test
    void shouldReadExplicitJsonNullFieldsAsNull() {
        when(hearingQueryClient.getPtphDetail(SEEDING_HEARING_ID)).thenReturn(response(createObjectBuilder()
                .add("tier", "TIER_4")
                .add("listType", "TYPE_2_FLEXIBLE")
                .addNull("keyReason")
                .add("finalised", true)
                .build()));

        final Optional<PtphDetail> result = ptphDetailService.getFinalisedPtphDetail(SEEDING_HEARING_ID, incoming());

        assertTrue(result.isPresent());
        assertEquals("TIER_4", result.get().getTier());
        assertEquals("TYPE_2_FLEXIBLE", result.get().getListType());
        assertNull(result.get().getKeyReason());
    }
}

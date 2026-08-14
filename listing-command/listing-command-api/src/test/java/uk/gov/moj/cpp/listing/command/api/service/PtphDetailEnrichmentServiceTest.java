package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.PtphDetails;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.HearingTypeFactory;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PtphDetailEnrichmentServiceTest {

    private static final UUID SEEDING_HEARING_ID = randomUUID();
    private static final UUID TRIAL_TYPE_ID = randomUUID();
    private static final UUID PTPH_TYPE_ID = randomUUID();

    @Mock
    private HearingTypeFactory hearingTypeFactory;

    @Mock
    private PtphDetailService ptphDetailService;

    @InjectMocks
    private PtphDetailEnrichmentService ptphDetailEnrichmentService;

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.list-next-hearings-v2").build(),
                createObjectBuilder().build());
    }

    private SeedingHearing seedingHearing() {
        return SeedingHearing.seedingHearing().withSeedingHearingId(SEEDING_HEARING_ID).build();
    }

    private HearingListingNeeds hearingOfType(final UUID typeId) {
        return hearingOfType(typeId, JurisdictionType.CROWN);
    }

    private HearingListingNeeds hearingOfType(final UUID typeId, final JurisdictionType jurisdictionType) {
        return hearingListingNeeds()
                .withId(randomUUID())
                .withJurisdictionType(jurisdictionType)
                .withType(HearingType.hearingType().withId(typeId).withDescription("desc").build())
                .build();
    }

    @Test
    void shouldNotCallHearingContextWhenNoHearingIsATrial() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(hearingOfType(PTPH_TYPE_ID)), seedingHearing(), envelope());

        verifyNoInteractions(ptphDetailService);
        assertNull(result.get(0).getTier());
        assertNull(result.get(0).getListType());
    }

    @Test
    void shouldStampTrialHearingWhenSeedingRecordFinalised() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_3", "TYPE_1_FIXED", "Vulnerable witness")));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertEquals("TIER_3", result.get(0).getTier());
        assertEquals("TYPE_1_FIXED", result.get(0).getListType());
        assertEquals("Vulnerable witness", result.get(0).getKeyReason());
    }

    @Test
    void shouldLeaveHearingUntouchedWhenSeedingRecordNotFinalised() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.empty());

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertNull(result.get(0).getTier());
        assertNull(result.get(0).getListType());
        assertNull(result.get(0).getKeyReason());
    }

    @Test
    void shouldStampOnlyTrialHearingsInAMixedCommand() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_1", "TYPE_2_FLEXIBLE", null)));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                Arrays.asList(hearingOfType(PTPH_TYPE_ID), hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertNull(result.get(0).getTier());
        assertEquals("TIER_1", result.get(1).getTier());
    }

    @Test
    void shouldFetchOncePerCommandForMultipleTrials() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_2", "TYPE_2_FLEXIBLE", null)));

        ptphDetailEnrichmentService.enrichWithPtphDetail(
                Arrays.asList(hearingOfType(TRIAL_TYPE_ID), hearingOfType(TRIAL_TYPE_ID)), seedingHearing(), envelope());

        verify(ptphDetailService).getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class));
    }

    @Test
    void shouldOverwriteAnyInboundValues() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_5", "TYPE_2_FLEXIBLE", null)));

        final HearingListingNeeds spoofed = hearingListingNeeds()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withId(TRIAL_TYPE_ID).withDescription("desc").build())
                .withTier("TIER_1")
                .withListType("TYPE_1_FIXED")
                .withKeyReason("spoofed")
                .build();

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(spoofed), seedingHearing(), envelope());

        assertEquals("TIER_5", result.get(0).getTier());
        assertEquals("TYPE_2_FLEXIBLE", result.get(0).getListType());
        assertNull(result.get(0).getKeyReason());
    }

    @Test
    void shouldReturnHearingsUnchangedWhenNoSeedingHearingId() {
        final List<HearingListingNeeds> hearings = singletonList(hearingOfType(TRIAL_TYPE_ID));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                hearings, SeedingHearing.seedingHearing().build(), envelope());

        assertEquals(hearings, result);
        verifyNoInteractions(ptphDetailService);
        verify(hearingTypeFactory, never()).getTrialHearingTypeIds(any(JsonEnvelope.class));
    }

    private HearingUnscheduledListingNeeds unscheduledHearingOfType(final UUID hearingId, final UUID typeId) {
        return unscheduledHearingOfType(hearingId, typeId, JurisdictionType.CROWN);
    }

    private HearingUnscheduledListingNeeds unscheduledHearingOfType(final UUID hearingId, final UUID typeId,
                                                                    final JurisdictionType jurisdictionType) {
        return HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withId(hearingId)
                .withJurisdictionType(jurisdictionType)
                .withType(HearingType.hearingType().withId(typeId).withDescription("desc").build())
                .build();
    }

    @Test
    void shouldNotCallHearingContextForUnscheduledWhenNoHearingIsATrial() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));

        final List<PtphDetails> result = ptphDetailEnrichmentService.resolvePtphDetails(
                singletonList(unscheduledHearingOfType(randomUUID(), PTPH_TYPE_ID)), seedingHearing(), envelope());

        verifyNoInteractions(ptphDetailService);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnOneEntryForTheTrialHearingWhenSeedingRecordFinalised() {
        final UUID trialHearingId = randomUUID();
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_3", "TYPE_1_FIXED", "Vulnerable witness")));

        final List<PtphDetails> result = ptphDetailEnrichmentService.resolvePtphDetails(
                singletonList(unscheduledHearingOfType(trialHearingId, TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertEquals(1, result.size());
        assertEquals(trialHearingId, result.get(0).getHearingId());
        assertEquals("TIER_3", result.get(0).getTier());
        assertEquals("TYPE_1_FIXED", result.get(0).getListType());
        assertEquals("Vulnerable witness", result.get(0).getKeyReason());
    }

    @Test
    void shouldReturnNoEntriesForUnscheduledWhenSeedingRecordNotFinalised() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.empty());

        final List<PtphDetails> result = ptphDetailEnrichmentService.resolvePtphDetails(
                singletonList(unscheduledHearingOfType(randomUUID(), TRIAL_TYPE_ID)), seedingHearing(), envelope());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEntriesOnlyForTrialHearingsInAMixedUnscheduledCommand() {
        final UUID trialHearingId = randomUUID();
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_1", "TYPE_2_FLEXIBLE", null)));

        final List<PtphDetails> result = ptphDetailEnrichmentService.resolvePtphDetails(
                Arrays.asList(unscheduledHearingOfType(randomUUID(), PTPH_TYPE_ID),
                        unscheduledHearingOfType(trialHearingId, TRIAL_TYPE_ID)),
                seedingHearing(), envelope());

        assertEquals(1, result.size());
        assertEquals(trialHearingId, result.get(0).getHearingId());
        assertEquals("TIER_1", result.get(0).getTier());
    }

    @Test
    void shouldReturnNoEntriesForUnscheduledWhenNoSeedingHearingId() {
        final List<PtphDetails> result = ptphDetailEnrichmentService.resolvePtphDetails(
                singletonList(unscheduledHearingOfType(randomUUID(), TRIAL_TYPE_ID)),
                SeedingHearing.seedingHearing().build(), envelope());

        assertTrue(result.isEmpty());
        verifyNoInteractions(ptphDetailService);
        verify(hearingTypeFactory, never()).getTrialHearingTypeIds(any(JsonEnvelope.class));
    }

    /**
     * Guard cases. A command with nothing to enrich must cost nothing — no reference-data
     * lookup and no hearing-context call — and must hand back exactly what it was given.
     */
    @Test
    void shouldReturnUnchangedWhenThereAreNoHearingsToEnrich() {
        assertNull(ptphDetailEnrichmentService.enrichWithPtphDetail(null, seedingHearing(), envelope()));
        assertTrue(ptphDetailEnrichmentService.enrichWithPtphDetail(emptyList(), seedingHearing(), envelope()).isEmpty());

        verifyNoInteractions(ptphDetailService);
        verify(hearingTypeFactory, never()).getTrialHearingTypeIds(any(JsonEnvelope.class));
    }

    @Test
    void shouldReturnNoEntriesForUnscheduledWhenThereAreNoHearings() {
        assertTrue(ptphDetailEnrichmentService.resolvePtphDetails(null, seedingHearing(), envelope()).isEmpty());
        assertTrue(ptphDetailEnrichmentService.resolvePtphDetails(emptyList(), seedingHearing(), envelope()).isEmpty());

        verifyNoInteractions(ptphDetailService);
        verify(hearingTypeFactory, never()).getTrialHearingTypeIds(any(JsonEnvelope.class));
    }

    /**
     * A hearing listed outside the seeding-hearing flow carries no seeding hearing at all,
     * which is not an error — there is simply nothing to inherit from.
     */
    @Test
    void shouldReturnUnchangedWhenThereIsNoSeedingHearingAtAll() {
        final List<HearingListingNeeds> hearings = singletonList(hearingOfType(TRIAL_TYPE_ID));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                hearings, null, envelope());

        assertEquals(hearings, result);
        verifyNoInteractions(ptphDetailService);
    }

    @Test
    void shouldReturnNoEntriesForUnscheduledWhenThereIsNoSeedingHearingAtAll() {
        final List<PtphDetails> result = ptphDetailEnrichmentService.resolvePtphDetails(
                singletonList(unscheduledHearingOfType(randomUUID(), TRIAL_TYPE_ID)), null, envelope());

        assertTrue(result.isEmpty());
        verifyNoInteractions(ptphDetailService);
    }

    /**
     * The trial gate compares hearing type ids, so a hearing with no type — or a type with no
     * id — must simply not be a trial rather than blowing up mid-command.
     */
    @Test
    void shouldTreatAHearingWithNoTypeAsNotATrial() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));

        final HearingListingNeeds noType = hearingListingNeeds()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(noType), seedingHearing(), envelope());

        assertNull(result.get(0).getTier());
        verifyNoInteractions(ptphDetailService);
    }

    @Test
    void shouldTreatAHearingWhoseTypeHasNoIdAsNotATrial() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));

        final HearingListingNeeds typeWithoutId = hearingListingNeeds()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("no id").build())
                .build();

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(typeWithoutId), seedingHearing(), envelope());

        assertNull(result.get(0).getTier());
        verifyNoInteractions(ptphDetailService);
    }

    /**
     * LPT-2405 — tier and list type are a Crown Court PTPH concept, but reference data flags
     * magistrates trial types with `trialTypeFlag` too. Without the jurisdiction check a
     * magistrates trial would query the hearing context and inherit a tier that no Crown PTPH
     * ever set for it.
     */
    @Test
    void shouldNotCallHearingContextForAMagistratesTrial() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                singletonList(hearingOfType(TRIAL_TYPE_ID, JurisdictionType.MAGISTRATES)), seedingHearing(), envelope());

        verifyNoInteractions(ptphDetailService);
        assertNull(result.get(0).getTier());
        assertNull(result.get(0).getListType());
    }

    @Test
    void shouldStampOnlyTheCrownTrialWhenACommandMixesJurisdictions() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));
        when(ptphDetailService.getFinalisedPtphDetail(eq(SEEDING_HEARING_ID), any(JsonEnvelope.class)))
                .thenReturn(Optional.of(new PtphDetail("TIER_3", "TYPE_1_FIXED", null)));

        final List<HearingListingNeeds> result = ptphDetailEnrichmentService.enrichWithPtphDetail(
                Arrays.asList(hearingOfType(TRIAL_TYPE_ID, JurisdictionType.MAGISTRATES),
                        hearingOfType(TRIAL_TYPE_ID, JurisdictionType.CROWN)),
                seedingHearing(), envelope());

        assertNull(result.get(0).getTier());
        assertEquals("TIER_3", result.get(1).getTier());
    }

    @Test
    void shouldReturnNoEntriesForAMagistratesUnscheduledTrial() {
        when(hearingTypeFactory.getTrialHearingTypeIds(any(JsonEnvelope.class)))
                .thenReturn(Set.of(TRIAL_TYPE_ID.toString()));

        final List<PtphDetails> result = ptphDetailEnrichmentService.resolvePtphDetails(
                singletonList(unscheduledHearingOfType(randomUUID(), TRIAL_TYPE_ID, JurisdictionType.MAGISTRATES)),
                seedingHearing(), envelope());

        verifyNoInteractions(ptphDetailService);
        assertTrue(result.isEmpty());
    }
}

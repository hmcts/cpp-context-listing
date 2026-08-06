package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SplitDetectionServiceTest {

    @InjectMocks
    private SplitDetectionService splitDetectionService;

    @Mock
    private HearingLookupService hearingLookupService;

    private final UUID hearingId = UUID.randomUUID();
    private final UUID offence1 = UUID.randomUUID();
    private final UUID offence2 = UUID.randomUUID();

    @Test
    void shouldFlagSplit_whenRequestOffencesAreStrictSubset_andNoCourtRoom() {
        final UpdateHearingForListing hearing = crownHearing().build();
        when(hearingLookupService.findHearing(any(), any()))
                .thenReturn(Optional.of(storedHearingWithOffences(true, offence1, offence2)));

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1), mock(JsonEnvelope.class));

        // No court room => the carved-out cases go unallocated: matches the pre-existing
        // "unallocated" convention ExtendHearingForHearingListener branches on.
        assertThat(result.getSplitHearing(), is("unallocated"));
    }

    @Test
    void shouldFlagSplit_forMagistrates_ignoringSelectedCourtCentreRoom() {
        // resolveCourtRoomId's selectedCourtCentre branch is CROWN-only: for MAGS the room must
        // come from the command-level field (null here), so this is the no-room arm.
        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withHearingId(hearingId)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre()
                        .withId(UUID.randomUUID())
                        .withCourtRoomId(UUID.randomUUID())
                        .build())
                .build();
        when(hearingLookupService.findHearing(any(), any()))
                .thenReturn(Optional.of(storedHearingWithOffences(true, offence1, offence2)));

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1), mock(JsonEnvelope.class));

        assertThat(result.getSplitHearing(), is("unallocated"));
    }

    @Test
    void shouldFlagSplit_whenStrictSubset_roomPresent_hearingDaysPresent_andStoredAllocated() {
        final UpdateHearingForListing hearing = crownHearing()
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre()
                        .withId(UUID.randomUUID())
                        .withCourtRoomId(UUID.randomUUID())
                        .build())
                .withHearingDays(Collections.singletonList(HearingDay.hearingDay()
                        .withHearingDate(LocalDate.now().plusDays(5))
                        .withDurationMinutes(240)
                        .build()))
                .build();
        when(hearingLookupService.findHearing(any(), any()))
                .thenReturn(Optional.of(storedHearingWithOffences(true, offence1, offence2)));

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1), mock(JsonEnvelope.class));

        assertThat(result.getSplitHearing(), is("allocated"));
    }

    @Test
    void shouldFlagSplit_scheduleOnlyShape_roomPresent_nonDefaultDaysCarrySession_andStoredAllocated() {
        // Court-calendar CROWN split shape: NO hearingDays pre-enrichment; the chosen session
        // rides on nonDefaultDays (courtScheduleId + startTime) and the room comes from the
        // selectedCourtCentre. getOperationType only sees non-empty hearingDays because
        // enrichment seeds them from nonDefaultDays - detection must mirror that pre-enrichment.
        final UpdateHearingForListing hearing = crownHearing()
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre()
                        .withId(UUID.randomUUID())
                        .withCourtRoomId(UUID.randomUUID())
                        .build())
                .withNonDefaultDays(Collections.singletonList(NonDefaultDay.nonDefaultDay()
                        .withCourtScheduleId(UUID.randomUUID().toString())
                        .withStartTime(LocalDate.now().plusDays(5).atStartOfDay(ZoneOffset.UTC))
                        .withDuration(240)
                        .build()))
                .build();
        when(hearingLookupService.findHearing(any(), any()))
                .thenReturn(Optional.of(storedHearingWithOffences(true, offence1, offence2)));

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1), mock(JsonEnvelope.class));

        assertThat(result.getSplitHearing(), is("allocated"));
    }

    @Test
    void shouldNotFlag_whenRequestOffencesEqualStoredOffences() {
        final UpdateHearingForListing hearing = crownHearing().build();
        when(hearingLookupService.findHearing(any(), any()))
                .thenReturn(Optional.of(storedHearingWithOffences(true, offence1, offence2)));

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1, offence2), mock(JsonEnvelope.class));

        assertThat(result.getSplitHearing(), is(nullValue()));
    }

    @Test
    void shouldNotFlag_partialAllocationShape_subsetWithRoomButStoredUnallocated() {
        final UpdateHearingForListing hearing = crownHearing()
                .withCourtRoomId(UUID.randomUUID())
                .withHearingDays(Collections.singletonList(HearingDay.hearingDay()
                        .withHearingDate(LocalDate.now().plusDays(5))
                        .withDurationMinutes(240)
                        .build()))
                .build();
        when(hearingLookupService.findHearing(any(), any()))
                .thenReturn(Optional.of(storedHearingWithOffences(false, offence1, offence2)));

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1), mock(JsonEnvelope.class));

        assertThat(result.getSplitHearing(), is(nullValue()));
    }

    @Test
    void shouldPreserveCallerProvidedSplitHearing_withoutViewstoreLookup() {
        final UpdateHearingForListing hearing = crownHearing()
                .withSplitHearing("unallocated")
                .build();

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1), mock(JsonEnvelope.class));

        assertThat(result.getSplitHearing(), is("unallocated"));
        verify(hearingLookupService, never()).findHearing(any(), any());
    }

    @Test
    void shouldNotFlag_whenStoredHearingNotFound() {
        final UpdateHearingForListing hearing = crownHearing().build();
        when(hearingLookupService.findHearing(any(), any())).thenReturn(Optional.empty());

        final UpdateHearingForListing result = splitDetectionService.flagSplitIfDetected(
                hearing, requestPayloadWithOffences(offence1), mock(JsonEnvelope.class));

        assertThat(result.getSplitHearing(), is(nullValue()));
    }

    private UpdateHearingForListing.Builder crownHearing() {
        return UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingId(hearingId);
    }

    private JsonObject requestPayloadWithOffences(final UUID... offenceIds) {
        final var offencesArray = createArrayBuilder();
        for (final UUID offenceId : offenceIds) {
            offencesArray.add(createObjectBuilder().add("offenceId", offenceId.toString()));
        }
        return createObjectBuilder()
                .add("prosecutionCases", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", UUID.randomUUID().toString())
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("defendantId", UUID.randomUUID().toString())
                                                .add("offences", offencesArray)))))
                .build();
    }

    private JsonObject storedHearingWithOffences(final boolean allocated, final UUID... offenceIds) {
        final var offencesArray = createArrayBuilder();
        for (final UUID offenceId : offenceIds) {
            offencesArray.add(createObjectBuilder().add("id", offenceId.toString()));
        }
        return createObjectBuilder()
                .add("id", hearingId.toString())
                .add("allocated", allocated)
                .add("listedCases", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", UUID.randomUUID().toString())
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("id", UUID.randomUUID().toString())
                                                .add("offences", offencesArray)))))
                .build();
    }
}

package uk.gov.moj.cpp.listing.command.hearing.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.AvailableSlotsForHearingFreed.availableSlotsForHearingFreed;

import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtSchedulerListingEventProcessorTest {

    private static final UUID HEARING_ID = randomUUID();

    @Mock
    private HearingSlotsService hearingSlotsService;

    @InjectMocks
    private CourtSchedulerListingEventProcessor courtSchedulerListingEventProcessor;

    @Test
    void shouldDeleteSlotsInAzureWhenHearingSlotsAvailable() {
        doNothing().when(hearingSlotsService).delete(any(UUID.class));

        courtSchedulerListingEventProcessor.freeAvailableHearingSlots(event());

        verify(hearingSlotsService).delete(HEARING_ID);
    }

    @Test
    void aFailedReleaseIsLoggedAndDoesNotFailTheCommand() {
        doThrow(new IllegalStateException("courtscheduler down")).when(hearingSlotsService).delete(HEARING_ID);

        courtSchedulerListingEventProcessor.freeAvailableHearingSlots(event());

        verify(hearingSlotsService).delete(HEARING_ID);
    }

    private static RaisedEvent<AvailableSlotsForHearingFreed> event() {
        return new RaisedEvent<>(ProcessorTestSupport.metadataWithRandomUUID("listing.events.available-slots-for-hearing-freed").build(),
                availableSlotsForHearingFreed().withHearingId(HEARING_ID).build());
    }
}

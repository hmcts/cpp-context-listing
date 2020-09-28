package uk.gov.moj.cpp.listing.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.events.AvailableSlotsForHearingFreed.availableSlotsForHearingFreed;
import static uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay;

import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AzureListingEventProcessorTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime START_DATE_TIME = now();
    private static final String TEST_OUTPUT = "sample";

    @Mock
    private SlotsToJsonStringConverter slotsToJsonStringConverter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    @InjectMocks
    private AzureListingEventProcessor azureListingEventProcessor;

    @Test
    public void shouldUpdateSlotsInAzureWhenNonDefaultDaysAssigned() {
        final Envelope<NonDefaultDaysAssignedToHearing> envelope = (Envelope<NonDefaultDaysAssignedToHearing>) mock(Envelope.class);

        final NonDefaultDaysAssignedToHearing hearing = NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                .withNonDefaultDays(nonDefaultDays())
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);

        given(slotsToJsonStringConverter.convertNonDefaultDaysToJson(HEARING_ID, hearing.getNonDefaultDays())).willReturn(TEST_OUTPUT);

        azureListingEventProcessor.nonDefaultDaysAssignedForHearing(envelope);

        verify(hearingSlotsService).update(TEST_OUTPUT);
    }

    @Test
    public void shouldUpdateSlotsInAzureWhenNonDefaultDaysChanged() {
        final Envelope<NonDefaultDaysChangedForHearing> envelope = (Envelope<NonDefaultDaysChangedForHearing>) mock(Envelope.class);

        final NonDefaultDaysChangedForHearing hearing = NonDefaultDaysChangedForHearing.nonDefaultDaysChangedForHearing()
                .withNonDefaultDays(nonDefaultDays())
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);
        given(slotsToJsonStringConverter.convertNonDefaultDaysToJson(HEARING_ID, hearing.getNonDefaultDays())).willReturn(TEST_OUTPUT);

        azureListingEventProcessor.nonDefaultDaysChangedForHearing(envelope);

        verify(hearingSlotsService).update(TEST_OUTPUT);
    }

    @Test
    public void shouldDeleteSlotsInAzureWhenHearingSlotsAvailable() {
        final Envelope<AvailableSlotsForHearingFreed> envelope = (Envelope<AvailableSlotsForHearingFreed>) mock(Envelope.class);
        when(hearingSlotsService.delete(any(UUID.class))).thenReturn(response);

        final AvailableSlotsForHearingFreed hearing = availableSlotsForHearingFreed()
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);

        azureListingEventProcessor.freeAvailableHearingSlots(envelope);

        verify(hearingSlotsService).delete(HEARING_ID);
    }

    private List<NonDefaultDay> nonDefaultDays() {

        final NonDefaultDay nonDefaultDay1 = nonDefaultDay()
                .withStartTime(START_DATE_TIME)
                .withDuration(of(1))
                .withCourtRoomId(of(123))
                .withCourtScheduleId(of("224686"))
                .withOucode(of("BA09US"))
                .withSession(of("AD"))
                .build();

        final NonDefaultDay nonDefaultDay2 = nonDefaultDay()
                .withStartTime(START_DATE_TIME)
                .withDuration(of(311))
                .withCourtRoomId(of(34))
                .withCourtScheduleId(of("224686"))
                .withOucode(of("BA09US"))
                .withSession(of("AD"))
                .build();


        return Arrays.asList(nonDefaultDay1, nonDefaultDay2);
    }
}
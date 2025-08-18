package uk.gov.moj.cpp.listing.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.AvailableSlotsForHearingFreed.availableSlotsForHearingFreed;
import static uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay;

import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtSchedulerListingEventProcessorTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime START_DATE_TIME = now();
    private static final JsonArrayBuilder TEST_OUTPUT_ARRAY = createArrayBuilder();
    private final JsonObjectBuilder TEST_OUTPUT = createObjectBuilder().add("hearingSlots", TEST_OUTPUT_ARRAY.build());

    @Mock
    private SlotsToJsonStringConverter slotsToJsonStringConverter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    @InjectMocks
    private CourtSchedulerListingEventProcessor courtSchedulerListingEventProcessor;

    @Test
    public void shouldUpdateSlotsInAzureWhenNonDefaultDaysAssigned() {
        final Envelope<NonDefaultDaysAssignedToHearing> envelope = (Envelope<NonDefaultDaysAssignedToHearing>) mock(Envelope.class);

        final NonDefaultDaysAssignedToHearing hearing = NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                .withNonDefaultDays(nonDefaultDays())
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);

        given(slotsToJsonStringConverter.convertNonDefaultDaysToJson(HEARING_ID, hearing.getNonDefaultDays())).willReturn(TEST_OUTPUT_ARRAY);

        courtSchedulerListingEventProcessor.nonDefaultDaysAssignedForHearing(envelope);

        verify(hearingSlotsService).update(TEST_OUTPUT.build());
    }

    @Test
    public void shouldUpdateSlotsInAzureWhenNonDefaultDaysChanged() {
        final Envelope<NonDefaultDaysChangedForHearing> envelope = (Envelope<NonDefaultDaysChangedForHearing>) mock(Envelope.class);

        final NonDefaultDaysChangedForHearing hearing = NonDefaultDaysChangedForHearing.nonDefaultDaysChangedForHearing()
                .withNonDefaultDays(nonDefaultDays())
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);
        given(slotsToJsonStringConverter.convertNonDefaultDaysToJson(HEARING_ID, hearing.getNonDefaultDays())).willReturn(TEST_OUTPUT_ARRAY);

        courtSchedulerListingEventProcessor.nonDefaultDaysChangedForHearing(envelope);

        verify(hearingSlotsService).update(TEST_OUTPUT.build());
    }

    @Test
    public void shouldDeleteSlotsInAzureWhenHearingSlotsAvailable() {
        final Envelope<AvailableSlotsForHearingFreed> envelope = (Envelope<AvailableSlotsForHearingFreed>) mock(Envelope.class);
        doNothing().when(hearingSlotsService).delete(any(UUID.class));

        final AvailableSlotsForHearingFreed hearing = availableSlotsForHearingFreed()
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);

        courtSchedulerListingEventProcessor.freeAvailableHearingSlots(envelope);

        verify(hearingSlotsService).delete(HEARING_ID);
    }

    private List<NonDefaultDay> nonDefaultDays() {

        final NonDefaultDay nonDefaultDay1 = nonDefaultDay()
                .withStartTime(START_DATE_TIME)
                .withDuration(1)
                .withCourtRoomId(123)
                .withCourtScheduleId("224686")
                .withOucode("BA09US")
                .withSession("AD")
                .build();

        final NonDefaultDay nonDefaultDay2 = nonDefaultDay()
                .withStartTime(START_DATE_TIME)
                .withDuration(311)
                .withCourtRoomId(34)
                .withCourtScheduleId("224686")
                .withOucode("BA09US")
                .withSession("AD")
                .build();


        return Arrays.asList(nonDefaultDay1, nonDefaultDay2);
    }
}
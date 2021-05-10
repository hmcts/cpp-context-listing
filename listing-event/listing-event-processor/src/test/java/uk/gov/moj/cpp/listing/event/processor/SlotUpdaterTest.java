package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter.toJSONString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.builder.SlotDetailBuilder;
import uk.gov.moj.cpp.listing.event.processor.azure.data.SlotDetail;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SlotUpdaterTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final String TYPE = "Sentence";
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID JUDICIAL_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.now();
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String CIRCUIT_JUDGE = "CIRCUIT_JUDGE";
    private static final String TEST_OUTPUT = "sample";
    private String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);
    private final List<uk.gov.justice.listing.events.HearingDay> hearingDays = Arrays.asList(uk.gov.justice.listing.events.HearingDay.hearingDay()
            .withHearingDate(START_DATE).withDurationMinutes(10).build());

    @Mock
    private SlotsToJsonStringConverter slotsToJsonStringConverter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @InjectMocks
    private SlotUpdater slotUpdater;

    @Test
    public void shouldUpdateSlotsInAzureAfterHearingAllocatedForListingMessage() {

        final HearingConfirmed hearingConfirmed = hearingConfirmed(true);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingConfirmed.getConfirmedHearing(), false, hearingDays))
                .willReturn(sampleSlotDetails);

        final Response response = mock(Response.class);
        given(hearingSlotsService.update(slotDetailsPayload)).willReturn(response);

        final String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        slotUpdater.updateSlot(event, hearingConfirmed.getConfirmedHearing(), false, false, hearingDays);

        verify(hearingSlotsService).update(slotDetailsPayload);
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterHearingAllocatedForListingMessageAndIsSlotUpdatedTrue() {

        final HearingConfirmed hearingConfirmed = hearingConfirmed(true);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingConfirmed.getConfirmedHearing(), false, hearingDays))
                .willReturn(Collections.singletonList(SlotDetailBuilder.slotDetail().build()));

        final Response response = mock(Response.class);
        given(hearingSlotsService.update(slotDetailsPayload)).willReturn(response);

        final String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        slotUpdater.updateSlot(event, hearingConfirmed.getConfirmedHearing(), true, false, hearingDays);

        verify(hearingSlotsService, times(0)).update(slotDetailsPayload);
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterHearingAllocatedForListingMessageAndJurisdictionTypeNotMagistrates() {

        final HearingConfirmed hearingConfirmed = hearingConfirmed(false);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingConfirmed.getConfirmedHearing(), false, hearingDays))
                .willReturn(sampleSlotDetails);

        final Response response = mock(Response.class);
        given(hearingSlotsService.update(slotDetailsPayload)).willReturn(response);

        final String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        slotUpdater.updateSlot(event, hearingConfirmed.getConfirmedHearing(), false, false, hearingDays);

        verify(hearingSlotsService, times(0)).update(slotDetailsPayload);
    }

    @Test
    public void shouldUpdateSlotsInAzureAfterAllocatedHearingUpdatedForListingMessage() {

        final HearingUpdated hearingUpdated = hearingUpdated(true);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingUpdated.getUpdatedHearing(), false, hearingDays))
                .willReturn(sampleSlotDetails);

        final Response response = mock(Response.class);
        given(hearingSlotsService.update(slotDetailsPayload)).willReturn(response);

        final String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        slotUpdater.updateSlot(event, hearingUpdated.getUpdatedHearing(), false, false, hearingDays);

        verify(hearingSlotsService).update(slotDetailsPayload);
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterAllocatedHearingUpdatedForListingAndIsSlotUpdatedTrue() {

        final HearingUpdated hearingUpdated = hearingUpdated(true);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingUpdated.getUpdatedHearing(), true, hearingDays))
                .willReturn(sampleSlotDetails);

        final Response response = mock(Response.class);
        given(hearingSlotsService.update(slotDetailsPayload)).willReturn(response);

        final String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        slotUpdater.updateSlot(event, hearingUpdated.getUpdatedHearing(), true, false, hearingDays);

        verify(hearingSlotsService, times(0)).update(slotDetailsPayload);
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterAllocatedHearingUpdatedForListingAndAndJurisdictionTypeNotMagistrates() {

        final HearingUpdated hearingUpdated = hearingUpdated(false);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingUpdated.getUpdatedHearing(), false, hearingDays))
                .willReturn(sampleSlotDetails);

        final Response response = mock(Response.class);
        given(hearingSlotsService.update(slotDetailsPayload)).willReturn(response);

        final String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        slotUpdater.updateSlot(event, hearingUpdated.getUpdatedHearing(), false, false, hearingDays);

        verify(hearingSlotsService, times(0)).update(slotDetailsPayload);
    }


    private JsonEnvelope hearingAllocatedEvent() {

        final JsonObjectBuilder hearingDate = createObjectBuilder()
                .add("startDate", START_DATE.toString())
                .add("startTime", START_TIME.toString());

        final JsonObjectBuilder hearingAllocated = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TYPE)
                .add("estimatedMinutes", ESTIMATED_MINUTES)
                .add("judgeId", JUDICIAL_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), hearingAllocated.build());
    }

    private HearingConfirmed hearingConfirmed(final boolean isMagistrates) {

        return HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildHearing(formattedDateTime, isMagistrates))
                .build();
    }

    private uk.gov.justice.core.courts.ConfirmedHearing buildHearing(final String formattedDateTime, final boolean isMagistrates) {
        return uk.gov.justice.core.courts.ConfirmedHearing.confirmedHearing()
                .withId(HEARING_ID)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                        .withListedDurationMinutes(0)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(of(COURT_ROOM_ID))
                        .build())
                .withHearingLanguage(of(HearingLanguage.WELSH))
                .withCourtApplicationIds(Arrays.asList(UUID.randomUUID()))
                .withJurisdictionType(isMagistrates ? MAGISTRATES : JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription(TYPE).withId(UUID.randomUUID()).build())
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withJudicialId(JUDICIAL_ID)
                        .withJudicialRoleType(
                                uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(CIRCUIT_JUDGE)
                                        .withJudicialRoleTypeId(empty())
                                        .build())
                        .build()))
                .withProsecutionCases(Arrays.asList(uk.gov.justice.core.courts.ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(Arrays.asList(uk.gov.justice.core.courts.ConfirmedDefendant.confirmedDefendant()
                                .withId(DEFENDANT_ID)
                                .withOffences(Arrays.asList(uk.gov.justice.core.courts.ConfirmedOffence.confirmedOffence().withId(OFFENCE_ID).build()))
                                .build()))
                        .build()))
                .build();
    }

    private HearingUpdated hearingUpdated(final boolean isMagistrates) {

        return HearingUpdated.hearingUpdated()
                .withUpdatedHearing(buildHearing(formattedDateTime, isMagistrates))
                .build();
    }
}
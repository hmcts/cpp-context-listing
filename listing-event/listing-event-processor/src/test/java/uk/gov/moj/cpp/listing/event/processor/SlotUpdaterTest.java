package uk.gov.moj.cpp.listing.event.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter.toJSONString;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.builder.SlotDetailBuilder;
import uk.gov.moj.cpp.listing.event.processor.azure.data.SlotDetail;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail()
                .withDuration(60).withHearingStartTime(new Date().toString())
                .withSessionDate(LocalDate.now().toString())
                .withCourtScheduleId("CourtSchedule-1")
                .withSession("AM")
                .withCourtRoomId(2330)
                .withOuCode("B01LY00")
                .withHearingId(hearingConfirmed.getConfirmedHearing().getId().toString())
                .build());

        final JsonObject slotDetailsPayload = createObjectBuilder()
                .add("hearingSlots", SlotsToJsonStringConverter.buildJsonArrayBuilder(sampleSlotDetails).build())
                .build();

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingConfirmed.getConfirmedHearing(), false, hearingDays))
                .willReturn(sampleSlotDetails);

        JsonObjectBuilder slotUpdateRespJsonObjBuilder = createObjectBuilder();
        JsonArrayBuilder schedulesArrJsonBuilder = createArrayBuilder();
        slotUpdateRespJsonObjBuilder.add("status", "success");
        sampleSlotDetails.forEach(slotDetail -> {
            JsonObjectBuilder hearingDayScheduleJsonObjBuilder = createObjectBuilder();
            hearingDayScheduleJsonObjBuilder.add("hearingDate", slotDetail.getSessionDate());
            hearingDayScheduleJsonObjBuilder.add("courtScheduleId", slotDetail.getCourtScheduleId());
            schedulesArrJsonBuilder.add(hearingDayScheduleJsonObjBuilder.build());
        });
        slotUpdateRespJsonObjBuilder.add("schedules", schedulesArrJsonBuilder.build());
        when(hearingSlotsService.update(slotDetailsPayload)).thenReturn(slotUpdateRespJsonObjBuilder.build());

        Optional<List<SlotDetail>> slotDetails = slotUpdater.updateSlot(event, hearingConfirmed.getConfirmedHearing(), false, false, hearingDays);

        verify(hearingSlotsService).update(slotDetailsPayload);
        assertEquals(1, slotDetails.get().size());
        assertEquals(LocalDate.now().toString(), slotDetails.get().get(0).getSessionDate());
        assertEquals("CourtSchedule-1", slotDetails.get().get(0).getCourtScheduleId());
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterHearingAllocatedForListingMessageAndIsSlotUpdatedTrue() {

        final HearingConfirmed hearingConfirmed = hearingConfirmed(true);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        slotUpdater.updateSlot(event, hearingConfirmed.getConfirmedHearing(), true, false, hearingDays);

        verify(hearingSlotsService, times(0)).update(slotDetailsPayload);
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterHearingAllocatedForListingMessageAndJurisdictionTypeNotMagistrates() {

        final HearingConfirmed hearingConfirmed = hearingConfirmed(false);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        slotUpdater.updateSlot(event, hearingConfirmed.getConfirmedHearing(), false, false, hearingDays);

        verify(hearingSlotsService, times(0)).update(slotDetailsPayload);
    }

    @Test
    public void shouldUpdateSlotsInAzureAfterAllocatedHearingUpdatedForListingMessage() {

        final HearingUpdated hearingUpdated = hearingUpdated(true);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail()
                .withDuration(60).withHearingStartTime(new Date().toString())
                .withSessionDate(LocalDate.now().toString())
                .withCourtScheduleId("CourtSchedule-1")
                .withSession("AM")
                .withCourtRoomId(2330)
                .withOuCode("B01LY00")
                .withHearingId(hearingUpdated.getUpdatedHearing().getId().toString())
                .build());

        final JsonObject slotDetailsPayload = createObjectBuilder()
                .add("hearingSlots", SlotsToJsonStringConverter.buildJsonArrayBuilder(sampleSlotDetails).build())
                .build();

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingUpdated.getUpdatedHearing(), false, hearingDays))
                .willReturn(sampleSlotDetails);

        JsonObjectBuilder slotUpdateRespJsonObjBuilder = createObjectBuilder();
        JsonArrayBuilder schedulesArrJsonBuilder = createArrayBuilder();
        slotUpdateRespJsonObjBuilder.add("status", "success");
        sampleSlotDetails.forEach(slotDetail -> {
            JsonObjectBuilder hearingDayScheduleJsonObjBuilder = createObjectBuilder();
            hearingDayScheduleJsonObjBuilder.add("hearingDate", slotDetail.getSessionDate());
            hearingDayScheduleJsonObjBuilder.add("courtScheduleId", slotDetail.getCourtScheduleId());
            schedulesArrJsonBuilder.add(hearingDayScheduleJsonObjBuilder.build());
        });
        slotUpdateRespJsonObjBuilder.add("schedules", schedulesArrJsonBuilder.build());
        when(hearingSlotsService.update(slotDetailsPayload)).thenReturn(slotUpdateRespJsonObjBuilder.build());

        slotUpdater.updateSlot(event, hearingUpdated.getUpdatedHearing(), false, false, hearingDays);

        verify(hearingSlotsService).update(slotDetailsPayload);
        assertEquals(1, sampleSlotDetails.size());
        assertEquals(LocalDate.now().toString(), sampleSlotDetails.get(0).getSessionDate());
        assertEquals("CourtSchedule-1", sampleSlotDetails.get(0).getCourtScheduleId());
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterAllocatedHearingUpdatedForListingAndIsSlotUpdatedTrue() {

        final HearingUpdated hearingUpdated = hearingUpdated(true);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

        slotUpdater.updateSlot(event, hearingUpdated.getUpdatedHearing(), true, false, hearingDays);

        verify(hearingSlotsService, times(0)).update(slotDetailsPayload);
    }

    @Test
    public void shouldNotUpdateSlotsInAzureAfterAllocatedHearingUpdatedForListingAndAndJurisdictionTypeNotMagistrates() {

        final HearingUpdated hearingUpdated = hearingUpdated(false);

        final JsonEnvelope event = hearingAllocatedEvent();

        final List<SlotDetail> sampleSlotDetails = Collections.singletonList(SlotDetailBuilder.slotDetail().build());
        final String slotDetailsPayload = toJSONString(sampleSlotDetails);

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
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtApplicationIds(Arrays.asList(UUID.randomUUID()))
                .withJurisdictionType(isMagistrates ? MAGISTRATES : JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription(TYPE).withId(UUID.randomUUID()).build())
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withJudicialId(JUDICIAL_ID)
                        .withJudicialRoleType(
                                uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(CIRCUIT_JUDGE)
                                        .withJudicialRoleTypeId(null)
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
package uk.gov.moj.cpp.listing.event.listener;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.events.HearingDay.hearingDay;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.CrownHearingMigratedToCourtschedule;
import uk.gov.justice.listing.events.HearingDayCourtSchedule;
import uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingDaysUpdateEventListenerTest {

    private static final LocalDate NOW_DATE = LocalDate.now();
    private final ZonedDateTime NOW_DATE_TIME = new UtcClock().now();
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Mock
    Hearing hearing;

    @Mock
    ObjectNode properties;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Captor
    private ArgumentCaptor<JsonNode> hearingDaysCaptor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    private HearingDaysUpdateEventListener hearingDaysUpdateEventListener;

    @Test
    public void testHearingDaysWithoutCourtCentreCorrected() throws JsonProcessingException {
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = hearingDays(true, null);

        HearingDaysWithoutCourtCentreCorrected corrected = HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(hearingDays.stream().map(hearingDay -> hearingDay().withValuesFrom(hearingDay).withCourtRoomId(courtRoomId).withCourtCentreId(courtCentreId).build()).collect(toList()))
                .withId(hearingId)
                .build();
        final Envelope<HearingDaysWithoutCourtCentreCorrected> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-days-without-court-centre-corrected"),
                corrected);
        final NonDefaultDay nonDefaultDay = new NonDefaultDay(randomUUID().toString(), 1, randomUUID().toString(),
                1, "oucode", "roomId", "session", ZonedDateTime.now());
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);
        final uk.gov.justice.listing.events.Hearing dbHearingPayload = uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId)
                .withNonDefaultDays(nonDefaultDays).withHearingDays(hearingDays).build();
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        final JsonNode t = objectMapper.valueToTree(dbHearingPayload);
        when(hearing.getProperties()).thenReturn(t);
        hearingDaysUpdateEventListener.hearingDaysWithoutCourtCentreCorrected(listenerEnvelope);
        verify(hearing).setProperties(hearingDaysCaptor.capture());

        final JsonNode properties = hearingDaysCaptor.getValue();
        assertThat(properties.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(1)),
                withJsonPath("$.nonDefaultDays", hasSize(1)),
                withJsonPath("$.hearingDays[0].hearingDate", equalTo(LocalDates.to(NOW_DATE))),
                withJsonPath("$.hearingDays[0].startTime", equalTo(ISO_8601_FORMATTER.format(NOW_DATE_TIME))),
                withJsonPath("$.hearingDays[0].isCancelled", equalTo(false))
        )));
    }

    @Test
    public void testUpdateHearingDayCourtSchedule() throws JsonProcessingException {
        UUID hearingId = randomUUID();
        UUID courtScheduleId = randomUUID();
        LocalDate hearingDate1 = LocalDate.now();
        LocalDate hearingDate2 = LocalDate.now().plusDays(1);

        List<HearingDayCourtSchedule> hearingDayCourtSchedules =
                of(new HearingDayCourtSchedule(courtScheduleId, hearingDate1),
                   new HearingDayCourtSchedule(courtScheduleId, hearingDate2));
        HearingDayCourtScheduleUpdated event =
                new HearingDayCourtScheduleUpdated(hearingDayCourtSchedules, hearingId);
        Envelope<HearingDayCourtScheduleUpdated> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-day-court-schedule-updated"),
                event);

        List<HearingDay> hearingDays =
                of(HearingDay.hearingDay().withHearingDate(hearingDate1).withCourtScheduleId(courtScheduleId).build(),
                   HearingDay.hearingDay().withHearingDate(hearingDate2).withCourtScheduleId(courtScheduleId).build());
        Set<HearingDays> dbHearingDays = Set.of(HearingDays.builder().withHearingDate(hearingDate1).build(),
                                           HearingDays.builder().withHearingDate(hearingDate2).build());
        uk.gov.justice.listing.events.Hearing dbHearingPayload =
                uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId).withHearingDays(hearingDays).build();

        Hearing hearing = Hearing.builder()
                .withId(hearingId)
                .withProperties(objectMapper.valueToTree(dbHearingPayload))
                .withHearingDays(dbHearingDays)
                .build();
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        hearingDaysUpdateEventListener.hearingDayCourtScheduleUpdated(listenerEnvelope);

        verify(hearingRepository, times(1)).save(hearing);
        verify(hearingSearchSyncService, times(1)).sync(hearingId);
    }

    @Test
    public void testCrownHearingMigratedToCourtSchedule() throws JsonProcessingException {
        UUID hearingId = randomUUID();
        UUID courtScheduleId = randomUUID();
        LocalDate hearingDate1 = LocalDate.now();
        LocalDate hearingDate2 = LocalDate.now().plusDays(1);

        List<HearingDayCourtSchedule> hearingDayCourtSchedules =
                of(new HearingDayCourtSchedule(courtScheduleId, hearingDate1),
                   new HearingDayCourtSchedule(courtScheduleId, hearingDate2));
        CrownHearingMigratedToCourtschedule event =
                CrownHearingMigratedToCourtschedule.crownHearingMigratedToCourtschedule()
                        .withHearingId(hearingId)
                        .withHearingDayCourtSchedules(hearingDayCourtSchedules)
                        .build();
        Envelope<CrownHearingMigratedToCourtschedule> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.crown-hearing-migrated-to-courtschedule"),
                event);

        List<HearingDay> hearingDays =
                of(HearingDay.hearingDay().withHearingDate(hearingDate1).withCourtScheduleId(courtScheduleId).build(),
                   HearingDay.hearingDay().withHearingDate(hearingDate2).withCourtScheduleId(courtScheduleId).build());
        Set<HearingDays> dbHearingDays = Set.of(HearingDays.builder().withHearingDate(hearingDate1).build(),
                                           HearingDays.builder().withHearingDate(hearingDate2).build());
        uk.gov.justice.listing.events.Hearing dbHearingPayload =
                uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId).withHearingDays(hearingDays).build();

        Hearing hearing = Hearing.builder()
                .withId(hearingId)
                .withProperties(objectMapper.valueToTree(dbHearingPayload))
                .withHearingDays(dbHearingDays)
                .build();
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        hearingDaysUpdateEventListener.crownHearingMigratedToCourtSchedule(listenerEnvelope);

        verify(hearingRepository, times(1)).save(hearing);
        verify(hearingSearchSyncService, times(1)).sync(hearingId);
    }

    @Test
    public void testHearingDaysWithoutCourtCentreCorrectedWithNullCourtRoomId() throws JsonProcessingException {
        // Given: courtRoomId is null, which was causing the NullPointerException
        final UUID courtCentreId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = hearingDays(true, null);
        
        // Create hearing days with null courtRoomId
        HearingDaysWithoutCourtCentreCorrected corrected = HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(hearingDays.stream().map(hearingDay -> hearingDay().withValuesFrom(hearingDay)
                        .withCourtRoomId(null)  // null courtRoomId
                        .withCourtCentreId(courtCentreId).build()).collect(toList()))
                .withId(hearingId)
                .build();
        final Envelope<HearingDaysWithoutCourtCentreCorrected> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-days-without-court-centre-corrected"),
                corrected);
        
        // Create NonDefaultDay without roomId and courtCentreId to trigger the correction logic
        final NonDefaultDay nonDefaultDay = NonDefaultDay.nonDefaultDay()
                .withRoomId(null)
                .withCourtCentreId(null)
                .withStartTime(ZonedDateTime.now())
                .withDuration(30)
                .withSession("MORNING")
                .build();
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);
        final uk.gov.justice.listing.events.Hearing dbHearingPayload = uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId)
                .withNonDefaultDays(nonDefaultDays).withHearingDays(hearingDays).build();
        
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        final JsonNode t = objectMapper.valueToTree(dbHearingPayload);
        when(hearing.getProperties()).thenReturn(t);
        
        // When: Should not throw NullPointerException
        hearingDaysUpdateEventListener.hearingDaysWithoutCourtCentreCorrected(listenerEnvelope);
        
        // Then: Verify the method completed successfully and properties were set
        verify(hearing).setProperties(hearingDaysCaptor.capture());
        final JsonNode properties = hearingDaysCaptor.getValue();
        assertThat(properties.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(1)),
                withJsonPath("$.nonDefaultDays", hasSize(1)),
                // roomId should not be present since courtRoomId was null
                withoutJsonPath("$.nonDefaultDays[0].roomId"),
                // courtCentreId should be set from the event
                withJsonPath("$.nonDefaultDays[0].courtCentreId", is(courtCentreId.toString()))
        )));
    }

    @Test
    public void testHearingDaysWithoutCourtCentreCorrectedWithNullCourtCentreId() throws JsonProcessingException {
        // Given: courtCentreId is null
        final UUID courtRoomId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = hearingDays(true, null);
        
        HearingDaysWithoutCourtCentreCorrected corrected = HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(hearingDays.stream().map(hearingDay -> hearingDay().withValuesFrom(hearingDay)
                        .withCourtRoomId(courtRoomId)
                        .withCourtCentreId(null)  // null courtCentreId
                        .build()).collect(toList()))
                .withId(hearingId)
                .build();
        final Envelope<HearingDaysWithoutCourtCentreCorrected> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-days-without-court-centre-corrected"),
                corrected);
        
        final NonDefaultDay nonDefaultDay = NonDefaultDay.nonDefaultDay()
                .withRoomId(null)
                .withCourtCentreId(null)
                .withStartTime(ZonedDateTime.now())
                .withDuration(30)
                .withSession("MORNING")
                .build();
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);
        final uk.gov.justice.listing.events.Hearing dbHearingPayload = uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId)
                .withNonDefaultDays(nonDefaultDays).withHearingDays(hearingDays).build();
        
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        final JsonNode t = objectMapper.valueToTree(dbHearingPayload);
        when(hearing.getProperties()).thenReturn(t);
        
        // When: Should not throw NullPointerException
        hearingDaysUpdateEventListener.hearingDaysWithoutCourtCentreCorrected(listenerEnvelope);
        
        // Then: Verify the method completed successfully
        verify(hearing).setProperties(hearingDaysCaptor.capture());
        final JsonNode properties = hearingDaysCaptor.getValue();
        assertThat(properties.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(1)),
                withJsonPath("$.nonDefaultDays", hasSize(1)),
                // roomId should be set from courtRoomId
                withJsonPath("$.nonDefaultDays[0].roomId", is(courtRoomId.toString())),
                // courtCentreId should not be present since it was null in the event
                withoutJsonPath("$.nonDefaultDays[0].courtCentreId")
        )));
    }

    @Test
    public void testHearingDaysWithoutCourtCentreCorrectedWithBothNull() throws JsonProcessingException {
        // Given: Both courtRoomId and courtCentreId are null
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = hearingDays(true, null);
        
        HearingDaysWithoutCourtCentreCorrected corrected = HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(hearingDays.stream().map(hearingDay -> hearingDay().withValuesFrom(hearingDay)
                        .withCourtRoomId(null)  // null courtRoomId
                        .withCourtCentreId(null)  // null courtCentreId
                        .build()).collect(toList()))
                .withId(hearingId)
                .build();
        final Envelope<HearingDaysWithoutCourtCentreCorrected> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-days-without-court-centre-corrected"),
                corrected);
        
        final NonDefaultDay nonDefaultDay = NonDefaultDay.nonDefaultDay()
                .withRoomId(null)
                .withCourtCentreId(null)
                .withStartTime(ZonedDateTime.now())
                .withDuration(30)
                .withSession("MORNING")
                .build();
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);
        final uk.gov.justice.listing.events.Hearing dbHearingPayload = uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId)
                .withNonDefaultDays(nonDefaultDays).withHearingDays(hearingDays).build();
        
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        final JsonNode t = objectMapper.valueToTree(dbHearingPayload);
        when(hearing.getProperties()).thenReturn(t);
        
        // When: Should not throw NullPointerException
        hearingDaysUpdateEventListener.hearingDaysWithoutCourtCentreCorrected(listenerEnvelope);
        
        // Then: Verify the method completed successfully with both values as null
        verify(hearing).setProperties(hearingDaysCaptor.capture());
        final JsonNode properties = hearingDaysCaptor.getValue();
        assertThat(properties.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(1)),
                withJsonPath("$.nonDefaultDays", hasSize(1)),
                // Both roomId and courtCentreId should not be present since both were null
                withoutJsonPath("$.nonDefaultDays[0].roomId"),
                withoutJsonPath("$.nonDefaultDays[0].courtCentreId")
        )));
    }

    @Test
    public void testHearingDaysWithoutCourtCentreCorrectedWithExistingNonDefaultDayValues() throws JsonProcessingException {
        // Given: NonDefaultDay already has roomId and courtCentreId, they should be preserved
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final UUID existingRoomId = randomUUID();
        final String existingCourtCentreId = randomUUID().toString();
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = hearingDays(true, null);
        
        HearingDaysWithoutCourtCentreCorrected corrected = HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(hearingDays.stream().map(hearingDay -> hearingDay().withValuesFrom(hearingDay)
                        .withCourtRoomId(courtRoomId)
                        .withCourtCentreId(courtCentreId).build()).collect(toList()))
                .withId(hearingId)
                .build();
        final Envelope<HearingDaysWithoutCourtCentreCorrected> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-days-without-court-centre-corrected"),
                corrected);
        
        // NonDefaultDay already has values, they should not be overwritten
        final NonDefaultDay nonDefaultDay = NonDefaultDay.nonDefaultDay()
                .withRoomId(existingRoomId.toString())
                .withCourtCentreId(existingCourtCentreId)
                .withStartTime(ZonedDateTime.now())
                .withDuration(30)
                .withSession("MORNING")
                .build();
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);
        final uk.gov.justice.listing.events.Hearing dbHearingPayload = uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId)
                .withNonDefaultDays(nonDefaultDays).withHearingDays(hearingDays).build();
        
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        final JsonNode t = objectMapper.valueToTree(dbHearingPayload);
        when(hearing.getProperties()).thenReturn(t);
        
        // When
        hearingDaysUpdateEventListener.hearingDaysWithoutCourtCentreCorrected(listenerEnvelope);
        
        // Then: Existing values should be preserved
        verify(hearing).setProperties(hearingDaysCaptor.capture());
        final JsonNode properties = hearingDaysCaptor.getValue();
        assertThat(properties.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(1)),
                withJsonPath("$.nonDefaultDays", hasSize(1)),
                withJsonPath("$.nonDefaultDays[0].roomId", is(existingRoomId.toString())),
                withJsonPath("$.nonDefaultDays[0].courtCentreId", is(existingCourtCentreId))
        )));
    }

    private List<HearingDay> hearingDays(boolean isAnyCancelled, UUID courtScheduleId) {
        return Stream.of(
                hearingDay()
                        .withHearingDate(NOW_DATE)
                        .withCourtScheduleId(courtScheduleId)
                        .withIsCancelled(false)
                        .withStartTime(NOW_DATE_TIME)
                        .withEndTime(NOW_DATE_TIME.plusMinutes(30))
                        .build(),
                hearingDay()
                        .withHearingDate(NOW_DATE.plusDays(1))
                        .withCourtScheduleId(courtScheduleId)
                        .withIsCancelled(isAnyCancelled)
                        .withStartTime(NOW_DATE_TIME.plusDays(1))
                        .withEndTime(NOW_DATE_TIME.plusMinutes(30).plusDays(1))
                        .build()).collect(toList());
    }
}
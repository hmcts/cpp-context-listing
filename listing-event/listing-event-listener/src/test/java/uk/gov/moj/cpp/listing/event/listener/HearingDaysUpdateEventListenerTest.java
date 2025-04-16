package uk.gov.moj.cpp.listing.event.listener;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.events.HearingDay.hearingDay;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.listing.events.HearingDay;
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
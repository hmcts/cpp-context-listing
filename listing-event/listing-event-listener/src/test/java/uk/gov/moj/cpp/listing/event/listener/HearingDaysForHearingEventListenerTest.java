package uk.gov.moj.cpp.listing.event.listener;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.HearingDay.hearingDay;
import static uk.gov.justice.listing.events.HearingDaysCancelled.hearingDaysCancelled;
import static uk.gov.justice.listing.events.HearingDaysSequenced.hearingDaysSequenced;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysCancelled;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingDaysForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime START_TIME = ZonedDateTime.now(ZoneId.of("UTC"));
    private static final String HEARING_DAYS = "hearingDays";
    private static final String TEST_JSON = "{ \"" + HEARING_DAYS + "\": {\"test\": \"test\"} }";
    private static final int DURATION_MINUTES = 15;
    private static final ZonedDateTime END_TIME = START_TIME.plusMinutes(DURATION_MINUTES);
    private static final int SEQUENCE_1 = 1;
    private static final int SEQUENCE_2 = 2;
    private static final Integer SEQUENCE_3 = 3;
    private static final String DATE_1 = "2018-11-10";
    private static final String DATE_2 = "2018-11-11";
    private static final String DATE_3 = "2018-11-12";

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @InjectMocks
    private HearingDaysForHearingEventListener hearingDaysForHearingEventListener;

    @Mock
    private Hearing hearing;

    @Captor
    private ArgumentCaptor<JsonNode> propertiesCaptor;

    @Before
    public void setup() throws IOException {
        final ObjectNode properties = (ObjectNode) mapper.readTree(TEST_JSON);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
    }

    @Test
    public void shouldUpdateHearingDaysWhenHearingDaysChangedForHearing() {
        //given
        final Envelope<HearingDaysChangedForHearing> envelope = mock(Envelope.class);
        final HearingDaysChangedForHearing hearingData = HearingDaysChangedForHearing.hearingDaysChangedForHearing()
                .withHearingDays(ImmutableList.of(hearingDay()
                        .withStartTime(START_TIME)
                        .withDurationMinutes(DURATION_MINUTES)
                        .withEndTime(END_TIME)
                        .withSequence(1)
                        .build()))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        //when
        hearingDaysForHearingEventListener.hearingDaysChangedForHearing(envelope);

        //then
        verify(hearingRepository).save(hearing);
        verify(hearing).setProperties(propertiesCaptor.capture());
        final JsonNode actualValue = propertiesCaptor.getValue();
        assertThat(actualValue.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(1)),
                withoutJsonPath("$.hearingDays[0].hearingDate"),
                withJsonPath("$.hearingDays[0].startTime", equalTo(ZonedDateTimes.toString(hearingData.getHearingDays().get(0).getStartTime()))),
                withJsonPath("$.hearingDays[0].durationMinutes", equalTo(hearingData.getHearingDays().get(0).getDurationMinutes())),
                withJsonPath("$.hearingDays[0].endTime", equalTo(ZonedDateTimes.toString(hearingData.getHearingDays().get(0).getEndTime()))),
                withJsonPath("$.hearingDays[0].sequence", equalTo(hearingData.getHearingDays().get(0).getSequence()))
        )));
    }

    @Test
    public void shouldUpdateHearingDaysWhenHearingDaysSequenced() {
        //given
        final Envelope<HearingDaysSequenced> envelope = mock(Envelope.class);
        final HearingDaysSequenced hearingData = hearingDaysSequenced()
                .withHearingId(HEARING_ID)
                .withHearingDays(getHearingDays())
                .build();
        given(envelope.payload()).willReturn(hearingData);

        //when
        hearingDaysForHearingEventListener.hearingDaysSequenced(envelope);

        //then
        verify(hearingRepository).save(hearing);
        verify(hearing).setProperties(propertiesCaptor.capture());
        final JsonNode actualValue = propertiesCaptor.getValue();
        assertThat(actualValue.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(2)),
                withJsonPath("$.hearingDays[0].hearingDate", equalTo(LocalDates.to(hearingData.getHearingDays().get(0).getHearingDate()))),
                withJsonPath("$.hearingDays[0].sequence", equalTo(hearingData.getHearingDays().get(0).getSequence())),
                withJsonPath("$.hearingDays[1].hearingDate", equalTo(LocalDates.to(hearingData.getHearingDays().get(1).getHearingDate()))),
                withJsonPath("$.hearingDays[1].sequence", equalTo(hearingData.getHearingDays().get(1).getSequence()))
        )));
    }

    @Test
    public void shouldFilterCancelledHearingDaysAndUpdateOnlyNotCancelledDaysWhenHearingDaysSequenced() {
        //given
        final Envelope<HearingDaysSequenced> envelope = mock(Envelope.class);
        final HearingDaysSequenced hearingData = hearingDaysSequenced()
                .withHearingId(HEARING_ID)
                .withHearingDays(getHearingDaysWithCancelledDays())
                .build();
        given(envelope.payload()).willReturn(hearingData);

        //when
        hearingDaysForHearingEventListener.hearingDaysSequenced(envelope);

        //then
        verify(hearingRepository).save(hearing);
        verify(hearing).setProperties(propertiesCaptor.capture());
        final JsonNode actualValue = propertiesCaptor.getValue();
        assertThat(actualValue.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(2)),
                withJsonPath("$.hearingDays[0].hearingDate", equalTo(LocalDates.to(hearingData.getHearingDays().get(0).getHearingDate()))),
                withJsonPath("$.hearingDays[0].sequence", equalTo(hearingData.getHearingDays().get(0).getSequence())),
                withJsonPath("$.hearingDays[1].hearingDate", equalTo(LocalDates.to(hearingData.getHearingDays().get(1).getHearingDate()))),
                withJsonPath("$.hearingDays[1].sequence", equalTo(hearingData.getHearingDays().get(1).getSequence()))
        )));
    }

    @Test
    public void shouldFilterCancelledHearingDaysAndUpdateOnlyNotCancelledDaysWhenHearingDaysCancelled() {
        //given
        final Envelope<HearingDaysCancelled> envelope = mock(Envelope.class);
        final HearingDaysCancelled hearingData = hearingDaysCancelled()
                .withHearingId(HEARING_ID)
                .withHearingDays(getHearingDaysWithCancelledDays())
                .build();
        given(envelope.payload()).willReturn(hearingData);

        //when
        hearingDaysForHearingEventListener.hearingDaysCancelled(envelope);

        //then
        verify(hearingRepository).save(hearing);
        verify(hearing).setProperties(propertiesCaptor.capture());
        final JsonNode actualValue = propertiesCaptor.getValue();
        assertThat(actualValue.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(2)),
                withJsonPath("$.hearingDays[0].hearingDate", equalTo(LocalDates.to(hearingData.getHearingDays().get(0).getHearingDate()))),
                withJsonPath("$.hearingDays[0].sequence", equalTo(hearingData.getHearingDays().get(0).getSequence())),
                withJsonPath("$.hearingDays[1].hearingDate", equalTo(LocalDates.to(hearingData.getHearingDays().get(1).getHearingDate()))),
                withJsonPath("$.hearingDays[1].sequence", equalTo(hearingData.getHearingDays().get(1).getSequence()))
        )));
    }

    private List<HearingDay> getHearingDays() {
        final HearingDay hearingDay1 = hearingDay()
                .withSequence(SEQUENCE_1)
                .withHearingDate(LocalDate.parse(DATE_1))
                .build();

        final HearingDay hearingDay2 = hearingDay()
                .withSequence(SEQUENCE_2)
                .withHearingDate(LocalDate.parse(DATE_2))
                .build();

        return asList(hearingDay1, hearingDay2);
    }

    private List<HearingDay> getHearingDaysWithCancelledDays() {
        final HearingDay hearingDay1 = hearingDay()
                .withSequence(SEQUENCE_1)
                .withHearingDate(LocalDate.parse(DATE_1))
                .build();

        final HearingDay hearingDay2 = hearingDay()
                .withSequence(SEQUENCE_2)
                .withHearingDate(LocalDate.parse(DATE_2))
                .build();

        final HearingDay hearingDay3 = hearingDay()
                .withSequence(SEQUENCE_3)
                .withHearingDate(LocalDate.parse(DATE_3))
                .withIsCancelled(true)
                .build();

        return asList(hearingDay1, hearingDay2, hearingDay3);
    }
}
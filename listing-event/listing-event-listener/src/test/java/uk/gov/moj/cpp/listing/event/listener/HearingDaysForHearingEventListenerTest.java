package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class HearingDaysForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime START_TIME = ZonedDateTime.now();
    private static final String HEARING_DAYS = "hearingDays";
    private static final String TEST_JSON = "{ \"" + HEARING_DAYS + "\": {\"test\": \"test\"} }";
    private static final int DURATION_MINUTES = 15;
    private static final ZonedDateTime END_TIME = START_TIME.plusMinutes(DURATION_MINUTES);
    public static final int SEQUENCE_1 = 1;
    public static final int SEQUENCE_2 = 2;
    public static final String DATE_1 = "2018-11-10";
    public static final String DATE_2 = "2018-11-11";


    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode properties;

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HearingDaysForHearingEventListener hearingDaysForHearingEventListener;

    @Mock
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    Hearing hearing;


    @Before
    public  void setup() throws IOException {
        properties = (ObjectNode) mapper.readTree(TEST_JSON);
    }


    @Test
    public void hearingDaysChangedForHearing() {

        //given
        Envelope<HearingDaysChangedForHearing> envelope = (Envelope<HearingDaysChangedForHearing>) mock(Envelope.class);
        HearingDaysChangedForHearing hearingData = HearingDaysChangedForHearing.hearingDaysChangedForHearing()
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withStartTime(START_TIME)
                        .withDurationMinutes(DURATION_MINUTES)
                        .withEndTime(END_TIME)
                        .withSequence(1)
                        .build()))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(objectToJsonObjectConverter.convert(any(List.class))).willReturn(createTestJsonObject());

        //when
        hearingDaysForHearingEventListener.hearingDaysChangedForHearing(envelope);

        //then
        verify(hearingRepository).save(hearing);

    }

    private JsonObject createTestJsonObject() {
        try (final JsonReader jsonReader =  Json.createReader(new StringReader("{\"test\": \"test\"}"))) {
            return jsonReader.readObject();
        }
    }

    private List<HearingDay> getHearingDays() {

        HearingDay hearingDay_1 = HearingDay.hearingDay()
                .withSequence(SEQUENCE_1)
                .withHearingDate(LocalDate.parse(DATE_1))
                .build();

        HearingDay hearingDay_2 = HearingDay.hearingDay()
                .withSequence(SEQUENCE_2)
                .withHearingDate(LocalDate.parse(DATE_2))
                .build();

        return Arrays.asList(hearingDay_1,hearingDay_2);
    }

    @Test
    public void hearingDaysSequenced() {
        //given
        Envelope<HearingDaysSequenced> envelope = (Envelope<HearingDaysSequenced>) mock(Envelope.class);
        HearingDaysSequenced hearingData = HearingDaysSequenced.hearingDaysSequenced()
                .withHearingId(HEARING_ID)
                .withHearingDays(getHearingDays())
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(objectToJsonObjectConverter.convert(any(List.class))).willReturn(createTestJsonObject());

        //when
        hearingDaysForHearingEventListener.hearingDaysSequenced(envelope);

        //then
        verify(hearingRepository).save(hearing);

    }
}
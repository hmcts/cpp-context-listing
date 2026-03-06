package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NonDefaultDaysForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime START_TIME = ZonedDateTime.now();
    private static final String START_TIMES = "startTimes";
    private static final String TEST_JSON = "{ \"" + START_TIMES + "\": {\"test\": \"test\"} }";


    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode properties;

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private NonDefaultDaysForHearingEventListener defaultDaysForHearingEventListener;

    @Mock
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    Hearing hearing;


    @BeforeEach
    public  void setup() throws IOException {
        properties = (ObjectNode) mapper.readTree(TEST_JSON);
    }

    @Test
    public void shouldAssignStartTimeToHearing() {

        //given
        Envelope<NonDefaultDaysAssignedToHearing> envelope = (Envelope<NonDefaultDaysAssignedToHearing>) mock(Envelope.class);
        NonDefaultDaysAssignedToHearing hearingData = NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                .withNonDefaultDays(Arrays.asList(NonDefaultDay.nonDefaultDay()
                        .withStartTime(START_TIME)
                        .withDuration(null)
                        .build()))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        //when
        defaultDaysForHearingEventListener.nonDefaultDaysAssignedForHearing(envelope);

        //then
        verify(hearingRepository).save(hearing);

    }

    @Test
    public void shouldChangeStartTimeForHearing() {
        //given
        Envelope<NonDefaultDaysChangedForHearing> envelope = (Envelope<NonDefaultDaysChangedForHearing>) mock(Envelope.class);
        NonDefaultDaysChangedForHearing hearingData = NonDefaultDaysChangedForHearing.nonDefaultDaysChangedForHearing()
                .withNonDefaultDays(Arrays.asList(NonDefaultDay.nonDefaultDay()
                        .withStartTime(START_TIME)
                        .withDuration(null)
                        .build()))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        //when
        defaultDaysForHearingEventListener.nonDefaultDaysChangedForHearing(envelope);

        //then
        verify(hearingRepository).save(hearing);
    }

    private JsonObject createTestJsonObject() {
        try (final JsonReader jsonReader =  JsonObjects.createReader(new StringReader("{\"test\": \"test\"}"))) {
            return jsonReader.readObject();
        }
    }
}
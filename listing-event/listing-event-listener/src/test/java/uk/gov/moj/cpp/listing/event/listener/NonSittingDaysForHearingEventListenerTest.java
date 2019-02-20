package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.nonSittingDaysAssignedToHearing;
import static uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.nonSittingDaysChangedForHearing;

@RunWith(value = MockitoJUnitRunner.class)
public class NonSittingDaysForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final LocalDate NON_SITTING_DAY = LocalDate.now();
    private static final String NON_SITTING_DAYS = "nonSittingDays";
    private static final String TEST_JSON = "{ \"" + NON_SITTING_DAYS + "\": {\"test\": \"test\"} }";

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode properties;

    @Mock
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private NonSittingDaysForHearingEventListener nonSittingDaysForHearingEventListener;

    @Mock
    Hearing hearing;

    @Before
    public  void setup() throws IOException {
        properties = (ObjectNode) mapper.readTree(TEST_JSON);
    }

    @Test
    public void shouldAssignNonSittingDaysToHearing() {
        //given
        Envelope<NonSittingDaysAssignedToHearing> envelope = (Envelope< NonSittingDaysAssignedToHearing>) mock(Envelope.class);
        NonSittingDaysAssignedToHearing hearingData = nonSittingDaysAssignedToHearing()
                .withNonSittingDays(Arrays.asList(NON_SITTING_DAY))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        //when
        nonSittingDaysForHearingEventListener.nonSittingDaysAssignedForHearing(envelope);

        //then
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldChangeNonSittingDaysForHearing() {
        //given
        Envelope<NonSittingDaysChangedForHearing> envelope = (Envelope< NonSittingDaysChangedForHearing>) mock(Envelope.class);
        NonSittingDaysChangedForHearing hearingData = nonSittingDaysChangedForHearing()
                .withNonSittingDays(Arrays.asList(NON_SITTING_DAY))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(objectToJsonObjectConverter.convert(any(List.class))).willReturn(createTestJsonObject());

         //when
        nonSittingDaysForHearingEventListener.nonSittingDaysChangedForHearing(envelope);

        //then
        verify(hearingRepository).save(hearing);
    }

    private JsonObject createTestJsonObject() {
        try (final JsonReader jsonReader =  Json.createReader(new StringReader("{\"test\": \"test\"}"))) {
            return jsonReader.readObject();
        }
    }
}
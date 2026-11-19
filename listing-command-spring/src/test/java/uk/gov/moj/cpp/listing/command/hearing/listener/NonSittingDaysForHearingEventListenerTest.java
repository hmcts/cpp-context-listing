package uk.gov.moj.cpp.listing.command.hearing.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.nonSittingDaysAssignedToHearing;
import static uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.nonSittingDaysChangedForHearing;

import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public  void setup() throws IOException {
        properties = (ObjectNode) mapper.readTree(TEST_JSON);
    }

    @Test
    public void shouldAssignNonSittingDaysToHearing() {
        //given
        RaisedEvent<NonSittingDaysAssignedToHearing> envelope = (RaisedEvent< NonSittingDaysAssignedToHearing>) mock(RaisedEvent.class);
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
        RaisedEvent<NonSittingDaysChangedForHearing> envelope = (RaisedEvent< NonSittingDaysChangedForHearing>) mock(RaisedEvent.class);
        NonSittingDaysChangedForHearing hearingData = nonSittingDaysChangedForHearing()
                .withNonSittingDays(Arrays.asList(NON_SITTING_DAY))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

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
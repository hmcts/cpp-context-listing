package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.JudicialRole.judicialRole;
import static uk.gov.justice.listing.events.JudiciaryRemovedFromHearing.judiciaryRemovedFromHearing;

import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JudiciaryForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final String JUDICIARY = "judiciary";
    private static final UUID JUDICIAL_ROLE_ID = randomUUID();
    private static final String TEST_JSON = "{ \"" + JUDICIARY + "\": {\"test\": \"test\"} }";

    @Mock
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ObjectNode properties;

    @Mock
    Hearing hearing;

    @InjectMocks
    private JudiciaryForHearingEventListener judiciaryForHearingEventListener;

    @Test
    public void shouldAssignJudiciaryToHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode properties = (ObjectNode) objectMapper.readTree(TEST_JSON);
        Envelope<JudiciaryAssignedToHearing>  envelope = (Envelope<JudiciaryAssignedToHearing>) mock(Envelope.class);
        JudicialRole judicialRole = judicialRole()
                .withIsBenchChairman(true)
                .withIsDeputy(false)
                .withJudicialId(fromString("5a4ce2e5-4b4e-43bd-963c-b98c2150b74d"))
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType("CIRCUIT_JUDGE")
                        .withJudicialRoleTypeId(null)
                        .build())
                .build();

        JudiciaryAssignedToHearing hearingData = JudiciaryAssignedToHearing.judiciaryAssignedToHearing()
                .withJudiciary(singletonList(judicialRole))
                .withHearingId(HEARING_ID)
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        judiciaryForHearingEventListener.judiciaryAssignedToHearing(envelope);

        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldChangeJudiaciaryForHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode properties = (ObjectNode) objectMapper.readTree(TEST_JSON);
        Envelope<JudiciaryChangedForHearing> envelope = (Envelope<JudiciaryChangedForHearing>) mock(Envelope.class);
        JudicialRole judicialRole = judicialRole()
                .withIsBenchChairman(true)
                .withIsDeputy(false)
                .withJudicialId(JUDICIAL_ROLE_ID)
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType("CIRCUIT_JUDGE")
                        .withJudicialRoleTypeId(null)
                        .build())
                .build();

        JudiciaryChangedForHearing hearingData = JudiciaryChangedForHearing.judiciaryChangedForHearing()
                .withJudiciary(singletonList(judicialRole))
                .withHearingId(HEARING_ID)
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);

        judiciaryForHearingEventListener.judiciaryChangedForHearing(envelope);

        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRemoveJudiciaryFromHearing() throws Exception {
        Envelope<JudiciaryRemovedFromHearing> envelope = (Envelope<JudiciaryRemovedFromHearing>) mock(Envelope.class);
        JudiciaryRemovedFromHearing hearingData = judiciaryRemovedFromHearing()
                .withHearingId(HEARING_ID)
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode properties = (ObjectNode) objectMapper.readTree(TEST_JSON);
        given(hearing.getProperties()).willReturn(properties);

        judiciaryForHearingEventListener.judiciaryRemovedFromHearing(envelope);

        verify(hearingRepository).save(hearing);
    }

    private JsonObject createTestJsonObject() {
        try (final JsonReader jsonReader =  JsonObjects.createReader(new StringReader("{\"test\": \"test\"}"))) {
            return jsonReader.readObject();
        }
    }

}

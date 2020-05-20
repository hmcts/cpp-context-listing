package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ExtendHearingForHearingListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private uk.gov.moj.cpp.listing.persistence.entity.Hearing hearingEntity;
    @Mock
    private JsonNode jsonNode;
    @Mock
    private HearingDeleted hearingDeleted;
    @InjectMocks
    private ExtendHearingForHearingListener extendHearingForHearingListener;
    @Mock
    private uk.gov.moj.cpp.listing.persistence.entity.Hearing hearing;
    @Mock
    ObjectNode properties;
    private static final String LISTED_CASES_FIELD = "listedCases";
    @Spy
    private ObjectMapper mapper =  new ObjectMapperProducer().objectMapper();

    @Test
    public void shouldAddedCasesForHearing() throws IOException {
        final Envelope<AddedCasesForHearing> envelope = (Envelope<AddedCasesForHearing>) mock(Envelope.class);

        final List<ListedCase> listedCaseList = new ArrayList<>();
        listedCaseList.add(ListedCase.listedCase().withId(randomUUID()).build());

       final AddedCasesForHearing addedCasesForHearing = AddedCasesForHearing.addedCasesForHearing()
                .withHearingId(UUID.randomUUID())
                .withUnAllocatedListedCases(listedCaseList)
                .build();

        final ObjectMapper objectMapper = new ObjectMapper();
        final List<uk.gov.justice.listing.events.ListedCase> testCases = createListedCases();
        final String testCasesString =  mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        given(envelope.payload()).willReturn(addedCasesForHearing);
        given(hearingRepository.findBy(any((UUID.class)))).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES_FIELD)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        extendHearingForHearingListener.hearingAddedCasesForHearing(envelope);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(uk.gov.moj.cpp.listing.persistence.entity.Hearing.class));
    }

    @Test
    public void shouldHearingBeDeleted() {
        final Envelope<HearingDeleted> envelope = (Envelope<HearingDeleted>) mock(Envelope.class);

        hearingDeleted = HearingDeleted.hearingDeleted()
                .withHearingIdToBeDeleted(HEARING_ID)
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.createHearingBuilder()
                .setId(hearingDeleted.getHearingIdToBeDeleted()).build();

        given(envelope.payload()).willReturn(hearingDeleted);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingDeleted.getHearingIdToBeDeleted());

        extendHearingForHearingListener.hearingDeleted(envelope);

        verify(hearingRepository).findBy(hearingDeleted.getHearingIdToBeDeleted());
        verify(hearingRepository).remove(hearingEntity);
    }

    private List<ListedCase> createListedCases() {
        return singletonList(ListedCase.listedCase()
                .withId(randomUUID())
                .build());
    }
}

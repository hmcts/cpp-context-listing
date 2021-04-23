package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.HEARING_ID;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.buildEventProsecutionCases;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.buildHearingEntity;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.createListedCases;

import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

@RunWith(MockitoJUnitRunner.class)
public class ExtendHearingForHearingListenerTest {

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private uk.gov.moj.cpp.listing.persistence.entity.Hearing hearingEntity;
    @Mock
    private JsonNode jsonNode;
    @Mock
    private HearingDeleted hearingDeleted;
    @Mock
    private HearingPartiallyUpdated hearingPartiallyUpdated;
    @InjectMocks
    private ExtendHearingForHearingListener extendHearingForHearingListener;
    @Mock
    private uk.gov.moj.cpp.listing.persistence.entity.Hearing hearing;
    @Mock
    ObjectNode properties;
    private static final String LISTED_CASES_FIELD = "listedCases";

    @Spy
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Test
    public void shouldAddedCasesForHearing() throws IOException {

        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID caseId3 = randomUUID();
        final UUID defId1 = randomUUID();
        final UUID defId2 = randomUUID();
        final UUID defId3 = randomUUID();
        final UUID defId4 = randomUUID();
        final UUID offId1 = randomUUID();
        final UUID offId2 = randomUUID();
        final UUID offId3 = randomUUID();
        final UUID offId4 = randomUUID();
        final UUID offId5 = randomUUID();
        final UUID offId6 = randomUUID();

        final Envelope<AddedCasesForHearing> envelope = (Envelope<AddedCasesForHearing>) mock(Envelope.class);

        final List<ListedCase> listedCasesToAdd = createListedCasesToAdd(caseId2, caseId3, defId2, defId3, defId4, offId4, offId5, offId6);

        final AddedCasesForHearing addedCasesForHearing = AddedCasesForHearing.addedCasesForHearing()
                .withHearingId(UUID.randomUUID())
                .withUnAllocatedListedCases(listedCasesToAdd)
                .build();

        final ObjectMapper objectMapper = new ObjectMapper();
        final List<uk.gov.justice.listing.events.ListedCase> testCases = createListedCases(caseId1, caseId2, defId1, defId2, offId1, offId2, offId3);
        final String testCasesString = mapper.writeValueAsString(testCases);
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
    public void shouldHandleCasesAddedToHearing() throws IOException {

        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID caseId3 = randomUUID();
        final UUID defId1 = randomUUID();
        final UUID defId2 = randomUUID();
        final UUID defId3 = randomUUID();
        final UUID defId4 = randomUUID();
        final UUID offId1 = randomUUID();
        final UUID offId2 = randomUUID();
        final UUID offId3 = randomUUID();
        final UUID offId4 = randomUUID();
        final UUID offId5 = randomUUID();
        final UUID offId6 = randomUUID();

        final Envelope<CasesAddedToHearing> envelope = (Envelope<CasesAddedToHearing>) mock(Envelope.class);

        final List<ListedCase> listedCasesToAdd = createListedCasesToAdd(caseId2, caseId3, defId2, defId3, defId4, offId4, offId5, offId6);

        final CasesAddedToHearing casesAddedToHearing = CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(UUID.randomUUID())
                .withUnAllocatedListedCases(listedCasesToAdd)
                .build();

        final ObjectMapper objectMapper = new ObjectMapper();
        final List<uk.gov.justice.listing.events.ListedCase> testCases = createListedCases(caseId1, caseId2, defId1, defId2, offId1, offId2, offId3);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        given(envelope.payload()).willReturn(casesAddedToHearing);
        given(hearingRepository.findBy(any((UUID.class)))).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES_FIELD)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        extendHearingForHearingListener.handleCasesAddedToHearingEvent(envelope);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(uk.gov.moj.cpp.listing.persistence.entity.Hearing.class));
    }

    private List<ListedCase> createListedCasesToAdd(final UUID caseId2, final UUID caseId3, final UUID defId2, final UUID defId3, final UUID defId4, final UUID offId4, final UUID offId5, final UUID offId6) {
        final List<ListedCase> listedCasesToAdd = new ArrayList<>();
        listedCasesToAdd.add(ListedCase.listedCase().withId(caseId3)
                .withDefendants(Arrays.asList(Defendant.defendant().withId(defId3)
                        .withOffences(Arrays.asList(Offence.offence().withId(offId4).build()))
                        .build()))
                .build());
        listedCasesToAdd.add(ListedCase.listedCase().withId(caseId2)
                .withDefendants(Arrays.asList(Defendant.defendant().withId(defId2)
                                .withOffences(Arrays.asList(Offence.offence().withId(offId5).build()))
                                .build(),
                        Defendant.defendant().withId(defId4)
                                .withOffences(Arrays.asList(Offence.offence().withId(offId6).build()))
                                .build()))
                .build());
        return listedCasesToAdd;
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


    @Test
    public void shouldHearingBeUpdatedPartially() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.createHearingBuilder()
                .setId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .setProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);
    }


}

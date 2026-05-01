package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.listing.events.*;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.*;

import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class ExtendHearingForHearingListenerTest {
    @Spy
    private OffenceComparator manageOffence;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private HearingSearchSyncService hearingSearchSyncService;
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
    private static final String DEFENCE_COUNSELS = "defenceCounsels";

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
        verify(properties).replace(any(), objectNodeCaptor.capture());
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
        verify(properties).replace(any(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(uk.gov.moj.cpp.listing.persistence.entity.Hearing.class));
    }

    @Test
    public void shouldHandleDuplicateCasesAddedToHearing() throws IOException {

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
        listedCasesToAdd.addAll(createListedCasesToAdd(caseId2, caseId3, defId2, defId3, defId4, offId4, offId5, offId6));
        final CasesAddedToHearing casesAddedToHearing = CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(UUID.randomUUID())
                .withUnAllocatedListedCases(listedCasesToAdd)
                .build();

        final ObjectMapper objectMapper = new ObjectMapper();
        final List<uk.gov.justice.listing.events.ListedCase> testCases = createListedCases(caseId1, caseId2, defId1, defId2, offId1, offId2, offId3);
        testCases.addAll(createListedCases(caseId1, caseId2, defId1, defId2, offId1, offId2, offId3));
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        given(envelope.payload()).willReturn(casesAddedToHearing);
        given(hearingRepository.findBy(any((UUID.class)))).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES_FIELD)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);


        extendHearingForHearingListener.handleCasesAddedToHearingEvent(envelope);
        verify(properties).replace(any(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(uk.gov.moj.cpp.listing.persistence.entity.Hearing.class));

        assertThat(objectNodeCaptor.getValue().size(), Matchers.is(3));

    }

    @Test
    public void shouldHearingBeDeleted() {
        final Envelope<HearingDeleted> envelope = (Envelope<HearingDeleted>) mock(Envelope.class);

        hearingDeleted = HearingDeleted.hearingDeleted()
                .withHearingIdToBeDeleted(HEARING_ID)
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingDeleted.getHearingIdToBeDeleted()).build();

        given(envelope.payload()).willReturn(hearingDeleted);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingDeleted.getHearingIdToBeDeleted());

        extendHearingForHearingListener.hearingDeleted(envelope);

        verify(hearingRepository).findBy(hearingDeleted.getHearingIdToBeDeleted());
        verify(hearingRepository).remove(hearingEntity);
    }

    @Test
    public void shouldHearingBeDeletedWhenThereIsNoHearing() {
        final Envelope<HearingDeleted> envelope = (Envelope<HearingDeleted>) mock(Envelope.class);

        hearingDeleted = HearingDeleted.hearingDeleted()
                .withHearingIdToBeDeleted(HEARING_ID)
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingDeleted.getHearingIdToBeDeleted()).build();

        given(envelope.payload()).willReturn(hearingDeleted);

        doReturn(null).when(hearingRepository).findBy(hearingDeleted.getHearingIdToBeDeleted());

        extendHearingForHearingListener.hearingDeleted(envelope);

        verify(hearingRepository).findBy(hearingDeleted.getHearingIdToBeDeleted());
        verify(hearingRepository, never()).remove(any());
    }


    @Test
    public void shouldHearingBeUpdatedPartially() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);
    }

    @Test
    public void shouldHearingBeUpdatedPartiallyForUnallocatedHearings() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);
    }

    @Test
    public void shouldHearingBeUpdatedPartiallyByCases() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        verify(hearingRepository).save(hearingEntity);
    }


    @Test
    public void shouldHearingBeUpdatedPartiallyByOffences() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .withSplitHearing("unallocated")
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);
    }

    @Test
    public void shouldHearingWithDefenceCounselsBeUpdatedPartially() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingEntityProperties = buildHearingEntityProperties();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingEntityProperties).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);


        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertThat(hearingEntity.getProperties().get(DEFENCE_COUNSELS).toString().contains(DEF_ID1.toString()), Matchers.is(false));
        assertThat(hearingEntity.getProperties().get(DEFENCE_COUNSELS).toString().contains(DEF_ID2.toString()), Matchers.is(true));
    }

    @Test
    public void shouldPartiallyUpdateUnallocatedHearingForFirstOffenceSplitInMultipleOffences() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingEntityProperties = buildHearingEntityWithSingleDefMultiOffenceProperties();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .withSplitHearing("unallocated")
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingEntityProperties).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID1.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID2.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID3.toString()));
    }

    @Test
    public void shouldPartiallyUpdateUnallocatedHearingForFirstCaseInMultiCasesDefendantsOffences() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .withSplitHearing("unallocated")
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(CASE_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID1.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(CASE_ID2.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID2.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID2.toString()));
    }

    @Test
    public void shouldPartiallyUpdateUnalloactedHearinForSecondCaseInMultiCasesDefendantsOffences() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCase2())
                .withSplitHearing("unallocated")
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(CASE_ID1.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID1.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(CASE_ID2.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID2.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID2.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID3.toString()));

    }

    @Test
    public void shouldPartiallyUpdateUnalloactedHearinForMultiCasesDefendantsOffences() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingObj = buildHearingEntity();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCaseForMultiple())
                .withSplitHearing("unallocated")
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(CASE_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(CASE_ID2.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID2.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID2.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID3.toString()));

    }

    @Test
    public void shouldPartiallyUpdateUnalloactedHearinForCasesMultiDefendantsOffences() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        JsonObject incomingHearingJsonObject;



        final JsonNode hearingObj = buildHearingEntityForMultiDefendants();


        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCaseForMultipleDefendants())
                .withSplitHearing("unallocated")
                .build();


        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingObj).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(CASE_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID2.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID2.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID3.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID4.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID3.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID5.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID6.toString()));

    }

    @Test
    public void shouldPartiallUpdateUnallocatedHearingWithSecondOffenceInMultipleOffences() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingEntityProperties = buildHearingEntityWithSingleDefMultiOffenceProperties();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases(OFF_ID2))
                .withSplitHearing("unallocated")
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingEntityProperties).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);

        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID1.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID2.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID3.toString()));
    }

    @Test
    public void shouldPartiallyUpdateUnallocatedHearingForFirstOffenceInMultiOffenceSplit() throws IOException {
        final Envelope<HearingPartiallyUpdated> envelope = (Envelope<HearingPartiallyUpdated>) mock(Envelope.class);
        final JsonNode hearingEntityProperties = buildHearingEntityWithSingleDefMultiOffenceProperties();

        hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(HEARING_ID)
                .withProsecutionCases(buildEventProsecutionCases())
                .withSplitHearing("unallocated")
                .build();

        hearingEntity = uk.gov.moj.cpp.listing.persistence.entity.Hearing.builder()
                .withId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProperties(hearingEntityProperties).build();

        given(envelope.payload()).willReturn(hearingPartiallyUpdated);


        doReturn(hearingEntity).when(hearingRepository).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());

        extendHearingForHearingListener.hearingPartiallyUpdated(envelope);

        verify(hearingRepository, times(2)).findBy(hearingPartiallyUpdated.getHearingIdToBeUpdated());
        verify(hearingRepository).save(hearingEntity);

        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(DEF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID1.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID2.toString()));
        assertFalse(hearingEntity.getProperties().get("listedCases").toString().contains(OFF_ID3.toString()));
    }

    private List<ListedCase> createListedCaseWithADefendantAndThreeOffencesToAdd(final UUID caseId, final UUID defendantId,
                                                                                 final UUID offId1, final int listingNumber1,
                                                                                 final UUID offId2, final int listingNumber2,
                                                                                 final UUID offId3, final int listingNumber3) {
        final List<ListedCase> listedCasesToAdd = new ArrayList<>();
        listedCasesToAdd.add(ListedCase.listedCase().withId(caseId)
                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId)
                        .withOffences(Arrays.asList(
                                Offence.offence().withId(offId1).withListingNumber(listingNumber1).build(),
                                Offence.offence().withId(offId2).withListingNumber(listingNumber2).build(),
                                Offence.offence().withId(offId3).withListingNumber(listingNumber3).build()
                        ))
                        .build()))
                .build());

        return listedCasesToAdd;
    }

    private List<ListedCase> createListedCaseWithADefendantAndFourOffencesToAdd(final UUID caseId, final UUID defendantId,
                                                                                final UUID offId1, final int listingNumber1,
                                                                                final UUID offId2, final int listingNumber2,
                                                                                final UUID offId3, final int listingNumber3,
                                                                                final UUID offId4, final int listingNumber4) {
        final List<ListedCase> listedCasesToAdd = new ArrayList<>();
        listedCasesToAdd.add(ListedCase.listedCase().withId(caseId)
                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId)
                        .withOffences(Arrays.asList(
                                Offence.offence().withId(offId1).withListingNumber(listingNumber1).build(),
                                Offence.offence().withId(offId2).withListingNumber(listingNumber2).build(),
                                Offence.offence().withId(offId3).withListingNumber(listingNumber3).build(),
                                Offence.offence().withId(offId4).withListingNumber(listingNumber4).withShadowListed(true).build()
                        ))
                        .build()))
                .build());

        return listedCasesToAdd;
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
}
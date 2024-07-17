package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.DEF_ID1;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.DEF_ID2;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.HEARING_ID;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.buildEventProsecutionCases;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.buildHearingEntity;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.buildHearingEntityProperties;
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
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@RunWith(DataProviderRunner.class)
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

    @DataProvider
    public static Object[][] listingNumberVariations() {
        return new Object[][]{
                {"true", 1},
                {"false", 0}
        };
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @UseDataProvider("listingNumberVariations")
    public void shouldAddedCasesForHearing(final String allocated, final int listingNumber) throws IOException {

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
        final JsonNode allocatedValue = objectMapper.readTree(allocated);

        given(envelope.payload()).willReturn(addedCasesForHearing);
        given(hearingRepository.findBy(any((UUID.class)))).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES_FIELD)).willReturn(testCasesProperties);
        given(properties.get("allocated")).willReturn(allocatedValue);
        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        extendHearingForHearingListener.hearingAddedCasesForHearing(envelope);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(uk.gov.moj.cpp.listing.persistence.entity.Hearing.class));

        final ArrayNode cases = objectNodeCaptor.getValue();

        if (listingNumber > 0) {
            assertNull(cases.get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber"));
            assertNull(cases.get(0).get("defendants").get(0).get("offences").get(1).get("listingNumber"));
            assertNull(cases.get(1).get("defendants").get(0).get("offences").get(0).get("listingNumber"));
            assertThat(cases.get(1).get("defendants").get(0).get("offences").get(1).get("id").asText(), equalTo(offId5.toString()));
            assertThat(cases.get(1).get("defendants").get(1).get("offences").get(0).get("id").asText(), equalTo(offId6.toString()));
            assertThat(cases.get(2).get("defendants").get(0).get("offences").get(0).get("id").asText(), equalTo(offId4.toString()));
        } else {
            assertThat(cases.get(1).get("defendants").get(0).get("offences").get(1).get("id").asText(), equalTo(offId5.toString()));
            assertThat(cases.get(1).get("defendants").get(1).get("offences").get(0).get("id").asText(), equalTo(offId6.toString()));
            assertThat(cases.get(2).get("defendants").get(0).get("offences").get(0).get("id").asText(), equalTo(offId4.toString()));

        }

    }

    @Test
    @UseDataProvider("listingNumberVariations")
    public void shouldHandleCasesAddedToHearing(final String allocated, final int listingNumber) throws IOException {

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
        final JsonNode allocatedValue = objectMapper.readTree(allocated);

        given(envelope.payload()).willReturn(casesAddedToHearing);
        given(hearingRepository.findBy(any((UUID.class)))).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES_FIELD)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);
        given(properties.get("allocated")).willReturn(allocatedValue);
        extendHearingForHearingListener.handleCasesAddedToHearingEvent(envelope);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(uk.gov.moj.cpp.listing.persistence.entity.Hearing.class));

        final ArrayNode cases = objectNodeCaptor.getValue();

        assertNull(cases.get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber"));
        assertNull(cases.get(0).get("defendants").get(0).get("offences").get(1).get("listingNumber"));
        assertNull(cases.get(1).get("defendants").get(0).get("offences").get(0).get("listingNumber"));
        if (listingNumber > 0) {
            assertThat(cases.get(1).get("defendants").get(0).get("offences").get(1).get("id").asText(), equalTo(offId5.toString()));
            assertThat(cases.get(1).get("defendants").get(1).get("offences").get(0).get("id").asText(), equalTo(offId6.toString()));
            assertThat(cases.get(2).get("defendants").get(0).get("offences").get(0).get("id").asText(), equalTo(offId4.toString()));
        } else {

            assertThat(cases.get(1).get("defendants").get(0).get("offences").get(1).get("id").asText(), equalTo(offId5.toString()));
            assertThat(cases.get(1).get("defendants").get(1).get("offences").get(0).get("id").asText(), equalTo(offId6.toString()));
            assertThat(cases.get(2).get("defendants").get(0).get("offences").get(0).get("id").asText(), equalTo(offId4.toString()));
        }

    }

    @Test
    @UseDataProvider("listingNumberVariations")
    public void shouldReplaceOffenceWithLatestListingNumber(final String allocated, final int listingNumber) throws IOException {

        final UUID caseId1 = randomUUID();
        final UUID defId1 = randomUUID();
        final UUID offId1 = randomUUID();
        final UUID offId2 = randomUUID();
        final UUID offId3 = randomUUID();

        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCaseWithADefendantAndThreeOffencesToAdd(caseId1, defId1, offId1, 1, offId2, 1, offId3, 1);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        final JsonNode allocatedValue = objectMapper.readTree(allocated);

        given(hearingRepository.findBy(any((UUID.class)))).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES_FIELD)).willReturn(testCasesProperties);
        given(properties.get("allocated")).willReturn(allocatedValue);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        final Envelope<CasesAddedToHearing> envelope = (Envelope<CasesAddedToHearing>) mock(Envelope.class);

        final List<ListedCase> listedCasesToAdd = createListedCaseWithADefendantAndThreeOffencesToAdd(caseId1, defId1, offId1, 1, offId2, 2, offId3, 1);

        final CasesAddedToHearing casesAddedToHearing = CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(randomUUID())
                .withUnAllocatedListedCases(listedCasesToAdd)
                .build();
        given(envelope.payload()).willReturn(casesAddedToHearing);

        extendHearingForHearingListener.handleCasesAddedToHearingEvent(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(Hearing.class));

        final ArrayNode cases = objectNodeCaptor.getValue();
        assertThat(cases.get(0).get("defendants").get(0).get("offences").size(), CoreMatchers.is(3));

        final JsonNode defendant = cases.get(0).get("defendants").get(0);
        if (listingNumber > 0) {
            assertThat(defendant.get("offences").get(0).get("id").asText(), equalTo(offId1.toString()));
            assertThat(defendant.get("offences").get(0).get("listingNumber").asInt(), equalTo(1));
            assertThat(defendant.get("offences").get(1).get("id").asText(), equalTo(offId2.toString()));
            assertThat(defendant.get("offences").get(1).get("listingNumber").asInt(), equalTo(2));
            assertThat(defendant.get("offences").get(2).get("id").asText(), equalTo(offId3.toString()));
            assertThat(defendant.get("offences").get(2).get("listingNumber").asInt(), equalTo(1));
        }

    }


    @Test
    @UseDataProvider("listingNumberVariations")
    public void shouldAddNewOffenceToExistingDBOffences(final String allocated, final int listingNumber) throws IOException {

        final UUID caseId1 = randomUUID();
        final UUID defId1 = randomUUID();
        final UUID offId1 = randomUUID();
        final UUID offId2 = randomUUID();
        final UUID offId3 = randomUUID();
        final UUID offId4 = randomUUID();

        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCaseWithADefendantAndThreeOffencesToAdd(caseId1, defId1, offId1, 1, offId2, 1, offId3, 1);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        final JsonNode allocatedValue = objectMapper.readTree(allocated);

        given(hearingRepository.findBy(any((UUID.class)))).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES_FIELD)).willReturn(testCasesProperties);
        given(properties.get("allocated")).willReturn(allocatedValue);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        final Envelope<CasesAddedToHearing> envelope = (Envelope<CasesAddedToHearing>) mock(Envelope.class);

        final List<ListedCase> listedCasesToAdd = createListedCaseWithADefendantAndFourOffencesToAdd(caseId1, defId1, offId1, 1, offId2, 2, offId3, 1, offId4, 1);

        final CasesAddedToHearing casesAddedToHearing = CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(randomUUID())
                .withUnAllocatedListedCases(listedCasesToAdd)
                .build();
        given(envelope.payload()).willReturn(casesAddedToHearing);

        extendHearingForHearingListener.handleCasesAddedToHearingEvent(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(any(Hearing.class));

        final ArrayNode cases = objectNodeCaptor.getValue();
        assertThat(cases.get(0).get("defendants").get(0).get("offences").size(), CoreMatchers.is(4));

        final JsonNode defendant = cases.get(0).get("defendants").get(0);
        if (listingNumber > 0) {

            assertThat(defendant.get("offences").get(0).get("id").asText(), equalTo(offId1.toString()));
            assertThat(defendant.get("offences").get(0).get("listingNumber").asInt(), equalTo(1));
            assertThat(defendant.get("offences").get(1).get("id").asText(), equalTo(offId2.toString()));
            assertThat(defendant.get("offences").get(1).get("listingNumber").asInt(), equalTo(2));
            assertThat(defendant.get("offences").get(2).get("id").asText(), equalTo(offId3.toString()));
            assertThat(defendant.get("offences").get(2).get("listingNumber").asInt(), equalTo(1));
            assertThat(defendant.get("offences").get(3).get("id").asText(), equalTo(offId4.toString()));
            assertThat(defendant.get("offences").get(3).get("listingNumber").asInt(), equalTo(1));
        } else {

            assertThat(defendant.get("offences").get(0).get("id").asText(), equalTo(offId1.toString()));
            assertThat(defendant.get("offences").get(0).get("listingNumber").asInt(), equalTo(1));
            assertThat(defendant.get("offences").get(1).get("id").asText(), equalTo(offId2.toString()));
            assertThat(defendant.get("offences").get(1).get("listingNumber").asInt(), equalTo(2));
            assertThat(defendant.get("offences").get(2).get("id").asText(), equalTo(offId3.toString()));
            assertThat(defendant.get("offences").get(2).get("listingNumber").asInt(), equalTo(1));
            assertThat(defendant.get("offences").get(3).get("id").asText(), equalTo(offId4.toString()));
            assertThat(defendant.get("offences").get(3).get("listingNumber").asInt(), equalTo(1));
        }
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

        assertFalse(hearingEntity.getProperties().get(DEFENCE_COUNSELS).toString().contains(DEF_ID1.toString()));
        assertTrue(hearingEntity.getProperties().get(DEFENCE_COUNSELS).toString().contains(DEF_ID2.toString()));
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

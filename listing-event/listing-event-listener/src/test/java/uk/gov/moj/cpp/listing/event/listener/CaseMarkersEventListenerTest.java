package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.Marker.marker;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.NewCaseMarkerUpdated;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseMarkersEventListenerTest {

    private static final String LISTED_CASES = "listedCases";
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID CASE_MARKER_ID_1 = randomUUID();
    private static final String CASE_MARKER_CODE_1 = "Case Marker Code 1";
    private static final String CASE_MARKER_DESCRIPTION_1 = "Case Marker Description 1";
    private static final UUID CASE_MARKER_TYPE_ID_1 = randomUUID();
    private static final UUID CASE_MARKER_ID_2 = randomUUID();
    private static final String CASE_MARKER_CODE_2 = "Case Marker Code 2";
    private static final String CASE_MARKER_DESCRIPTION_2 = "Case Marker Description 2";
    private static final UUID CASE_MARKER_TYPE_ID_2 = randomUUID();
    private static final UUID CASE_MARKER_ID_3 = randomUUID();
    private static final String CASE_MARKER_CODE_3 = "Case Marker Code 3";
    private static final String CASE_MARKER_DESCRIPTION_3 = "Case Marker Description 3";
    private static final UUID CASE_MARKER_TYPE_ID_3 = randomUUID();

    @Spy
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private CaseMarkersEventListener caseMarkersEventListener;

    @Mock
    private Envelope<NewCaseMarkerUpdated> caseMarkersToBeUpdatedEnvelope;

    @Mock
    private Hearing hearing;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private ObjectNode properties;

    @Test
    public void shouldHandleCaseMarkerToBeUpdated() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final Marker marker1 = marker()
                .withId(CASE_MARKER_ID_1)
                .withMarkerTypeid(CASE_MARKER_TYPE_ID_1)
                .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_1)
                .withMarkerTypeCode(CASE_MARKER_CODE_1)
                .build();
        final Marker marker2 = marker()
                .withId(CASE_MARKER_ID_2)
                .withMarkerTypeid(CASE_MARKER_TYPE_ID_2)
                .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_2)
                .withMarkerTypeCode(CASE_MARKER_CODE_2)
                .build();

        final NewCaseMarkerUpdated caseMarkersToBeUpdated = NewCaseMarkerUpdated.newCaseMarkerUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withCaseMarkers(Arrays.asList(marker1, marker2))
                .build();

        given(caseMarkersToBeUpdatedEnvelope.payload()).willReturn(caseMarkersToBeUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        caseMarkersEventListener.handleCaseMarkersUpdated(caseMarkersToBeUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptur.capture());
        final JsonNode newListedCase = objectNodeCaptur.getValue().get(0);
        final int markersSize = newListedCase.get("markers").size();
        MatcherAssert.assertThat(markersSize, equalTo(2));
        final JsonNode caseMarkers1 = objectNodeCaptur.getValue().get(0).get("markers").get(0);
        final JsonNode caseMarkers2 = objectNodeCaptur.getValue().get(0).get("markers").get(1);
        MatcherAssert.assertThat(caseMarkers1.get("markerTypeCode").toString(), equalTo("\"" + CASE_MARKER_CODE_1 + "\""));
        MatcherAssert.assertThat(caseMarkers1.get("markerTypeDescription").toString(), equalTo("\"" + CASE_MARKER_DESCRIPTION_1 + "\""));
        MatcherAssert.assertThat(caseMarkers1.get("markerTypeid").toString(), equalTo("\"" + CASE_MARKER_TYPE_ID_1.toString() + "\""));
        MatcherAssert.assertThat(caseMarkers2.get("markerTypeCode").toString(), equalTo("\"" + CASE_MARKER_CODE_2 + "\""));
        MatcherAssert.assertThat(caseMarkers2.get("markerTypeDescription").toString(), equalTo("\"" + CASE_MARKER_DESCRIPTION_2 + "\""));
        MatcherAssert.assertThat(caseMarkers2.get("markerTypeid").toString(), equalTo("\"" + CASE_MARKER_TYPE_ID_2.toString() + "\""));
        verify(hearingRepository).save(hearing);

        MatcherAssert.assertThat(newListedCase.get("id").textValue() , equalTo(testCases.get(0).getId().toString()));
        MatcherAssert.assertThat(newListedCase.get("caseIdentifier").get("caseReference").textValue() , equalTo(testCases.get(0).getCaseIdentifier().getCaseReference().toString()));
        MatcherAssert.assertThat(newListedCase.get("shadowListed").asBoolean() , equalTo(testCases.get(0).getShadowListed().get()));
    }

    @Test
    public void shouldHandleCaseMarkerToBeUpdatedWithEmpty() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final NewCaseMarkerUpdated caseMarkersToBeUpdated = NewCaseMarkerUpdated.newCaseMarkerUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withCaseMarkers(EMPTY_LIST)
                .build();

        given(caseMarkersToBeUpdatedEnvelope.payload()).willReturn(caseMarkersToBeUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        caseMarkersEventListener.handleCaseMarkersUpdated(caseMarkersToBeUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptur.capture());
        final int markersSize = objectNodeCaptur.getValue().get(0).get("markers").size();
        MatcherAssert.assertThat(markersSize, equalTo(0));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleCaseMarkerToBeUpdatedRemoveOldAndAddNewOnce() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCasesWithCaseMarkersToBeRemoved();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        MatcherAssert.assertThat(testCases.get(0).getMarkers().size(), equalTo(3));

        final Marker marker1 = marker()
                .withId(CASE_MARKER_ID_1)
                .withMarkerTypeid(CASE_MARKER_TYPE_ID_1)
                .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_1)
                .withMarkerTypeCode(CASE_MARKER_CODE_1)
                .build();
        final Marker marker2 = marker()
                .withId(CASE_MARKER_ID_2)
                .withMarkerTypeid(CASE_MARKER_TYPE_ID_2)
                .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_2)
                .withMarkerTypeCode(CASE_MARKER_CODE_2)
                .build();

        final NewCaseMarkerUpdated caseMarkersToBeUpdated = NewCaseMarkerUpdated.newCaseMarkerUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withCaseMarkers(Arrays.asList(marker1, marker2))
                .build();

        given(caseMarkersToBeUpdatedEnvelope.payload()).willReturn(caseMarkersToBeUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        caseMarkersEventListener.handleCaseMarkersUpdated(caseMarkersToBeUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptur.capture());
        final int markersSize = objectNodeCaptur.getValue().get(0).get("markers").size();
        MatcherAssert.assertThat(markersSize, equalTo(2));
        final JsonNode caseMarkers1 = objectNodeCaptur.getValue().get(0).get("markers").get(0);
        final JsonNode caseMarkers2 = objectNodeCaptur.getValue().get(0).get("markers").get(1);
        MatcherAssert.assertThat(caseMarkers1.get("markerTypeCode").toString(), equalTo("\"" + CASE_MARKER_CODE_1 + "\""));
        MatcherAssert.assertThat(caseMarkers1.get("markerTypeDescription").toString(), equalTo("\"" + CASE_MARKER_DESCRIPTION_1 + "\""));
        MatcherAssert.assertThat(caseMarkers1.get("markerTypeid").toString(), equalTo("\"" + CASE_MARKER_TYPE_ID_1.toString() + "\""));
        MatcherAssert.assertThat(caseMarkers2.get("markerTypeCode").toString(), equalTo("\"" + CASE_MARKER_CODE_2 + "\""));
        MatcherAssert.assertThat(caseMarkers2.get("markerTypeDescription").toString(), equalTo("\"" + CASE_MARKER_DESCRIPTION_2 + "\""));
        MatcherAssert.assertThat(caseMarkers2.get("markerTypeid").toString(), equalTo("\"" + CASE_MARKER_TYPE_ID_2.toString() + "\""));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleCaseMarkerToBeUpdatedWhenItsNull() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCasesWithNullMarker();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final Marker marker1 = marker()
                .withId(CASE_MARKER_ID_1)
                .withMarkerTypeid(CASE_MARKER_TYPE_ID_1)
                .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_1)
                .withMarkerTypeCode(CASE_MARKER_CODE_1)
                .build();

        final NewCaseMarkerUpdated caseMarkersToBeUpdated = NewCaseMarkerUpdated.newCaseMarkerUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withCaseMarkers(Arrays.asList(marker1))
                .build();

        given(caseMarkersToBeUpdatedEnvelope.payload()).willReturn(caseMarkersToBeUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        caseMarkersEventListener.handleCaseMarkersUpdated(caseMarkersToBeUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptur.capture());
        final int markersSize = objectNodeCaptur.getValue().get(0).get("markers").size();
        MatcherAssert.assertThat(markersSize, equalTo(1));
        verify(hearingRepository).save(hearing);
    }


    private List<ListedCase> createListedCases() {
        return singletonList(ListedCase.listedCase()
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(STRING.next())
                        .withAuthorityId(randomUUID())
                        .withCaseReference(STRING.next())
                        .build())
                .withDefendants(buildDefendantList())
                .withMarkers(buildCaseMarkersList())
                .withShadowListed(Optional.of(Boolean.TRUE))
                .withId(CASE_ID)
                .build());
    }


    private List<ListedCase> createListedCasesWithCaseMarkersToBeRemoved() {
        return singletonList(ListedCase.listedCase()
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(STRING.next())
                        .withAuthorityId(randomUUID())
                        .withCaseReference(STRING.next())
                        .build())
                .withDefendants(buildDefendantList())
                .withMarkers(buildCaseMarkersListWithCaseMarkerToBeRemoved())
                .withId(CASE_ID)
                .build());
    }

    private List<Marker> buildCaseMarkersList() {
        return singletonList(marker()
                .withId(randomUUID())
                .withMarkerTypeCode(STRING.next())
                .withMarkerTypeDescription(STRING.next())
                .withMarkerTypeid(randomUUID())
                .build());
    }

    private List<Marker> buildCaseMarkersListWithCaseMarkerToBeRemoved() {
        return Arrays.asList(marker()
                        .withId(CASE_MARKER_ID_1)
                        .withMarkerTypeCode(CASE_MARKER_CODE_1)
                        .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_1)
                        .withMarkerTypeid(CASE_MARKER_TYPE_ID_1)
                        .build(),

                marker().withId(CASE_MARKER_ID_2)
                        .withMarkerTypeCode(CASE_MARKER_CODE_2)
                        .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_2)
                        .withMarkerTypeid(CASE_MARKER_TYPE_ID_2)
                        .build(),

                marker().withId(CASE_MARKER_ID_3)
                        .withMarkerTypeCode(CASE_MARKER_CODE_3)
                        .withMarkerTypeDescription(CASE_MARKER_DESCRIPTION_3)
                        .withMarkerTypeid(CASE_MARKER_TYPE_ID_3)
                        .build());
    }

    private List<Defendant> buildDefendantList() {
        return singletonList(Defendant.defendant()
                .withSpecificRequirements(empty())
                .withFirstName(of("FirstName"))
                .withDatesToAvoid(of("Dates to avoid"))
                .withId(DEFENDANT_ID)
                .withMasterDefendantId(java.util.Optional.of(DEFENDANT_ID))
                .withCourtProceedingsInitiated(java.util.Optional.of(ZonedDateTime.now()))
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).withDescription("Custody or remanded into custody").build()))
                .withOffences(singletonList(Offence.offence()
                        .withId(OFFENCE_ID)
                        .withOffenceCode(STRING.next())
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withLegislation(of(STRING.next()))
                                .withTitle(STRING.next())
                                .build())
                        .build()))
                .build());
    }

    private List<ListedCase> createListedCasesWithNullMarker() {
        return singletonList(ListedCase.listedCase()
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(STRING.next())
                        .withAuthorityId(randomUUID())
                        .withCaseReference(STRING.next())
                        .build())
                .withDefendants(buildDefendantList())
                .withMarkers(null)
                .withId(CASE_ID)
                .build());
    }

}
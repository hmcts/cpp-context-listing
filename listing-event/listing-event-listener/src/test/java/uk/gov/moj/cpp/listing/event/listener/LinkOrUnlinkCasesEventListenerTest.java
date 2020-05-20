package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.LinkedCases;
import uk.gov.justice.listing.events.LinkedCasesUpdated;
import uk.gov.justice.listing.events.LinkedToCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.events.Marker.marker;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;


@RunWith(MockitoJUnitRunner.class)
public class LinkOrUnlinkCasesEventListenerTest {
    private static final String LISTED_CASES = "listedCases";
    private static final UUID CASE_ID = randomUUID();
    private static final String CASE_URN = STRING.next();

    private static final UUID CASE_LINKED1 = randomUUID();
    private static final String CASE_LINKED_URN1 = STRING.next();
    private static final UUID CASE_LINKED2 = randomUUID();
    private static final String CASE_LINKED_URN2 = STRING.next();

    private static final UUID CASE_TO_BE_LINKED_UNLINKED = randomUUID();
    private static final String CASE_TO_BE_LINKED_UNLINKED_URN = STRING.next();

    @InjectMocks
    private LinkOrUnlinkCasesEventListener linkOrUnlinkCasesEventListener;

    @Mock
    private Envelope<LinkedCasesUpdated> envelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ObjectNode properties;

    @Mock
    private Hearing hearing;

    @Spy
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Captor
    private ArgumentCaptor<JsonNode> argumentCaptor;

    private JsonNode testCasesProperties;

    @Before
    public void setUp() throws Exception {
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        testCasesProperties = mapper.readTree(testCasesString);

        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);
        when(hearing.getProperties()).thenReturn(properties);
        when(hearingRepository.findBy(any())).thenReturn(hearing);
    }

    @Test
    public void shouldUpdateLinkedCases() throws Exception {
        final LinkedCasesUpdated event = createEvent("LINK");
        when(envelope.payload()).thenReturn(event);

        linkOrUnlinkCasesEventListener.handleLinkedCasesUpdated(envelope);
        verify(properties).replace(anyObject(), argumentCaptor.capture());

        final JsonNode caseNode = argumentCaptor.getValue().get(0);
        assertThat(caseNode.get("id").textValue(), equalTo(CASE_ID.toString()));
        final JsonNode caseIdentifierNode = caseNode.get("caseIdentifier");
        assertThat(caseIdentifierNode.get("caseReference").textValue(), equalTo(CASE_URN));
        final JsonNode linkedCasesNode = caseNode.get("linkedCases");
        assertThat(linkedCasesNode.size(), equalTo(3));

        final Matcher<String> idMatcher = anyOf(is(CASE_LINKED1.toString()), is(CASE_LINKED2.toString()), is(CASE_TO_BE_LINKED_UNLINKED.toString()));
        final Matcher<String> urnMatcher = anyOf(is(CASE_LINKED_URN1.toString()), is(CASE_LINKED_URN2.toString()), is(CASE_TO_BE_LINKED_UNLINKED_URN.toString()));
        assertThat(linkedCasesNode.get(0).get("caseId").textValue(), idMatcher);
        assertThat(linkedCasesNode.get(0).get("caseUrn").textValue(), urnMatcher);
        assertThat(linkedCasesNode.get(1).get("caseId").textValue(), idMatcher);
        assertThat(linkedCasesNode.get(1).get("caseUrn").textValue(), urnMatcher);
        assertThat(linkedCasesNode.get(2).get("caseId").textValue(), idMatcher);
        assertThat(linkedCasesNode.get(2).get("caseUrn").textValue(), urnMatcher);
    }

    @Test
    public void shouldUpdateUnlinkedCases() throws Exception {
        final LinkedCasesUpdated event = createEvent("UNLINK");
        when(envelope.payload()).thenReturn(event);

        linkOrUnlinkCasesEventListener.handleLinkedCasesUpdated(envelope);

        verify(properties).replace(anyObject(), argumentCaptor.capture());

        final JsonNode caseNode = argumentCaptor.getValue().get(0);
        assertThat(caseNode.get("id").textValue(), equalTo(CASE_ID.toString()));
        final JsonNode caseIdentifierNode = caseNode.get("caseIdentifier");
        assertThat(caseIdentifierNode.get("caseReference").textValue(), equalTo(CASE_URN.toString()));
        final JsonNode linkedCasesNode = caseNode.get("linkedCases");
        assertThat(linkedCasesNode.size(), equalTo(1));
        assertThat(linkedCasesNode.get(0).get("caseId").textValue(), equalTo(CASE_LINKED2.toString()));
        assertThat(linkedCasesNode.get(0).get("caseUrn").textValue(), equalTo(CASE_LINKED_URN2));
    }

    private LinkedCasesUpdated createEvent(final String linkActionType){
        return LinkedCasesUpdated.linkedCasesUpdated()
                .withCaseId(CASE_ID)
                .withCaseUrn(CASE_URN)
                .withHearingId(randomUUID())
                .withLinkActionType(linkActionType)
                .withLinkedToCases(Arrays.asList(
                        LinkedToCases.linkedToCases()
                                .withCaseId(CASE_TO_BE_LINKED_UNLINKED)
                                .withCaseUrn(CASE_TO_BE_LINKED_UNLINKED_URN)
                                .build(),
                        LinkedToCases.linkedToCases()
                                .withCaseId(CASE_LINKED1)
                                .withCaseUrn(CASE_LINKED_URN1)
                                .build()))
                .build();

    }

    private List<ListedCase> createListedCases() {
        return singletonList(ListedCase.listedCase()
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(STRING.next())
                        .withAuthorityId(randomUUID())
                        .withCaseReference(CASE_URN)
                        .build())
                .withDefendants(buildDefendantList())
                .withMarkers(buildCaseMarkersList())
                .withLinkedCases(Arrays.asList(
                        LinkedCases.linkedCases()
                                .withCaseId(CASE_LINKED1)
                                .withCaseUrn(CASE_LINKED_URN1)
                                .build(),
                        LinkedCases.linkedCases()
                                .withCaseId(CASE_LINKED2)
                                .withCaseUrn(CASE_LINKED_URN2)
                                .build()))
                .withId(CASE_ID)
                .build());
    }

    private List<Defendant> buildDefendantList() {
        return singletonList(Defendant.defendant().build());
    }

    private List<Marker> buildCaseMarkersList() {
        return singletonList(marker().build());
    }

}

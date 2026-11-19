package uk.gov.moj.cpp.listing.command.hearing.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.moj.cpp.listing.command.hearing.listener.ListenerTestSupport.metadataWithRandomUUID;

import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
public class CaseRemovedFromGroupCasesEventListenerTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CaseRemovedFromGroupCasesEventListener caseRemovedFromGroupCasesEventListener;

    @Mock
    private RaisedEvent<CaseRemovedFromGroupCases> caseRemovedFromGroupCasesEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private Hearing hearing;

    private final static UUID HEARING_ID = randomUUID();
    private final static UUID GROUP_ID = randomUUID();
    private final static UUID MEMBER_CASE_ID = randomUUID();
    private final static UUID MASTER_CASE_ID = randomUUID();

    @BeforeEach
    public void setUp() {
    }


    @Test
    public void memberCaseRemovedFromGroupCases() {
        final ListedCase removedCase = getListedCase(MEMBER_CASE_ID, GROUP_ID, Boolean.FALSE, Boolean.FALSE);

        final CaseRemovedFromGroupCases caseRemovedFromGroupCases =
                CaseRemovedFromGroupCases.caseRemovedFromGroupCases()
                        .withHearingId(HEARING_ID)
                        .withGroupId(GROUP_ID)
                        .withRemovedCase(removedCase)
                        .build();

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);

        caseRemovedFromGroupCasesEventListener.caseRemovedFromGroupCases(
                ListenerTestSupport.jsonEnvelopeFrom(metadataWithRandomUUID("hearing.events.cases-updated-after-case-removed-from-group-cases"),
                        objectToJsonObjectConverter.convert(caseRemovedFromGroupCases)));

        verify(hearingSearchSyncService).syncEntity(hearing, asList(removedCase));
    }

    @Test
    public void masterCaseRemovedFromGroupCases() {
        final ListedCase removedCase = getListedCase(MASTER_CASE_ID, GROUP_ID, Boolean.FALSE, Boolean.FALSE);
        final ListedCase newGroupMaster = getListedCase(MEMBER_CASE_ID, GROUP_ID, Boolean.TRUE, Boolean.TRUE);

        final CaseRemovedFromGroupCases caseRemovedFromGroupCases =
                CaseRemovedFromGroupCases.caseRemovedFromGroupCases()
                        .withHearingId(HEARING_ID)
                        .withGroupId(GROUP_ID)
                        .withRemovedCase(removedCase)
                        .withNewGroupMaster(newGroupMaster)
                        .build();

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);

        caseRemovedFromGroupCasesEventListener.caseRemovedFromGroupCases(
                ListenerTestSupport.jsonEnvelopeFrom(metadataWithRandomUUID("hearing.events.cases-updated-after-case-removed-from-group-cases"),
                        objectToJsonObjectConverter.convert(caseRemovedFromGroupCases)));

        verify(hearingSearchSyncService).syncEntity(hearing, asList(removedCase, newGroupMaster));
    }

    @Test
    public void shouldWriteNumberOfGroupCasesIntoHearingPropertiesWhenPresent() {
        final ListedCase removedCase = getListedCase(MEMBER_CASE_ID, GROUP_ID, Boolean.FALSE, Boolean.FALSE);
        final Hearing hearingEntity = new Hearing(HEARING_ID, groupHearingProperties(3));

        final CaseRemovedFromGroupCases caseRemovedFromGroupCases =
                CaseRemovedFromGroupCases.caseRemovedFromGroupCases()
                        .withHearingId(HEARING_ID)
                        .withGroupId(GROUP_ID)
                        .withRemovedCase(removedCase)
                        .withNumberOfGroupCases(2)
                        .build();

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearingEntity);

        caseRemovedFromGroupCasesEventListener.caseRemovedFromGroupCases(
                ListenerTestSupport.jsonEnvelopeFrom(metadataWithRandomUUID("listing.events.case-removed-from-group-cases"),
                        objectToJsonObjectConverter.convert(caseRemovedFromGroupCases)));

        assertThat(hearingEntity.getProperties().get("numberOfGroupCases").asInt(), is(2));
        assertThat(hearingEntity.getProperties().get("isGroupProceedings").asBoolean(), is(true));
        verify(hearingSearchSyncService).syncEntity(hearingEntity, asList(removedCase));
    }

    @Test
    public void shouldLeaveHearingPropertiesUnchangedWhenNumberOfGroupCasesAbsent() {
        final ListedCase removedCase = getListedCase(MEMBER_CASE_ID, GROUP_ID, Boolean.FALSE, Boolean.FALSE);
        final ObjectNode properties = groupHearingProperties(3);
        final ObjectNode original = properties.deepCopy();
        final Hearing hearingEntity = new Hearing(HEARING_ID, properties);

        final CaseRemovedFromGroupCases caseRemovedFromGroupCases =
                CaseRemovedFromGroupCases.caseRemovedFromGroupCases()
                        .withHearingId(HEARING_ID)
                        .withGroupId(GROUP_ID)
                        .withRemovedCase(removedCase)
                        .build();

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearingEntity);

        caseRemovedFromGroupCasesEventListener.caseRemovedFromGroupCases(
                ListenerTestSupport.jsonEnvelopeFrom(metadataWithRandomUUID("listing.events.case-removed-from-group-cases"),
                        objectToJsonObjectConverter.convert(caseRemovedFromGroupCases)));

        assertThat(hearingEntity.getProperties(), is(original));
        verify(hearingSearchSyncService).syncEntity(hearingEntity, asList(removedCase));
    }

    private ObjectNode groupHearingProperties(final int numberOfGroupCases) {
        final ObjectNode properties = new ObjectMapper().createObjectNode();
        properties.put("id", HEARING_ID.toString());
        properties.put("isGroupProceedings", true);
        properties.put("numberOfGroupCases", numberOfGroupCases);
        return properties;
    }

    private ListedCase getListedCase(final UUID caseId, final UUID groupId,
                                     final Boolean isGroupMember, final Boolean isGroupMaster) {
        return ListedCase.listedCase()
                .withId(caseId)
                .withIsCivil(Boolean.TRUE)
                .withGroupId(groupId)
                .withIsGroupMember(isGroupMember)
                .withIsGroupMaster(isGroupMaster)
                .build();
    }
}

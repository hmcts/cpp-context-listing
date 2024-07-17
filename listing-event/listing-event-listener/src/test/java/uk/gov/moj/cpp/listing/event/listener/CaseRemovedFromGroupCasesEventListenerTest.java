package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseRemovedFromGroupCasesEventListenerTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CaseRemovedFromGroupCasesEventListener caseRemovedFromGroupCasesEventListener;

    @Mock
    private Envelope<CaseRemovedFromGroupCases> caseRemovedFromGroupCasesEnvelope;

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

    @Before
    public void setUp() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
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

        given(caseRemovedFromGroupCasesEnvelope.payload()).willReturn(caseRemovedFromGroupCases);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);

        caseRemovedFromGroupCasesEventListener.caseRemovedFromGroupCases(
                JsonEnvelope.envelopeFrom(metadataWithRandomUUID("hearing.events.cases-updated-after-case-removed-from-group-cases"),
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

        given(caseRemovedFromGroupCasesEnvelope.payload()).willReturn(caseRemovedFromGroupCases);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);

        caseRemovedFromGroupCasesEventListener.caseRemovedFromGroupCases(
                JsonEnvelope.envelopeFrom(metadataWithRandomUUID("hearing.events.cases-updated-after-case-removed-from-group-cases"),
                        objectToJsonObjectConverter.convert(caseRemovedFromGroupCases)));

        verify(hearingSearchSyncService).syncEntity(hearing, asList(removedCase, newGroupMaster));
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

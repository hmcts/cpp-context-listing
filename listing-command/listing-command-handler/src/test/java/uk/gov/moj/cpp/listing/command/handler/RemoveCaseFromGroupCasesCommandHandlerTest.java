package uk.gov.moj.cpp.listing.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.command.utils.EventStreamHelperUtil.verifyAndGetEvents;
import static uk.gov.moj.cpp.listing.command.utils.ObjectConverters.asPojo;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.MasterCaseUpdatedForGroup;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class RemoveCaseFromGroupCasesCommandHandlerTest {
    private static final UUID HEARING1_ID = randomUUID();
    private static final UUID HEARING2_ID = randomUUID();
    private static final UUID HEARING3_ID = randomUUID();
    private static final UUID GROUP_ID = randomUUID();
    private static final UUID MASTER_CASE_ID = randomUUID();
    private static final UUID CASE1_ID = randomUUID();
    private static final UUID CASE2_ID = randomUUID();

    @InjectMocks
    private RemoveCaseFromGroupCasesCommandHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream masterCaseEventStream;

    @Mock
    private EventStream case1EventStream;

    @Mock
    private EventStream case2EventStream;

    @Mock
    private EventStream hearing1EventStream;

    @Mock
    private EventStream hearing2EventStream;

    @Mock
    private EventStream hearing3EventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseRemovedFromGroupCases.class, MasterCaseUpdatedForGroup.class);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private final CommandToDomainConverter commandToDomainConverter = new CommandToDomainConverter();

    private Case masterCaseAggregate;
    private Case case1Aggregate;
    private Case case2Aggregate;

    private Hearing hearing1Aggregate;
    private Hearing hearing2Aggregate;
    private Hearing hearing3Aggregate;

    private final ProsecutionCase masterCase = getProsecutionCase(GROUP_ID, MASTER_CASE_ID, Boolean.TRUE, Boolean.TRUE);
    private final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, Boolean.FALSE, Boolean.FALSE);
    private final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, Boolean.FALSE, Boolean.FALSE);

    @BeforeEach
    public void setup() {
        masterCaseAggregate = new Case();
        case1Aggregate = new Case();
        case2Aggregate = new Case();

        hearing1Aggregate = new Hearing();
        hearing2Aggregate = new Hearing();
        hearing3Aggregate = new Hearing();


        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreatePrivateEvent_WhenCaseRemovedFromGroupCases() throws EventStreamException {

        when(eventSource.getStreamById(MASTER_CASE_ID)).thenReturn(masterCaseEventStream);
        when(aggregateService.get(masterCaseEventStream, Case.class)).thenReturn(masterCaseAggregate);
        when(eventSource.getStreamById(HEARING1_ID)).thenReturn(hearing1EventStream);
        when(aggregateService.get(hearing1EventStream, Hearing.class)).thenReturn(hearing1Aggregate);

        setInitialDataIntoCaseAggregate(MASTER_CASE_ID, asList(HEARING1_ID));
        setInitialDataIntoHearingAggregate(hearing1Aggregate, asList(masterCase, case1, case2));

        handler.removeCaseFromGroupCases(getJsonEnvelopeForRemoveCommand(GROUP_ID, MASTER_CASE_ID,
                case1, null));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(hearing1EventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("listing.events.case-removed-from-group-cases"),
                        payload().isJson(allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING1_ID.toString())),
                                withJsonPath("$.groupId", equalTo(GROUP_ID.toString())),
                                withJsonPath("$.removedCase.id", equalTo(CASE1_ID.toString())),
                                withJsonPath("$.removedCase.groupId", equalTo(GROUP_ID.toString())),
                                withJsonPath("$.removedCase.isCivil", equalTo(true)),
                                withJsonPath("$.removedCase.isGroupMember", equalTo(false)),
                                withJsonPath("$.removedCase.isGroupMaster", equalTo(false)),
                                withoutJsonPath("$.newGroupMaster"))
                        ))));
    }

    @Test
    public void shouldCreatePrivateEvent_WhenGroupMasterRemoved() throws EventStreamException {

        when(eventSource.getStreamById(MASTER_CASE_ID)).thenReturn(masterCaseEventStream);
        when(eventSource.getStreamById(CASE2_ID)).thenReturn(case2EventStream);

        when(aggregateService.get(masterCaseEventStream, Case.class)).thenReturn(masterCaseAggregate);
        when(aggregateService.get(case2EventStream, Case.class)).thenReturn(case2Aggregate);

        when(eventSource.getStreamById(HEARING1_ID)).thenReturn(hearing1EventStream);

        when(aggregateService.get(hearing1EventStream, Hearing.class)).thenReturn(hearing1Aggregate);

        setInitialDataIntoCaseAggregate(MASTER_CASE_ID, asList(HEARING1_ID));
        setInitialDataIntoHearingAggregate(hearing1Aggregate, asList(masterCase, case1, case2));

        handler.removeCaseFromGroupCases(getJsonEnvelopeForRemoveCommand(GROUP_ID, MASTER_CASE_ID,
                getProsecutionCase(GROUP_ID, MASTER_CASE_ID, Boolean.FALSE, Boolean.FALSE),
                getProsecutionCase(GROUP_ID, CASE2_ID, Boolean.TRUE, Boolean.TRUE)));

        final Stream<JsonEnvelope> envelopeStream1 = verifyAppendAndGetArgumentFrom(hearing1EventStream);
        assertThat(envelopeStream1, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("listing.events.case-removed-from-group-cases"),
                        payload().isJson(allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING1_ID.toString())),
                                withJsonPath("$.groupId", equalTo(GROUP_ID.toString())),
                                withJsonPath("$.removedCase.id", equalTo(MASTER_CASE_ID.toString())),
                                withJsonPath("$.removedCase.groupId", equalTo(GROUP_ID.toString())),
                                withJsonPath("$.removedCase.isCivil", equalTo(true)),
                                withJsonPath("$.removedCase.isGroupMember", equalTo(false)),
                                withJsonPath("$.removedCase.isGroupMaster", equalTo(false)),
                                withJsonPath("$.newGroupMaster.id", equalTo(CASE2_ID.toString())),
                                withJsonPath("$.newGroupMaster.groupId", equalTo(GROUP_ID.toString())),
                                withJsonPath("$.newGroupMaster.isCivil", equalTo(true)),
                                withJsonPath("$.newGroupMaster.isGroupMember", equalTo(true)),
                                withJsonPath("$.newGroupMaster.isGroupMaster", equalTo(true))
                        )))));

        final Stream<JsonEnvelope> envelopeStream2 = verifyAppendAndGetArgumentFrom(case2EventStream);
        assertThat(envelopeStream2, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("listing.events.master-case-updated-for-group"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(CASE2_ID.toString())),
                                withJsonPath("$.hearingIds[0]", equalTo(HEARING1_ID.toString()))
                        )))));
    }

    @Test
    public void shouldCreatePrivateEvent_WhenCaseRemovedWithMultipleHearings() throws EventStreamException {

        when(eventSource.getStreamById(MASTER_CASE_ID)).thenReturn(masterCaseEventStream);
        when(eventSource.getStreamById(CASE2_ID)).thenReturn(case2EventStream);

        when(aggregateService.get(masterCaseEventStream, Case.class)).thenReturn(masterCaseAggregate);
        when(aggregateService.get(case2EventStream, Case.class)).thenReturn(case2Aggregate);

        when(eventSource.getStreamById(HEARING1_ID)).thenReturn(hearing1EventStream);
        when(eventSource.getStreamById(HEARING2_ID)).thenReturn(hearing2EventStream);
        when(eventSource.getStreamById(HEARING3_ID)).thenReturn(hearing3EventStream);

        when(aggregateService.get(hearing1EventStream, Hearing.class)).thenReturn(hearing1Aggregate);
        when(aggregateService.get(hearing2EventStream, Hearing.class)).thenReturn(hearing2Aggregate);
        when(aggregateService.get(hearing3EventStream, Hearing.class)).thenReturn(hearing3Aggregate);

        setInitialDataIntoCaseAggregate(MASTER_CASE_ID, asList(HEARING1_ID, HEARING2_ID, HEARING3_ID));
        setInitialDataIntoHearingAggregate(hearing1Aggregate, asList(masterCase, case1, case2));
        setInitialDataIntoHearingAggregate(hearing2Aggregate, asList(masterCase, case1, case2));
        setInitialDataIntoHearingAggregate(hearing3Aggregate, asList(masterCase, case1, case2));

        handler.removeCaseFromGroupCases(getJsonEnvelopeForRemoveCommand(GROUP_ID, MASTER_CASE_ID,
                getProsecutionCase(GROUP_ID, MASTER_CASE_ID, Boolean.FALSE, Boolean.FALSE),
                getProsecutionCase(GROUP_ID, CASE2_ID, Boolean.FALSE, Boolean.FALSE)));

        final List<JsonEnvelope> events1 = verifyAndGetEvents(hearing1EventStream, 1);
        final List<JsonEnvelope> events2 = verifyAndGetEvents(hearing2EventStream, 1);
        final List<JsonEnvelope> events3 = verifyAndGetEvents(hearing3EventStream, 1);

        final CaseRemovedFromGroupCases removed1 = asPojo(events1.get(0), CaseRemovedFromGroupCases.class);
        final CaseRemovedFromGroupCases removed2 = asPojo(events2.get(0), CaseRemovedFromGroupCases.class);
        final CaseRemovedFromGroupCases removed3 = asPojo(events3.get(0), CaseRemovedFromGroupCases.class);

        assertThat(removed1.getGroupId(), equalTo(GROUP_ID));
        assertThat(removed1.getRemovedCase().getId(), equalTo(MASTER_CASE_ID));
        assertThat(removed1.getHearingId(), equalTo(HEARING1_ID));
        assertThat(removed1.getNewGroupMaster().getId(), equalTo(CASE2_ID));

        assertThat(removed2.getGroupId(), equalTo(GROUP_ID));
        assertThat(removed2.getRemovedCase().getId(), equalTo(MASTER_CASE_ID));
        assertThat(removed2.getHearingId(), equalTo(HEARING2_ID));
        assertThat(removed2.getNewGroupMaster().getId(), equalTo(CASE2_ID));

        assertThat(removed3.getGroupId(), equalTo(GROUP_ID));
        assertThat(removed3.getRemovedCase().getId(), equalTo(MASTER_CASE_ID));
        assertThat(removed3.getHearingId(), equalTo(HEARING3_ID));
        assertThat(removed3.getNewGroupMaster().getId(), equalTo(CASE2_ID));

        final List<JsonEnvelope> eventsForCase = verifyAndGetEvents(case2EventStream, 1);
        final MasterCaseUpdatedForGroup masterCaseUpdatedForGroup = asPojo(eventsForCase.get(0), MasterCaseUpdatedForGroup.class);
        assertThat(masterCaseUpdatedForGroup.getCaseId(), equalTo(CASE2_ID));
        assertThat(masterCaseUpdatedForGroup.getHearingIds().size(), equalTo(3));
        assertTrue(masterCaseUpdatedForGroup.getHearingIds().containsAll(asList(HEARING1_ID, HEARING2_ID, HEARING3_ID)));
    }

    private JsonEnvelope getJsonEnvelopeForRemoveCommand(final UUID groupId, final UUID masterCaseId, final ProsecutionCase removedCase, final ProsecutionCase newGroupMaster) {
        JsonObjectBuilder builder = JsonObjects.createObjectBuilder()
                .add("groupId", groupId.toString())
                .add("masterCaseId", masterCaseId.toString())
                .add("removedCase", objectToJsonObjectConverter.convert(removedCase));

        if (Objects.nonNull(newGroupMaster)) {
            builder.add("newGroupMaster", objectToJsonObjectConverter.convert(newGroupMaster));
        }

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("hearing.command.remove-case-from-group-cases"),
                builder.build());

        return envelope;
    }

    private Case setInitialDataIntoCaseAggregate(final UUID caseId, final List<UUID> hearingIds) {
        masterCaseAggregate.updateMasterCaseForGroup(caseId, hearingIds);
        return masterCaseAggregate;
    }

    private Hearing setInitialDataIntoHearingAggregate(Hearing hearingAggregate, final List<ProsecutionCase> cases) {
        hearingAggregate.addCasesForHearing(cases, Collections.emptyList());
        return hearingAggregate;
    }

    private ProsecutionCase getProsecutionCase(final UUID groupId, final UUID caseId, final Boolean isGroupMember, final Boolean isGroupMaster) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withIsCivil(Boolean.TRUE)
                .withGroupId(groupId)
                .withIsGroupMember(isGroupMember)
                .withIsGroupMaster(isGroupMaster)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(randomUUID().toString())
                        .build())
                .withDefendants(asList(Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(asList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .withIsYouth(false)
                        .build()))
                .build();
    }
}
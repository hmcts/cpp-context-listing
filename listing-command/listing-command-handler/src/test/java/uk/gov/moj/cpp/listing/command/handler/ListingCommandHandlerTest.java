package uk.gov.moj.cpp.listing.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.listing.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate;
import uk.gov.moj.cpp.listing.event.AllocatedHearingUpdatedForListing;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.listing.event.CourtRoomAssignedToHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomChangedForHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomRemovedFromHearing;
import uk.gov.moj.cpp.listing.event.EstimateMinutesChangedForHearing;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.HearingUnallocatedForListing;
import uk.gov.moj.cpp.listing.event.JudgeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.JudgeChangedForHearing;
import uk.gov.moj.cpp.listing.event.JudgeRemovedFromHearing;
import uk.gov.moj.cpp.listing.event.NotBeforeSelectedForHearing;
import uk.gov.moj.cpp.listing.event.NotBeforeUnselectedForHearing;
import uk.gov.moj.cpp.listing.event.StartDateChangedForHearing;
import uk.gov.moj.cpp.listing.event.StartTimeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.StartTimeChangedForHearing;
import uk.gov.moj.cpp.listing.event.StartTimeRemovedFromHearing;
import uk.gov.moj.cpp.listing.event.TypeChangedForHearing;
import uk.gov.moj.cpp.listing.event.UnallocatedHearingListed;

import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingCommandHandlerTest {

    private static final String CASE_SENT_FOR_LISTING_EVENT = "listing.events.case-sent-for-listing";
    private static final String UNALLOCATED_HEARING_LISTED_EVENT = "listing.events.unallocated-hearing-listed";
    private static final String TYPE_CHANGED_FOR_HEARING_EVENT = "listing.events.type-changed-for-hearing";
    private static final String START_DATE_CHANGED_FOR_HEARING_EVENT = "listing.events.start-date-changed-for-hearing";
    private static final String ESTIMATE_MINUTES_CHANGED_FOR_HEARING_EVENT = "listing.events.estimate-minutes-changed-for-hearing";
    private static final String JUDGE_ASSIGNED_TO_HEARING_EVENT = "listing.events.judge-assigned-to-hearing";
    private static final String COURT_ROOM_ASSIGNED_TO_HEARING_EVENT = "listing.events.court-room-assigned-to-hearing";
    private static final String START_TIME_ASSIGNED_TO_HEARING_EVENT = "listing.events.start-time-assigned-to-hearing";
    private static final String NOT_BEFORE_SELECTED_FOR_HEARING_EVENT = "listing.events.not-before-selected-for-hearing";
    private static final String HEARING_ALLOCATED_FOR_LISTING_EVENT = "listing.events.hearing-allocated-for-listing";

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID1 = UUID.randomUUID();
    private static final UUID OFFENCE_ID1 = UUID.randomUUID();

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final String FIRST_NAME = "Test Recipe";
    private static final String LAST_NAME = "Last Name";
    private static final String DATE_OF_BIRTH = "1980-07-15";
    private static final String PTP_TYPE = "PTP";
    private static final String TRIAL_TYPE = "Trial";
    private static final String INITIAL_START_DATE = "2018-05-01";
    private static final String UPDATED_START_DATE = "2018-06-01";
    private static final String OFFENCE_START_DATE = "2018-06-01";
    private static final String OFFENCE_END_DATE = "2018-06-01";
    private static final int INITIAL_ESTIMATE_MINUTES = 360;
    private static final int UPDATED_ESTIMATE_MINUTES = 720;
    private static final String CONDITIONAL_BAIL_STATUS = "conditional";
    private static final String DEFENCE_ORGANISATION = "XYZ Organisation";
    private static final String URN = "urn";
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final String STATEMENT_OF_OFFENCE_TITLE = "title";
    private static final String STATEMENT_OF_OFFENCE_LEGISLATION = "Legislation";
    private static final String START_TIME = "10:30";
    private static final boolean NOT_BEFORE = true;
    private static final String CUSTODY_TIME_LIMIT = "2017-10-05";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream listHearingEventStream;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseSentForListing.class,
            UnallocatedHearingListed.class, TypeChangedForHearing.class, StartDateChangedForHearing.class,
            EstimateMinutesChangedForHearing.class, JudgeAssignedToHearing.class, JudgeChangedForHearing.class,
            JudgeRemovedFromHearing.class, CourtRoomAssignedToHearing.class, CourtRoomChangedForHearing.class,
            CourtRoomRemovedFromHearing.class, StartTimeAssignedToHearing.class, StartTimeChangedForHearing.class,
            StartTimeRemovedFromHearing.class, NotBeforeSelectedForHearing.class, NotBeforeUnselectedForHearing.class,
            HearingAllocatedForListing.class, AllocatedHearingUpdatedForListing.class, HearingUnallocatedForListing.class);

    @InjectMocks
    private ListingCommandHandler listingCommandHandler;

    @Test
    public void listingCommandHandlerShouldTriggerCaseSentForListingEvent() throws Exception {
        final JsonEnvelope commandEnvelope = sendCaseForListingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(new CaseAggregate());

        listingCommandHandler.sendCaseForListing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(CASE_SENT_FOR_LISTING_EVENT)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.caseId", equalTo(command.getString("caseId"))),
                                        withJsonPath("$.urn", equalTo(command.getString("urn"))),
                                        withJsonPath("$.hearings[0].id",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.hearings[0].courtCentreId",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("courtCentreId"))),
                                        withJsonPath("$.hearings[0].type",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("type"))),
                                        withJsonPath("$.hearings[0].startDate",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("startDate"))),
                                        withJsonPath("$.hearings[0].estimateMinutes",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getInt("estimateMinutes"))),
                                        withJsonPath("$.hearings[0].defendants[0].id",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.hearings[0].defendants[0].personId",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("personId"))),
                                        withJsonPath("$.hearings[0].defendants[0].firstName",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("firstName"))),
                                        withJsonPath("$.hearings[0].defendants[0].lastName",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("lastName"))),
                                        withJsonPath("$.hearings[0].defendants[0].dateOfBirth",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("dateOfBirth"))),
                                        withJsonPath("$.hearings[0].defendants[0].bailStatus",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("bailStatus"))),
                                        withJsonPath("$.hearings[0].defendants[0].defenceOrganisation",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0)
                                                        .getString("defenceOrganisation"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].id",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].offenceCode", equalTo
                                                (command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("offenceCode"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].startDate",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("endDate"))),

                                        withJsonPath("$.hearings[0].defendants[0].offences[0]" +
                                                        ".statementOfOffence.title",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getJsonObject
                                                                ("statementOfOffence").getString
                                                                ("title"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0]" +
                                                        ".statementOfOffence.legislation",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getJsonObject("statementOfOffence")
                                                        .getString("legislation")))
                                        )
                                )
                        )
                )
        );
    }


    @Test
    public void listingCommandHandlerShouldTriggerUnallocatedHearingListedEvent() throws Exception {
        final JsonEnvelope commandEnvelope = listHearingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(new HearingAggregate());

        listingCommandHandler.listHearing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(UNALLOCATED_HEARING_LISTED_EVENT)
                                .withCausationIds(commandEnvelope.metadata().id()),payload()
                                .isJson(allOf(
                                        withJsonPath("$.hearingId",
                                                equalTo(command.getString("hearingId"))),
                                        withJsonPath("$.caseId",
                                                equalTo(command.getString("caseId"))),
                                        withJsonPath("$.courtCentreId",
                                                equalTo(command.getString("courtCentreId"))),
                                        withJsonPath("$.type",
                                                equalTo(command.getString("type"))),
                                        withJsonPath("$.startDate",
                                                equalTo(command.getString("startDate"))),
                                        withJsonPath("$.estimateMinutes",
                                                equalTo(command.getInt("estimateMinutes"))),
                                        withJsonPath("$.defendants[0].id",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.defendants[0].personId",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("personId"))),
                                        withJsonPath("$.defendants[0].firstName",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("firstName"))),
                                        withJsonPath("$.defendants[0].lastName",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("lastName"))),
                                        withJsonPath("$.defendants[0].dateOfBirth",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("dateOfBirth"))),
                                        withJsonPath("$.defendants[0].bailStatus",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("bailStatus"))),
                                        withJsonPath("$.defendants[0].defenceOrganisation",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0)
                                                        .getString("defenceOrganisation"))),
                                        withJsonPath("$.defendants[0].offences[0].id",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.defendants[0].offences[0].offenceCode", equalTo
                                                (command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("offenceCode"))),
                                        withJsonPath("$.defendants[0].offences[0].startDate",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("endDate"))),

                                        withJsonPath("$.defendants[0].offences[0]" +
                                                        ".statementOfOffence.title",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getJsonObject
                                                                ("statementOfOffence").getString
                                                                ("title"))),
                                        withJsonPath("$.defendants[0].offences[0]" +
                                                        ".statementOfOffence.legislation",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getJsonObject("statementOfOffence")
                                                        .getString("legislation")))
                                        )
                                )
                        )
                )
        );
    }


    @Test
    public void listingCommandHandlerShouldOnlyTriggerEventsForDataThatHasChangedWhenUpdating() throws Exception {
        HearingAggregate hearingAggregate = new HearingAggregate();

        givenHearingHasBeenListed(hearingAggregate);

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final JsonEnvelope updateHearingEnvelope = updateHearingCommandEnvelopeWithOnlyMandatoryDataChanges();
        final JsonObject command = updateHearingEnvelope.payloadAsJsonObject();

        listingCommandHandler.updateHearingForListing(updateHearingEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(TYPE_CHANGED_FOR_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.type", equalTo(command.getString("type")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(START_DATE_CHANGED_FOR_HEARING_EVENT ),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.startDate", equalTo(command.getString("startDate")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(ESTIMATE_MINUTES_CHANGED_FOR_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.estimateMinutes", equalTo(command.getInt("estimateMinutes")))
                        )))));
    }

    @Test
    public void listingCommandHandlerShouldTriggerHearingAllocatedForListingEvent() throws Exception {
        HearingAggregate hearingAggregate = new HearingAggregate();

        givenHearingHasBeenListed(hearingAggregate);

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final JsonEnvelope updateHearingEnvelope = updateHearingCommandEnvelopeWithCompleteChanges();
        final JsonObject command = updateHearingEnvelope.payloadAsJsonObject();

        listingCommandHandler.updateHearingForListing(updateHearingEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(TYPE_CHANGED_FOR_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.type", equalTo(command.getString("type")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(START_DATE_CHANGED_FOR_HEARING_EVENT ),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.startDate", equalTo(command.getString("startDate")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(ESTIMATE_MINUTES_CHANGED_FOR_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.estimateMinutes", equalTo(command.getInt("estimateMinutes")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(START_TIME_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.startTime", equalTo(command.getString("startTime")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(NOT_BEFORE_SELECTED_FOR_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString()))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(JUDGE_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.judgeId", equalTo(command.getString("judgeId")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(COURT_ROOM_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.courtRoomId", equalTo(command.getString("courtRoomId")))
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(HEARING_ALLOCATED_FOR_LISTING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.type", equalTo(command.getString("type"))),
                                withJsonPath("$.startDate", equalTo(command.getString("startDate"))),
                                withJsonPath("$.estimateMinutes", equalTo(command.getInt("estimateMinutes"))),
                                withJsonPath("$.judgeId", equalTo(command.getString("judgeId"))),
                                withJsonPath("$.courtRoomId", equalTo(command.getString("courtRoomId"))),
                                withJsonPath("$.startTime", equalTo(command.getString("startTime"))),
                                withJsonPath("$.notBefore", equalTo(command.getBoolean("notBefore")))
                        )))));
    }

    private void givenHearingHasBeenListed(HearingAggregate hearingAggregate) throws Exception {
        when(eventSource.getStreamById(HEARING_ID)).thenReturn(listHearingEventStream);
        when(aggregateService.get(listHearingEventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final JsonEnvelope listHearingEnvelope = listHearingCommandEnvelope();
        listingCommandHandler.listHearing(listHearingEnvelope);
    }

    private JsonEnvelope sendCaseForListingCommandEnvelope() {
        JsonObject caseJson = createSendCaseForListingJson();
        return createEnvelope("listing.command.send-case-for-listing" , caseJson);
    }

    private JsonEnvelope listHearingCommandEnvelope() {
        JsonObject hearingJson = createListHearingJson();
        return createEnvelope("listing.command.list-hearing" , hearingJson);
    }

    private JsonEnvelope updateHearingCommandEnvelopeWithOnlyMandatoryDataChanges() {
        JsonObject hearingJson = createUpdateHearingJsonWhereOnlyMandatoryDataHasChanged();
        return createEnvelope("listing.command.update-hearing-for-listing" , hearingJson);
    }

    private JsonEnvelope updateHearingCommandEnvelopeWithCompleteChanges() {
        JsonObject hearingJson = createUpdateHearingJsonWhereAllDataHasChanged();
        return createEnvelope("listing.command.update-hearing-for-listing" , hearingJson);
    }

    private JsonObject createSendCaseForListingJson() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("urn", URN)
                .add("hearings", createHearingsJson())
                .build();
    }

    private JsonObject createListHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", PTP_TYPE)
                .add("startDate", INITIAL_START_DATE)
                .add("estimateMinutes", INITIAL_ESTIMATE_MINUTES)
                .add("caseId", CASE_ID.toString())
                .add("courtCentreId", COURT_CENTRE_ID.toString())
                .add("defendants", createDefendantsJson())
                .build();
    }

    private JsonObject createUpdateHearingJsonWhereOnlyMandatoryDataHasChanged() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TRIAL_TYPE)
                .add("startDate", UPDATED_START_DATE)
                .add("estimateMinutes", UPDATED_ESTIMATE_MINUTES)
                .build();
    }

    private JsonObject createUpdateHearingJsonWhereAllDataHasChanged() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TRIAL_TYPE)
                .add("startDate", UPDATED_START_DATE)
                .add("estimateMinutes", UPDATED_ESTIMATE_MINUTES)
                .add("judgeId", JUDGE_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("startTime", START_TIME)
                .add("notBefore", NOT_BEFORE)
                .build();
    }

    private JsonArray createHearingsJson() {
        JsonObject hearing =  createObjectBuilder()
                .add("id", HEARING_ID.toString())
                .add("courtCentreId", COURT_CENTRE_ID.toString())
                .add("type", PTP_TYPE)
                .add("startDate", INITIAL_START_DATE)
                .add("estimateMinutes", INITIAL_ESTIMATE_MINUTES)
                .add("defendants", createDefendantsJson())
                .build();

        JsonArrayBuilder hearings = createArrayBuilder().add(hearing);
        return hearings.build();
    }

    private JsonArray createDefendantsJson() {
        JsonObject defendant = createObjectBuilder()
                .add("id", DEFENDANT_ID1.toString())
                .add("personId", PERSON_ID.toString())
                .add("firstName", FIRST_NAME)
                .add("lastName", LAST_NAME)
                .add("dateOfBirth", DATE_OF_BIRTH)
                .add("bailStatus", CONDITIONAL_BAIL_STATUS)
                .add("custodyTimeLimit", CUSTODY_TIME_LIMIT)
                .add("defenceOrganisation", DEFENCE_ORGANISATION)
                .add("offences", createOffencesJson())
                .build();

        JsonArrayBuilder defendants = createArrayBuilder().add(defendant);

        return defendants.build();
    }

    private JsonArray createOffencesJson() {
        JsonObject statementOfOffence = createObjectBuilder()
                .add("title", STATEMENT_OF_OFFENCE_TITLE)
                .add("legislation", STATEMENT_OF_OFFENCE_LEGISLATION)
                .build();

        JsonObject offence = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("offenceCode", PERSON_ID.toString())
                .add("startDate", OFFENCE_START_DATE)
                .add("endDate", OFFENCE_END_DATE)
                .add("statementOfOffence", statementOfOffence)
                .build();

        JsonArrayBuilder offences = createArrayBuilder().add(offence);

        return offences.build();
    }
}

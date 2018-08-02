package uk.gov.moj.cpp.listing.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import org.mockito.*;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.*;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ListingCommandHandlerTest {

    public static final String ADDRESS_LINE_1 = "Address line 1";
    private static final String CASE_SENT_FOR_LISTING_EVENT = "listing.events.case-sent-for-listing";
    private static final String DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated";
    private static final String OFFENCE_UPDATED = "listing.events.offence-updated";
    private static final String OFFENCE_ADDED = "listing.events.offence-added";
    private static final String OFFENCE_DELETED = "listing.events.offence-deleted";
    private static final String OFFENCES_TO_BE_UPDATED = "listing.events.offences-to-be-updated";
    private static final String OFFENCES_TO_BE_ADDED = "listing.events.offences-to-be-added";
    private static final String OFFENCES_TO_BE_DELETED = "listing.events.offences-to-be-deleted";
    private static final String DEFENDANT_DETAILS_UPDATED = "listing.events.defendant-details-updated";
    private static final String HEARING_LISTED_EVENT = "listing.events.hearing-listed";
    private static final String TYPE_CHANGED_FOR_HEARING_EVENT = "listing.events.type-changed-for-hearing";
    private static final String START_DATE_CHANGED_FOR_HEARING_EVENT = "listing.events.start-date-changed-for-hearing";
    private static final String END_DATE_ASSIGNED_TO_HEARING = "listing.events.end-date-assigned-to-hearing";
    private static final String START_TIMES_ASSIGNED_TO_HEARING_EVENT = "listing.events.start-times-assigned-to-hearing";
    private static final String JUDGE_ASSIGNED_TO_HEARING_EVENT = "listing.events.judge-assigned-to-hearing";
    private static final String COURT_ROOM_ASSIGNED_TO_HEARING_EVENT = "listing.events.court-room-assigned-to-hearing";
    private static final String NON_SITTING_DAY_ASSIGNED_TO_HEARING_EVENT = "listing.events.non-sitting-days-assigned-to-hearing";
    private static final String HEARING_ALLOCATED_FOR_LISTING_EVENT = "listing.events.hearing-allocated-for-listing";

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID1 = UUID.randomUUID();
    private static final UUID OFFENCE_ID1 = UUID.randomUUID();

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID ADDED_OFFENCE_CASE_ID = UUID.randomUUID();
    private static final UUID UPDATED_OFFENCE_CASE_ID = UUID.randomUUID();
    private static final UUID DELETED_OFEENCE_CASE_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final String FIRST_NAME = "Test Recipe";
    private static final String LAST_NAME = "Last Name";
    private static final String DATE_OF_BIRTH = "1980-07-15";
    private static final String PTP_TYPE = "PTP";
    private static final String SENTENCE_TYPE = "Sentence";
    private static final String INITIAL_START_DATE = "2018-05-30";
    private static final String END_DATE = "2018-06-03";
    private static final String UPDATED_START_DATE = "2018-06-01";
    private static final String UPDATED_END_DATE = "2018-06-03";
    private static final String UPDATED_START_TIME = "2018-06-01T11:30:00Z";
    private static final String OFFENCE_START_DATE = "2018-06-01";
    private static final String OFFENCE_END_DATE = "2018-06-07";
    private static final String NON_SITTING_DAY = "2018-06-02";
    private static final int INITIAL_ESTIMATE_MINUTES = 360;
    private static final int UPDATED_ESTIMATE_MINUTES = 720;
    private static final String DEFENCE_ORGANISATION = "XYZ Organisation";
    private static final String URN = "urn";
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final String STATEMENT_OF_OFFENCE_TITLE = "title";
    private static final String STATEMENT_OF_OFFENCE_LEGISLATION = "Legislation";
    private static final String START_TIME = "10:30";
    private static final String CUSTODY_TIME_LIMIT = "2017-10-05";
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final String ADDRESS_LINE_2 = "Address line 1";
    private static final String ADDRESS_LINE_3 = "Address line 1";
    private static final String ADDRESS_LINE_4 = "Address line 1";
    private static final String POSTCODE = "Postcode";
    private static final String ADDRESS_ID = "2bf82a1d-159a-49a9-9df4-bd78ad678938";
    private static final String PERSON_TITLE = "title";
    private static final String PERSON_FIRST_NAME1 = "firstName";
    private static final String PERSON_LAST_NAME1 = "lastName";
    private static final String PERSON_DOB = "1980-06-01";
    private static final String PERSON_NATIONALITY = "La La Land";
    private static final String PERSON_GENDER = "gender";
    private static final String PERSON_HOME_TELEPHONE = "04948938938";
    private static final String PERSON_WORK_TELEPHONE = "04838394833";
    private static final String PERSON_MOBILE = "049837848383";
    private static final String PERSON_FAX = "04593849393";
    private static final String PERSON_EMAIL = "email@email.com";
    private static final String MODIFIED_DATE = "2018-01-01";
    private static final String OFFENCE_WORDING = "wording";
    private static final int OFFENCE_COUNT = 1;
    private static final String OFFENCE_CONVICTION_DATE = "2017-10-05";


    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream listHearingEventStream;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventStream updatedEventStream;

    @Mock
    private EventStream deletedEventStream;

    @Mock
    private EventStream addedEventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseSentForListing.class,
            HearingListed.class, TypeChangedForHearing.class, StartDateChangedForHearing.class,
            JudgeAssignedToHearing.class, JudgeChangedForHearing.class,
            JudgeRemovedFromHearing.class, CourtRoomAssignedToHearing.class, CourtRoomChangedForHearing.class,
            CourtRoomRemovedFromHearing.class, StartTimesAssignedToHearing.class, StartTimesChangedForHearing.class,
            HearingAllocatedForListing.class, EndDateAssignedToHearing.class, EndDateChangedForHearing.class,
            NonSittingDaysAssignedToHearing.class, NonSittingDaysChangedForHearing.class,
            AllocatedHearingUpdatedForListing.class, HearingUnallocatedForListing.class, DefendantsToBeUpdated.class,
            DefendantDetailsUpdated.class, OffencesToBeUpdated.class, OffencesToBeAdded.class, OffencesToBeDeleted.class,
            ArrayList.class, OffenceUpdated.class, OffenceAdded.class, OffenceDeleted.class);

    @InjectMocks
    private ListingCommandHandler listingCommandHandler;


    private boolean hasCustodyTimeLimit = true;

    @Test
    public void listingCommandHandlerShouldTriggerCaseSentForListingEvent() throws Exception {
        givenEventStream(CASE_ID, eventStream, new Case(), Case.class);

        final JsonEnvelope commandEnvelope = sendCaseForListingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

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
                                        withJsonPath("$.hearings[0].defendants[0].custodyTimeLimit",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("custodyTimeLimit"))),
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
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].offenceCode",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("offenceCode"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].startDate",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("startDate"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].endDate",
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
                                )))//TODO reintroduce .thatMatchesSchema()
                )
        );
    }

    @Test
    public void listingCommandHandlerShouldTriggerDefendantsToBeUpdatedEvent() throws Exception {
        Case aCase = givenCaseSentForListing(CASE_ID);
        givenEventStream(CASE_ID, eventStream, aCase, Case.class);

        final JsonEnvelope commandEnvelope = updateCaseDefendantDetailsCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.updateCaseDefendantDetails(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(DEFENDANTS_TO_BE_UPDATED)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.hearings", hasSize(1)),
                                        withJsonPath("$.defendants[0].id",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.defendants[0].personId",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonObject("person").getString("id"))),
                                        withJsonPath("$.defendants[0].firstName",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonObject("person").getString("firstName"))),
                                        withJsonPath("$.defendants[0].lastName",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonObject("person").getString("lastName"))),
                                        withJsonPath("$.defendants[0].dateOfBirth",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonObject("person").getString("dateOfBirth"))),
                                        withJsonPath("$.defendants[0].bailStatus",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("bailStatus"))),
                                        withJsonPath("$.defendants[0].custodyTimeLimit",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("custodyTimeLimitDate"))),
                                        withJsonPath("$.defendants[0].defenceOrganisation",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("defenceOrganisation")))
                                )))//TODO reintroduce .thatMatchesSchema()
                )
        );
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceUpdatedEvents() throws Exception {
        Hearing hearing = givenHearingListed(HEARING_ID);
        givenEventStream(HEARING_ID, eventStream, hearing, Hearing.class);

        final JsonEnvelope commandEnvelope = updateOffencesForHearingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.updateOffencesForHearing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(OFFENCE_UPDATED)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.offence.id",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.offence.offenceCode",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("offenceCode"))),
                                        withJsonPath("$.offence.defendantId",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("defendantId"))),
                                        withJsonPath("$.offence.startDate",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("startDate"))),
                                        withJsonPath("$.offence.endDate",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("endDate"))),
                                        withJsonPath("$.offence.statementOfOffence.title",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0)
                                                        .getJsonObject("statementOfOffence")
                                                        .getString("title"))),
                                        withJsonPath("$.offence.statementOfOffence.legislation",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0)
                                                        .getJsonObject("statementOfOffence")
                                                        .getString("legislation")))
                                )))//TODO reintroduce .thatMatchesSchema()
                )
        );
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceDeletedEvents() throws Exception {
        Hearing hearing = givenHearingListed(HEARING_ID);
        givenEventStream(HEARING_ID, eventStream, hearing, Hearing.class);

        final JsonEnvelope commandEnvelope = deleteOffencesForHearingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.deleteOffencesForHearing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(OFFENCE_DELETED)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.offenceId",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.defendantId",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("defendantId")))
                                )))//TODO reintroduce .thatMatchesSchema()
                )
        );
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceAddedEvents() throws Exception {
        Hearing hearing = givenHearingListed(HEARING_ID);
        givenEventStream(HEARING_ID, eventStream, hearing, Hearing.class);

        final JsonEnvelope commandEnvelope = addOffencesForHearingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.addOffencesForHearing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(OFFENCE_ADDED)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.offence.id",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.offence.offenceCode",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("offenceCode"))),
                                        withJsonPath("$.offence.defendantId",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("defendantId"))),
                                        withJsonPath("$.offence.startDate",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("startDate"))),
                                        withJsonPath("$.offence.endDate",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0).getString("endDate"))),
                                        withJsonPath("$.offence.statementOfOffence.title",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0)
                                                        .getJsonObject("statementOfOffence")
                                                        .getString("title"))),
                                        withJsonPath("$.offence.statementOfOffence.legislation",
                                                equalTo(command.getJsonArray("offences")
                                                        .getJsonObject(0)
                                                        .getJsonObject("statementOfOffence")
                                                        .getString("legislation")))
                                )))//TODO reintroduce .thatMatchesSchema()
                )
        );
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceUpdateEvents() throws Exception {
        Case updatedCase = givenCaseSentForListing(UPDATED_OFFENCE_CASE_ID);
        Case deletedCase = givenCaseSentForListing(DELETED_OFEENCE_CASE_ID);
        Case addedCase = givenCaseSentForListing(ADDED_OFFENCE_CASE_ID);
        givenEventStream(UPDATED_OFFENCE_CASE_ID, updatedEventStream, updatedCase, Case.class);
        givenEventStream(DELETED_OFEENCE_CASE_ID, deletedEventStream, deletedCase, Case.class);
        givenEventStream(ADDED_OFFENCE_CASE_ID, addedEventStream, addedCase, Case.class);

        final JsonEnvelope commandEnvelope = updateCaseDefendantOffencesCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.updateCaseDefendantOffences(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(updatedEventStream),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(OFFENCES_TO_BE_UPDATED)
                                        .withCausationIds(commandEnvelope.metadata().id()), payload()
                                        .isJson(allOf(
                                                withJsonPath("$.hearings", hasSize(1)),
                                                withJsonPath("$.offences[0].defendantId",
                                                        equalTo(command.getJsonArray("updatedOffences")
                                                                .getJsonObject(0).getString("defendantId"))),
                                                withJsonPath("$.offences[0].endDate",
                                                        equalTo(command.getJsonArray("updatedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("endDate"))),
                                                withJsonPath("$.offences[0].startDate",
                                                        equalTo(command.getJsonArray("updatedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("startDate"))),
                                                withJsonPath("$.offences[0].offenceCode",
                                                        equalTo(command.getJsonArray("updatedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("offenceCode"))),
                                                withJsonPath("$.offences[0].statementOfOffence.title",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getJsonObject("statementOfOffence")
                                                                .getString("title"))),
                                                withJsonPath("$.offences[0].statementOfOffence.legislation",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getJsonObject("statementOfOffence")
                                                                .getString("legislation"))),
                                                withJsonPath("$.offences[0].id",
                                                        equalTo(command.getJsonArray("updatedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("id")))
                                        )))//TODO reintroduce .thatMatchesSchema()

                )
        );

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(addedEventStream),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(OFFENCES_TO_BE_ADDED)
                                        .withCausationIds(commandEnvelope.metadata().id()), payload()
                                        .isJson(allOf(
                                                withJsonPath("$.hearings", hasSize(1)),
                                                withJsonPath("$.offences[0].defendantId",
                                                        equalTo(command.getJsonArray("addedOffences")
                                                                .getJsonObject(0).getString("defendantId"))),
                                                withJsonPath("$.offences[0].endDate",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("endDate"))),
                                                withJsonPath("$.offences[0].startDate",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("startDate"))),
                                                withJsonPath("$.offences[0].offenceCode",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("offenceCode"))),
                                                withJsonPath("$.offences[0].statementOfOffence.title",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getJsonObject("statementOfOffence")
                                                                .getString("title"))),
                                                withJsonPath("$.offences[0].statementOfOffence.legislation",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getJsonObject("statementOfOffence")
                                                                .getString("legislation"))),
                                                withJsonPath("$.offences[0].id",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("id")))
                                        )))//TODO reintroduce .thatMatchesSchema()

                )
        );

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(deletedEventStream),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(OFFENCES_TO_BE_DELETED)
                                        .withCausationIds(commandEnvelope.metadata().id()), payload()
                                        .isJson(allOf(
                                                withJsonPath("$.hearings", hasSize(1)),
                                                withJsonPath("$.offences[0].defendantId",
                                                        equalTo(command.getJsonArray("addedOffences")
                                                                .getJsonObject(0).getString("defendantId"))),

                                                withJsonPath("$.offences[0].id",
                                                        equalTo(command.getJsonArray("addedOffences").getJsonObject(0)
                                                                .getJsonArray("offences").getJsonObject(0)
                                                                .getString("id")))
                                        )))//TODO reintroduce .thatMatchesSchema()

                )
        );
    }

    @Test
    public void listingCommandHandlerShouldTriggerDefendantDetailsUpdatedEvents() throws Exception {
        Hearing hearing = givenHearingListed(HEARING_ID);
        givenEventStream(HEARING_ID, eventStream, hearing, Hearing.class);

        final JsonEnvelope commandEnvelope = updateDefendantsForHearingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.updateDefendantsForHearing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(DEFENDANT_DETAILS_UPDATED)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.hearingId", equalTo(command.getString("hearingId"))),

                                        withJsonPath("$.defendant.id",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.defendant.personId",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("personId"))),
                                        withJsonPath("$.defendant.firstName",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("firstName"))),
                                        withJsonPath("$.defendant.lastName",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("lastName"))),
                                        withJsonPath("$.defendant.dateOfBirth",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("dateOfBirth"))),
                                        withJsonPath("$.defendant.bailStatus",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("bailStatus"))),
                                        withJsonPath("$.defendant.custodyTimeLimit",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("custodyTimeLimit"))),
                                        withJsonPath("$.defendant.defenceOrganisation",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getString("defenceOrganisation")))
                                )))//TODO reintroduce .thatMatchesSchema()
                )
        );
    }

    private Case givenCaseSentForListing(UUID caseId) {

        final uk.gov.justice.listing.events.Hearing hearing = EventBuilder.buildHearing();
        List<uk.gov.justice.listing.events.Hearing> hearings = Collections.singletonList(hearing);
        return new Case() {{
            apply(Stream.of(new CaseSentForListing(caseId, hearings, URN)));
        }};
    }

    private Hearing givenHearingListed(UUID hearingId) {
        return new Hearing() {{
            apply(Stream.of(new HearingListed(
                    CASE_ID,
                    COURT_CENTRE_ID,
                    COURT_ROOM_ID,
                    EventBuilder.buildDefendants(DEFENDANT_ID1, OFFENCE_ID1),
                    LocalDate.now().plusDays(10),
                    INITIAL_ESTIMATE_MINUTES,
                    hearingId,
                    JUDGE_ID,
                    LocalDates.from(INITIAL_START_DATE),
                    Collections.singletonList(ZonedDateTime.parse(UPDATED_START_TIME)),
                    PTP_TYPE,
                    URN
            )));
        }};
    }

    @Test
    public void listingCommandHandlerShouldTriggerReListingCaseSentForListingEvent() throws Exception {
        givenEventStream(CASE_ID, eventStream, new Case(), Case.class);

        final JsonEnvelope commandEnvelope = sendReListedCaseForListingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

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
                                        withJsonPath("$.hearings[0].courtRoomId",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("courtRoomId"))),
                                        withJsonPath("$.hearings[0].judgeId",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("judgeId"))),
                                        withJsonPath("$.hearings[0].startTime",
                                                equalTo(command.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("startTime")))

                                )))//TODO reintroduce .thatMatchesSchema()
                )
        );
    }


    @Test
    public void listingCommandHandlerShouldTriggerHearingListedEvent() throws Exception {
        givenEventStream(HEARING_ID, eventStream, new Hearing(), Hearing.class);

        hasCustodyTimeLimit = false;
        final JsonEnvelope commandEnvelope = listHearingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.listHearing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(HEARING_LISTED_EVENT)
                                .withCausationIds(commandEnvelope.metadata().id()),payload()
                                .isJson(allOf(
                                        withJsonPath("$.hearingId",
                                                equalTo(command.getString("hearingId"))),
                                        withJsonPath("$.caseId",
                                                equalTo(command.getString("caseId"))),
                                        withJsonPath("$.urn",
                                                equalTo(command.getString("urn"))),
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
                                        withoutJsonPath("$.defendants[0].custodyTimeLimit"),
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
                                        withJsonPath("$.defendants[0].offences[0].offenceCode",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("offenceCode"))),
                                        withJsonPath("$.defendants[0].offences[0].startDate",
                                                equalTo(command.getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("startDate"))),
                                        withJsonPath("$.defendants[0].offences[0].endDate",
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
                                        ))) //TODO reintroduce .thatMatchesSchema()
                )
        );
      
    }

    @Test
    public void listingCommandHandlerShouldTriggerHearingListedEventAfterRelisting() throws Exception {
        givenEventStream(HEARING_ID, eventStream, new Hearing(), Hearing.class);

        final JsonEnvelope commandEnvelope = relistHearingCommandEnvelope();
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.listHearing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(HEARING_LISTED_EVENT)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.hearingId",
                                                equalTo(command.getString("hearingId"))),
                                        withJsonPath("$.startDate",
                                                equalTo(command.getString("startDate"))),
                                        withJsonPath("$.endDate",
                                                equalTo(command.getString("endDate"))),
                                        withJsonPath("$.startTimes[0]",
                                                equalTo(ZonedDateTimes.toString(ZonedDateTime.of(LocalDate.parse(command.getString("startDate")),
                                                        LocalTime.parse(command.getString("startTime")),
                                                        BST)))),
                                        withJsonPath("$.courtRoomId",
                                                equalTo(command.getString("courtRoomId"))),
                                        withJsonPath("$.judgeId",
                                                equalTo(command.getString("judgeId")))
                                ))),//TODO reintroduce .thatMatchesSchema()
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(HEARING_ALLOCATED_FOR_LISTING_EVENT),
                                payloadIsJson(CoreMatchers.allOf(
                                        withJsonPath("$.hearingId",  equalTo(command.getString("hearingId"))),
                                        withJsonPath("$.type", equalTo(command.getString("type"))),
                                        withJsonPath("$.estimateMinutes", equalTo(command.getInt("estimateMinutes"))),
                                        withJsonPath("$.judgeId", equalTo(command.getString("judgeId"))),
                                        withJsonPath("$.courtRoomId", equalTo(command.getString("courtRoomId"))),
                                        withJsonPath("$.courtCentreId",  equalTo(command.getString("courtCentreId"))),
                                        withJsonPath("$.defendantsOffenceIds[0].id", equalTo(DEFENDANT_ID1.toString())),
                                        withJsonPath("$.defendantsOffenceIds[0].offenceIds[0]", equalTo(OFFENCE_ID1.toString())),
                                        withJsonPath("$.hearingDate.startDate", equalTo(command.getString("startDate"))),
                                        withJsonPath("$.hearingDate.startTime", equalTo(command.getString("startTime")))
                                )))  //TODO reintroduce .thatMatchesSchema() test when Techpod introduce json schema test resolver in next framework release
                )
        );

    }

    @Test
    public void listingCommandHandlerShouldOnlyTriggerEventsForDataThatHasChangedWhenUpdating() throws Exception {
        Hearing hearing = new Hearing();
        givenHearingHasBeenListed(hearing);
        givenEventStream(HEARING_ID, eventStream, hearing, Hearing.class);

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
                        ))), //TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(START_DATE_CHANGED_FOR_HEARING_EVENT ),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.startDate", equalTo(command.getString("startDate")))
                        ))),//TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(START_TIMES_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.startTimes[0]",
                                        equalTo(getStartTime(command)))
                        ))), //TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(END_DATE_ASSIGNED_TO_HEARING),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.endDate", equalTo(command.getString("endDate")))
                        ))),//TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(NON_SITTING_DAY_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.nonSittingDays", hasSize(0))
                        )))//TODO reintroduce .thatMatchesSchema()
        ));
    }

    @Test
    public void listingCommandHandlerShouldTriggerHearingAllocatedForListingEvent() throws Exception {
        Hearing hearing = new Hearing();

        givenHearingHasBeenListed(hearing);
        givenEventStream(HEARING_ID, eventStream, hearing, Hearing.class);

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
                        ))),//TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(START_DATE_CHANGED_FOR_HEARING_EVENT ),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.startDate", equalTo(command.getString("startDate")))
                        ))), //TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(START_TIMES_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.startTimes[0]", equalTo(getStartTime(command)))
                        ))), //TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(END_DATE_ASSIGNED_TO_HEARING ),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                               withJsonPath("$.endDate", equalTo(command.getString("endDate")))
                        ))), //TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(NON_SITTING_DAY_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.nonSittingDays[0]",
                                        equalTo(((JsonString)command.getJsonArray("nonSittingDays").get(0)).getString()))

                        ))),//TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(JUDGE_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.judgeId", equalTo(command.getString("judgeId")))
                        ))),//TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(COURT_ROOM_ASSIGNED_TO_HEARING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.courtRoomId", equalTo(command.getString("courtRoomId")))
                        ))), //TODO reintroduce .thatMatchesSchema()
                jsonEnvelope(
                        withMetadataEnvelopedFrom(updateHearingEnvelope)
                                .withName(HEARING_ALLOCATED_FOR_LISTING_EVENT),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                                withJsonPath("$.type", equalTo(command.getString("type"))),
                                withJsonPath("$.judgeId", equalTo(command.getString("judgeId"))),
                                withJsonPath("$.courtRoomId", equalTo(command.getString("courtRoomId"))),
                                withJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.defendantsOffenceIds[0].id", equalTo(DEFENDANT_ID1.toString())),
                                withJsonPath("$.defendantsOffenceIds[0].offenceIds[0]", equalTo(OFFENCE_ID1.toString())),
                                withJsonPath("$.hearingDate.startDate", equalTo(command.getString("startDate"))),
                                withJsonPath("$.hearingDate.startTime",
                                        equalTo(ZonedDateTime.parse(UPDATED_START_TIME).withZoneSameInstant(BST).toLocalTime().toString()))
                        )))     //TODO reintroduce .thatMatchesSchema() test when Techpod introduce json schema test resolver in next framework release
        ));
    }

    private String getStartTime(JsonObject command) {
        return ZonedDateTimes.toString(ZonedDateTime.parse(command.getJsonArray("startTimes").getString(0)));
    }

    private void givenHearingHasBeenListed(Hearing hearing) throws Exception {
        when(eventSource.getStreamById(HEARING_ID)).thenReturn(listHearingEventStream);
        when(aggregateService.get(listHearingEventStream, Hearing.class)).thenReturn(hearing);

        final JsonEnvelope listHearingEnvelope = listHearingCommandEnvelope();
        listingCommandHandler.listHearing(listHearingEnvelope);
    }

    private <T extends Aggregate> void givenEventStream(UUID id, EventStream eventStream, T aggregate, Class<T> clz) {
        when(this.eventSource.getStreamById(id)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, clz)).thenReturn(aggregate);
    }

    private JsonEnvelope sendCaseForListingCommandEnvelope() {
        JsonObject caseJson = createSendCaseForListingJson();
        return createEnvelope("listing.command.send-case-for-listing" , caseJson);
    }

    private JsonEnvelope updateDefendantsForHearingCommandEnvelope() {
        JsonObject caseJson = createUpdateDefendantsForHearingJson();
        return createEnvelope("listing.command.update-defendants-for-hearing" , caseJson);
    }

    private JsonEnvelope deleteOffencesForHearingCommandEnvelope() {
        JsonObject caseJson = createDeleteOffencesForHearingJson();
        return createEnvelope("listing.command.delete-offences-for-hearing" , caseJson);
    }

    private JsonEnvelope updateOffencesForHearingCommandEnvelope() {
        JsonObject caseJson = createUpdateOffencesForHearingJson();
        return createEnvelope("listing.command.update-offences-for-hearing" , caseJson);
    }

    private JsonEnvelope addOffencesForHearingCommandEnvelope() {
        JsonObject caseJson = createAddOffencesForHearingJson();
        return createEnvelope("listing.command.add-offences-for-hearing" , caseJson);
    }

    private JsonEnvelope updateCaseDefendantDetailsCommandEnvelope() {
        JsonObject caseJson = createUpdateCaseDefendantDetailsJson();
        return createEnvelope("listing.command.update-case-defendant-details" , caseJson);
    }
    private JsonEnvelope updateCaseDefendantOffencesCommandEnvelope() {
        JsonObject caseJson = createUpdateCaseDefendantOffencesJson();
        return createEnvelope("listing.command.update-case-defendant-offences" , caseJson);
    }

    private JsonEnvelope sendReListedCaseForListingCommandEnvelope() {
        JsonObject caseJson = createReListedSendCaseForListingJson();
        return createEnvelope("listing.command.send-case-for-listing" , caseJson);
    }


    private JsonEnvelope listHearingCommandEnvelope() {
        JsonObject hearingJson = createCommandListHearingJson();
        return createEnvelope("listing.command.list-hearing" , hearingJson);
    }

    private JsonEnvelope relistHearingCommandEnvelope() {
        JsonObject hearingJson = createCommandReListHearingJson();
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

    private JsonObject  createSendCaseForListingJson() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("urn", URN)
                .add("hearings", createHearingsJson())
                .build();
    }

    private JsonObject  createUpdateDefendantsForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("defendants", createDefendantsJson())
                .build();
    }

    private JsonObject  createUpdateOffencesForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("offences", createAddedOrUpdatedOffencesForHearing())
                .build();
    }

    private JsonObject  createDeleteOffencesForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("offences", createDeletedOffencesForHearing())
                .build();
    }

    private JsonObject  createAddOffencesForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("offences", createAddedOrUpdatedOffencesForHearing())
                .build();
    }

    private JsonObject createUpdateCaseDefendantDetailsJson() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendants", createProgressionDefendantsJson())
                .build();
    }
    private JsonObject createUpdateCaseDefendantOffencesJson() {
        return createObjectBuilder()
                .add("modifiedDate", MODIFIED_DATE)
                .add("updatedOffences", createUpdatedCaseDefendantOffences())
                .add("deletedOffences", createDeletedCaseDefendantOffences())
                .add("addedOffences", createAddedCaseDefendantOffences())
                .build();
    }

    private JsonArray createUpdatedCaseDefendantOffences() {
        JsonObject updatedCase = createObjectBuilder()
                .add("caseId", UPDATED_OFFENCE_CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .add("offences", createAddedOrUpdatedOffences())
                .build();

        return createArrayBuilder().add(updatedCase).build();

    }

    private JsonArray createDeletedCaseDefendantOffences() {
        JsonObject deletedCase =  createObjectBuilder()
                .add("caseId", DELETED_OFEENCE_CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .add("offences", createDeletedOffences())
                .build();

        return createArrayBuilder().add(deletedCase).build();
    }

    private JsonArray createAddedCaseDefendantOffences() {
        JsonObject addedCase = createObjectBuilder()
                .add("caseId", ADDED_OFFENCE_CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .add("offences", createAddedOrUpdatedOffences())
                .build();

        return createArrayBuilder().add(addedCase).build();
    }

    private JsonObject createReListedSendCaseForListingJson() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("urn", URN)
                .add("hearings", createReListedHearingsJson())
                .build();
    }

    private JsonObject createCommandListHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", PTP_TYPE)
                .add("startDate", INITIAL_START_DATE)
                .add("estimateMinutes", INITIAL_ESTIMATE_MINUTES)
                .add("caseId", CASE_ID.toString())
                .add("courtCentreId", COURT_CENTRE_ID.toString())
                .add("urn", URN)
                .add("defendants", createDefendantsJson())
                .build();
    }

    private JsonObject createCommandReListHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", PTP_TYPE)
                .add("startDate", INITIAL_START_DATE)
                .add("endDate", END_DATE)
                .add("startTime", START_TIME)
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("judgeId", JUDGE_ID.toString())
                .add("estimateMinutes", INITIAL_ESTIMATE_MINUTES)
                .add("caseId", CASE_ID.toString())
                .add("courtCentreId", COURT_CENTRE_ID.toString())
                .add("urn", URN)
                .add("defendants", createDefendantsJson())
                .build();
    }

    private JsonObject createUpdateHearingJsonWhereOnlyMandatoryDataHasChanged() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", SENTENCE_TYPE)
                .add("startDate", UPDATED_START_DATE)
                .add("endDate", UPDATED_END_DATE)
                .add("startTimes", createStartTimesJson())
                .add("nonSittingDays", createArrayBuilder().build())
                .add("estimateMinutes", UPDATED_ESTIMATE_MINUTES)
                .build();
    }

    private JsonArray createStartTimesJson() {
        return createArrayBuilder().add(UPDATED_START_TIME).build();
    }

    private JsonArray createNonSittingDays() {
        return createArrayBuilder().add(NON_SITTING_DAY).build();
    }



    private JsonObject createUpdateHearingJsonWhereAllDataHasChanged() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", SENTENCE_TYPE)
                .add("startDate", UPDATED_START_DATE)
                .add("endDate", END_DATE)
                .add("startTimes", createStartTimesJson())
                .add("nonSittingDays", createNonSittingDays())
                .add("judgeId", JUDGE_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
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

    private JsonArray createReListedHearingsJson() {
        JsonObject hearing =  createObjectBuilder()
                .add("id", HEARING_ID.toString())
                .add("courtCentreId", COURT_CENTRE_ID.toString())
                .add("type", PTP_TYPE)
                .add("startDate", INITIAL_START_DATE)
                .add("estimateMinutes", INITIAL_ESTIMATE_MINUTES)
                .add("startTime", START_TIME)
                .add("judgeId", JUDGE_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("defendants", createDefendantsJson())
                .build();

        JsonArrayBuilder hearings = createArrayBuilder().add(hearing);
        return hearings.build();
    }

    private JsonArray createDefendantsJson() {
        JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("id", DEFENDANT_ID1.toString())
                .add("personId", PERSON_ID.toString())
                .add("firstName", FIRST_NAME)
                .add("lastName", LAST_NAME)
                .add("dateOfBirth", DATE_OF_BIRTH)
                .add("bailStatus", BailStatus.CONDITIONAL.toString())
                .add("defenceOrganisation", DEFENCE_ORGANISATION)
                .add("offences", createAddedOrUpdatedOffences());

        if(hasCustodyTimeLimit) {
            defendantBuilder.add("custodyTimeLimit", CUSTODY_TIME_LIMIT);
        }
        JsonArrayBuilder defendants = createArrayBuilder().add(defendantBuilder.build());

        return defendants.build();
    }

    private JsonArray createProgressionDefendantsJson() {
        JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("id", DEFENDANT_ID1.toString())
                .add("bailStatus", BailStatus.CONDITIONAL.toString())
                .add("defenceOrganisation", DEFENCE_ORGANISATION)
                .add("person", createProgressionPersonJson());

        if(hasCustodyTimeLimit) {
            defendantBuilder.add("custodyTimeLimitDate", CUSTODY_TIME_LIMIT);
        }
        JsonArrayBuilder defendants = createArrayBuilder().add(defendantBuilder.build());

        return defendants.build();
    }

    private JsonObject createProgressionPersonJson() {
        return createObjectBuilder()
                .add("id", PERSON_ID.toString())
                .add("title", PERSON_TITLE)
                .add("firstName", PERSON_FIRST_NAME1)
                .add("lastName", PERSON_LAST_NAME1)
                .add("dateOfBirth", PERSON_DOB)
                .add("nationality", PERSON_NATIONALITY)
                .add("gender", PERSON_GENDER)
                .add("homeTelephone", PERSON_HOME_TELEPHONE)
                .add("workTelephone", PERSON_WORK_TELEPHONE)
                .add("mobile", PERSON_MOBILE)
                .add("fax", PERSON_FAX)
                .add("email", PERSON_EMAIL)
                .add("address", createAddressJson())
                .build();
    }

    private JsonObject createAddressJson() {
        return createObjectBuilder()
                .add("addressId", ADDRESS_ID)
                .add("address1", ADDRESS_LINE_1)
                .add("address1", ADDRESS_LINE_2)
                .add("address1", ADDRESS_LINE_3)
                .add("address1", ADDRESS_LINE_4)
                .add("postCode", POSTCODE)
                .build();
    }

    private JsonArray createDeletedOffencesForHearing() {

        JsonObject offence = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .build();

        JsonArrayBuilder offences = createArrayBuilder().add(offence);

        return offences.build();
    }

    private JsonArray createDeletedOffences() {

        JsonArrayBuilder offences = createArrayBuilder().add(OFFENCE_ID1.toString());

        return offences.build();
    }

    private JsonArray createAddedOrUpdatedOffences() {
        JsonObject statementOfOffence = createObjectBuilder()
                .add("title", STATEMENT_OF_OFFENCE_TITLE)
                .add("legislation", STATEMENT_OF_OFFENCE_LEGISLATION)
                .build();

        JsonObject offence = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("wording", OFFENCE_WORDING)
                .add("offenceCode", PERSON_ID.toString())
                .add("startDate", OFFENCE_START_DATE)
                .add("endDate", OFFENCE_END_DATE)
                .add("count", OFFENCE_COUNT)
                .add("convictionDate", OFFENCE_CONVICTION_DATE)
                .add("statementOfOffence", statementOfOffence)
                .build();

        JsonArrayBuilder offences = createArrayBuilder().add(offence);

        return offences.build();
    }

    private JsonArray createAddedOrUpdatedOffencesForHearing() {
        JsonObject statementOfOffence = createObjectBuilder()
                .add("title", STATEMENT_OF_OFFENCE_TITLE)
                .add("legislation", STATEMENT_OF_OFFENCE_LEGISLATION)
                .build();

        JsonObject offence = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .add("offenceCode", PERSON_ID.toString())
                .add("startDate", OFFENCE_START_DATE)
                .add("endDate", OFFENCE_END_DATE)
                .add("statementOfOffence", statementOfOffence)
                .build();

        JsonArrayBuilder offences = createArrayBuilder().add(offence);

        return offences.build();
    }
}

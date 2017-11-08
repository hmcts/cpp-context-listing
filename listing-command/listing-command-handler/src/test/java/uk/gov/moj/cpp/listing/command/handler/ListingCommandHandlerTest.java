package uk.gov.moj.cpp.listing.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingCommandHandlerTest {

    private static final UUID COMMAND_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID1 = UUID.randomUUID();
    private static final UUID OFFENCE_ID1 = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final String FIRST_NAME = "Test Recipe";
    private static final String LAST_NAME = "Last Name";
    private static final String DATE_OF_BIRTH = "1980-07-15";
    private static final String COURT_CENTRE = "Liverpool";
    private static final String HEARING_TYPE = "TRIAL";
    private static final String START_DATE = "2018-06-01";
    private static final String OFFENCE_START_DATE = "2018-06-01";
    private static final String OFFENCE_END_DATE = "2018-06-01";
    private static final int ESTIMATE_MINUTES = 7200;
    private static final String BAIL_STATUS = "OnBail";
    private static final String DEFENCE_ORGANISATION = "XYZ Organisation";
    private static final String URN = "urn";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String STATEMENT_OF_OFFENCE_TITLE = "title";
    private static final String STATEMENT_OF_OFFENCE_LEGISLATION = "Legislation";
    private static final String CUSTODY_TIME_LIMIT = "2017-10-05";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseSentForListing.class);

    @InjectMocks
    private ListingCommandHandler listingCommandHandler;

    @Test
    public void listingCommandHandlerShouldTriggerCaseSentForListingEvent() throws Exception {
        final JsonEnvelope commandEnvelope = listingCommandEnvelope(COMMAND_ID);

        JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();

        when(eventSource.getStreamById(commandEnvelope.metadata().id())).thenReturn(eventStream);

        listingCommandHandler.sendCaseForListing(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.events.case-sent-for-listing")
                                .withCausationIds(commandEnvelope.metadata().id()),payload()
                                .isJson(allOf(
                                    withJsonPath("$.caseProgressionId", equalTo(commandPayload.getString("caseProgressionId"))),
                                    withJsonPath("$.urn", equalTo(commandPayload.getString("urn"))),
                                        withJsonPath("$.hearings[0].id",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.hearings[0].courtCentreId",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("courtCentreId"))),
                                        withJsonPath("$.hearings[0].type",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("type"))),
                                        withJsonPath("$.hearings[0].startDate",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getString("startDate"))),
                                        withJsonPath("$.hearings[0].estimateMinutes",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getInt("estimateMinutes"))),
                                        withJsonPath("$.hearings[0].defendants[0].id",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.hearings[0].defendants[0].personId",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("personId"))),
                                        withJsonPath("$.hearings[0].defendants[0].firstName",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("firstName"))),
                                        withJsonPath("$.hearings[0].defendants[0].lastName",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("lastName"))),
                                        withJsonPath("$.hearings[0].defendants[0].dateOfBirth",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("dateOfBirth"))),
                                        withJsonPath("$.hearings[0].defendants[0].bailStatus",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("bailStatus"))),
                                        withJsonPath("$.hearings[0].defendants[0].custodyTimeLimit",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getString("custodyTimeLimit"))),
                                        withJsonPath("$.hearings[0].defendants[0].defenceOrganisation",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0)
                                                        .getString("defenceOrganisation"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].id",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("id"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].offenceCode", equalTo
                                                (commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("offenceCode"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0].startDate",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getString("endDate"))),

                                        withJsonPath("$.hearings[0].defendants[0].offences[0]" +
                                                        ".statementOfOffence.title",
                                                equalTo(commandPayload.getJsonArray("hearings")
                                                        .getJsonObject(0).getJsonArray("defendants")
                                                        .getJsonObject(0).getJsonArray("offences")
                                                        .getJsonObject(0).getJsonObject
                                                                ("statementOfOffence").getString
                                                                ("title"))),
                                        withJsonPath("$.hearings[0].defendants[0].offences[0]" +
                                                        ".statementOfOffence.legislation",
                                                equalTo(commandPayload.getJsonArray("hearings")
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

    private JsonEnvelope listingCommandEnvelope(final UUID id) {

        JsonObject caseJson = createCaseJson();
        return createEnvelope("listing.command.send-case-for-listing" , caseJson);
    }

    private JsonObject createCaseJson() {
        return createObjectBuilder()
                .add("caseProgressionId", CASE_ID.toString())
                .add("urn", URN)
                .add("hearings", createHearingsJson())
                .build();

    }

    private JsonArray createHearingsJson() {
        JsonObject hearing1 =  createObjectBuilder()
                .add("id", HEARING_ID.toString())
                .add("courtCentreId", COURT_CENTRE)
                .add("type", HEARING_TYPE)
                .add("startDate", START_DATE)
                .add("estimateMinutes", ESTIMATE_MINUTES)
                .add("defendants", createDefendantsJson())
                .build();

        JsonArrayBuilder hearings = createArrayBuilder().add(hearing1);
        return hearings.build();
    }

    private JsonArray createDefendantsJson() {
        JsonObject defendant1 = createObjectBuilder()
                .add("id", DEFENDANT_ID1.toString())
                .add("personId", PERSON_ID.toString())
                .add("firstName", FIRST_NAME)
                .add("lastName", LAST_NAME)
                .add("dateOfBirth", DATE_OF_BIRTH)
                .add("bailStatus", BAIL_STATUS)
                .add("custodyTimeLimit", CUSTODY_TIME_LIMIT)
                .add("defenceOrganisation", DEFENCE_ORGANISATION)
                .add("offences", createOffencesJson())
                .build();

        JsonArrayBuilder defendants = createArrayBuilder().add(defendant1);

        return defendants.build();
    }

    private JsonArray createOffencesJson() {
        JsonObject statementOfOffence = createObjectBuilder()
                .add("title", STATEMENT_OF_OFFENCE_TITLE)
                .add("legislation", STATEMENT_OF_OFFENCE_LEGISLATION)
                .build();

        JsonObject offence1 = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("offenceCode", PERSON_ID.toString())
                .add("startDate", OFFENCE_START_DATE)
                .add("endDate", OFFENCE_END_DATE)
                .add("statementOfOffence", statementOfOffence)
                .build();

        JsonArrayBuilder offences = createArrayBuilder().add(offence1);

        return offences.build();
    }
}

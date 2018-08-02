package uk.gov.moj.cpp.listing.event.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.*;
import uk.gov.justice.progression.events.*;
import uk.gov.justice.progression.events.Offence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.services.test.utils.core.random.Generator;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.external.HearingConfirmed;
import uk.gov.moj.cpp.listing.event.external.HearingUpdated;
import uk.gov.moj.cpp.listing.event.processor.command.*;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.*;

@RunWith(MockitoJUnitRunner.class)
public class ListingEventProcessorTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final String TYPE = "Sentence";
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.now();
    private static final String URN = RandomGenerator.STRING.next();
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");


    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private CaseSentForListing caseSentForListing;

    @Mock
    private DefendantsToBeUpdated defendantsToBeUpdated;

    @Mock
    private OffencesToBeUpdated offencesToBeUpdated;

    @Mock
    private OffencesToBeAdded offencesToBeAdded;

    @Mock
    private OffencesToBeDeleted offencesToBeDeleted;

    @Mock
    private HearingAllocatedForListing hearingAllocatedForListing;

    @Mock
    private AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing;

    @Mock
    private ListHearingCommandCollectionConverter listHearingCommandCollectionConverter;

    @Mock
    private UpdateDefendantsForHearingCommandCollectionConverter updateDefendantsForHearingCommandCollectionConverter;

    @Mock
    private UpdateOffencesForHearingCommandCollectionConverter updateOffencesForHearingCommandCollectionConverter;

    @Mock
    private AddOffencesForHearingCommandCollectionConverter addOffencesForHearingCommandCollectionConverter;

    @Mock
    private DeleteOffencesForHearingCommandCollectionConverter deleteOffencesForHearingCommandCollectionConverter;

    @Mock
    private List<Hearing> hearings;

    @Mock
    private ListHearingCommand listHearingCommand;

    @Mock
    private UpdateDefendantsForHearingCommand updateDefendantsForHearingCommand;

    @Mock
    private UpdateOffencesForHearingCommand updateOffencesForHearingCommand;

    @Mock
    private AddOffencesForHearingCommand addOffencesForHearingCommand;

    @Mock
    private DeleteOffencesForHearingCommand deleteOffencesForHearingCommand;

    @Mock
    private uk.gov.moj.cpp.listing.persistence.entity.Hearing hearing;

    @Mock
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Mock
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private ListingEventProcessor listingEventProcessor;

    @Spy
    ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Test
    public void shouldHandleCaseSentForListingEventMessage() throws Exception {
        //Given
        hearings = singletonList(EventBuilder.buildHearing());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CaseSentForListing.class)).willReturn(caseSentForListing);
        given(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_CASE_SENT_FOR_LISTING)).willReturn
                (enveloperFunction);
        given(enveloper.withMetadataFrom(envelope, "listing.command.list-hearing")).willReturn
                (enveloperFunction);
        given(enveloperFunction.apply(any(CaseSentForListing.class))).willReturn(finalEnvelope);
        given(enveloperFunction.apply(any(Hearing.class))).willReturn(finalEnvelope);
        given(caseSentForListing.getHearings()).willReturn(hearings);
        given(listHearingCommandCollectionConverter.convert(caseSentForListing)).willReturn(singletonList(listHearingCommand));


        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleCaseSentForListingMessage(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleDefendantsToBeUpdatedMessage() throws Exception {
        //Given
        final List<UUID> hearings = singletonList(UUID.randomUUID());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, DefendantsToBeUpdated.class)).willReturn(defendantsToBeUpdated);
        given(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_DEFENDANTS_FOR_HEARING)).willReturn
                (enveloperFunction);
        given(enveloperFunction.apply(any(DefendantsToBeUpdated.class))).willReturn(finalEnvelope);
        given(enveloperFunction.apply(any(Hearing.class))).willReturn(finalEnvelope);
        given(defendantsToBeUpdated.getHearings()).willReturn(hearings);
        given(updateDefendantsForHearingCommandCollectionConverter.convert(defendantsToBeUpdated)).willReturn(singletonList(updateDefendantsForHearingCommand));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleDefendantsToBeUpdatedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleOffencesToBeUpdatedMessage() throws Exception {
        //Given
        final List<UUID> hearings = singletonList(UUID.randomUUID());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, OffencesToBeUpdated.class)).willReturn(offencesToBeUpdated);
        given(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_OFFENCES_FOR_HEARING)).willReturn
                (enveloperFunction);
        given(enveloperFunction.apply(any(OffencesToBeUpdated.class))).willReturn(finalEnvelope);
        given(enveloperFunction.apply(any(Hearing.class))).willReturn(finalEnvelope);
        given(offencesToBeUpdated.getHearings()).willReturn(hearings);
        given(updateOffencesForHearingCommandCollectionConverter.convert(offencesToBeUpdated)).willReturn(singletonList(updateOffencesForHearingCommand));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleOffencesToBeUpdatedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleOffencesToBeAddedMessage() throws Exception {
        //Given
        final List<UUID> hearings = singletonList(UUID.randomUUID());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, OffencesToBeAdded.class)).willReturn(offencesToBeAdded);
        given(enveloper.withMetadataFrom(envelope, COMMAND_ADD_OFFENCES_FOR_HEARING)).willReturn
                (enveloperFunction);
        given(enveloperFunction.apply(any(OffencesToBeAdded.class))).willReturn(finalEnvelope);
        given(enveloperFunction.apply(any(Hearing.class))).willReturn(finalEnvelope);
        given(offencesToBeAdded.getHearings()).willReturn(hearings);
        given(addOffencesForHearingCommandCollectionConverter.convert(offencesToBeAdded)).willReturn(singletonList(addOffencesForHearingCommand));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleOffencesToBeAddedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleOffencesToBeDeletedMessage() throws Exception {
        //Given
        final List<UUID> hearings = singletonList(UUID.randomUUID());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, OffencesToBeDeleted.class)).willReturn(offencesToBeDeleted);
        given(enveloper.withMetadataFrom(envelope, COMMAND_DELETE_OFFENCES_FOR_HEARING)).willReturn
                (enveloperFunction);
        given(enveloperFunction.apply(any(OffencesToBeDeleted.class))).willReturn(finalEnvelope);
        given(enveloperFunction.apply(any(Hearing.class))).willReturn(finalEnvelope);
        given(offencesToBeDeleted.getHearings()).willReturn(hearings);
        given(deleteOffencesForHearingCommandCollectionConverter.convert(offencesToBeDeleted)).willReturn(singletonList(deleteOffencesForHearingCommand));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleOffencesToBeDeletedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleHearingAllocatedForListingMessage() throws Exception {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListing.class)).willReturn(hearingAllocatedForListing);
        given(hearingAllocatedForListing.getHearingId()).willReturn(HEARING_ID);


        given(hearing.getListingCaseId()).willReturn(CASE_ID);

        HearingConfirmed hearingConfirmed = hearingConfirmed();
        given(hearingConfirmedFactory.create(hearingAllocatedForListing)).willReturn(hearingConfirmed);


        //when
        listingEventProcessor.handleHearingAllocatedForListingMessage(event);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName(PUBLIC_EVENT_HEARING_CONFIRMED),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                        withJsonPath("$.urn", equalTo(URN)),
                        withJsonPath("$.hearing.id", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.hearing.type", equalTo(TYPE)),
                        withJsonPath("$.hearing.courtCentreId", equalTo(COURT_CENTRE_ID.toString())),
                        withJsonPath("$.hearing.courtRoomId", equalTo(COURT_ROOM_ID.toString())),
                        withJsonPath("$.hearing.hearingDays[0]",
                                equalTo(ZONED_DATE_TIME_FORMAT.format(hearingConfirmed.getHearing().getHearingDays().get(0)))),
                        withJsonPath("$.hearing.judgeId", equalTo(JUDGE_ID.toString())),
                        withJsonPath("$.hearing.defendants[0].id", equalTo(DEFENDANT_ID.toString())),
                        withJsonPath("$.hearing.defendants[0].offences[0].id", equalTo(OFFENCE_ID.toString()))
                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldHandleAllocatedHearingUpdatedForListingMessage() throws Exception {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(),
                AllocatedHearingUpdatedForListing.class)).willReturn(allocatedHearingUpdatedForListing);
        given(allocatedHearingUpdatedForListing.getHearingId()).willReturn(HEARING_ID);


        given(hearing.getListingCaseId()).willReturn(CASE_ID);

        HearingUpdated hearingUpdated = hearingUpdated();
        given(allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing))
                .willReturn(hearingUpdated);


        //when
        listingEventProcessor.handleAllocatedHearingUpdatedForListingMessage(event);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName(PUBLIC_EVENT_HEARING_UPDATED),
                payloadIsJson(allOf(
                        withJsonPath("$.hearing.id", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.hearing.type", equalTo(TYPE)),
                        withJsonPath("$.hearing.courtCentreId", equalTo(COURT_CENTRE_ID.toString())),
                        withJsonPath("$.hearing.courtRoomId", equalTo(COURT_ROOM_ID.toString())),
                        withJsonPath("$.hearing.hearingDays[0]",
                                equalTo(ZONED_DATE_TIME_FORMAT.format(hearingUpdated.getHearing().getHearingDays().get(0)))),
                        withJsonPath("$.hearing.judgeId", equalTo(JUDGE_ID.toString()))
                ))).thatMatchesSchema()
        ));
    }

    private JsonEnvelope hearingAllocatedEvent() {

        final JsonObjectBuilder hearingDate = createObjectBuilder()
                .add("startDate", START_DATE.toString())
                .add("startTime", START_TIME.toString());

        final JsonObjectBuilder hearingAllocated = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TYPE)
                .add("estimatedMinutes", ESTIMATED_MINUTES)
                .add("judgeId", JUDGE_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), hearingAllocated.build());
    }

    private HearingUpdated hearingUpdated() {
        String formattedTime = DATE_TIME_FORMAT.format(START_DATE_TIME);
        uk.gov.moj.cpp.listing.event.external.BaseHearing hearing =
                new uk.gov.moj.cpp.listing.event.external.BaseHearing(
                        HEARING_ID.toString(),
                        TYPE,
                        COURT_ROOM_ID.toString(),
                        JUDGE_ID.toString(),
                        asList(START_DATE_TIME),
                        COURT_CENTRE_ID.toString());

        return new HearingUpdated(hearing);
    }

    @Test
    public void shouldHandleCaseDefendantChangedMessage() throws Exception {
        final CaseDefendantChanged caseDefendantChanged = caseDefendantChanged();
        final JsonObject caseDefendantChangeJsonObject =  this.objectToJsonObjectConverter.convert(caseDefendantChanged);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), caseDefendantChangeJsonObject);
        final JsonEnvelopeMatcher jsonEnvelopeMatcher = new JsonEnvelopeMatcher();

        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                CaseDefendantChanged.class)).willReturn(caseDefendantChanged);

        //when
        listingEventProcessor.handleCaseDefendantChangedMessage(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelopeMatcher.withMetadataOf(
                withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_UPDATE_CASE_DEFENDANT_DETAILS))
//                .thatMatchesSchema() TODO: Framework fix required to add this back in
        ));
        final CaseDefendantChanged resultPayload = jsonObjectConverter
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), CaseDefendantChanged.class);
        assertThat(resultPayload, equalTo(caseDefendantChanged));
    }

    @Test
    public void shouldHandleDefendantOffencesChangedMessage() throws Exception {
        final DefendantOffencesChanged defendantOffencesChanged = defendantOffencesChanged();
        final JsonObject defendantOffencesChangedJsonObject =  this.objectToJsonObjectConverter.convert(defendantOffencesChanged);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), defendantOffencesChangedJsonObject);
        final JsonEnvelopeMatcher jsonEnvelopeMatcher = new JsonEnvelopeMatcher();

        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                DefendantOffencesChanged.class)).willReturn(defendantOffencesChanged);

        //when
        listingEventProcessor.handleDefendantOffencesChanged(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelopeMatcher.withMetadataOf(
                withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES))
//                .thatMatchesSchema() TODO: Framework fix required to add this back in
        ));
        final DefendantOffencesChanged resultPayload = jsonObjectConverter
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), DefendantOffencesChanged.class);
        assertThat(resultPayload, equalTo(defendantOffencesChanged));
    }

    private DefendantOffencesChanged defendantOffencesChanged() {

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final Generator<String> stringGenerator = string(5);
        final DefendantOffencesChanged.Builder defendantOffencesChangedBuilder = new DefendantOffencesChanged.Builder();
        final AddedOffences.Builder addedOffencesBuilder = new AddedOffences.Builder();
        final DeletedOffences.Builder deletedOffencesBuilder = new DeletedOffences.Builder();
        final Offence.Builder offenceBuilder = new Offence.Builder();
        final UpdatedOffences.Builder updatedOffenceBuilder = new UpdatedOffences.Builder();
        final StatementOfOffence.Builder statementOfOffenceBuilder = new StatementOfOffence.Builder();

        final StatementOfOffence statementOfOffence = statementOfOffenceBuilder
                .withTitle(stringGenerator.next())
                .withLegislation(stringGenerator.next())
                .build();

        final Offence offence = offenceBuilder
                .withOffenceCode(of(stringGenerator.next()))
                .withConvictionDate(of(stringGenerator.next()))
                .withCount(of(10))
                .withEndDate(of(stringGenerator.next()))
                .withId(UUID.randomUUID())
                .withStartDate(of(stringGenerator.next()))
                .withWording(of(stringGenerator.next()))
                .withStatementOfOffence(of(statementOfOffence))
                .build();

        final UpdatedOffences updatedOffence = updatedOffenceBuilder
                .withCaseId(UUID.randomUUID())
                .withDefendantId(UUID.randomUUID())
                .withOffences(asList((offence)))
                .build();

        final AddedOffences addedOffences = addedOffencesBuilder
                .withOffences(asList(offence))
                .withCaseId(UUID.randomUUID())
                .withDefendantId(UUID.randomUUID())
                .build();

        final DeletedOffences deletedOffences = deletedOffencesBuilder
                .withDefendantId(UUID.randomUUID())
                .withCaseId(UUID.randomUUID())
                .withOffences(asList(UUID.randomUUID()))
                .build();

        final DefendantOffencesChanged defendantOffencesChanged = defendantOffencesChangedBuilder
                .withAddedOffences(asList(addedOffences))
                .withDeletedOffences(asList(deletedOffences))
                .withUpdatedOffences(asList(updatedOffence))
                .withModifiedDate(stringGenerator.next())
                .build();

        return defendantOffencesChanged;
    }

    private CaseDefendantChanged caseDefendantChanged() {

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final Generator<String> stringGenerator = string(5);
        final ProgressionInterpreter.Builder progressionInterpreterBuilder = new ProgressionInterpreter.Builder();
        final ProgressionDefendant.Builder caseProgressionBuilder = new ProgressionDefendant.Builder();
        final ProgressionPerson.Builder progressionPersonBuilder = new ProgressionPerson.Builder();
        final CaseDefendantChanged.Builder caseDefendantChangedBuilder = new CaseDefendantChanged.Builder();
        final ProgressionAddress.Builder progressionAddressBuilder = new ProgressionAddress.Builder();


        final ProgressionInterpreter progressionInterpreter = progressionInterpreterBuilder
                .withLanguage(of(stringGenerator.next()))
                .withName(of(string(5).next()))
                .withNeeded(of(true))
                .build();

        final ProgressionAddress progressionAddress = progressionAddressBuilder
                .withAddress1(stringGenerator.next())
                .withAddress2(of(stringGenerator.next()))
                .withAddress3(of(stringGenerator.next()))
                .withAddress4(of(stringGenerator.next()))
                .withPostCode(of(stringGenerator.next()))
                .build();


        final ProgressionPerson progressionPerson = progressionPersonBuilder
                .withAddress(of(progressionAddress))
                .withDateOfBirth(of(stringGenerator.next()))
                .withEmail(of(stringGenerator.next()))
                .withFax(of(stringGenerator.next()))
                .withFirstName(of(stringGenerator.next()))
                .withGender(of(stringGenerator.next()))
                .withHomeTelephone(of(stringGenerator.next()))
                .withId(UUID.randomUUID())
                .withLastName(of(stringGenerator.next()))
                .withMobile(of(stringGenerator.next()))
                .withNationality(of(stringGenerator.next()))
                .withTitle(of(stringGenerator.next()))
                .withWorkTelephone(of(stringGenerator.next()))
                .build();

        final ProgressionDefendant progressionDefendant = caseProgressionBuilder
                .withId(UUID.randomUUID())
                .withBailStatus(of(stringGenerator.next()))
                .withCustodyTimeLimitDate(of(stringGenerator.next()))
                .withDefenceOrganisation(of(stringGenerator.next()))
                .withInterpreter(of(progressionInterpreter))
                .withPerson(progressionPerson)
                .build();

         final CaseDefendantChanged caseDefendantChanged = caseDefendantChangedBuilder.withCaseId(UUID.randomUUID())
                 .withDefendants(asList(progressionDefendant))
                 .withCaseId(UUID.randomUUID())
                 .build();

         return caseDefendantChanged;
    }

    private HearingConfirmed hearingConfirmed() {

        uk.gov.moj.cpp.listing.event.external.Offence offence =
                new uk.gov.moj.cpp.listing.event.external.Offence(OFFENCE_ID.toString());
        uk.gov.moj.cpp.listing.event.external.Defendant defendant =
                new uk.gov.moj.cpp.listing.event.external.Defendant(
                        DEFENDANT_ID.toString(),
                        asList(offence));
        String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);
        uk.gov.moj.cpp.listing.event.external.Hearing hearing =
                new uk.gov.moj.cpp.listing.event.external.Hearing(
                        HEARING_ID.toString(),
                        TYPE,
                        CASE_ID.toString(),
                        COURT_CENTRE_ID.toString(),
                        COURT_ROOM_ID.toString(),
                        JUDGE_ID.toString(),
                        Collections.singletonList(ZonedDateTimes.fromString(formattedDateTime)),
                        asList(defendant));

        return new HearingConfirmed(CASE_ID.toString(), URN, hearing);
    }

}

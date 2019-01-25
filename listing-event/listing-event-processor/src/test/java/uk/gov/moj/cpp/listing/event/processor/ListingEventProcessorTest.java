package uk.gov.moj.cpp.listing.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_ADD_OFFENCES_FOR_HEARING;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_DELETE_OFFENCES_FOR_HEARING;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_CASE_DEFENDANT_DETAILS;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_DEFENDANTS_FOR_HEARING;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_OFFENCES_FOR_HEARING;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_HEARING_CONFIRMED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_HEARING_LISTED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_HEARING_UPDATED;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.AddedOffences;
import uk.gov.justice.listing.courts.BailStatus;
import uk.gov.justice.listing.courts.Defendant;
import uk.gov.justice.listing.courts.DefendantUpdated;
import uk.gov.justice.listing.courts.DeletedOffences;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.courts.OffencesForDefendantUpdated;
import uk.gov.justice.listing.courts.UpdatedOffences;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.listing.events.StatementOfOffence;
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
import uk.gov.moj.cpp.listing.event.processor.command.AddHearingToCaseCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommandCollectionConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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
    private static final UUID JUDICIAL_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.now();
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final String CIRCUIT_JUDGE = "CIRCUIT_JUDGE";


    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private DefendantsToBeUpdated defendantsToBeUpdated;

    @Mock
    private OffencesToBeUpdated offencesToBeUpdated;

    @Mock
    private OffencesToBeAdded offencesToBeAdded;

    @Mock
    private HearingListed hearingListed;

    @Mock
    private OffencesToBeDeleted offencesToBeDeleted;

    @Mock
    private HearingAllocatedForListing hearingAllocatedForListing;

    @Mock
    private AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing;

    @Mock
    private AddHearingToCaseCommandCollectionConverter addHearingToCaseCommandCollectionConverter;

    @Mock
    private UpdateDefendantsForHearingCommandCollectionConverter updateDefendantsForHearingCommandCollectionConverter;

    @Mock
    private UpdateOffencesForHearingCommandCollectionConverter updateOffencesForHearingCommandCollectionConverter;

    @Mock
    private AddOffencesForHearingCommandCollectionConverter addOffencesForHearingCommandCollectionConverter;

    @Mock
    private DeleteOffencesForHearingCommandCollectionConverter deleteOffencesForHearingCommandCollectionConverter;

    @Mock
    private Hearing hearing;

    @Mock
    private AddHearingToCaseCommand addHearingToCaseCommand;

    @Mock
    private UpdateDefendantsForHearingCommand updateDefendantsForHearingCommand;

    @Mock
    private UpdateOffencesForHearingCommand updateOffencesForHearingCommand;

    @Mock
    private AddOffencesForHearingCommand addOffencesForHearingCommand;

    @Mock
    private DeleteOffencesForHearingCommand deleteOffencesForHearingCommand;

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
    public void shouldHandleHearingListedEventMessage() throws Exception {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed);
        given(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_HEARING_LISTED)).willReturn
                (enveloperFunction);
        given(enveloper.withMetadataFrom(envelope, "listing.command.add-hearing-to-case")).willReturn
                (enveloperFunction);
        given(enveloperFunction.apply(any(HearingListed.class))).willReturn(finalEnvelope);
        given(enveloperFunction.apply(any(Hearing.class))).willReturn(finalEnvelope);
        given(hearingListed.getHearing()).willReturn(hearing);
        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed)).willReturn(singletonList(addHearingToCaseCommand));


        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
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
                        withJsonPath("$.confirmedHearing.id", equalTo(HEARING_ID.toString()))
                )))
        ));
    }

    @Test
    public void shouldHandleAllocatedHearingUpdatedForListingMessage() throws Exception {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(),
                AllocatedHearingUpdatedForListing.class)).willReturn(allocatedHearingUpdatedForListing);

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
                        withJsonPath("$.updatedHearing.id", equalTo(HEARING_ID.toString()))
                )))
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
                .add("judgeId", JUDICIAL_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), hearingAllocated.build());
    }

    private HearingUpdated hearingUpdated() {
        String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);

        return HearingUpdated.hearingUpdated()
                .withUpdatedHearing(buildHearing(formattedDateTime))
                .build();
    }

    @Test
    public void shouldHandleCaseDefendantChangedMessage() throws Exception {
        final DefendantUpdated defendantUpdated = defendantUpdated();
        final JsonObject caseDefendantChangeJsonObject = this.objectToJsonObjectConverter.convert(defendantUpdated);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), caseDefendantChangeJsonObject);
        final JsonEnvelopeMatcher jsonEnvelopeMatcher = new JsonEnvelopeMatcher();

        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                DefendantUpdated.class)).willReturn(defendantUpdated);

        //when
        listingEventProcessor.handleCaseDefendantChangedMessage(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelopeMatcher.withMetadataOf(
                withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_UPDATE_CASE_DEFENDANT_DETAILS))
        ));
        final DefendantUpdated resultPayload = jsonObjectConverter
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), DefendantUpdated.class);

        assertThat(resultPayload, equalTo(defendantUpdated));
        assertThat(resultPayload, not(equalTo(defendantUpdated())));
    }

    @Test
    public void shouldHandleDefendantOffencesChangedMessage() throws Exception {
        final OffencesForDefendantUpdated offencesForDefendantUpdated = offencesForDefendantUpdated();
        final JsonObject defendantOffencesChangedJsonObject = this.objectToJsonObjectConverter.convert(offencesForDefendantUpdated);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), defendantOffencesChangedJsonObject);
        final JsonEnvelopeMatcher jsonEnvelopeMatcher = new JsonEnvelopeMatcher();

        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                OffencesForDefendantUpdated.class)).willReturn(offencesForDefendantUpdated);

        //when
        listingEventProcessor.handleDefendantOffencesChanged(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelopeMatcher.withMetadataOf(
                withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES))
        ));
        final OffencesForDefendantUpdated resultPayload = jsonObjectConverter
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), OffencesForDefendantUpdated.class);
        assertThat(resultPayload, equalTo(offencesForDefendantUpdated));
        assertThat(resultPayload, not(equalTo(offencesForDefendantUpdated())));
    }

    private OffencesForDefendantUpdated offencesForDefendantUpdated() throws IllegalAccessException {

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final Generator<String> stringGenerator = string(5);
        final OffencesForDefendantUpdated.Builder offencesForDefendantUpdated = new OffencesForDefendantUpdated.Builder();
        final AddedOffences.Builder addedOffencesBuilder = new AddedOffences.Builder();
        final DeletedOffences.Builder deletedOffencesBuilder = new DeletedOffences.Builder();
        final Offence.Builder offenceBuilder = new Offence.Builder();
        final UpdatedOffences.Builder updatedOffenceBuilder = new UpdatedOffences.Builder();
        final StatementOfOffence.Builder statementOfOffenceBuilder = new StatementOfOffence.Builder();

        final StatementOfOffence statementOfOffence = statementOfOffenceBuilder
                .withTitle(stringGenerator.next())
                .withWelshTitle(stringGenerator.next())
                .withLegislation(of(stringGenerator.next()))
                .build();

        final Offence offence = offenceBuilder
                .withArrestDate(empty())
                .withChargeDate(empty())
                .withOffenceCode(stringGenerator.next())
                .withConvictionDate(of(stringGenerator.next()))
                .withCount(10)
                .withEndDate(of(stringGenerator.next()))
                .withId(UUID.randomUUID())
                .withStartDate(stringGenerator.next())
                .withWording(stringGenerator.next())
                .build();

        final UpdatedOffences updatedOffence = updatedOffenceBuilder
                .withDefendantId(UUID.randomUUID())
                .withOffences(asList((offence)))
                .build();

        final AddedOffences addedOffences = addedOffencesBuilder
                .withOffences(asList(offence))
                .withProsecutionCaseId(UUID.randomUUID())
                .withDefendantId(UUID.randomUUID())
                .build();

        final DeletedOffences deletedOffences = deletedOffencesBuilder
                .withDefendantId(UUID.randomUUID())
                .withProsecutionCaseId(UUID.randomUUID())
                .withOffences(asList(UUID.randomUUID()))
                .build();

        final OffencesForDefendantUpdated defendantOffencesChanged = offencesForDefendantUpdated
                .withAddedOffences(asList(addedOffences))
                .withDeletedOffences(asList(deletedOffences))
                .withUpdatedOffences(asList(updatedOffence))
                .withModifiedDate(stringGenerator.next())
                .build();

        return defendantOffencesChanged;
    }

    private DefendantUpdated defendantUpdated() throws IllegalAccessException {

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final Generator<String> stringGenerator = string(5);
        final Defendant.Builder defendantBuilder = new Defendant.Builder();
        final PersonDefendant.Builder personDefendantBuilder = new PersonDefendant.Builder();
        final DefendantUpdated.Builder defendantUpdatedBuilder = new DefendantUpdated.Builder();
        final Address.Builder progressionAddressBuilder = new Address.Builder();
        final Person.Builder personBuilder = new Person.Builder();

        final Address address = progressionAddressBuilder
                .withAddress1(stringGenerator.next())
                .withAddress2(of(stringGenerator.next()))
                .withAddress3(of(stringGenerator.next()))
                .withAddress4(of(stringGenerator.next()))
                .withPostcode(of(stringGenerator.next()))
                .build();

        final Person person = personBuilder
                .withAdditionalNationalityCode(empty())
                .withAdditionalNationalityDescription(empty())
                .withAdditionalNationalityId(empty())
                .withAddress(of(address))
                .withContact(empty())
                .withDateOfBirth(of(stringGenerator.next()))
                .withDisabilityStatus(empty())
                .withDocumentationLanguageNeeds(empty())
                .withEthnicityCode(empty())
                .withEthnicityId(empty())
                .build();

        final PersonDefendant progressionPerson = personDefendantBuilder
                .withAliases(emptyList())
                .withArrestSummonsNumber(empty())
                .withAliases(emptyList())
                .withBailStatus(of(BailStatus.CONDITIONAL))
                .withCustodyTimeLimit(of("CTL"))
                .withDriverNumber(empty())
                .withEmployerOrganisation(empty())
                .withEmployerPayrollReference(empty())
                .withObservedEthnicityCode(empty())
                .withObservedEthnicityId(empty())
                .withPerceivedBirthYear(empty())
                .withPersonDetails(person)

                .build();

        final Defendant defendant = defendantBuilder
                .withId(UUID.randomUUID())
                .withAssociatedPersons(emptyList())
                .withDefenceOrganisation(empty())
                .withLegalEntityDefendant(empty())
                .withMitigation(empty())
                .withMitigationWelsh(empty())
                .withNumberOfPreviousConvictionsCited(empty())
                .withPersonDefendant(of(progressionPerson))
                .withProsecutionAuthorityReference(empty())
                .withProsecutionCaseId(UUID.randomUUID())
                .withWitnessStatement(empty())
                .withWitnessStatementWelsh(empty())
                .build();

        final DefendantUpdated defendantUpdated = defendantUpdatedBuilder.withDefendant(defendant)
                .build();

        return defendantUpdated;
    }

    private HearingConfirmed hearingConfirmed() {

        String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);

        return HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildHearing(formattedDateTime))
                .build();
    }

    private uk.gov.justice.core.courts.ConfirmedHearing buildHearing(String formattedDateTime) {
        return uk.gov.justice.core.courts.ConfirmedHearing.confirmedHearing()
                .withId(HEARING_ID)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                        .withListedDurationMinutes(0)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(of(COURT_ROOM_ID))
                        .build())
                .withHearingLanguage(of(HearingLanguage.WELSH))

                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription(TYPE).withId(UUID.randomUUID()).build())
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withJudicialId(JUDICIAL_ID)
                        .withJudicialRoleType(
                                uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(CIRCUIT_JUDGE)
                                        .withJudicialRoleTypeId(Optional.empty())
                                        .build())
                        .build()))
                .withProsecutionCases(Arrays.asList(uk.gov.justice.core.courts.ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(Arrays.asList(uk.gov.justice.core.courts.ConfirmedDefendant.confirmedDefendant()
                                .withId(DEFENDANT_ID)
                                .withOffences(Arrays.asList(uk.gov.justice.core.courts.ConfirmedOffence.confirmedOffence().withId(OFFENCE_ID).build()))
                                .build()))
                        .build()))
                .build();
    }

}

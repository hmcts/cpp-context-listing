package uk.gov.moj.cpp.listing.event.processor;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.HearingDay.hearingDay;
import static uk.gov.justice.hearing.courts.HearingDaysCancelled.hearingDaysCancelled;
import static uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings.newDefendantAddedForCourtProceedings;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_APPLICATION_EJECTED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_CASE_EJECTED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_CASE_OR_APPLICATION_EJECTED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_CASE_DEFENDANT_DETAILS;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_COURT_APPLICATION;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.COMMAND_UPDATE_HEARING_TO_CASE;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PRIVATE_COMMAND_HEARING_VACATE_TRIAL;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_HEARING_CHANGES_SAVED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_HEARING_CONFIRMED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_HEARING_UPDATED;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_VACATED_TRIAL_UPDATED;

import org.mockito.Mockito;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingDaysCancelled;
import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.AddedOffences;
import uk.gov.justice.listing.courts.CaseOrApplicationEjected;
import uk.gov.justice.listing.courts.Defendant;
import uk.gov.justice.listing.courts.DefendantUpdated;
import uk.gov.justice.listing.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.listing.courts.DeletedOffences;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.courts.OffencesForDefendantUpdated;
import uk.gov.justice.listing.courts.UpdatedOffences;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtApplicationToBeUpdated;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.DefendantOffenceIdsV2;
import uk.gov.justice.listing.events.Defendants;
import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingMarkedAsDeleted;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.listing.events.HearingMarkedForPartialUpdate;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.LinkedCasesToBeUpdated;
import uk.gov.justice.listing.events.LinkedToCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.Offences;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2;
import uk.gov.justice.listing.events.ProsecutionCases;
import uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.progression.courts.ApplicationLaaReferenceUpdatedForApplication;
import uk.gov.justice.progression.courts.ApplicationOffencesUpdated;
import uk.gov.justice.progression.courts.CaseLinked;
import uk.gov.justice.progression.courts.Cases;
import uk.gov.justice.progression.courts.CourtApplicationChanged;
import uk.gov.justice.progression.courts.HearingExtended;
import uk.gov.justice.progression.courts.LinkActionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.services.test.utils.core.random.Generator;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.SlotDetail;
import uk.gov.moj.cpp.listing.event.processor.command.AddCourtApplicationToHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddDefendantsForCourtProceedingsCommand;
import uk.gov.moj.cpp.listing.event.processor.command.AddDefendantsForCourtProceedingsCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddHearingToCaseCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.ExtendHearingToListedCaseCommandConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateCaseMarkersForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateCaseMarkersForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateUnallocatedHearingPartiallyCommandConverter;
import uk.gov.moj.cpp.listing.event.processor.service.HearingService;
import uk.gov.moj.cpp.listing.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.util.HearingListedToUpdateHearingForListingCommand;
import uk.gov.moj.cpp.listing.event.processor.util.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;
import uk.gov.moj.cpp.listing.event.utils.FileUtil;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class ListingEventProcessorTest {

    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_APPLICATION_ID = "applicationId";
    private static final UUID CASE_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String TYPE = "Sentence";
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final String ESTIMATED_DURATION = "1 week";
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID JUDICIAL_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.now();
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String CIRCUIT_JUDGE = "CIRCUIT_JUDGE";
    private static final String HEARING_IDS = "hearingIds";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String REMOVAL_REASON = "removalReason";
    private static final String ID = "id";
    private static final String HMI_SOURCE = "HMI";
    final Optional<String> OU_CODE = of("BAX0100");
    static final String OUCODE = "B06AN00";
    private final List<uk.gov.justice.listing.events.HearingDay> hearingDays = Arrays.asList(uk.gov.justice.listing.events.HearingDay.hearingDay().withHearingDate(START_DATE).withDurationMinutes(10).build());
    private static final String COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";
    private static final String COMMAND_CHANGE_NEXT_HEARING_DAY = "listing.command.change-next-hearing-day";
    private static final String EVENT_AVAILABLE_SLOTS_FOR_HEARING_FREED = "listing.events.available-slots-for-hearing-freed";
    private static final String COMMAND_MARK_HEARING_AS_DUPLICATE = "listing.command.mark-hearing-as-duplicate";

    private static final String HEARINGID = "hearingId";
    private static final String SEEDING_HERAING_ID = "seedingHearingId";
    private static final String SOURCE = "source";
    private static final String COURTCENTRE_ID = "courtCentreId";

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Spy
    private JsonObjectToObjectConverter jsonObjectConverterForTest = new JsonObjectToObjectConverter(objectMapper);
    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;
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
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Mock
    private DefendantsToBeUpdated defendantsToBeUpdated;
    @Mock
    private DefendantsToBeAddedForCourtProceedings defendantsToBeAddedForCourtProceedings;
    @Mock
    private CaseOrApplicationEjected caseOrApplicationEjected;
    @Mock
    private OffencesToBeUpdated offencesToBeUpdated;
    @Mock
    private OffencesToBeAdded offencesToBeAdded;
    @Mock
    private HearingListed hearingListed;
    @Mock
    private CourtApplicationAddedForHearing courtApplicationAddedForHearing;
    @Mock
    private CourtApplicationToBeUpdated courtApplicationToBeUpdated;
    @Mock
    private OffencesToBeDeleted offencesToBeDeleted;
    @Mock
    private TrialVacated trialVacated;
    @Mock
    private HearingRescheduled hearingRescheduled;
    @Mock
    private CourtListRestricted restrictCourtList;
    @Mock
    private CaseMarkersToBeUpdated caseMarkersToBeUpdated;
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
    private AddCourtApplicationToHearingCommandCollectionConverter addCourtApplicationToHearingCommandCollectionConverter;
    @Mock
    private AddDefendantsForCourtProceedingsCommandCollectionConverter addDefendantsForCourtProceedingsCommandCollectionConverter;
    @Mock
    private Hearing hearing;
    @Mock
    private AddHearingToCaseCommand addHearingToCaseCommand;
    @Mock
    private AddApplicationToHearingCommand addApplicationToHearingCommand;
    @Mock
    private UpdateDefendantsForHearingCommand updateDefendantsForHearingCommand;
    @Mock
    private AddDefendantsForCourtProceedingsCommand addDefendantsForCourtProceedingsCommand;
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
    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;
    @Captor
    private ArgumentCaptor<PublicListingNewDefendantAddedForCourtProceedings> publicEventPayloadCaptor;
    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<DefaultEnvelope<JsonObject>> senderDefaultEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<ApplicationOffencesUpdated>> applicationOffencesUpdatedeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<ApplicationLaaReferenceUpdatedForApplication>> applicationLaaReferenceUpdatedForApplicationCaptor;
    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;
    @Mock
    private AllocatedHearingExtendedForListing allocatedHearingExtendedForListing;

    @Mock
    private AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListingV2;
    @Mock
    private ExtendHearingToListedCaseCommandConverter extendHearingToListedCaseCommandConverter;
    @Mock
    private AllocatedHearingExtendedFactory allocatedHearingExtendedFactory;
    @Mock
    private HearingExtended hearingExtended;
    @Mock
    private HearingListedToUpdateHearingForListingCommand hearingListedToUpdateHearingForListingCommand;
    @Mock
    private HearingSlotsService hearingSlotsService;
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private HearingService hearingService;
    @Mock
    private HearingQueryView hearingQueryView;
    @Mock
    private UpdateCaseMarkersForHearingCommandCollectionConverter updateCaseMarkersForHearingCommandCollectionConverter;
    @Spy
    private UpdateUnallocatedHearingPartiallyCommandConverter updateUnallocatedHearingPartiallyCommandConverter;

    @Mock
    private HearingObjectsListingToCoreConverter hearingListingToCoreConverter;

    @Mock
    private Logger logger;

    @InjectMocks
    private ListingEventProcessor listingEventProcessor;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
        ReflectionUtil.setField(listingEventProcessor, "objectToJsonObjectConverter", objectToJsonObjectConverter);
    }

    @Test
    public void shouldHandleHearingListedEventMessage() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(hearingListed.getHearing()).willReturn(hearing);
        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed)).willReturn(singletonList(addHearingToCaseCommand));
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed)).
                willReturn(singletonList(addApplicationToHearingCommand));
        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mock(uk.gov.justice.core.courts.HearingType.class));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(4)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleHearingListedEventMessageHmiEnabled() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);

        JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();
        final HearingListed hearingListed2 = new EventBuilder(jsonObjectToObjectConverter2).buildHearingListed();

        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed2);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed2)).willReturn(singletonList(addHearingToCaseCommand));
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed2)).
                willReturn(singletonList(addApplicationToHearingCommand));

        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mock(uk.gov.justice.core.courts.HearingType.class));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(4)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleHearingListedEventMessageAndRaiseCommandUpdateHearingWhenBookedSlot() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(hearingListed.getHearing()).willReturn(hearing);
        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed)).willReturn(singletonList(addHearingToCaseCommand));
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed)).willReturn(singletonList(addApplicationToHearingCommand));

        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mock(uk.gov.justice.core.courts.HearingType.class));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(4)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(2).metadata().name(), is("public.listing.hearing-listed"));
    }

    @Test
    public void shouldHandleHearingListedEventMessageAndRaiseCommandUpdateHearingWhenBookedSlotHmiEnabled() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);

        JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();
        final HearingListed hearingListed2 = new EventBuilder(jsonObjectToObjectConverter2).buildHearingListed();

        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed2);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed2)).willReturn(singletonList(addHearingToCaseCommand));
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed2)).
                willReturn(singletonList(addApplicationToHearingCommand));
        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mock(uk.gov.justice.core.courts.HearingType.class));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);
        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(4)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(2).metadata().name(), is("public.listing.hearing-listed"));
    }

    @Test
    public void shouldHandleHearingListedEventMessageWhenProsecutionCasesIsNull() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(hearingListed.getHearing()).willReturn(hearing);
        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());

        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        uk.gov.justice.core.courts.HearingType mockHearingType = mock(uk.gov.justice.core.courts.HearingType.class);
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mockHearingType);
        given(mockHearingType.getDescription()).willReturn("Test Hearing Type");
        given(mockHearing.getId()).willReturn(randomUUID());
        given(mockHearing.getProsecutionCases()).willReturn(null);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        final List<JsonEnvelope> capturedEnvelopes = senderJsonEnvelopeCaptor.getAllValues();
        assertThat(capturedEnvelopes.get(0).metadata().name(), is("public.listing.hearing-listed"));
        assertThat(capturedEnvelopes.get(1).metadata().name(), is("public.listing.court-application-added-for-hearing"));
        
        // Verify the public event payload has empty caseUrns
        final JsonObject publicEventPayload = capturedEnvelopes.get(0).payloadAsJsonObject();
        assertThat(publicEventPayload.getJsonArray("caseUrns"), is(notNullValue()));
        assertThat(publicEventPayload.getJsonArray("caseUrns").size(), is(0));
    }

    @Test
    public void shouldHandleHearingListedEventMessageWhenProsecutionCasesIsEmpty() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(hearingListed.getHearing()).willReturn(hearing);
        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());

        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        uk.gov.justice.core.courts.HearingType mockHearingType = mock(uk.gov.justice.core.courts.HearingType.class);
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mockHearingType);
        given(mockHearingType.getDescription()).willReturn("Test Hearing Type");
        given(mockHearing.getId()).willReturn(randomUUID());
        given(mockHearing.getProsecutionCases()).willReturn(emptyList());

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        final List<JsonEnvelope> capturedEnvelopes = senderJsonEnvelopeCaptor.getAllValues();
        assertThat(capturedEnvelopes.get(0).metadata().name(), is("public.listing.hearing-listed"));
        assertThat(capturedEnvelopes.get(1).metadata().name(), is("public.listing.court-application-added-for-hearing"));
        
        // Verify the public event payload has empty caseUrns
        final JsonObject publicEventPayload = capturedEnvelopes.get(0).payloadAsJsonObject();
        assertThat(publicEventPayload.getJsonArray("caseUrns"), is(notNullValue()));
        assertThat(publicEventPayload.getJsonArray("caseUrns").size(), is(0));
    }

    @Test
    public void shouldHandleHearingListedEventMessageWhenProsecutionCaseIsNull() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(hearingListed.getHearing()).willReturn(hearing);
        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());

        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        uk.gov.justice.core.courts.HearingType mockHearingType = mock(uk.gov.justice.core.courts.HearingType.class);
        uk.gov.justice.core.courts.ProsecutionCase nullProsecutionCase = null;
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mockHearingType);
        given(mockHearingType.getDescription()).willReturn("Test Hearing Type");
        given(mockHearing.getId()).willReturn(randomUUID());
        given(mockHearing.getProsecutionCases()).willReturn(singletonList(nullProsecutionCase));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        final List<JsonEnvelope> capturedEnvelopes = senderJsonEnvelopeCaptor.getAllValues();
        assertThat(capturedEnvelopes.get(0).metadata().name(), is("public.listing.hearing-listed"));
        assertThat(capturedEnvelopes.get(1).metadata().name(), is("public.listing.court-application-added-for-hearing"));
        
        // Verify the public event payload has empty caseUrns (null prosecution case is filtered out)
        final JsonObject publicEventPayload = capturedEnvelopes.get(0).payloadAsJsonObject();
        assertThat(publicEventPayload.getJsonArray("caseUrns"), is(notNullValue()));
        assertThat(publicEventPayload.getJsonArray("caseUrns").size(), is(0));
    }

    @Test
    public void shouldHandleHearingListedEventMessageWhenProsecutionCaseIdentifierIsNull() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, HearingListed.class)).willReturn(hearingListed);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(hearingListed.getHearing()).willReturn(hearing);
        given(addHearingToCaseCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());
        given(addCourtApplicationToHearingCommandCollectionConverter.convert(hearingListed)).willReturn(emptyList());

        uk.gov.justice.core.courts.Hearing mockHearing = mock(uk.gov.justice.core.courts.Hearing.class);
        uk.gov.justice.core.courts.HearingType mockHearingType = mock(uk.gov.justice.core.courts.HearingType.class);
        uk.gov.justice.core.courts.ProsecutionCase mockProsecutionCase = mock(uk.gov.justice.core.courts.ProsecutionCase.class);
        given(hearingListingToCoreConverter.convert(any())).willReturn(mockHearing);
        given(mockHearing.getType()).willReturn(mockHearingType);
        given(mockHearingType.getDescription()).willReturn("Test Hearing Type");
        given(mockHearing.getId()).willReturn(randomUUID());
        given(mockHearing.getProsecutionCases()).willReturn(singletonList(mockProsecutionCase));
        given(mockProsecutionCase.getProsecutionCaseIdentifier()).willReturn(null);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleHearingListedMessage(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        final List<JsonEnvelope> capturedEnvelopes = senderJsonEnvelopeCaptor.getAllValues();
        assertThat(capturedEnvelopes.get(0).metadata().name(), is("public.listing.hearing-listed"));
        assertThat(capturedEnvelopes.get(1).metadata().name(), is("public.listing.court-application-added-for-hearing"));
        
        // Verify the public event payload has empty caseUrns (prosecution case with null identifier is filtered out)
        final JsonObject publicEventPayload = capturedEnvelopes.get(0).payloadAsJsonObject();
        assertThat(publicEventPayload.getJsonArray("caseUrns"), is(notNullValue()));
        assertThat(publicEventPayload.getJsonArray("caseUrns").size(), is(0));
    }

    @Test
    public void shouldHandleDefendantsToBeUpdatedMessage() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, DefendantsToBeUpdated.class)).willReturn(defendantsToBeUpdated);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateDefendantsForHearingCommandCollectionConverter.convert(defendantsToBeUpdated)).willReturn(singletonList(updateDefendantsForHearingCommand));
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleDefendantsToBeUpdatedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleDefendantsToBeUpdatedMessageHmiEnabled() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);

        JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

        given(jsonObjectConverter.convert(payload, DefendantsToBeUpdated.class)).willReturn(defendantsToBeUpdated);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateDefendantsForHearingCommandCollectionConverter.convert(defendantsToBeUpdated)).willReturn(singletonList(updateDefendantsForHearingCommand));
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleDefendantsToBeUpdatedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleDefendantsToBeAddedMessage() {
        //Given
        final List<UUID> hearings = singletonList(randomUUID());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, DefendantsToBeAddedForCourtProceedings.class)).willReturn(defendantsToBeAddedForCourtProceedings);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(addDefendantsForCourtProceedingsCommandCollectionConverter.convert(defendantsToBeAddedForCourtProceedings)).willReturn(singletonList(addDefendantsForCourtProceedingsCommand));
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleDefendantsToBeAddedForCourtProceedingsMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

    }

    @Test
    public void shouldHandleDefendantsToBeAddedMessageHmiEnabled() {
        //Given
        final List<UUID> hearings = singletonList(randomUUID());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, DefendantsToBeAddedForCourtProceedings.class)).willReturn(defendantsToBeAddedForCourtProceedings);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(addDefendantsForCourtProceedingsCommandCollectionConverter.convert(defendantsToBeAddedForCourtProceedings)).willReturn(singletonList(addDefendantsForCourtProceedingsCommand));
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleDefendantsToBeAddedForCourtProceedingsMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

    }

    @Test
    public void shouldHandleOffencesToBeUpdatedMessage() {
        //Given
        final List<UUID> hearings = singletonList(randomUUID());

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, OffencesToBeUpdated.class)).willReturn(offencesToBeUpdated);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateOffencesForHearingCommandCollectionConverter.convert(offencesToBeUpdated)).willReturn(singletonList(updateOffencesForHearingCommand));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleOffencesToBeUpdatedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleOffencesToBeAddedMessage() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, OffencesToBeAdded.class)).willReturn(offencesToBeAdded);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(addOffencesForHearingCommandCollectionConverter.convert(offencesToBeAdded)).willReturn(singletonList(addOffencesForHearingCommand));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleOffencesToBeAddedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleOffencesToBeDeletedMessage() {
        //Given
        final List<UUID> hearings = singletonList(randomUUID());
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, OffencesToBeDeleted.class)).willReturn(offencesToBeDeleted);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(deleteOffencesForHearingCommandCollectionConverter.convert(offencesToBeDeleted)).willReturn(singletonList(deleteOffencesForHearingCommand));

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleOffencesToBeDeletedMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleHearingAllocatedForListingMessage() {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        final HearingAllocatedForListing hearingAllocatedForListing = createHearingAllocatedForListing(false, false, false, null);
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListing.class)).willReturn(hearingAllocatedForListing);

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.CROWN);
        given(hearingConfirmedFactory.create(hearingAllocatedForListing, event)).willReturn(hearingConfirmed);

        //when
        listingEventProcessor.handleHearingAllocatedForListingMessage(event);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        verify(courtSchedulerServiceAdapter, never()).getJudicialRoles(anyString(), anyString(), any(), anyString());

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(PUBLIC_EVENT_HEARING_CHANGES_SAVED));

    }

    @Test
    public void shouldHandleHearingAllocatedForListingMessageWhenAdjournedAndJurisdictionTypeIsMagistrates() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        final HearingAllocatedForListing hearingAllocatedForListing = createHearingAllocatedForListing(true, true, true, null);
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListing.class)).willReturn(hearingAllocatedForListing);

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.MAGISTRATES);
        given(hearingConfirmedFactory.create(hearingAllocatedForListing, event)).willReturn(hearingConfirmed);

        final JsonObject hearingSlotsResponse = FileUtil.givenPayload("/stub-data/azure.rotasl.getHearingSlots.stub-data.json");
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = prepareRotaSLJudiciaryInfo(hearingSlotsResponse);

        //when
        listingEventProcessor.handleHearingAllocatedForListingMessage(event);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(PUBLIC_EVENT_HEARING_CHANGES_SAVED));

    }

    @Test
    public void shouldHandleHearingAllocatedForListingMessageWhenReceivedWithoutJudiciaryInfoAndGetFromRotaSL() {

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        //given
        final JsonEnvelope event = hearingAllocatedEvent();

        final HearingAllocatedForListing hearingAllocatedForListing = createHearingAllocatedForListing(false, true, false, null);

        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListing.class)).willReturn(hearingAllocatedForListing);

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.MAGISTRATES);
        given(hearingConfirmedFactory.create(hearingAllocatedForListing, event)).willReturn(hearingConfirmed);

        final JsonObject hearingSlotsResponse = FileUtil.givenPayload("/stub-data/azure.rotasl.getHearingSlots.stub-data.json");
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = prepareRotaSLJudiciaryInfo(hearingSlotsResponse);

        //when
        listingEventProcessor.handleHearingAllocatedForListingMessage(event);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(PUBLIC_EVENT_HEARING_CHANGES_SAVED));
    }

    @Test
    public void shouldHandleHearingAllocatedForListingV2Message() {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        final HearingAllocatedForListingV2 hearingAllocatedForListingV2 = new HearingAllocatedForListingV2.Builder()
                .withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .withHasAdjournmentDate(false)
                .withSendNotificationToParties(true)
                .build();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListingV2.class)).willReturn(hearingAllocatedForListingV2);

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.CROWN);
        given(hearingConfirmedFactory.createV2(hearingAllocatedForListingV2, event)).willReturn(hearingConfirmed);

        final ObjectToJsonValueConverter jsonValueConverter =  new JsonObjectConvertersFactory().objectToJsonValueConverter();
        final JsonValue hearingConfirmedJson = jsonValueConverter.convert(hearingConfirmed);

        given(objectToJsonValueConverter.convert(any())).willReturn(hearingConfirmedJson);
        //when
        listingEventProcessor.handleHearingAllocatedForListingV2Message(event);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope jsonEnvelope = senderJsonEnvelopeCaptor.getAllValues().get(0);
        assertThat(jsonEnvelope.metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(jsonEnvelope.payloadAsJsonObject().getBoolean("sendNotificationToParties"), is(true));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(PUBLIC_EVENT_HEARING_CHANGES_SAVED));

    }

    @Test
    public void shouldUpdateCourtScheduleForListingMessageWhenAdjournedAndJurisdictionTypeIsMagistrates() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        final HearingAllocatedForListing hearingAllocatedForListing = createHearingAllocatedForListing(true, true, true, null);
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListing.class)).willReturn(hearingAllocatedForListing);

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.MAGISTRATES);
        given(hearingConfirmedFactory.create(hearingAllocatedForListing, event)).willReturn(hearingConfirmed);

        final JsonObject hearingSlotsResponse = FileUtil.givenPayload("/stub-data/azure.rotasl.getHearingSlots.stub-data.json");
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = prepareRotaSLJudiciaryInfo(hearingSlotsResponse);

        //when
        listingEventProcessor.handleHearingAllocatedForListingMessage(event);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(PUBLIC_EVENT_HEARING_CHANGES_SAVED));
    }

    @Test
    public void shouldUpdateCourtScheduleFromAllocatedSlots() {
        final JsonEnvelope event = hearingAllocatedEvent();
        final UUID hearingId = randomUUID();
        final HearingAllocatedForListingV2 hearingAllocatedForListingV2 = new HearingAllocatedForListingV2.Builder()
                .withHearingId(hearingId)
                .withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .withHasAdjournmentDate(false)
                .withSendNotificationToParties(true)
                .build();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListingV2.class)).willReturn(hearingAllocatedForListingV2);

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.CROWN);
        given(hearingConfirmedFactory.createV2(hearingAllocatedForListingV2, event)).willReturn(hearingConfirmed);

        final ObjectToJsonValueConverter jsonValueConverter =  new JsonObjectConvertersFactory().objectToJsonValueConverter();
        final JsonValue hearingConfirmedJson = jsonValueConverter.convert(hearingConfirmed);
        given(objectToJsonValueConverter.convert(any())).willReturn(hearingConfirmedJson);

        final String courtScheduleId = randomUUID().toString();
        final String hearingDay = LocalDate.now().toString();
        SlotDetail slotDetail = new SlotDetail("ouCode",
                "ADULT",
                1,
                hearingDay,
                "session",
                randomUUID().toString(),
                30,
                courtScheduleId,
                randomUUID().toString(), "09:00:00");
        listingEventProcessor.handleHearingAllocatedForListingV2Message(event);

        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope jsonEnvelope = senderJsonEnvelopeCaptor.getAllValues().get(0);
        assertThat( jsonEnvelope.metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        final JsonObject commandJsonObj = jsonEnvelope.payloadAsJsonObject();
        final JsonObject confirmedHearing = commandJsonObj.getJsonObject("confirmedHearing");
        final JsonArray hearingDays = confirmedHearing.getJsonArray("hearingDays");
    }

    @Test
    public void shouldHandleHearingAllocatedForListingV2MessageWhenHearingIsSeeded() {
        //given
        final UUID hearingId = randomUUID();
        final JsonEnvelope event = hearingAllocatedEvent();

        final HearingAllocatedForListingV2 hearingAllocatedForListingV2 = createHearingAllocatedForListingV2(false, true, hearingId);
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListingV2.class)).willReturn(hearingAllocatedForListingV2);

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.CROWN);
        given(hearingConfirmedFactory.createV2(hearingAllocatedForListingV2, event)).willReturn(hearingConfirmed);

        //when
        listingEventProcessor.handleHearingAllocatedForListingV2Message(event);

        //then
        verify(sender, times(3)).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(COMMAND_CHANGE_NEXT_HEARING_DAY));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(2).metadata().name(), is(PUBLIC_EVENT_HEARING_CHANGES_SAVED));
        final JsonObject jsonObject = senderJsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject();
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldHandleAllocatedHearingUpdForListingMessage() {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing = new AllocatedHearingUpdatedForListing.Builder()
                .withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .build();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(),
                AllocatedHearingUpdatedForListing.class)).willReturn(allocatedHearingUpdatedForListing);

        final HearingUpdated hearingUpdated = hearingUpdated();
        given(allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing, event))
                .willReturn(hearingUpdated);

        //when
        listingEventProcessor.handleAllocatedHearingUpdatedForListingMessage(event);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_EVENT_HEARING_UPDATED));
    }

    @Test
    public void shouldUpdateCourtScheduleWithAllocatedHearingUpdatedForListingMessage() {
        JsonEnvelope event = hearingAllocatedEvent();
        UUID hearingId = randomUUID();
        AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing =
                new AllocatedHearingUpdatedForListing.Builder()
                                                     .withHearingId(hearingId)
                                                     .withUpdateSlot(false)
                                                     .withHearingDays(hearingDays)
                                                     .build();
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), AllocatedHearingUpdatedForListing.class))
                .thenReturn(allocatedHearingUpdatedForListing);

        HearingUpdated hearingUpdated = hearingUpdated();
        when(allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing, event)).thenReturn(hearingUpdated);

        listingEventProcessor.handleAllocatedHearingUpdatedForListingMessage(event);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_EVENT_HEARING_UPDATED));
    }

    @Test
    public void shouldNotUpdateCourtScheduleWithAllocatedHearingUpdatedForListingMessage() {
        final UUID courtScheduleId = randomUUID();
        JsonEnvelope event = hearingAllocatedEvent();
        UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = asList(HearingDay
                .hearingDay()
                .withCourtScheduleId(courtScheduleId)
                .withHearingDate(START_DATE)
                .withDurationMinutes(10).build());
        AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing =
                new AllocatedHearingUpdatedForListing.Builder()
                                                     .withHearingId(hearingId)
                                                     .withUpdateSlot(false)
                                                     .withHearingDays(hearingDays)
                                                     .build();

        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), AllocatedHearingUpdatedForListing.class))
                .thenReturn(allocatedHearingUpdatedForListing);

        HearingUpdated hearingUpdated = hearingUpdated();
        when(allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing, event)).thenReturn(hearingUpdated);

        SlotDetail slotDetail = new SlotDetail("ouCode",
                "ADULT",
                1,
                LocalDate.now().toString(),
                "AM",
                randomUUID().toString(),
                15,
                courtScheduleId.toString(),
                "Booking1", "09:00");

        listingEventProcessor.handleAllocatedHearingUpdatedForListingMessage(event);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_EVENT_HEARING_UPDATED));
    }

    @Test
    public void shouldHandleAllocatedHearingUpdatedForListingV2Message() {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListingV2 = new AllocatedHearingUpdatedForListingV2.Builder()
                .withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .build();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(),
                AllocatedHearingUpdatedForListingV2.class)).willReturn(allocatedHearingUpdatedForListingV2);

        final HearingUpdated hearingUpdated = hearingUpdated();
        given(allocatedHearingUpdatedFactory.createV2(allocatedHearingUpdatedForListingV2, event))
                .willReturn(hearingUpdated);

        //when
        listingEventProcessor.handleAllocatedHearingUpdatedForListingV2Message(event);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_EVENT_HEARING_UPDATED));
    }

    @Test
    public void shouldHandleAllocatedHearingUpdatedForListingV2MessageWhenHearingIsSeeded() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope event = hearingAllocatedEvent();

        final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListingV2 = createAllocatedHearingUpdatedForListingV2(hearingId);

        given(jsonObjectConverter.convert(event.payloadAsJsonObject(),
                AllocatedHearingUpdatedForListingV2.class)).willReturn(allocatedHearingUpdatedForListingV2);

        final HearingUpdated hearingUpdated = hearingUpdated();
        given(allocatedHearingUpdatedFactory.createV2(allocatedHearingUpdatedForListingV2, event))
                .willReturn(hearingUpdated);
        listingEventProcessor.handleAllocatedHearingUpdatedForListingV2Message(event);

        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(PUBLIC_EVENT_HEARING_UPDATED));
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(COMMAND_CHANGE_NEXT_HEARING_DAY));
        final JsonObject jsonObject = senderJsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject();
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));

    }

    @Test
    public void shouldHandleVacateTrialMessage() {
        //given
        final TrialVacated trialVacated = TrialVacated.trialVacated()
                .withHearingId(HEARING_ID)
                .withVacatedTrialReasonId(UUID.randomUUID())
                .withAllocated(true)
                .build();

        final JsonEnvelope event = generateTrialVacatedEvent(trialVacated);
        when(jsonObjectConverter.convert(any(), any())).thenReturn(trialVacated);

        //when
        listingEventProcessor.trialVacated(event);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_EVENT_VACATED_TRIAL_UPDATED));

    }

    @Test
    public void shouldHandlePublicHearingVacatedTrial() {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        //when
        listingEventProcessor.handleHearingTrialVacated(event);
        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PRIVATE_COMMAND_HEARING_VACATE_TRIAL));
    }

    @Test
    public void shouldHandlePublicApplicationOffenceUpdated() {
        final ApplicationOffencesUpdated applicationOffencesUpdated = ApplicationOffencesUpdated.applicationOffencesUpdated()
                .withApplicationId(randomUUID())
                .withOffenceId(randomUUID())
                .withSubjectId(randomUUID())
                .withLaaReference(LaaReference.laaReference().withStatusId(randomUUID()).build())
                .build();
        //given
        final Envelope<ApplicationOffencesUpdated> envelope = Envelope.envelopeFrom(metadataWithRandomUUID("public.progression.application-offences-updated"), applicationOffencesUpdated);
        //when
        listingEventProcessor.handleApplicationOffenceUpdated(envelope);
        //then
        verify(sender).send(applicationOffencesUpdatedeCaptor.capture());
        assertThat(applicationOffencesUpdatedeCaptor.getValue().metadata().name(), is("listing.command.update-laa-reference-for-application"));
        assertThat(applicationOffencesUpdatedeCaptor.getValue().payload(), is(applicationOffencesUpdated));
    }

    @Test
    public void shouldHandleApplicationLaaReferenceUpdatedForApplication() {
        final ApplicationLaaReferenceUpdatedForApplication applicationLaaReferenceUpdatedForApplication = ApplicationLaaReferenceUpdatedForApplication.applicationLaaReferenceUpdatedForApplication()
                .withApplicationId(randomUUID())
                .withSubjectId(randomUUID())
                .withLaaReference(LaaReference.laaReference().withStatusId(randomUUID()).build())
                .build();
        //given
        final Envelope<ApplicationLaaReferenceUpdatedForApplication> envelope = Envelope.envelopeFrom(metadataWithRandomUUID("public.progression.application-laa-reference-updated-for-application"), applicationLaaReferenceUpdatedForApplication);
        //when
        listingEventProcessor.handleApplicationLaaReferenceUpdatedForApplication(envelope);
        //then
        verify(sender).send(applicationLaaReferenceUpdatedForApplicationCaptor.capture());
        assertThat(applicationLaaReferenceUpdatedForApplicationCaptor.getValue().metadata().name(), is("listing.command.update-laa-reference-for-application"));
        assertThat(applicationLaaReferenceUpdatedForApplicationCaptor.getValue().payload(), is(applicationLaaReferenceUpdatedForApplication));
    }


    @Test
    public void shouldHandleHearingRescheduledMessage() {

        //given
        final HearingRescheduled hearingRescheduled = HearingRescheduled.hearingRescheduled()
                .withHearingId(HEARING_ID)
                .withAllocated(false)
                .build();
        final JsonEnvelope event = hearingRescheduledEvent(hearingRescheduled);

        when(jsonObjectConverter.convert(any(), any())).thenReturn(hearingRescheduled);

        //when
        listingEventProcessor.hearingRescheduled(event);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_EVENT_VACATED_TRIAL_UPDATED));
    }

    @Test
    public void shouldHandleRestrictCourtListForListingMessage() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CourtListRestricted.class)).willReturn(restrictCourtList);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleRestrictCourtListMessage(envelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

    }

    @Test
    public void shouldHandleCaseMarkerUpdateMessageForListingMessage() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CaseMarkersToBeUpdated.class)).willReturn(caseMarkersToBeUpdated);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        CaseMarker caseMarker = new CaseMarker(randomUUID(), "markerTypeCode", "markerTypeDescription", randomUUID());
        UpdateCaseMarkersForHearingCommand updateCaseMarkersForHearingCommand = new UpdateCaseMarkersForHearingCommand(randomUUID(), randomUUID(), asList(caseMarker));
        when(updateCaseMarkersForHearingCommandCollectionConverter.convert(any(CaseMarkersToBeUpdated.class))).thenReturn(asList(updateCaseMarkersForHearingCommand));

        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);

        //when
        listingEventProcessor.handleCaseMarkerUpdateMessage(envelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

    }

    @Test
    public void shouldHandleCaseMarkerUpdateMessageForListingMessageHmiEnabled() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CaseMarkersToBeUpdated.class)).willReturn(caseMarkersToBeUpdated);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        CaseMarker caseMarker = new CaseMarker(randomUUID(), "markerTypeCode", "markerTypeDescription", randomUUID());
        UpdateCaseMarkersForHearingCommand updateCaseMarkersForHearingCommand = new UpdateCaseMarkersForHearingCommand(randomUUID(), randomUUID(), asList(caseMarker));
        when(updateCaseMarkersForHearingCommandCollectionConverter.convert(any(CaseMarkersToBeUpdated.class))).thenReturn(asList(updateCaseMarkersForHearingCommand));

        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);

        //when
        listingEventProcessor.handleCaseMarkerUpdateMessage(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

    }

    private JsonEnvelope hearingAllocatedEvent() {

        final JsonObjectBuilder hearingDate = createObjectBuilder()
                .add("startDate", START_DATE.toString())
                .add("startTime", START_TIME.toString());

        final JsonObjectBuilder hearingAllocated = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TYPE)
                .add("estimatedMinutes", ESTIMATED_MINUTES)
                .add("estimatedDuration", ESTIMATED_DURATION)
                .add("judgeId", JUDICIAL_ID.toString())
                .add("updateSlot", true)
                .add("sendNotificationToParties", true)
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), hearingAllocated.build());
    }

    private JsonEnvelope generateTrialVacatedEvent(TrialVacated trialVacated) {

        final JsonObjectBuilder trialVacatedJson = createObjectBuilder()
                .add("hearingId", trialVacated.getHearingId().toString())
                .add("vacatedTrialReasonId", trialVacated.getVacatedTrialReasonId().toString())
                .add("allocated", trialVacated.getAllocated());

        return envelopeFrom(metadataWithRandomUUIDAndName(), trialVacatedJson.build());
    }

    private JsonEnvelope hearingRescheduledEvent(HearingRescheduled hearingRescheduled) {

        final JsonObjectBuilder hearingRescheduledJson = createObjectBuilder()
                .add("hearingId", hearingRescheduled.getHearingId().toString())
                .add("allocated", hearingRescheduled.getAllocated());

        return envelopeFrom(metadataWithRandomUUIDAndName(), hearingRescheduledJson.build());
    }

    private HearingUpdated hearingUpdated() {
        final String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);

        return HearingUpdated.hearingUpdated()
                .withUpdatedHearing(buildHearing(formattedDateTime, JurisdictionType.CROWN))
                .build();
    }

    @Test
    public void shouldHandleCaseDefendantChangedMessage() throws Exception {
        final DefendantUpdated defendantUpdated = defendantUpdated();
        final JsonObject caseDefendantChangeJsonObject = this.objectToJsonObjectConverter.convert(defendantUpdated);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), caseDefendantChangeJsonObject);

        //when
        listingEventProcessor.handleCaseDefendantChangedMessage(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_UPDATE_CASE_DEFENDANT_DETAILS));
        final DefendantUpdated resultPayload = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), DefendantUpdated.class);

        assertThat(resultPayload, equalTo(defendantUpdated));
        assertThat(resultPayload, not(equalTo(defendantUpdated())));
    }

    @Test
    public void shouldHandleDefendantsAddedForCourtProceedings() {
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = defendantsAddedToCourtProceedings();
        final JsonObject defendantAddedToCourtProceedingsJsonObject = this.objectToJsonObjectConverter.convert(defendantsAddedToCourtProceedings);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), defendantAddedToCourtProceedingsJsonObject);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.events.cps-prosecutor-updated"),
                Json.createObjectBuilder()
                        .add("prosecutionCaseId", "34d07e81-9770-4d23-af6f-84f1d7571bd3")
                        .add("hearingIds", Json.createArrayBuilder()
                                .add("a8448a33-68ab-4b9b-84c2-59cee4fe36f4")
                                .add("095d7412-ba76-4a15-942d-566d3aeae7c9")
                                .add("095d7412-ba76-4a15-942d-566d3aeae7c8")
                                .build())
                        .add("caseURN", "test Case URN")
                        .add("prosecutionAuthorityId", "test prosecutionAuthorityId")
                        .add("prosecutionAuthorityReference", "test prosecutionAuthorityReference")
                        .add("prosecutionAuthorityCode", "test prosecutionAuthorityCode")
                        .add("prosecutionAuthorityName", "test prosecutionAuthorityName")
                        .add("address", Json.createObjectBuilder()
                                .add("address1", "41 Manhattan House")
                                .add("postcode", "MK9 2BQ")
                                .build())
                        .build());

        JsonEnvelope queryEnvelope = envelopeFrom(metadataFrom(event.metadata()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add("hearings", createArrayBuilder()
                                .add(createObjectBuilder().add("id", "a8448a33-68ab-4b9b-84c2-59cee4fe36f4")
                                        .add("allocated", true).build())
                                .add(createObjectBuilder().add("id", "095d7412-ba76-4a15-942d-566d3aeae7c9")
                                        .add("allocated", false).build()))
        );

        listingEventProcessor.handleDefendantAddedForCourtProceedings(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS));
        final DefendantsAddedToCourtProceedings resultPayload = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), DefendantsAddedToCourtProceedings.class);
        assertThat(resultPayload, equalTo(defendantsAddedToCourtProceedings));

    }

    @Test
    public void shouldHandleDefendantsAddedForCourtProceedingsHmiEnabled() {
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = defendantsAddedToCourtProceedings();
        final JsonObject defendantAddedToCourtProceedingsJsonObject = this.objectToJsonObjectConverter.convert(defendantsAddedToCourtProceedings);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), defendantAddedToCourtProceedingsJsonObject);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.events.cps-prosecutor-updated"),
                Json.createObjectBuilder()
                        .add("prosecutionCaseId", "34d07e81-9770-4d23-af6f-84f1d7571bd3")
                        .add("hearingIds", Json.createArrayBuilder()
                                .add("a8448a33-68ab-4b9b-84c2-59cee4fe36f4")
                                .add("095d7412-ba76-4a15-942d-566d3aeae7c9")
                                .add("095d7412-ba76-4a15-942d-566d3aeae7c8")
                                .build())
                        .add("caseURN", "test Case URN")
                        .add("prosecutionAuthorityId", "test prosecutionAuthorityId")
                        .add("prosecutionAuthorityReference", "test prosecutionAuthorityReference")
                        .add("prosecutionAuthorityCode", "test prosecutionAuthorityCode")
                        .add("prosecutionAuthorityName", "test prosecutionAuthorityName")
                        .add("address", Json.createObjectBuilder()
                                .add("address1", "41 Manhattan House")
                                .add("postcode", "MK9 2BQ")
                                .build())
                        .build());

        JsonEnvelope queryEnvelope = envelopeFrom(metadataFrom(event.metadata()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add("hearings", createArrayBuilder()
                                .add(createObjectBuilder().add("id", "a8448a33-68ab-4b9b-84c2-59cee4fe36f4")
                                        .add("allocated", true).build())
                                .add(createObjectBuilder().add("id", "095d7412-ba76-4a15-942d-566d3aeae7c9")
                                        .add("allocated", false).build()))
        );

        JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

        listingEventProcessor.handleDefendantAddedForCourtProceedings(jsonEnvelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS));
        final DefendantsAddedToCourtProceedings resultPayload = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getAllValues().get(0).payloadAsJsonObject(), DefendantsAddedToCourtProceedings.class);
        assertThat(resultPayload, equalTo(defendantsAddedToCourtProceedings));

    }

    @Test
    public void shouldHandleDefendantOffencesChangedMessage() throws Exception {
        final OffencesForDefendantUpdated offencesForDefendantUpdated = offencesForDefendantUpdated();
        final JsonObject defendantOffencesChangedJsonObject = this.objectToJsonObjectConverter.convert(offencesForDefendantUpdated);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), defendantOffencesChangedJsonObject);

        //when
        listingEventProcessor.handleDefendantOffencesChanged(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES));
        final OffencesForDefendantUpdated resultPayload = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), OffencesForDefendantUpdated.class);
        assertThat(resultPayload, equalTo(offencesForDefendantUpdated));
        assertThat(resultPayload, not(equalTo(offencesForDefendantUpdated())));
    }

    @Test
    public void shouldHandleCourtApplicationUpdatedOnHearingMessage() {
        final CourtApplicationChanged courtApplicationChanged = courtApplicationChanged();
        final JsonObject courtApplicationChangedJsonObject = this.objectToJsonObjectConverter.convert(courtApplicationChanged);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), courtApplicationChangedJsonObject);

        //when
        listingEventProcessor.handleCourtApplicationChanged(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_UPDATE_COURT_APPLICATION));
        final CourtApplicationChanged resultPayload = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), CourtApplicationChanged.class);

        assertThat(resultPayload, equalTo(courtApplicationChanged));
        assertThat(resultPayload, not(equalTo(courtApplicationChanged())));
    }

    @Test
    public void shouldHandleCourtApplicationAddedForListedHearing() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CourtApplicationAddedForHearing.class)).willReturn(courtApplicationAddedForHearing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(courtApplicationAddedForHearing.getCourtApplication())
                .willReturn(uk.gov.justice.listing.events.CourtApplication.courtApplication().withId(randomUUID()).build());
        given(courtApplicationAddedForHearing.getHearingId())
                .willReturn(randomUUID());
        given(jsonObjectConverter.convert(payload, CourtApplicationAddedForHearing.class)).willReturn(courtApplicationAddedForHearing);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleCourtApplicationAddedForListedHearing(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleCourtApplicationAddedForListedHearingHmiEnabled() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CourtApplicationAddedForHearing.class)).willReturn(courtApplicationAddedForHearing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(courtApplicationAddedForHearing.getCourtApplication())
                .willReturn(uk.gov.justice.listing.events.CourtApplication.courtApplication().withId(randomUUID()).build());
        given(courtApplicationAddedForHearing.getHearingId())
                .willReturn(randomUUID());
        given(jsonObjectConverter.convert(payload, CourtApplicationAddedForHearing.class)).willReturn(courtApplicationAddedForHearing);

        JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleCourtApplicationAddedForListedHearing(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is("public.listing.court-application-added-for-hearing") );
    }

    @Test
    public void shouldHandleCourtApplicationToBeUpdated() {
        //Given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleCourtApplicationToBeUpdated(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    private CourtApplicationChanged courtApplicationChanged() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final CourtApplicationChanged.Builder courtApplicationChangedBuilder = new CourtApplicationChanged.Builder();
        final CourtApplication.Builder courtApplicationBuilder = new CourtApplication.Builder();
        final CourtApplicationParty.Builder courtApplicationPartyBuilder = new CourtApplicationParty.Builder();

        final CourtApplication courtApplication = courtApplicationBuilder
                .withId(randomUUID())
                .withApplicationReference(STRING.next())
                .withApplicationParticulars(STRING.next())
                .withApplicant(courtApplicationPartyBuilder
                        .withId(randomUUID())
                        .withPersonDetails(null)
                        .build())
                .build();


        final CourtApplicationChanged courtapplicationChanged = courtApplicationChangedBuilder
                .withCourtApplication(courtApplication)
                .build();

        return courtapplicationChanged;
    }

    @Test
    public void shouldHandleEjectEventFromProgressionAndPassToCommandHandler() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final CaseOrApplicationEjected ejectCaseOrApplication = CaseOrApplicationEjected.caseOrApplicationEjected()
                .withHearingIds(Arrays.asList(HEARING_ID))
                .withProsecutionCaseId(CASE_ID).build();
        final JsonObject ejectCaseOrApplicationObject = this.objectToJsonObjectConverter.convert(ejectCaseOrApplication);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), ejectCaseOrApplicationObject);
        final JsonEnvelopeMatcher jsonEnvelopeMatcher = new JsonEnvelopeMatcher();

        listingEventProcessor.handleEventsCaseOrApplicationEjected(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_CASE_OR_APPLICATION_EJECTED));

        final CaseOrApplicationEjected resultPayload = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), CaseOrApplicationEjected.class);
        assertThat(resultPayload, equalTo(ejectCaseOrApplication));

    }

    @Test
    public void shouldHandleEventsCaseEjectedForAllHearingsAndPassToCommandHandler() {

        final JsonObject ejectCaseForAllHearingObject = createObjectBuilder()
                .add(HEARING_IDS, Json.createArrayBuilder().add(HEARING_ID.toString()).build())
                .add(PROSECUTION_CASE_ID, CASE_ID.toString())
                .add(REMOVAL_REASON, "removal Reason")
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), ejectCaseForAllHearingObject);
        final JsonEnvelopeMatcher jsonEnvelopeMatcher = new JsonEnvelopeMatcher();

        //given

        listingEventProcessor.handleEventsCaseEjectedForAllHearings(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_CASE_EJECTED));
        final JsonObject commandPayloadObject = senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayloadObject.getString(FIELD_HEARING_ID), is(HEARING_ID.toString()));
        assertThat(commandPayloadObject.getString(PROSECUTION_CASE_ID), is(CASE_ID.toString()));
        assertThat(commandPayloadObject.getString(REMOVAL_REASON), is("removal Reason"));
    }

    @Test
    public void shouldHandleEventsApplicationEjectedForAllHearingsAndPassToCommandHandler() {

        final JsonObject ejectApplicationForAllHearingObject = createObjectBuilder()
                .add(HEARING_IDS, Json.createArrayBuilder().add(HEARING_ID.toString()).build())
                .add(FIELD_APPLICATION_ID, APPLICATION_ID.toString())
                .add(REMOVAL_REASON, "removal Reason")
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), ejectApplicationForAllHearingObject);
        final JsonEnvelopeMatcher jsonEnvelopeMatcher = new JsonEnvelopeMatcher();

        //given

        listingEventProcessor.handleEventsApplicationEjectedForAllHearings(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_APPLICATION_EJECTED));
        final JsonObject commandPayloadObject = senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayloadObject.getString(FIELD_HEARING_ID), is(HEARING_ID.toString()));
        assertThat(commandPayloadObject.getString(FIELD_APPLICATION_ID), is(APPLICATION_ID.toString()));
        assertThat(commandPayloadObject.getString(REMOVAL_REASON), is("removal Reason"));
    }


    @Test
    public void shouldHandleCourtApplicationAddedOnHearingMessage() {
        final CourtApplication courtApplicationAddedForHearings = courtApplicationAdded();
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();

        final HearingExtended hearingExtended = HearingExtended.hearingExtended()
                .withCourtApplication(courtApplicationAddedForHearings)
                .withProsecutionCases(prosecutionCases)
                .build();
        final JsonObject hearingExtendedJsonObject = this.objectToJsonObjectConverter.convert(hearingExtended);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), hearingExtendedJsonObject);

        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                HearingExtended.class)).willReturn(hearingExtended);

        //when
        listingEventProcessor.handleExtendListedHearingForCourtApplication(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING));
        final HearingExtended hearingExtendedResult = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), HearingExtended.class);

        assertThat(hearingExtendedResult.getCourtApplication(), equalTo(courtApplicationAddedForHearings));
        assertThat(hearingExtendedResult.getCourtApplication(), not(equalTo(courtApplicationAdded())));
    }

    @Test
    public void shouldHandleProsecutionCasesOnlyWhenNoApplicationAddedOnHearingMessage() {
        final CourtApplication courtApplicationAddedForHearings = courtApplicationAdded();
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        prosecutionCases.add(prosecutionCase);

        final HearingExtended hearingExtended = HearingExtended.hearingExtended()
                .withProsecutionCases(prosecutionCases)
                .build();

        final JsonObject hearingExtendedJsonObject = this.objectToJsonObjectConverter.convert(hearingExtended);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), hearingExtendedJsonObject);


        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                HearingExtended.class)).willReturn(hearingExtended);

        //when
        listingEventProcessor.handleExtendListedHearingForCourtApplication(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.add-cases-to-hearing"));
        final HearingExtended hearingExtendedResult = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(), HearingExtended.class);

        assertThat(hearingExtendedResult.getProsecutionCases().size(), equalTo(1));
        assertThat(hearingExtendedResult.getProsecutionCases().get(0), equalTo(prosecutionCase));
    }

    @Test
    public void shouldHandleBothProsecutionCasesApplicationAddedOnHearingMessage() {
        final CourtApplication courtApplicationAddedForHearings = courtApplicationAdded();
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        prosecutionCases.add(prosecutionCase);


        final HearingExtended hearingExtended = HearingExtended.hearingExtended()
                .withProsecutionCases(prosecutionCases)
                .withCourtApplication(courtApplicationAddedForHearings)
                .build();
        final JsonObject hearingExtendedJsonObject = this.objectToJsonObjectConverter.convert(hearingExtended);

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), hearingExtendedJsonObject);


        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                HearingExtended.class)).willReturn(hearingExtended);

        //when
        listingEventProcessor.handleExtendListedHearingForCourtApplication(jsonEnvelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is("listing.command.add-cases-to-hearing"));
        final HearingExtended hearingExtendedResult = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getAllValues().get(0).payloadAsJsonObject(), HearingExtended.class);

        assertThat(hearingExtendedResult.getProsecutionCases().size(), equalTo(1));
        assertThat(hearingExtendedResult.getProsecutionCases().get(0), equalTo(prosecutionCase));
        assertThat(hearingExtendedResult.getCourtApplication(), nullValue());

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is("listing.command.add-court-application-for-hearing"));
        final HearingExtended addCourtApplicationForHearing = jsonObjectConverterForTest
                .convert(senderJsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject(), HearingExtended.class);

        assertThat(addCourtApplicationForHearing.getProsecutionCases().size(), equalTo(1));
        assertThat(addCourtApplicationForHearing.getProsecutionCases().get(0), equalTo(prosecutionCase));
        assertThat(addCourtApplicationForHearing.getCourtApplication(), equalTo(courtApplicationAddedForHearings));

    }

    @Test
    public void shouldProcessPublicHearingExtendedOnlyForCourtHearingAndNotBoxWork() {
        final CourtApplication courtApplicationAddedForHearings = courtApplicationAdded();
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        prosecutionCases.add(prosecutionCase);
        final JsonObject courtApplicationChangedJsonObject = this.objectToJsonObjectConverter.convert(courtApplicationAddedForHearings);
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), courtApplicationChangedJsonObject);

        final HearingExtended hearingExtended = HearingExtended.hearingExtended()
                .withProsecutionCases(prosecutionCases)
                .withIsBoxWorkRequest(true)
                .build();
        //given
        given(jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(),
                HearingExtended.class)).willReturn(hearingExtended);
        //when
        listingEventProcessor.handleExtendListedHearingForCourtApplication(jsonEnvelope);
        //then
        verify(sender, never()).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void testDefendantLegalAidStatusUpdate() {

        final String LEGAL_AID_STATUS = "legalAidStatus";

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendantId", DEFENDANT_ID.toString())
                .add("caseId", CASE_ID.toString())
                .add(LEGAL_AID_STATUS, "Granted")
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.defendant-legalaid-status-updated"),
                eventPayload);

        listingEventProcessor.defendantLegalStatusUpdate(event);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> events = this.senderJsonEnvelopeCaptor.getAllValues();

        assertThat(events.get(0).metadata().name(), is("listing.command.update-defendant-legalaid-status"));

        assertThat(events.get(0).payloadAsJsonObject().getString("defendantId"), is(DEFENDANT_ID.toString()));

        assertThat(events.get(0).payloadAsJsonObject().getString("caseId"), is(CASE_ID.toString()));

        assertThat(events.get(0).payloadAsJsonObject().getString(LEGAL_AID_STATUS), is("Granted"));

    }

    @Test
    public void testHandleDefendantLegalStatusUpdateForHearings() {

        final String LEGAL_AID_STATUS = "legalAidStatus";

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendantId", DEFENDANT_ID.toString())
                .add("caseId", CASE_ID.toString())
                .add("hearingIds", Json.createArrayBuilder().add(HEARING_ID.toString()).build())
                .add(LEGAL_AID_STATUS, "Granted")
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.defendant-legalaid-status-updated"),
                eventPayload);

        listingEventProcessor.handleDefendantLegalStatusUpdateForHearings(event);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> events = this.senderJsonEnvelopeCaptor.getAllValues();

        assertThat(events.get(0).metadata().name(), is("listing.command.update-defendant-legalaid-status-for-hearing"));

        assertThat(events.get(0).payloadAsJsonObject().getString("defendantId"), is(DEFENDANT_ID.toString()));

        assertThat(events.get(0).payloadAsJsonObject().getString("defendantId"), is(DEFENDANT_ID.toString()));

        assertThat(events.get(0).payloadAsJsonObject().getString("caseId"), is(CASE_ID.toString()));

        assertThat(events.get(0).payloadAsJsonObject().getString(LEGAL_AID_STATUS), is("Granted"));

    }


    @Test
    public void shouldHandleHearingResultedCaseUpdatedEvent() {
        final ProsecutionCase prosecutionCase = getProsecutionCase();
        final JsonObject eventPayload = createObjectBuilder()
                .add("prosecutionCase", prosecutionCase.toString())
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.event.hearing-resulted-case-updated"),
                eventPayload);

        listingEventProcessor.handleHearingResultedAndCaseUpdated(event);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> events = this.senderJsonEnvelopeCaptor.getAllValues();

        assertThat(events.get(0).metadata().name(), is("listing.command.update-case-resulted-defendant-proceedings-concluded"));
        assertThat(events.get(0).payloadAsJsonObject().getString("prosecutionCase"), is(prosecutionCase.toString()));
    }

    @Test
    public void shouldHandleCaseLinkedPublicEvent() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final CaseLinked caseLinked = CaseLinked.caseLinked()
                .withLinkActionType(LinkActionType.LINK)
                .withCases(asList(
                        Cases.cases()
                                .withCaseId(CASE_ID)
                                .withCaseUrn("URN")
                                .withLinkedToCases(asList(
                                        uk.gov.justice.progression.courts.LinkedToCases.linkedToCases()
                                                .withCaseUrn("URN")
                                                .withCaseId(randomUUID())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final JsonObject eventPayload = objectToJsonObjectConverter.convert(caseLinked);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.case-linked"),
                eventPayload);

        listingEventProcessor.handleCaseLinkedPublicEvent(event);

        verify(this.sender, times(1)).send(this.senderDefaultEnvelopeCaptor.capture());

        final List<DefaultEnvelope<JsonObject>> events = this.senderDefaultEnvelopeCaptor.getAllValues();
        assertThat(events.get(0).metadata().name(), is("listing.command.update-linked-cases"));
        assertThat(events.get(0).payload().getString("linkActionType"), is(caseLinked.getLinkActionType().toString()));
    }

    @Test
    public void shouldHandleCaseLinkedPrivateEvent() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final UUID hearing1 = randomUUID();
        final UUID hearing2 = randomUUID();

        final LinkedCasesToBeUpdated linkedCasesToBeUpdated = LinkedCasesToBeUpdated.linkedCasesToBeUpdated()
                .withCaseId(CASE_ID)
                .withCaseUrn("URN")
                .withHearingIds(asList(hearing1, hearing2))
                .withLinkActionType("LINK")
                .withLinkedToCases(asList(
                        LinkedToCases.linkedToCases()
                                .withCaseId(randomUUID())
                                .withCaseUrn("URN")
                                .build()
                ))
                .build();

        final JsonObject eventPayload = objectToJsonObjectConverter.convert(linkedCasesToBeUpdated);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.linked-cases-to-be-updated"),
                eventPayload);

        given(jsonObjectConverter.convert(eventPayload, LinkedCasesToBeUpdated.class)).willReturn(linkedCasesToBeUpdated);

        listingEventProcessor.handleCaseLinkedPrivateEvent(event);
        verify(this.sender, times(2)).send(this.senderDefaultEnvelopeCaptor.capture());
        final List<DefaultEnvelope<JsonObject>> events = this.senderDefaultEnvelopeCaptor.getAllValues();
        assertThat(events.get(0).metadata().name(), is("listing.command.update-linked-case-in-hearing"));
        assertThat(events.get(0).payload().getString("hearingId"), is(hearing1.toString()));

        assertThat(events.get(1).metadata().name(), is("listing.command.update-linked-case-in-hearing"));
        assertThat(events.get(1).payload().getString("hearingId"), is(hearing2.toString()));
    }

    @Test
    public void shouldHandleDefendantProceedingsConcludedUpdatedEvent() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final ProsecutionCase prosecutionCase = getProsecutionCase();
        final UUID hearingId = randomUUID();
        final CaseResultedDefendantProceedingsConcluded caseResultedDefendantProceedingsConcluded = CaseResultedDefendantProceedingsConcluded
                .caseResultedDefendantProceedingsConcluded()
                .withHearingIds(singletonList(hearingId))
                .withProsecutionCase(prosecutionCase)
                .build();
        final JsonObject eventPayload = createObjectBuilder()
                .add("hearingIds", caseResultedDefendantProceedingsConcluded.getHearingIds().toString())
                .add("prosecutionCase", caseResultedDefendantProceedingsConcluded.getProsecutionCase().toString())
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.case-resulted-defendant-proceedings-updated"),
                eventPayload);

        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseResultedDefendantProceedingsConcluded.class)).willReturn(caseResultedDefendantProceedingsConcluded);

        listingEventProcessor.handleCaseResultedAndDefendantProceedingsUpdated(event);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());
        final List<JsonEnvelope> events = this.senderJsonEnvelopeCaptor.getAllValues();

        assertThat(events.get(0).metadata().name(), is("listing.command.update-defendant-court-proceedings"));
        assertThat(events.get(0).payloadAsJsonObject().getString("hearingId"), is(caseResultedDefendantProceedingsConcluded.getHearingIds().get(0).toString()));
        assertThat(events.get(0).payloadAsJsonObject().getJsonObject("prosecutionCase"), notNullValue());
    }

    @Test
    public void shouldHandleAllocatedHearingExtendedForListingMessage() {
        //given
        final JsonEnvelope event = allocatedHearingExtendedForListingEvent();
        final ListedCase listedCase = ListedCase.listedCase().withId(randomUUID()).build();
        final List<ListedCase> listedCases = new ArrayList<>();
        listedCases.add(listedCase);

        allocatedHearingExtendedForListing = AllocatedHearingExtendedForListing.allocatedHearingExtendedForListing()
                .withUnAllocatedListedCases(listedCases).build();

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.CROWN);

        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), AllocatedHearingExtendedForListing.class))
                .willReturn(allocatedHearingExtendedForListing);
        given(allocatedHearingExtendedFactory.create(allocatedHearingExtendedForListing, event))
                .willReturn(hearingConfirmed);

        final JsonObject jsonObject = createObjectBuilder().add("confirmedHearing", createObjectBuilder().add(ID, HEARING_ID.toString())).build();
        lenient().when(objectToJsonValueConverter.convert(any(HearingConfirmed.class))).thenReturn(jsonObject);
        //when
        listingEventProcessor.handleAllocatedHearingExtendedForListingMessage(event);

        //then
        verify(sender, times(2)).send(senderDefaultEnvelopeCaptor.capture());
        final List<DefaultEnvelope<JsonObject>> allValues = senderDefaultEnvelopeCaptor.getAllValues();

        assertThat(allValues.get(0).metadata().name(), is(COMMAND_UPDATE_HEARING_TO_CASE));
        assertThat(allValues.get(1).metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(allValues.get(1).payload().getJsonObject("confirmedHearing").getString(ID), equalTo(HEARING_ID.toString()));
    }

    @Test
    public void shouldHandleAllocatedHearingExtendedForListingV2Message() {
        //given
        final JsonEnvelope event = allocatedHearingExtendedForListingEvent();
        final ListedCase listedCase = ListedCase.listedCase().withId(randomUUID()).build();
        final List<ListedCase> listedCases = new ArrayList<>();
        listedCases.add(listedCase);

        allocatedHearingExtendedForListingV2 = AllocatedHearingExtendedForListingV2.allocatedHearingExtendedForListingV2()
                .withUnAllocatedListedCases(listedCases).build();

        final HearingConfirmed hearingConfirmed = hearingConfirmed(JurisdictionType.CROWN);

        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), AllocatedHearingExtendedForListingV2.class))
                .willReturn(allocatedHearingExtendedForListingV2);
        given(allocatedHearingExtendedFactory.create(allocatedHearingExtendedForListingV2, event))
                .willReturn(hearingConfirmed);

        final JsonObject jsonObject = createObjectBuilder().add("confirmedHearing", createObjectBuilder().add(ID, HEARING_ID.toString())).build();
        lenient().when(objectToJsonValueConverter.convert(any(HearingConfirmed.class))).thenReturn(jsonObject);
        //when
        listingEventProcessor.handleAllocatedHearingExtendedForListingV2Message(event);

        //then
        verify(sender, times(2)).send(senderDefaultEnvelopeCaptor.capture());
        final List<DefaultEnvelope<JsonObject>> allValues = senderDefaultEnvelopeCaptor.getAllValues();

        assertThat(allValues.get(0).metadata().name(), is(COMMAND_UPDATE_HEARING_TO_CASE));
        assertThat(allValues.get(1).metadata().name(), is(PUBLIC_EVENT_HEARING_CONFIRMED));
        assertThat(allValues.get(1).payload().getJsonObject("confirmedHearing").getString(ID), equalTo(HEARING_ID.toString()));
    }

    @Test
    public void shouldHandleHearingDaysCancelledPublicEvent() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final HearingDaysCancelled hearingDaysCancelledEvent = generateHearingDaysCancelledEvent();
        final JsonObject eventPayload = objectToJsonObjectConverter.convert(hearingDaysCancelledEvent);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.hearing.hearing-days-cancelled"),
                eventPayload);

        listingEventProcessor.handleHearingDaysCancelledPublicEvent(event);

        verify(this.sender, times(1)).send(this.senderDefaultEnvelopeCaptor.capture());

        final List<DefaultEnvelope<JsonObject>> events = this.senderDefaultEnvelopeCaptor.getAllValues();
        assertThat(events.get(0).metadata().name(), is("listing.command.cancel-hearing-days"));
        assertThat(events.get(0).payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingDaysCancelledEvent.getHearingId().toString())),
                withJsonPath("$.hearingDays", hasSize(2)),
                withJsonPath("$.hearingDays[0].listedDurationMinutes", equalTo(hearingDaysCancelledEvent.getHearingDays().get(0).getListedDurationMinutes())),
                withJsonPath("$.hearingDays[0].listingSequence", equalTo(hearingDaysCancelledEvent.getHearingDays().get(0).getListingSequence())),
                withJsonPath("$.hearingDays[0].sittingDay", equalTo(hearingDaysCancelledEvent.getHearingDays().get(0).getSittingDay().toString())),
                withJsonPath("$.hearingDays[0].isCancelled", equalTo(hearingDaysCancelledEvent.getHearingDays().get(0).getIsCancelled())),
                withJsonPath("$.hearingDays[1].listedDurationMinutes", equalTo(hearingDaysCancelledEvent.getHearingDays().get(1).getListedDurationMinutes())),
                withJsonPath("$.hearingDays[1].listingSequence", equalTo(hearingDaysCancelledEvent.getHearingDays().get(1).getListingSequence())),
                withJsonPath("$.hearingDays[1].sittingDay", equalTo(hearingDaysCancelledEvent.getHearingDays().get(1).getSittingDay().toString())),
                withJsonPath("$.hearingDays[1].isCancelled", equalTo(hearingDaysCancelledEvent.getHearingDays().get(1).getIsCancelled()))
        )));
    }

    @Test
    public void shouldHandleHearingMarkedAsDuplicate() {
        final String hearingId = randomUUID().toString();
        final String case1Id = randomUUID().toString();
        final String case2Id = randomUUID().toString();
        final String defendant1Id = randomUUID().toString();
        final String defendant2Id = randomUUID().toString();
        final String offence1Id = randomUUID().toString();
        final String offence2Id = randomUUID().toString();
        final JsonObject hearingMarkedAsDuplicate = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("prosecutionCaseIds", Json.createArrayBuilder()
                        .add(case1Id)
                        .add(case2Id)
                        .build())
                .add("defendantIds", Json.createArrayBuilder()
                        .add(defendant1Id)
                        .add(defendant2Id)
                        .build())
                .add("offenceIds", Json.createArrayBuilder()
                        .add(offence1Id)
                        .add(offence2Id)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.commandEvent.hearing.marked-as-duplicate"),
                hearingMarkedAsDuplicate);

        listingEventProcessor.handleHearingMarkedAsDuplicate(event);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is(COMMAND_MARK_HEARING_AS_DUPLICATE));

        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.prosecutionCaseIds[0]", equalTo(case1Id)),
                withJsonPath("$.prosecutionCaseIds[1]", equalTo(case2Id)))));
    }

    @Test
    public void shouldHandleHearingMarkedAsDuplicateForCase() {
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();

        final JsonObject hearingMarkedAsDuplicate = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseIds", Json.createArrayBuilder()
                        .add(case1Id.toString())
                        .add(case2Id.toString())
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-marked-as-duplicate"),
                hearingMarkedAsDuplicate);

        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingMarkedAsDuplicate.class))
                .thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                        .withHearingId(hearingId)
                        .withCaseIds(Arrays.asList(case1Id, case2Id))
                        .build());

        listingEventProcessor.handlePrivateHearingMarkedAsDuplicate(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(0);
        final JsonEnvelope secondCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(1);

        assertThat(firstCommandEvent.metadata().name(), is("listing.command.mark-hearing-as-duplicate-for-case"));
        assertThat(firstCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.caseId", equalTo(case1Id.toString())))));

        assertThat(secondCommandEvent.metadata().name(), is("listing.command.mark-hearing-as-duplicate-for-case"));
        assertThat(secondCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.caseId", equalTo(case2Id.toString())))));
    }

    @Test
    public void shouldHearingUnallocatedForListingWhenHearingIsNotSeededAndSourceAsCP() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final JsonObject hearingUnallocatedForListing = createObjectBuilder()
                .add(HEARINGID, hearingId.toString())
                .add(COURTCENTRE_ID, courtCentreId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-unallocated-for-listing.json"),
                hearingUnallocatedForListing);

        when(jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingUnallocatedForListing.class))
                .thenReturn(HearingUnallocatedForListing.hearingUnallocatedForListing()
                        .withHearingId(hearingId)
                        .withCourtCentreId(courtCentreId)
                        .build());

        listingEventProcessor.handleHearingUnallocatedForListing(envelope);

        verify(this.sender, never()).send(this.senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHearingUnallocatedForListingWhenHearingIsNotSeededAndSourceAsHMI() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final JsonObject hearingUnallocatedForListing = createObjectBuilder()
                .add(HEARINGID, hearingId.toString())
                .add(COURTCENTRE_ID, courtCentreId.toString())
                .add(SOURCE, HMI_SOURCE)
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-unallocated-for-listing.json"),
                hearingUnallocatedForListing);

        when(jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingUnallocatedForListing.class))
                .thenReturn(HearingUnallocatedForListing.hearingUnallocatedForListing()
                        .withHearingId(hearingId)
                        .withCourtCentreId(courtCentreId)
                        .withSource(HMI_SOURCE)
                        .build());
        listingEventProcessor.handleHearingUnallocatedForListing(envelope);

        verify(this.sender, never()).send(this.senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHearingUnallocatedForListingWhenHearingIsSeededAndSourceAsCP() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final JsonObject hearingUnallocatedForListing = createObjectBuilder()
                .add(HEARINGID, hearingId.toString())
                .add(SEEDING_HERAING_ID, seedingHearingId.toString())
                .add(COURTCENTRE_ID, courtCentreId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-unallocated-for-listing.json"),
                hearingUnallocatedForListing);

        when(jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingUnallocatedForListing.class))
                .thenReturn(HearingUnallocatedForListing.hearingUnallocatedForListing()
                        .withHearingId(hearingId)
                        .withSeededHearing(true)
                        .withCourtCentreId(courtCentreId)
                        .build());
        listingEventProcessor.handleHearingUnallocatedForListing(envelope);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_CHANGE_NEXT_HEARING_DAY));
        final JsonObject jsonObject = senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));

    }

    @Test
    public void shouldHearingUnallocatedForListingWhenHearingIsSeededAndSourceAsHMI() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final JsonObject hearingUnallocatedForListing = createObjectBuilder()
                .add(HEARINGID, hearingId.toString())
                .add(SEEDING_HERAING_ID, seedingHearingId.toString())
                .add(SOURCE, HMI_SOURCE)
                .add(COURTCENTRE_ID, courtCentreId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-unallocated-for-listing.json"),
                hearingUnallocatedForListing);

        when(jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingUnallocatedForListing.class))
                .thenReturn(HearingUnallocatedForListing.hearingUnallocatedForListing()
                        .withHearingId(hearingId)
                        .withSeededHearing(true)
                        .withCourtCentreId(courtCentreId)
                        .withSource(HMI_SOURCE)
                        .build());

        listingEventProcessor.handleHearingUnallocatedForListing(envelope);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(COMMAND_CHANGE_NEXT_HEARING_DAY));
        final JsonObject jsonObject = senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));

    }

    @Test
    public void shouldHandleNewDefendantAddedForCourtProceedings() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final UUID offenceId = randomUUID();
        final CourtCentre courtCentre = courtCentre().withId(courtCentreId).withRoomId(courtRoomId).build();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now();

        final NewDefendantAddedForCourtProceedings privateEventPayload = newDefendantAddedForCourtProceedings()
                .withDefendant(uk.gov.justice.listing.events.Defendant.defendant().withId(defendantId)
                        .withOffences(singletonList(uk.gov.justice.listing.events.Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build())
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withCaseId(caseId)
                .withHearingDateTime(hearingDateTime)
                .withHearingId(hearingId)
                .build();

        given(jsonObjectConverter.convert(payload, NewDefendantAddedForCourtProceedings.class)).willReturn(privateEventPayload);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.new-defendant-added-for-court-proceedings"), payload);

        listingEventProcessor.handleNewDefendantAddedForCourtProceedings(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        verify(hearingConfirmedFactory).buildCourtCentre(eq(courtCentreId), eq(courtRoomId), (any(JsonEnvelope.class)));

        final JsonEnvelope onlyPublicEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(0);

        assertThat(onlyPublicEvent.metadata().name(), is("public.listing.new-defendant-added-for-court-proceedings"));
        assertThat(onlyPublicEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.caseId", equalTo(caseId.toString())),
                withJsonPath("$.defendantId", equalTo(privateEventPayload.getDefendant().getId().toString())))));
        withJsonPath("$.courtCentre", equalTo(courtCentre));
        withJsonPath("$.hearingDateTime", equalTo(hearingDateTime.toString()));
    }

    @Test
    public void shouldHandleNewDefendantAddedForCourtProceedings_NoPublicEventRaisedAsMissingCourtCentreId() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now();

        final NewDefendantAddedForCourtProceedings privateEventPayload = newDefendantAddedForCourtProceedings()
                .withDefendant(uk.gov.justice.listing.events.Defendant.defendant().withId(defendantId)
                        .withOffences(singletonList(uk.gov.justice.listing.events.Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build())
                .withCourtRoomId(courtRoomId)
                .withCaseId(caseId)
                .withHearingDateTime(hearingDateTime)
                .withHearingId(hearingId)
                .build();

        given(jsonObjectConverter.convert(payload, NewDefendantAddedForCourtProceedings.class)).willReturn(privateEventPayload);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.new-defendant-added-for-court-proceedings"), payload);

        listingEventProcessor.handleNewDefendantAddedForCourtProceedings(event);

        verify(sender, never()).send(this.senderJsonEnvelopeCaptor.capture());
        verify(hearingConfirmedFactory, never()).buildCourtCentre(any(), any(Optional.class), (any(JsonEnvelope.class)));
    }

    @Test
    public void shouldHandleNewDefendantAddedForCourtProceedings_NoPublicEventRaisedAsMissingCourtRoomId() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final UUID courtCentreId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now();

        final NewDefendantAddedForCourtProceedings privateEventPayload = newDefendantAddedForCourtProceedings()
                .withDefendant(uk.gov.justice.listing.events.Defendant.defendant().withId(defendantId)
                        .withOffences(singletonList(uk.gov.justice.listing.events.Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build())
                .withCourtCentreId(courtCentreId)
                .withCaseId(caseId)
                .withHearingDateTime(hearingDateTime)
                .withHearingId(hearingId)
                .build();

        given(jsonObjectConverter.convert(payload, NewDefendantAddedForCourtProceedings.class)).willReturn(privateEventPayload);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.new-defendant-added-for-court-proceedings"), payload);

        listingEventProcessor.handleNewDefendantAddedForCourtProceedings(event);

        verify(sender, never()).send(this.senderJsonEnvelopeCaptor.capture());
        verify(hearingConfirmedFactory, never()).buildCourtCentre(any(), any(Optional.class), (any(JsonEnvelope.class)));
    }

    @Test
    public void shouldHandleNewDefendantAddedForCourtProceedings_NoPublicEventRaisedAsMissingHearingDateTime() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();

        final NewDefendantAddedForCourtProceedings privateEventPayload = newDefendantAddedForCourtProceedings()
                .withDefendant(uk.gov.justice.listing.events.Defendant.defendant().withId(defendantId)
                        .withOffences(singletonList(uk.gov.justice.listing.events.Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build())
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .build();

        given(jsonObjectConverter.convert(payload, NewDefendantAddedForCourtProceedings.class)).willReturn(privateEventPayload);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.new-defendant-added-for-court-proceedings"), payload);

        listingEventProcessor.handleNewDefendantAddedForCourtProceedings(event);

        verify(sender, never()).send(this.senderJsonEnvelopeCaptor.capture());
        verify(hearingConfirmedFactory, never()).buildCourtCentre(any(), any(Optional.class), (any(JsonEnvelope.class)));
    }

    @Test
    public void ShouldPublicEventWhenRaisedHearingDaysWithoutCourtCentreCorrected() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = createObjectBuilder().add(ID, hearingId.toString())
                .add("hearingDays", createArrayBuilder()
                        .add(createObjectBuilder().add("courtCentreId", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                                .add("courtRoomId", "f1ead1d2-4b26-3230-b781-508d6aaafd26")
                                .add("durationMinutes", 0)
                                .add("endTime", "2020-08-25T09:00:00.000Z")
                                .add("hearingDate", "2020-08-25")
                                .add("sequence", 3)
                                .add("startTime", "2020-08-25T09:00:00.000Z").build())).build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-days-without-court-centre-corrected"), payload);
        listingEventProcessor.hearingDaysWithoutCourtCentreCorrected(event);
        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope onlyPublicEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(0);
        assertThat(onlyPublicEvent.metadata().name(), is("public.events.listing.hearing-days-without-court-centre-corrected"));
        assertThat(onlyPublicEvent.payloadAsJsonObject().getString(ID), is(hearingId.toString()));
        assertThat(onlyPublicEvent.payloadAsJsonObject().getJsonArray("hearingDays").getJsonObject(0).getString("courtCentreId"), is("f8254db1-1683-483e-afb3-b87fde5a0a26"));
        assertThat(onlyPublicEvent.payloadAsJsonObject().getJsonArray("hearingDays").getJsonObject(0).getString("courtRoomId"), is("f1ead1d2-4b26-3230-b781-508d6aaafd26"));
        assertThat(onlyPublicEvent.payloadAsJsonObject().getJsonArray("hearingDays").getJsonObject(0).getInt("listedDurationMinutes"), is(0));
        assertThat(onlyPublicEvent.payloadAsJsonObject().getJsonArray("hearingDays").getJsonObject(0).getInt("listingSequence"), is(3));
        assertThat(onlyPublicEvent.payloadAsJsonObject().getJsonArray("hearingDays").getJsonObject(0).getString("sittingDay"), is("2020-08-25T09:00:00.000Z"));

    }

    @Test
    public void shouldHandleMarkHearingAsHearingDeleted() {

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final UUID hearingId = randomUUID();

        final HearingMarkedAsDeleted hearingMarkedAsDeleted = HearingMarkedAsDeleted.hearingMarkedAsDeleted()
                .withHearingIdToDelete(hearingId)
                .build();

        final JsonObject eventPayload = objectToJsonObjectConverter.convert(hearingMarkedAsDeleted);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-marked-as-deleted"),
                eventPayload);

        given(jsonObjectConverter.convert(eventPayload, HearingMarkedAsDeleted.class)).willReturn(hearingMarkedAsDeleted);

        listingEventProcessor.handleHearingMarkedAsDeleted(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("listing.command.mark-hearing-as-deleted"));
        assertThat(commandEvent.payload().toString(),
                isJson(allOf(
                        withJsonPath("$.hearingIdToMarkAsDeleted", equalTo(hearingId.toString())))
                ));

    }

    @Test
    public void shouldHandleHearingMarkedForPartialUpdate() {

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();


        final HearingMarkedForPartialUpdate hearingMarkedForPartialUpdate = HearingMarkedForPartialUpdate.hearingMarkedForPartialUpdate()
                .withHearingIdToBeUpdated(hearingId)
                .withProsecutionCases(Arrays.asList(
                        ProsecutionCases.prosecutionCases().withCaseId(caseId)
                                .withDefendants(Arrays.asList(Defendants.defendants().withDefendantId(defendantId)
                                        .withOffences(Arrays.asList(Offences.offences().withOffenceId(offenceId).build()))
                                        .build()))
                                .build()
                ))
                .build();

        final JsonObject eventPayload = objectToJsonObjectConverter.convert(hearingMarkedForPartialUpdate);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-marked-for-partial-update"),
                eventPayload);

        given(jsonObjectConverter.convert(eventPayload, HearingMarkedForPartialUpdate.class)).willReturn(hearingMarkedForPartialUpdate);

        listingEventProcessor.handleHearingMarkedForPartialUpdate(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("listing.command.remove-partially-merged-offences-from-original-hearing"));
        assertThat(commandEvent.payload().toString(),
                isJson(allOf(
                        withJsonPath("$.hearingIdToBeUpdated", equalTo(hearingId.toString())),
                        withJsonPath("$.prosecutionCasesToRemove", hasSize(1)))
                ));

    }

    @Test
    public void shouldHandleHearingAddedToCase(){
        final String caseId = randomUUID().toString();
        final String hearingId = randomUUID().toString();

        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", caseId)
                .add("hearingId", hearingId)
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-added-to-case"),
                eventPayload);

        listingEventProcessor.handleHearingAddedToCase(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("public.listing.hearing-added-to-case"));
        assertThat(commandEvent.payload().toString(),
                isJson(allOf(
                        withJsonPath("$.hearingId", equalTo(hearingId)),
                        withJsonPath("$.caseId", equalTo(caseId)))
                ));
    }

    private JsonEnvelope allocatedHearingExtendedForListingEvent() {

        final JsonObjectBuilder hearingDate = createObjectBuilder()
                .add("startDate", START_DATE.toString())
                .add("startTime", START_TIME.toString());

        final JsonObjectBuilder allocatedHearingExtendedForListing = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TYPE)
                .add("estimatedMinutes", ESTIMATED_MINUTES)
                .add("judgeId", JUDICIAL_ID.toString())
                .add("updateSlot", true)
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), allocatedHearingExtendedForListing.build());
    }

    private HearingDaysCancelled generateHearingDaysCancelledEvent() {
        return hearingDaysCancelled()
                .withHearingId(randomUUID())
                .withHearingDays(ImmutableList.of(
                        hearingDay()
                                .withListedDurationMinutes(30)
                                .withListingSequence(0)
                                .withSittingDay(ZonedDateTimes.fromString("2020-08-18T01:22:12.381Z"))
                                .withIsCancelled(true)
                                .build(),
                        hearingDay()
                                .withListedDurationMinutes(10)
                                .withListingSequence(1)
                                .withSittingDay(ZonedDateTimes.fromString("2020-08-19T01:22:12.381Z"))
                                .withIsCancelled(false)
                                .build()
                ))
                .build();
    }

    private CourtApplication courtApplicationAdded() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final CourtApplication.Builder courtApplicationBuilder = new CourtApplication.Builder();
        final CourtApplicationParty.Builder courtApplicationPartyBuilder = new CourtApplicationParty.Builder();

        return courtApplicationBuilder
                .withId(randomUUID())
                .withApplicationReference(STRING.next())
                .withApplicationParticulars(STRING.next())
                .withApplicant(courtApplicationPartyBuilder
                        .withId(randomUUID())
                        .withPersonDetails(null)
                        .build())
                .build();
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
                .withLegislation(stringGenerator.next())
                .build();

        final Offence offence = offenceBuilder
                .withArrestDate(null)
                .withChargeDate(null)
                .withOffenceCode(stringGenerator.next())
                .withConvictionDate(stringGenerator.next())
                .withCount(10)
                .withEndDate(stringGenerator.next())
                .withId(randomUUID())
                .withStartDate(stringGenerator.next())
                .withWording(stringGenerator.next())
                .build();

        final UpdatedOffences updatedOffence = updatedOffenceBuilder
                .withDefendantId(randomUUID())
                .withOffences(asList((offence)))
                .build();

        final AddedOffences addedOffences = addedOffencesBuilder
                .withOffences(asList(offence))
                .withProsecutionCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build();

        final DeletedOffences deletedOffences = deletedOffencesBuilder
                .withDefendantId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withOffences(asList(randomUUID()))
                .build();

        final OffencesForDefendantUpdated defendantOffencesChanged = offencesForDefendantUpdated
                .withAddedOffences(asList(addedOffences))
                .withDeletedOffences(asList(deletedOffences))
                .withUpdatedOffences(asList(updatedOffence))
                .withModifiedDate(stringGenerator.next())
                .build();

        return defendantOffencesChanged;
    }

    private DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final Generator<String> stringGenerator = string(5);
        final DefendantsAddedToCourtProceedings.Builder defendantsAddedToCourtProceedingsBuilder = new DefendantsAddedToCourtProceedings.Builder();
        final Address.Builder progressionAddressBuilder = new Address.Builder();
        final uk.gov.justice.core.courts.Defendant.Builder defendantBuilder = new uk.gov.justice.core.courts.Defendant.Builder();
        final Person.Builder personBuilder = new Person.Builder();
        final PersonDefendant.Builder personDefendantBuilder = new PersonDefendant.Builder();
        final ListDefendantRequest.Builder listDefendantRequestBuilder = new ListDefendantRequest.Builder();
        final ListHearingRequest.Builder listHearingRequestBuilder = new ListHearingRequest.Builder();

        final Address address = progressionAddressBuilder
                .withAddress1(stringGenerator.next())
                .withAddress2(stringGenerator.next())
                .withAddress3(stringGenerator.next())
                .withAddress4(stringGenerator.next())
                .withPostcode(stringGenerator.next())
                .build();

        final Person person = personBuilder
                .withAdditionalNationalityCode(null)
                .withAdditionalNationalityDescription(null)
                .withAdditionalNationalityId(null)
                .withAddress(address)
                .withContact(null)
                .withDateOfBirth(stringGenerator.next())
                .withDisabilityStatus(null)
                .withDocumentationLanguageNeeds(null)
                .withEthnicity(Ethnicity.ethnicity().build())
                .build();

        final PersonDefendant progressionPerson = personDefendantBuilder
                .withArrestSummonsNumber(null)
                .withBailStatus(new BailStatus.Builder().withId(UUID.fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).withCode("P").withDescription("Conditional Bail with Pre-Release conditions").build())
                .withCustodyTimeLimit("CTL")
                .withDriverNumber(null)
                .withEmployerOrganisation(null)
                .withEmployerPayrollReference(null)
                .withPerceivedBirthYear(null)
                .withPersonDetails(person)
                .build();

        final uk.gov.justice.core.courts.Defendant defendant = defendantBuilder
                .withId(randomUUID())
                .withAssociatedPersons(emptyList())
                .withDefenceOrganisation(null)
                .withLegalEntityDefendant(null)
                .withMitigation(null)
                .withMitigationWelsh(null)
                .withNumberOfPreviousConvictionsCited(null)
                .withPersonDefendant(progressionPerson)
                .withProsecutionAuthorityReference(null)
                .withProsecutionCaseId(randomUUID())
                .withWitnessStatement(null)
                .withWitnessStatementWelsh(null)
                .build();

        final ListDefendantRequest listDefendantRequest = listDefendantRequestBuilder
                .withDefendantOffences(asList(randomUUID()))
                .withProsecutionCaseId(randomUUID())
                .build();

        final ListHearingRequest listHearingRequest = listHearingRequestBuilder
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingType(HearingType.hearingType()
                        .withDescription(TYPE).withId(randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(asList(listDefendantRequest))
                .build();

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = defendantsAddedToCourtProceedingsBuilder
                .withDefendants(asList(defendant))
                .withListHearingRequests(asList(listHearingRequest))
                .build();

        return defendantsAddedToCourtProceedings;

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
                .withAddress2(stringGenerator.next())
                .withAddress3(stringGenerator.next())
                .withAddress4(stringGenerator.next())
                .withPostcode(stringGenerator.next())
                .build();

        final Person person = personBuilder
                .withAdditionalNationalityCode(null)
                .withAdditionalNationalityDescription(null)
                .withAdditionalNationalityId(null)
                .withAddress(address)
                .withContact(null)
                .withDateOfBirth(stringGenerator.next())
                .withDisabilityStatus(null)
                .withDocumentationLanguageNeeds(null)
                .withEthnicity(Ethnicity.ethnicity().build())
                .build();

        final PersonDefendant progressionPerson = personDefendantBuilder
                .withArrestSummonsNumber(null)
                .withBailStatus(new BailStatus.Builder().withId(UUID.fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).withCode("P").withDescription("Conditional Bail with Pre-Release conditions").build())
                .withCustodyTimeLimit("CTL")
                .withDriverNumber(null)
                .withEmployerOrganisation(null)
                .withEmployerPayrollReference(null)
                .withPerceivedBirthYear(null)
                .withPersonDetails(person)
                .build();

        final Defendant defendant = defendantBuilder
                .withId(randomUUID())
                .withAssociatedPersons(emptyList())
                .withDefenceOrganisation(null)
                .withLegalEntityDefendant(null)
                .withMitigation(null)
                .withMitigationWelsh(null)
                .withNumberOfPreviousConvictionsCited(null)
                .withPersonDefendant(progressionPerson)
                .withProsecutionAuthorityReference(null)
                .withProsecutionCaseId(randomUUID())
                .withWitnessStatement(null)
                .withWitnessStatementWelsh(null)
                .build();

        final DefendantUpdated defendantUpdated = defendantUpdatedBuilder.withDefendant(defendant)
                .build();

        return defendantUpdated;
    }

    private HearingConfirmed hearingConfirmed(final JurisdictionType jurisdictionType) {

        final String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);

        return HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildHearing(formattedDateTime, jurisdictionType))
                .withSendNotificationToParties(true)
                .build();
    }

    private uk.gov.justice.core.courts.ConfirmedHearing buildHearing(final String formattedDateTime, final JurisdictionType jurisdictionType) {
        return uk.gov.justice.core.courts.ConfirmedHearing.confirmedHearing()
                .withExistingHearingId(randomUUID())
                .withId(HEARING_ID)
                .withEstimatedDuration(ESTIMATED_DURATION)
                .withHearingDays(Arrays.asList(hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                        .withListedDurationMinutes(0)
                        .build()))
                .withCourtCentre(courtCentre()
                        .withCode("BAX0100")
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtApplicationIds(Arrays.asList(randomUUID()))
                .withJurisdictionType(jurisdictionType)
                .withType(HearingType.hearingType().withDescription(TYPE).withId(randomUUID()).build())
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withJudicialId(JUDICIAL_ID)
                        .withJudicialRoleType(
                                uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(CIRCUIT_JUDGE)
                                        .withJudicialRoleTypeId(null)
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

    private ProsecutionCase getProsecutionCase() {
        final uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withProsecutionCaseId(randomUUID())
                .build();
        final List<uk.gov.justice.core.courts.Defendant> defendants = singletonList(defendant);
        return ProsecutionCase.prosecutionCase()
                .withCaseStatus("CLOSED")
                .withDefendants(defendants)
                .withId(randomUUID())
                .withCaseMarkers(Collections.EMPTY_LIST)
                .withOriginatingOrganisation(null)
                .build();
    }

    private List<uk.gov.moj.cpp.listing.domain.JudicialRole> prepareRotaSLJudiciaryInfo(final JsonObject hearingSlotsResponse) {
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = new ArrayList<>();
        ((JsonObject) hearingSlotsResponse.getJsonArray("hearingSlots").get(0))
                .getJsonArray("judiciaries")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(judiciaryJsonObject ->
                        judicialRoles.add(uk.gov.moj.cpp.listing.domain.JudicialRole.judicialRole()
                                .withIsBenchChairman(of(judiciaryJsonObject.getBoolean("benchChairman")))
                                .withIsDeputy(of(judiciaryJsonObject.getBoolean("deputy")))
                                .withJudicialId(UUID.fromString(judiciaryJsonObject.getString("judiciaryId")))
                                .withJudicialRoleType(
                                        uk.gov.moj.cpp.listing.domain.JudicialRoleType.judicialRoleType()
                                                .withJudiciaryType(judiciaryJsonObject.getString("judiciaryType"))
                                                .build())
                                .build())
                );
        return judicialRoles;
    }

    private HearingAllocatedForListing createHearingAllocatedForListing(final boolean hasAdjournmentDate, final boolean hearingAndCourtRoomFlag, final boolean offenceIdFlag, final String source) {
        HearingAllocatedForListing.Builder builder = new HearingAllocatedForListing.Builder();
        builder.withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .withHasAdjournmentDate(hasAdjournmentDate)
                .withSource(source);
        if (hearingAndCourtRoomFlag) {
            builder.withHearingId(randomUUID())
                    .withCourtRoomId(randomUUID())
                    .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES);
        }
        if (offenceIdFlag) {
            builder.withProsecutionCaseDefendantsOffenceIds(Arrays.asList(
                    ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(randomUUID())
                            .withDefendants(asList(DefendantOffenceIds.defendantOffenceIds()
                                    .withId(randomUUID())
                                    .withOffenceIds(Arrays.asList(OFFENCE_ID))
                                    .build()))
                            .build()))
                    .build();
        }
        return builder.build();
    }

    private HearingAllocatedForListingV2 createHearingAllocatedForListingV2(final boolean hasAdjournmentDate, final boolean offenceIdFlag, final UUID hearingId) {
        HearingAllocatedForListingV2.Builder builder = new HearingAllocatedForListingV2.Builder();
        builder.withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .withHasAdjournmentDate(hasAdjournmentDate)
                .withHearingId(hearingId)
                .withCourtRoomId(randomUUID());
        if (offenceIdFlag) {
            builder.withProsecutionCaseDefendantsOffenceIds(Arrays.asList(
                    ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                            .withId(randomUUID())
                            .withDefendants(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                    .withId(randomUUID())
                                    .withOffenceIds(Arrays.asList(OffenceIds.offenceIds()
                                            .withId(OFFENCE_ID)
                                            .withSeedingHearing(SeedingHearing.seedingHearing()
                                                    .withSeedingHearingId(randomUUID())
                                                    .build())
                                            .build()))
                                    .build()))
                            .build()))
                    .build();
        }
        return builder.build();
    }

    private AllocatedHearingUpdatedForListingV2 createAllocatedHearingUpdatedForListingV2(final UUID hearingId) {
        return new AllocatedHearingUpdatedForListingV2.Builder()
                .withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(Arrays.asList(ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                        .withId(randomUUID())
                        .withDefendants(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                .withId(randomUUID())
                                .withOffenceIds(asList(OffenceIds.offenceIds()
                                        .withId(randomUUID())
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withSeedingHearingId(randomUUID())
                                                .build())
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }

    private HearingAllocatedForListingV2 createHearingAllocatedForListingV2ForHMIVerification(final boolean sourceFlag) {
        return new HearingAllocatedForListingV2.Builder()
                .withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .withHasAdjournmentDate(false)
                .withSource(sourceFlag ? HMI_SOURCE : null)
                .build();
    }

    private AllocatedHearingUpdatedForListingV2 createAllocatedHearingUpdatedForListingV2ForHMIVerification(final boolean sourceFlag) {
        return new AllocatedHearingUpdatedForListingV2.Builder()
                .withUpdateSlot(false)
                .withHearingDays(hearingDays)
                .withSource(sourceFlag ? HMI_SOURCE : null)
                .build();
    }

    private HearingAllocatedForListingV2 createHearingAllocatedForListingV2(final UUID hearingId) {
        return new HearingAllocatedForListingV2.Builder()
                .withUpdateSlot(false)
                .withHasAdjournmentDate(false)
                .withHearingDays(hearingDays)
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(Arrays.asList(ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                        .withId(randomUUID())
                        .withDefendants(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                .withId(randomUUID())
                                .withOffenceIds(asList(OffenceIds.offenceIds()
                                        .withId(randomUUID())
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withSeedingHearingId(randomUUID())
                                                .build())
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }

}

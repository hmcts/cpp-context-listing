package uk.gov.moj.cpp.listing.command.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.parse;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import static uk.gov.justice.listing.event.PublishCourtListExportFailed.publishCourtListExportFailed;
import static uk.gov.justice.listing.event.PublishCourtListExportSuccessful.publishCourtListExportSuccessful;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.command.utils.FileUtil.givenPayload;
import static uk.gov.moj.cpp.listing.domain.HearingLanguage.valueFor;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.courts.AddCourtApplicationForHearing;
import uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand;
import uk.gov.justice.listing.courts.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.Gender;
import uk.gov.justice.listing.courts.SequenceHearings;
import uk.gov.justice.listing.courts.Title;
import uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings;
import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.ApplicationEjected;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CourtApplicationAddedToHearing;
import uk.gov.justice.listing.events.CourtApplicationToBeUpdated;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.justice.listing.events.NewDefendantDetailsUpdated;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.utils.CommandDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CommandOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CommandSimpleOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtApplicationToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtsAddedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CourtsDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtsDeletedOffenceToDomainCaseSimpleOffence;
import uk.gov.moj.cpp.listing.command.utils.CourtsOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CourtsUpdatedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.domain.ApplicantRespondent;

import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.RestrictCourtList;
import uk.gov.moj.cpp.listing.domain.SequenceHearing;
import uk.gov.moj.cpp.listing.domain.SimpleOffence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.aggregate.Application;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.CourtListAggregate;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@SuppressWarnings({"squid:S1607"})
@RunWith(MockitoJUnitRunner.class)
public class ListingCommandHandlerTest {


    private static final String ADDRESS_LINE_1 = "Address line 1";
    private static final String HEARING_ADDED_TO_CASE_EVENT = "listing.events.hearing-added-to-case";
    private static final String COURT_APPLICATION_ADDED_TO_HEARING_EVENT = "listing.events.court-application-added-to-hearing";

    private static final UUID PERSON_ID = randomUUID();
    private static final UUID DEFENDANT_ID1 = randomUUID();
    private static final UUID DEFENDANT_ID2 = randomUUID();
    private static final UUID DEFENDANT_ID3 = randomUUID();
    private static final UUID OFFENCE_ID1 = randomUUID();
    private static final UUID UPDATED_OFFENCE_ID1 = randomUUID();
    private static final UUID UPDATED_OFFENCE_ID2 = randomUUID();
    private static final UUID UPDATED_OFFENCE_ID3 = randomUUID();
    private static final UUID ADDED_OFFENCE_ID1 = randomUUID();
    private static final UUID ADDED_OFFENCE_ID2 = randomUUID();
    private static final UUID ADDED_OFFENCE_ID3 = randomUUID();
    private static final UUID DELETED_OFFENCE_ID1 = randomUUID();
    private static final UUID DELETED_OFFENCE_ID2 = randomUUID();
    private static final UUID DELETED_OFFENCE_ID3 = randomUUID();
    private static final UUID DELETED_OFFENCE_ID4 = randomUUID();

    private static final UUID HEARING_ID_1 = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID ADDED_OFFENCE_CASE_ID = randomUUID();
    private static final UUID UPDATED_OFFENCE_CASE_ID = randomUUID();
    private static final UUID DELETED_OFEENCE_CASE_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String FIRST_NAME = "Test Recipe";
    private static final String LAST_NAME = "Last Name";
    private static final String DATE_OF_BIRTH = "1980-07-15";
    private static final String PTP_TYPE = "PTP";
    private static final String SENTENCE_TYPE = "Sentence";
    private static final String INITIAL_START_DATE = "2018-05-30";
    private static final String END_DATE = "2018-06-03";
    private static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.now();
    private static final LocalDate WEEK_COMMENCING_END_DATE = LocalDate.now().plusDays(7l);
    private static final Integer WEEK_COMMENCING_DURATION = 1;
    private static final String UPDATED_START_DATE = "2018-06-01";
    private static final String UPDATED_END_DATE = "2018-06-03";
    private static final String UPDATED_START_TIME = "2018-06-01T11:30:00Z";
    private static final String OFFENCE_START_DATE = "2018-06-01";
    private static final String OFFENCE_END_DATE = "2018-06-07";
    private static final String NON_SITTING_DAY = "2018-06-02";
    private static final List<LocalDate> NON_SITTING_DAYS1 = singletonList(LocalDate.parse(NON_SITTING_DAY));
    private static final String NON_DEFAULT_DAY = "2018-06-04T11:00:00Z";
    private static final int INITIAL_ESTIMATE_MINUTES = 640;
    private static final int UPDATED_ESTIMATE_MINUTES = 720;
    private static final String DEFENCE_ORGANISATION_NAME = "XYZ Organisation";
    private static final UUID DEFENCE_ORGANISATION_ID = randomUUID();
    private static final String URN = "urn";
    private static final UUID JUDGE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final String STATEMENT_OF_OFFENCE_TITLE = "title";
    private static final String STATEMENT_OF_OFFENCE_LEGISLATION = "Legislation";
    private static final String START_TIME = "10:30";
    private static final String CUSTODY_TIME_LIMIT = "2017-10-05";
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final String ADDRESS_LINE_2 = "Address line 1";
    private static final String ADDRESS_LINE_3 = "Address line 1";
    private static final String ADDRESS_LINE_4 = "Address line 1";
    private static final String POSTCODE = "Postcode";
    private static final String PERSON_FIRST_NAME1 = "firstName";
    private static final String PERSON_LAST_NAME1 = "lastName";
    private static final String PERSON_DOB = "1980-06-01";
    private static final String PERSON_NATIONALITY = "La La Land";
    private static final String MODIFIED_DATE = "2018-01-01";
    private static final String OFFENCE_WORDING = "wording";
    private static final int OFFENCE_COUNT = 1;
    private static final String OFFENCE_CONVICTION_DATE = "2017-10-05";
    private static final String LISTING_DIRECTIONS = "wheelchair access required";
    private static final LocalDate START_DATE = LocalDate.parse("2018-01-02");
    private static final String REPORTING_RESTRICTIONS = "Automatic anonymity under the Sexual Offences (Amendment) Act 1992";
    private static final String PROSECUTOR_DATES_TO_AVOID = "Can't do Mondays & Tuesdays";
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final Type HEARING_TYPE = Type.type()
            .withId(UUID.fromString("6e1bef55-7e13-4615-b3ba-8663f4438e16"))
            .withDescription("Trial")
            .build();
    private static final List NON_SITTING_DAYS = Collections.EMPTY_LIST;
    private static final String EARLIEST_START_TIME = "2012-12-12T01:02:33Z";


    private static final UUID JUDICIAL_ID_1 = randomUUID();
    private static final UUID JUDICIAL_ID_2 = randomUUID();
    private static final String JUDICIAL_ROLE_TYPE = "MAGISTRATE";
    private static final Boolean IS_DEPUTY = false;
    private static final Boolean IS_BENCH_CHAIRMAN = true;
    private static final UUID AUTHORITY_ID = randomUUID();
    private static final String HEARING_LANGUAGE = "ENGLISH";
    private static final String DEFAULT_DURATION = "6";
    private static final String DEFAULT_START_TIME = "10:30";
    private static final String SEQUENCE_1 = "1";
    private static final String SEQUENCE_2 = "2";
    private static final String HEARING_DATE_1 = "2012-12-11";
    private static final String HEARING_DATE_2 = "2012-12-12";

    private static final UUID COURT_APPLICATION_ID = randomUUID();
    private static final String COURT_APPLICATION_TYPE = STRING.next();
    @Mock
    CaseOffences caseOffences;
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
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();
    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @Before
    public void setup() {
        EnveloperFactory.createEnveloperWithEvents(HearingAddedToCase.class,
                HearingListed.class, TypeChangedForHearing.class, StartDateChangedForHearing.class,
                JudiciaryAssignedToHearing.class, JudiciaryChangedForHearing.class,
                JudiciaryRemovedFromHearing.class, CourtRoomAssignedToHearing.class, CourtRoomChangedForHearing.class,
                CourtRoomRemovedFromHearing.class, NonDefaultDaysAssignedToHearing.class, NonDefaultDaysChangedForHearing.class,
                HearingAllocatedForListing.class, EndDateChangedForHearing.class,
                NonSittingDaysAssignedToHearing.class, NonSittingDaysChangedForHearing.class,
                AllocatedHearingUpdatedForListing.class, HearingUnallocatedForListing.class, DefendantsToBeUpdated.class,
                NewDefendantDetailsUpdated.class, OffencesToBeUpdated.class, OffencesToBeAdded.class, OffencesToBeDeleted.class,
                ArrayList.class, OffenceUpdated.class, HearingDaysChangedForHearing.class, OffenceAdded.class,
                OffenceDeleted.class, SequenceHearings.class, UpdateCourtApplicationForHearings.class, AddCourtApplicationForHearing.class,
                CourtApplicationAddedToHearing.class, CourtApplicationToBeUpdated.class, CourtListRestricted.class, CaseEjected.class, ApplicationEjected.class,
                PublishCourtListExportFailed.class, PublishCourtListExportSuccessful.class);
    }


    @InjectMocks
    @Spy
    private ListingCommandHandler listingCommandHandler;
    @Spy
    private CommandToDomainConverter commandToDomainConverter = new CommandToDomainConverter();
    @Spy
    private CourtsDefendantToDomainConverter defendantUpdatedToDomainConverter = new CourtsDefendantToDomainConverter();
    @Spy
    private CourtsOffenceToDomainOffence courtsOffenceToDomainOffence;
    @Spy
    private CommandDefendantToDomainConverter commandDefendantToDomainConverter = new CommandDefendantToDomainConverter();
    @Spy
    private CourtsUpdatedOffenceToDomainOffence courtsUpdatedOffenceToDomainOffence;
    @Spy
    private CourtsDeletedOffenceToDomainCaseSimpleOffence courtsDeletedOffenceToDomainCaseSimpleOffence;
    @Spy
    private CourtsAddedOffenceToDomainOffence courtsAddedOffenceToDomainOffence;
    @Spy
    private CommandSimpleOffenceToDomainOffence commandSimpleOffenceToDomainOffence;
    @Spy
    private CourtApplicationToDomainConverter courtApplicationToDomainConverter;
    @Spy
    private CommandOffenceToDomainOffence commandOffenceToDomainOffence;
    private boolean hasCustodyTimeLimit = true;
    @Mock
    private Hearing hearing;
    @Mock
    private Case aCase;
    @Mock
    private Application anApplication;

    @Mock
    private HearingTypeFactory hearingTypeFactory;

    @Mock
    private CourtListAggregate courtListAggregate;
    @Mock
    private Stream<Object> events;
    @Captor
    private ArgumentCaptor<List<JudicialRole>> judicialRoleCaptor;
    @Captor
    private ArgumentCaptor<UUID> hearingIdCaptor;
    @Captor
    private ArgumentCaptor<CaseOffences> updatedCaseOffencesCaptor;
    @Captor
    private ArgumentCaptor<List<Offence>> domainOffencesCaptor;
    @Captor
    private ArgumentCaptor<List<SimpleOffence>> domainSimpleOffencesCaptor;
    @Captor
    private ArgumentCaptor<CaseOffences> addedCaseOffencesCaptor;
    @Captor
    private ArgumentCaptor<CaseSimpleOffences> deletedCaseOffencesCaptor;
    @Captor
    private ArgumentCaptor<CourtApplication> courtApplicationCaptor;

    @Test
    public void listingCommandHandlerShouldListHearing() throws Exception {
        final JsonEnvelope commandEnvelope = listCourtHearingCommandEnvelope();

        LocalDate endDate = null;

        List<ListedCase> listedCases = Arrays.asList(createdListedCase());
        List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicalRoles = createJudicalRoles();

        CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(Integer.valueOf(DEFAULT_DURATION))
                .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                .withCourtCentreId(COURT_CENTRE_ID)
                .build();

        List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplication());

        List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(UUID.fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(UUID.fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));
        when(hearing.list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES), eq(listedCases), eq(COURT_CENTRE_ID), eq(judicalRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30))).thenReturn(events);

        listingCommandHandler.listCourtHearing(commandEnvelope);

        verify(hearing).list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES), eq(listedCases), eq(COURT_CENTRE_ID), eq(judicalRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30));

    }

    private CourtApplication getCourtApplication() {
        return CourtApplication.courtApplication()
                .withLinkedCaseId(UUID.fromString("19e9d562-6abb-4871-bfb3-2d777aa90371"))
                .withParentApplicationId(UUID.fromString("9d9a431a-0f12-4386-878a-2bf6c4a0877e"))
                .withApplicationType("App Type")
                .withId(UUID.fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withIsRespondent(false)
                        .withId(UUID.fromString("22b1078b-9430-4cef-ba46-eea40a129ca8"))
                        .withFirstName("Fred")
                        .withLastName("Perry")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .build())
                .withRespondents(Collections.singletonList(ApplicantRespondent.applicantRespondent()
                        .withIsRespondent(true)
                        .withId(UUID.fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withFirstName("Dan")
                        .withLastName("Brown")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .build()))
                .withApplicationReference(Optional.of("REF-1"))
                .build();
    }

    @Test
    public void listingCommandHandlerShouldUpdateJudiciaryForHearings() throws Exception {
        final JsonEnvelope commandEnvelope = changeJudiciaryForHearingsCommandEnvelope();

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(eventSource.getStreamById(HEARING_ID_2)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        when(hearing.assignJudiciary(any(), eq(HEARING_ID_1))).thenReturn(events);
        when(hearing.assignJudiciary(any(), eq(HEARING_ID_2))).thenReturn(events);

        listingCommandHandler.changeJudiciaryForHearings(commandEnvelope);


        verify(hearing, atLeast(2)).assignJudiciary(judicialRoleCaptor.capture(), hearingIdCaptor.capture());
        verify(hearing, times(2)).applyAllocationRules();

        final List<List<JudicialRole>> judicialRoleArguments = judicialRoleCaptor.getAllValues();

        assertThat(judicialRoleArguments.get(0).get(0).getJudicialId(), equalTo(JUDICIAL_ID_1));
        assertThat(judicialRoleArguments.get(0).get(1).getJudicialId(), equalTo(JUDICIAL_ID_2));
        assertThat(judicialRoleArguments.get(1).get(0).getJudicialId(), equalTo(JUDICIAL_ID_1));
        assertThat(judicialRoleArguments.get(1).get(1).getJudicialId(), equalTo(JUDICIAL_ID_2));

        final List<UUID> hearingIdArguments = hearingIdCaptor.getAllValues();

        assertThat(hearingIdArguments.get(0), equalTo(HEARING_ID_1));
        assertThat(hearingIdArguments.get(1), equalTo(HEARING_ID_2));
    }


    @Test
    public void listingCommandHandlerShouldUpdateHearingForListing() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingCommandEnvelope();

        final List<NonDefaultDay> nonDefaultDays = singletonList(NonDefaultDay.nonDefaultDay()
                .withStartTime(parse(NON_DEFAULT_DAY).withZoneSameInstant(ZoneId.of("UTC")))
                .withDuration(Optional.empty())
                .build());

        final List<JudicialRole> judicialRoles = Arrays.asList(JudicialRole.judicialRole()
                .withIsBenchChairman(of(IS_BENCH_CHAIRMAN))
                .withIsDeputy(of(IS_DEPUTY))
                .withJudicialId(JUDICIAL_ID_1)
                .withJudicialRoleType(
                        JudicialRoleType.judicialRoleType()
                                .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                                .build())
                .build());


        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        when(hearing.changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonDefaultDays(nonDefaultDays, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeStartDate(START_DATE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeType(HEARING_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignJudiciary(judicialRoles, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), Integer.valueOf(DEFAULT_DURATION)));
        when(hearing.removeWeekCommencingDates(HEARING_ID_1)).thenReturn(mock(Stream.class));

        listingCommandHandler.updateHearingForListing(commandEnvelope);

        verify(hearing).changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1);
        verify(hearing).assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1);
        verify(hearing).changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1);
        verify(hearing).assignNonDefaultDays(nonDefaultDays, HEARING_ID_1);
        verify(hearing).assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1);
        verify(hearing).changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1);
        verify(hearing).changeStartDate(START_DATE, HEARING_ID_1);
        verify(hearing).changeType(HEARING_TYPE, HEARING_ID_1);
        verify(hearing).changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1);
        verify(hearing).assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1);
        verify(hearing).assignJudiciary(judicialRoles, HEARING_ID_1);
        verify(hearing).assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS1, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1);
        verify(hearing).removeWeekCommencingDates(HEARING_ID_1);
    }

    @Test
    public void listingCommandHandlerShouldUpdateHearingForListingForWeekCommencingDate() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingwithWeekCommencingCommandEnvelope();

        final List<NonDefaultDay> nonDefaultDays = singletonList(NonDefaultDay.nonDefaultDay()
                .withStartTime(parse(NON_DEFAULT_DAY).withZoneSameInstant(ZoneId.of("UTC")))
                .withDuration(Optional.empty())
                .build());

        final List<JudicialRole> judicialRoles = Arrays.asList(JudicialRole.judicialRole()
                .withIsBenchChairman(of(IS_BENCH_CHAIRMAN))
                .withIsDeputy(of(IS_DEPUTY))
                .withJudicialId(JUDICIAL_ID_1)
                .withJudicialRoleType(
                        JudicialRoleType.judicialRoleType()
                                .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                                .build())
                .build());


        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        when(hearing.changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonDefaultDays(nonDefaultDays, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.removeStartDate(HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.removeEndDate(HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeType(HEARING_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignJudiciary(judicialRoles, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignHearingDays(null, null, NON_SITTING_DAYS, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeWeekCommencingDate(WEEK_COMMENCING_START_DATE, WEEK_COMMENCING_END_DATE, WEEK_COMMENCING_DURATION,HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), Integer.valueOf(DEFAULT_DURATION)));


        listingCommandHandler.updateHearingForListing(commandEnvelope);

        verify(hearing).changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1);
        verify(hearing).assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1);
        verify(hearing).changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1);
        verify(hearing).assignNonDefaultDays(nonDefaultDays, HEARING_ID_1);
        verify(hearing).assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1);
        verify(hearing).removeEndDate(HEARING_ID_1);
        verify(hearing).removeStartDate(HEARING_ID_1);
        verify(hearing).removeEndDate(HEARING_ID_1);
        verify(hearing).changeType(HEARING_TYPE, HEARING_ID_1);
        verify(hearing).changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1);
        verify(hearing).assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1);
        verify(hearing).assignJudiciary(judicialRoles, HEARING_ID_1);
        verify(hearing).assignHearingDays(null,null, NON_SITTING_DAYS1, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1);
        verify(hearing).changeWeekCommencingDate(WEEK_COMMENCING_START_DATE, WEEK_COMMENCING_END_DATE, WEEK_COMMENCING_DURATION,HEARING_ID_1);
    }


    @Test
    public void listingCommandHandlerShouldTriggerDefendantsToBeUpdatedEvent() throws Exception {
        givenEventStream(CASE_ID, eventStream, aCase, Case.class);

        Defendant defendant = createDomainDefendantForUpdateDefendant();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(aCase.updateDefendant(eq(CASE_ID), any(Defendant.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = updateCaseDefendantDetailsCommandEnvelope();

        listingCommandHandler.updateCaseDefendantDetails(commandEnvelope);

        verify(aCase).updateDefendant(CASE_ID, defendant);
    }


    @Test
    public void listingCommandHandlerShouldTriggerDefendantsToBeAddedToCourtProceedingsEvent() throws Exception {
        givenEventStream(CASE_ID, eventStream, aCase, Case.class);

        Defendant defendant = createDomainDefendantForAddDefendantToCourtProceedings();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(aCase.addedDefendantForCourtProceedings(eq(CASE_ID), any(Defendant.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = addDefendantsForCourtProceedingsCommandEnvelope();

        listingCommandHandler.addDefendantsToCourtProceedings(commandEnvelope);

        verify(aCase, atLeast(1)).addedDefendantForCourtProceedings(CASE_ID, defendant);
    }

    @Test
    public void listingCommandHandlerShouldTriggerDefendantOffencesUpdatedEvent() throws Exception {
        givenEventStream(CASE_ID, eventStream, aCase, Case.class);

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(aCase.updateDefendantOffences(any(CaseOffences.class))).thenReturn(mock(Stream.class));
        when(aCase.addedDefendantOffences(any(CaseOffences.class))).thenReturn(mock(Stream.class));
        when(aCase.deleteDefendantOffences(any(CaseSimpleOffences.class))).thenReturn(mock(Stream.class));
        when(aggregateService.get(any(), any(Class.class))).thenReturn(aCase);

        final JsonEnvelope commandEnvelope = updateCaseDefendantOffencesCommandEnvelope();
        listingCommandHandler.updateCaseDefendantOffences(commandEnvelope);

        verify(aCase, atLeast(1)).updateDefendantOffences(updatedCaseOffencesCaptor.capture());
        verify(aCase, atLeast(1)).deleteDefendantOffences(deletedCaseOffencesCaptor.capture());
        verify(aCase, atLeast(1)).addedDefendantOffences(addedCaseOffencesCaptor.capture());

        final List<CaseOffences> updatedCaseOffences = updatedCaseOffencesCaptor.getAllValues();
        final List<CaseOffences> addedCaseOffences = addedCaseOffencesCaptor.getAllValues();
        final List<CaseSimpleOffences> deletedCaseOffences = deletedCaseOffencesCaptor.getAllValues();

        final String expectedUpdatedCaseOffences1 = "{\n" +
                "  \"caseId\": \"" + CASE_ID + "\",\n" +
                "  \"defendantId\": \"" + DEFENDANT_ID1 + "\",\n" +
                "  \"offences\": [\n" +
                "    {\n" +
                "      \"endDate\": \"2011-08-01\",\n" +
                "      \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                "      \"offenceCode\": \"H8189\",\n" +
                "      \"offenceWording\": \"on 01/08/2009 at the county public house, unlawfully and maliciously wounded, John Smith\",\n" +
                "      \"startDate\": \"2010-08-01\",\n" +
                "      \"statementOfOffence\": {" +
                "        \"legislation\": \"Welsh legislation\",\n" +
                "        \"title\": \"Wounding with intent\",\n" +
                "        \"welshLegislation\": \"legislation\",\n" +
                "        \"welshTitle\": \"Wounding with intent in Welsh\"\n}\n" +
                "       },\n" +
                "    {\n" +
                "      \"endDate\": \"2011-08-01\",\n" +
                "      \"id\": \"" + UPDATED_OFFENCE_ID2 + "\",\n" +
                "      \"offenceCode\": \"H8189\",\n" +
                "      \"offenceWording\": \"on 01/08/2009 at the county public house, unlawfully and maliciously wounded, Fred Smith\",\n" +
                "      \"startDate\": \"2010-08-01\",\n" +
                "      \"statementOfOffence\": {" +
                "        " +
                "        \"legislation\": \"Welsh legislation\",\n" +
                "        \"title\": \"Wounding with intent\",\n" +
                "        \"welshLegislation\": \"legislation\",\n" +
                "        \"welshTitle\": \"Wounding with intent in Welsh\"\n     " +
                "       }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        final String expectedUpdatedCaseOffences2 = "{\n" +
                "  \"caseId\": \"" + CASE_ID + "\",\n" +
                "  \"defendantId\": \"" + DEFENDANT_ID2 + "\",\n" +
                "  \"offences\": [\n" +
                "    {\n" +
                "      \"endDate\": \"2011-08-01\",\n" +
                "      \"id\": \"" + UPDATED_OFFENCE_ID2 + "\",\n" +
                "      \"offenceCode\": \"H8189\",\n" +
                "      \"offenceWording\": \"on 01/08/2009 at the county public house, unlawfully and maliciously wounded, Jack Smith\",\n" +
                "      \"startDate\": \"2010-08-01\",\n" +
                "      \"statementOfOffence\": {" +
                "        \"legislation\": \"Welsh legislation\",\n" +
                "        \"title\": \"Wounding with intent\",\n" +
                "        \"welshLegislation\": \"legislation\",\n" +
                "        \"welshTitle\": \"Wounding with intent in Welsh\"\n}\n" +
                "       },\n" +
                "    {\n" +
                "      \"endDate\": \"2011-08-01\",\n" +
                "      \"id\": \"" + UPDATED_OFFENCE_ID3 + "\",\n" +
                "      \"offenceCode\": \"H8189\",\n" +
                "      \"offenceWording\": \"on 01/08/2009 at the county public house, unlawfully and maliciously wounded, Jack Smith\",\n" +
                "      \"startDate\": \"2010-08-01\",\n" +
                "      \"statementOfOffence\": {" +
                "        " +
                "        \"legislation\": \"Welsh legislation\",\n" +
                "        \"title\": \"Wounding with intent\",\n" +
                "        \"welshLegislation\": \"legislation\",\n" +
                "        \"welshTitle\": \"Wounding with intent in Welsh\"\n     " +
                "       }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        final String expectedAddedCaseOffences1 =
                "{\n" +
                        "  \"caseId\": \"" + CASE_ID + "\",\n" +
                        "  \"defendantId\": \"" + DEFENDANT_ID1 + "\",\n" +
                        "  \"offences\": [\n" +
                        "    {\n" +
                        "      \"endDate\": \"2011-08-01\",\n" +
                        "      \"id\": \"" + ADDED_OFFENCE_ID1 + "\",\n" +
                        "      \"offenceCode\": \"H8189\",\n" +
                        "      \"offenceWording\": \"on 01/08/2009 at the county public house, unlawfully and maliciously wounded, Billy Smith\",\n" +
                        "      \"startDate\": \"2010-08-01\",\n" +
                        "      \"statementOfOffence\": {\n" +
                        "        \"legislation\": \"Welsh legislation\",\n" +
                        "        \"title\": \"Wounding with intent\",\n" +
                        "        \"welshLegislation\": \"legislation\",\n" +
                        "        \"welshTitle\": \"Wounding with intent in Welsh\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        final String expectedAddedCaseOffences2 =
                "{\n" +
                        "  \"caseId\": \"" + CASE_ID + "\",\n" +
                        "  \"defendantId\": \"" + DEFENDANT_ID2 + "\",\n" +
                        "  \"offences\": [\n" +
                        "    {\n" +
                        "      \"endDate\": \"2011-08-01\",\n" +
                        "       \"id\": \"" + ADDED_OFFENCE_ID2 + "\",\n" +
                        "      \"offenceCode\": \"H8189\",\n" +
                        "      \"offenceWording\": \"on 01/08/2009 at the county public house, unlawfully and maliciously wounded, Jack Smith\",\n" +
                        "      \"startDate\": \"2010-08-01\",\n" +
                        "      \"statementOfOffence\": {\n" +
                        "        \"legislation\": \"Welsh legislation\",\n" +
                        "        \"title\": \"Wounding with intent\",\n" +
                        "        \"welshLegislation\": \"legislation\",\n" +
                        "        \"welshTitle\": \"Wounding with intent in Welsh\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"endDate\": \"2011-08-01\",\n" +
                        "      \"id\": \"" + ADDED_OFFENCE_ID3 + "\",\n" +
                        "      \"offenceCode\": \"H8189\",\n" +
                        "      \"offenceWording\": \"on 01/08/2009 at the county public house, unlawfully and maliciously wounded, Jack Smith\",\n" +
                        "      \"startDate\": \"2010-08-01\",\n" +
                        "      \"statementOfOffence\": {\n" +
                        "        \"legislation\": \"Welsh legislation\",\n" +
                        "        \"title\": \"Wounding with intent\",\n" +
                        "        \"welshLegislation\": \"legislation\",\n" +
                        "        \"welshTitle\": \"Wounding with intent in Welsh\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        final String expectedDeletedCaseOffences1 =
                "{\n" +
                        "  \"caseId\": \"" + CASE_ID + "\",\n" +
                        "  \"defendantId\": \"" + DEFENDANT_ID1 + "\",\n" +
                        "  \"offences\": [\n" +
                        "    {\n" +
                        "      \"id\": \"" + DELETED_OFFENCE_ID1 + "\",\n" +
                        "      \"defendantId\": \"" + DEFENDANT_ID1 + "\",\n" +
                        "    },\n" +
                        "    {\n" +
                        "       \"id\": \"" + DELETED_OFFENCE_ID2 + "\",\n" +
                        "      \"defendantId\": \"" + DEFENDANT_ID1 + "\",\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        final String expectedDeletedCaseOffences2 =
                "{\n" +
                        "  \"caseId\": \"" + CASE_ID + "\",\n" +
                        "  \"defendantId\": \"" + DEFENDANT_ID2 + "\",\n" +
                        "  \"offences\": [\n" +
                        "    {\n" +
                        "      \"id\": \"" + DELETED_OFFENCE_ID3 + "\",\n" +
                        "      \"defendantId\": \"" + DEFENDANT_ID2 + "\",\n" +
                        "    },\n" +
                        "    {\n" +
                        "       \"id\": \"" + DELETED_OFFENCE_ID4 + "\",\n" +
                        "      \"defendantId\": \"" + DEFENDANT_ID2 + "\",\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        assertEquals(expectedUpdatedCaseOffences1, objectMapper.writeValueAsString(updatedCaseOffences.get(0)), true);
        assertEquals(expectedUpdatedCaseOffences2, objectMapper.writeValueAsString(updatedCaseOffences.get(1)), true);
        assertEquals(expectedAddedCaseOffences1, objectMapper.writeValueAsString(addedCaseOffences.get(0)), true);
        assertEquals(expectedAddedCaseOffences2, objectMapper.writeValueAsString(addedCaseOffences.get(1)), true);
        assertEquals(expectedDeletedCaseOffences1, objectMapper.writeValueAsString(deletedCaseOffences.get(0)), true);
        assertEquals(expectedDeletedCaseOffences2, objectMapper.writeValueAsString(deletedCaseOffences.get(1)), true);
    }


    @Test
    public void listingCommandHandlerShouldTriggerDefendantDetailsUpdatedEvents() throws Exception {
        givenEventStream(HEARING_ID_1, eventStream, hearing, Hearing.class);

        List<Defendant> defendants = singletonList(createDomainDefendantForUpdateDefendantsForHearing());
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearing.updateDefendants(eq(CASE_ID), anyObject())).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = updateDefendantsForHearingCommandEnvelope();

        listingCommandHandler.updateDefendantsForHearing(commandEnvelope);

        verify(hearing).updateDefendants(CASE_ID, defendants);
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceUpdatedEvents() throws Exception {
        givenEventStream(HEARING_ID_1, eventStream, hearing, Hearing.class);

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(hearing.updateOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(Offence.class))).thenReturn(mock(Stream.class));
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        final JsonEnvelope commandEnvelope = updateOffencesForHearingCommandEnvelope();
        listingCommandHandler.updateOffencesForHearing(commandEnvelope);

        verify(hearing, atMost(1)).updateOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainOffencesCaptor.capture());

        final List<Offence> capturedDomainOffences = domainOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-01\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                        "    \"offenceCode\": \"H8189\",\n" +
                        "    \"startDate\": \"2010-08-01\",\n" +
                        "    \"statementOfOffence\": {\n" +
                        "      \"legislation\": \"Welsh legislation\",\n" +
                        "      \"title\": \"Wounding with intent\",\n" +
                        "      \"welshLegislation\": \"legislation\",\n" +
                        "      \"welshTitle\": \"Wounding with intent in Welsh\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-20\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID2 + "\",\n" +
                        "    \"offenceCode\": \"H8189X\",\n" +
                        "    \"startDate\": \"2010-08-10\",\n" +
                        "    \"statementOfOffence\": {\n" +
                        "      \"legislation\": \"Welsh legislation2\",\n" +
                        "      \"title\": \"Wounding with intent2\",\n" +
                        "      \"welshLegislation\": \"legislation2\",\n" +
                        "      \"welshTitle\": \"Wounding with intent in Welsh2\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "]\n";

        assertEquals(expectedDomainOffences, objectMapper.writeValueAsString(capturedDomainOffences), true);
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceDeletedEvents() throws Exception {
        givenEventStream(HEARING_ID_1, eventStream, hearing, Hearing.class);

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(hearing.deleteOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(SimpleOffence.class))).thenReturn(mock(Stream.class));
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        final JsonEnvelope commandEnvelope = deleteOffencesForHearingCommandEnvelope();
        listingCommandHandler.deleteOffencesForHearing(commandEnvelope);

        verify(hearing, atMost(1)).deleteOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainSimpleOffencesCaptor.capture());

        final List<SimpleOffence> capturedDomainOffences = domainSimpleOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"id\": \"" + DELETED_OFFENCE_ID1 + "\",\n" +
                        "    \"defendantId\": \"" + DEFENDANT_ID1 + "\",\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"id\": \"" + DELETED_OFFENCE_ID2 + "\",\n" +
                        "    \"defendantId\": \"" + DEFENDANT_ID1 + "\",\n" +
                        "  }\n" +
                        "]\n";

        assertEquals(expectedDomainOffences, objectMapper.writeValueAsString(capturedDomainOffences), true);
    }

    @Test
    public void listingCommandHandlerShouldTriggerAddCourtApplicationToHearingEvents() throws Exception {

        final JsonEnvelope commandEnvelope = addCourtApplicationForHearingCommandEnvelope();

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        CourtApplication courtApplication = getNewCourtApplication();

        when(hearing.addCourtApplication(HEARING_ID_1, courtApplication)).thenReturn(mock(Stream.class));

        listingCommandHandler.addCourtApplicationForHearing(commandEnvelope);

        verify(hearing).addCourtApplication(HEARING_ID_1, courtApplication);

        verify(hearing).addCourtApplication(hearingIdCaptor.capture(), courtApplicationCaptor.capture());
        final CourtApplication courtApplicationArguments = courtApplicationCaptor.getValue();
        assertThat(courtApplicationArguments.getApplicant().getFirstName(), equalTo(courtApplication.getApplicant().getFirstName()));
        assertThat(courtApplicationArguments.getApplicant().getLastName(), equalTo(courtApplication.getApplicant().getLastName()));
        assertThat(courtApplicationArguments.getRespondents().get(0).getFirstName(), equalTo(courtApplication.getRespondents().get(0).getFirstName()));
        assertThat(courtApplicationArguments.getRespondents().get(0).getLastName(), equalTo(courtApplication.getRespondents().get(0).getLastName()));
        assertThat(courtApplicationArguments.getId(), equalTo(courtApplication.getId()));
        assertThat(courtApplicationArguments.getApplicant().getCourtApplicationPartyType(), equalTo(courtApplication.getApplicant().getCourtApplicationPartyType()));
        final UUID hearingIdArguments = hearingIdCaptor.getValue();
        assertThat(hearingIdArguments, equalTo(HEARING_ID_1));

    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceAddedEvents() throws Exception {
        givenEventStream(HEARING_ID_1, eventStream, hearing, Hearing.class);

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(hearing.addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(Offence.class))).thenReturn(mock(Stream.class));
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        final JsonEnvelope commandEnvelope = addOffencesForHearingCommandEnvelope();
        listingCommandHandler.addOffencesForHearing(commandEnvelope);

        verify(hearing, atMost(1)).addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainOffencesCaptor.capture());

        final List<Offence> capturedDomainOffences = domainOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-01\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                        "    \"offenceCode\": \"H8189\",\n" +
                        "    \"startDate\": \"2010-08-01\",\n" +
                        "    \"statementOfOffence\": {\n" +
                        "      \"legislation\": \"Welsh legislation\",\n" +
                        "      \"title\": \"Wounding with intent\",\n" +
                        "      \"welshLegislation\": \"legislation\",\n" +
                        "      \"welshTitle\": \"Wounding with intent in Welsh\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-20\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID2 + "\",\n" +
                        "    \"offenceCode\": \"H8189X\",\n" +
                        "    \"startDate\": \"2010-08-10\",\n" +
                        "    \"statementOfOffence\": {\n" +
                        "      \"legislation\": \"Welsh legislation2\",\n" +
                        "      \"title\": \"Wounding with intent2\",\n" +
                        "      \"welshLegislation\": \"legislation2\",\n" +
                        "      \"welshTitle\": \"Wounding with intent in Welsh2\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "]\n";

        assertEquals(expectedDomainOffences, objectMapper.writeValueAsString(capturedDomainOffences), true);
    }

    @Test
    public void listingCommandHandlerShouldTriggerHearingAddedToCase() throws Exception {
        givenEventStream(CASE_ID, eventStream, new Case(), Case.class);

        final AddHearingToCaseCommand addHearingToCaseCommand = AddHearingToCaseCommand.addHearingToCaseCommand()
                .withCaseId(CASE_ID)
                .withHearingId(HEARING_ID_1)
                .build();
        final JsonObject addHearingToCaseCommandJsonObject = (JsonObject) objectToJsonValueConverter.convert(addHearingToCaseCommand);
        final JsonEnvelope commandEnvelope = addHearingToCaseCommandEnvelope(addHearingToCaseCommandJsonObject);
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.addHearingToCase(commandEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(HEARING_ADDED_TO_CASE_EVENT)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.caseId",
                                                equalTo(command.getString("caseId"))),
                                        withJsonPath("$.hearingId",
                                                equalTo(command.getString("hearingId")))
                                )))
                )
        );
    }

    @Test
    public void listingCommandHandlerShouldTriggerHearingDaySequenceChange() throws Exception {

        final JsonEnvelope commandEnvelope = updateSequenceForHearingDayCommandEnvelope();

        HearingDay hearingDay1 = HearingDay.hearingDay()
                .withSequence(Integer.valueOf(SEQUENCE_1))
                .withHearingDate(LocalDate.parse(HEARING_DATE_1))
                .build();
        HearingDay hearingDay2 = HearingDay.hearingDay()
                .withSequence(Integer.valueOf(SEQUENCE_2))
                .withHearingDate(LocalDate.parse(HEARING_DATE_2))
                .build();
        SequenceHearing sequenceHearing = SequenceHearing.sequenceHearing()
                .withId(HEARING_ID_1)
                .withHearingDays(Arrays.asList(hearingDay1, hearingDay2))
                .build();

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        when(hearing.sequenceHearingDays(sequenceHearing)).thenReturn(Stream.of(HearingDaysSequenced.hearingDaysSequenced().build()));
        when(hearing.applyAllocationRules()).thenReturn(mock(Stream.class));
        listingCommandHandler.sequenceHearings(commandEnvelope);
        verify(hearing).sequenceHearingDays(sequenceHearing);
    }

    @Test
    public void listingCommandHandlerShouldTriggerUpdateCourtApplicationForHearingChange() throws Exception {

        final JsonEnvelope commandEnvelope = updateCourtApplicationForHearingsCommandEnvelope();

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(eventSource.getStreamById(HEARING_ID_2)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        CourtApplication courtApplication = getCourtApplication();

        when(hearing.updateCourtApplication(any(), any())).thenReturn(mock(Stream.class));
        listingCommandHandler.updateCourtApplicationForHearings(commandEnvelope);

        verify(hearing).updateCourtApplication(HEARING_ID_1, courtApplication);
        verify(hearing, times(1)).updateCourtApplication(HEARING_ID_1, courtApplication);
        verify(hearing).updateCourtApplication(HEARING_ID_2, courtApplication);
        verify(hearing, times(1)).updateCourtApplication(HEARING_ID_2, courtApplication);

        verify(hearing, atLeast(2)).updateCourtApplication(hearingIdCaptor.capture(), courtApplicationCaptor.capture());

        final CourtApplication courtApplicationArguments = courtApplicationCaptor.getValue();

        assertThat(courtApplicationArguments.getApplicant().getFirstName(), equalTo(courtApplication.getApplicant().getFirstName()));
        assertThat(courtApplicationArguments.getApplicant().getLastName(), equalTo(courtApplication.getApplicant().getLastName()));
        assertThat(courtApplicationArguments.getRespondents().get(0).getFirstName(), equalTo(courtApplication.getRespondents().get(0).getFirstName()));
        assertThat(courtApplicationArguments.getRespondents().get(0).getLastName(), equalTo(courtApplication.getRespondents().get(0).getLastName()));
        assertThat(courtApplicationArguments.getId(), equalTo(courtApplication.getId()));

        final List<UUID> hearingIdArguments = hearingIdCaptor.getAllValues();

        assertThat(hearingIdArguments.get(0), equalTo(HEARING_ID_1));
        assertThat(hearingIdArguments.get(1), equalTo(HEARING_ID_2));
    }
    @Test
    public void shouldAddApplicationToHearing() throws Exception{
        givenEventStream(APPLICATION_ID, eventStream, new Application(), Application.class);

        final AddCourtApplicationToHearingCommand addCourtApplicationToHearingCommand = AddCourtApplicationToHearingCommand
                .addCourtApplicationToHearingCommand()
                .withApplicationId(APPLICATION_ID)
                .withHearingId(HEARING_ID_1)
                .build();
        final JsonObject addCourtApplicationToHearingJsonCommand = (JsonObject) objectToJsonValueConverter.convert(addCourtApplicationToHearingCommand);
        final JsonEnvelope commandEnvelope = addCourtApplicationToHearingCommandEnvelope(addCourtApplicationToHearingJsonCommand);
        final JsonObject command = commandEnvelope.payloadAsJsonObject();

        listingCommandHandler.addCourtApplicationToHearing(commandEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName(COURT_APPLICATION_ADDED_TO_HEARING_EVENT)
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.applicationId",
                                                equalTo(command.getString("applicationId"))),
                                        withJsonPath("$.hearingId",
                                                equalTo(command.getString("hearingId")))
                                )))
                )
        );

    }
    @Test
    public void shouldUpdateCourtApplication() throws Exception{
        givenEventStream(APPLICATION_ID, eventStream, anApplication, Application.class);
        final uk.gov.justice.core.courts.CourtApplication courtApplication = uk.gov.justice.core.courts
                .CourtApplication.courtApplication().withId(APPLICATION_ID).build();
        when(eventSource.getStreamById(APPLICATION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Application.class)).thenReturn(anApplication);
        when(anApplication.update(any(CourtApplication.class))).thenReturn(mock(Stream.class));
        final JsonEnvelope commandEnvelope = updateCourtApplicationCommandEnvelope();

        listingCommandHandler.updateCourtApplication(commandEnvelope);
        verify(anApplication).update(any(CourtApplication.class));

    }

    @Test
    public void shouldRestrictCaseFromCourtListing() throws Exception{

        final JsonEnvelope commandEnvelope = restrictCourtListCommandEnvelope();

        givenEventStream(HEARING_ID_1, eventStream, hearing, Hearing.class);

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearing.restrictDetailsFromCourt(eq(HEARING_ID_1), anyObject())).thenReturn(mock(Stream.class));

        RestrictCourtList restrictCourtList = RestrictCourtList.restrictCourtList()
                .withHearingId(HEARING_ID_1)
                .withCaseIds(Arrays.asList(CASE_ID))
                .withCourtApplicatonIds(Arrays.asList(COURT_APPLICATION_ID))
                .withCourtApplicationType(COURT_APPLICATION_TYPE)
                .build();
        listingCommandHandler.restrictFromCourtList(commandEnvelope);

        verify(hearing).restrictDetailsFromCourt((HEARING_ID_1),restrictCourtList);

    }

    @Test
    public void shouldEjectCaseFromCourtListing() throws Exception {

        final JsonEnvelope commandEnvelope = ejectCaseCommandEnvelope();

        givenEventStream(CASE_ID, eventStream, aCase, Case.class);

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(aCase.ejectCase(eq(Arrays.asList(HEARING_ID_1)), eq(CASE_ID), eq(Optional.of("SomeReason")))).thenReturn(mock(Stream.class));

        listingCommandHandler.ejectCaseOrApplication(commandEnvelope);

        verify(aCase).ejectCase((Arrays.asList(HEARING_ID_1)), CASE_ID, Optional.of("SomeReason"));

    }

    @Test
    public void shouldEjectApplicationFromCourtListing() throws Exception {

        final JsonEnvelope commandEnvelope = ejectApplicationCommandEnvelope();
        givenEventStream(COURT_APPLICATION_ID, eventStream, anApplication, Application.class);

        when(eventSource.getStreamById(COURT_APPLICATION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Application.class)).thenReturn(anApplication);
        when(anApplication.ejectApplication(eq(Arrays.asList(HEARING_ID_1)), eq(COURT_APPLICATION_ID),eq(Optional.of("SomeReason")))).thenReturn(mock(Stream.class));

        listingCommandHandler.ejectCaseOrApplication(commandEnvelope);

        verify(anApplication).ejectApplication((Arrays.asList(HEARING_ID_1)), COURT_APPLICATION_ID,Optional.of("SomeReason"));

    }

    @Test
    public void listingCommandHandlerShouldTriggerDefendantsAddedForCourtProceedingsEvents() throws Exception {
        givenEventStream(HEARING_ID_1, eventStream, hearing, Hearing.class);

        List<Defendant> defendants = singletonList(createDomainDefendantForUpdateDefendantsForHearing());
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearing.addDefendantsForCourtProceedings(eq(CASE_ID), anyObject())).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = addDefendantsForHearingCommandEnvelope();

        listingCommandHandler.addDefendantsToCourtProceedingsForHearing(commandEnvelope);

        ArgumentCaptor<UUID> caseIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<List> defendantListCaptor = ArgumentCaptor.forClass(List.class);

        //verify(hearing).addDefendantsForCourtProceedings(CASE_ID, defendants);
        verify(hearing).addDefendantsForCourtProceedings(caseIdCaptor.capture(), defendantListCaptor.capture());
        Assert.assertThat(CASE_ID, Matchers.is(caseIdCaptor.getValue()));
        List<Defendant> actualDefendantList = defendantListCaptor.getValue();
        Assert.assertThat(actualDefendantList, Matchers.is(defendants));
    }

    @Test
    public void listingCommandHandlerShouldTriggerExportFailedForPublishEvent() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final String failedTime = "2016-09-09T08:31:40Z";
        final String errorMessage = "Error message";
        final String documentName = randomAlphanumeric(30).toString();
        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtListAggregate.class)).thenReturn(courtListAggregate);
        when(courtListAggregate.recordCourtListExportFailed(any(ZonedDateTime.class), eq(documentName), eq(errorMessage))).thenReturn(Stream.of(publishCourtListExportFailed().build()));

        final String jsonString = givenPayload("/test-data/listing.command.mark-as-export-failed.json").toString()
                .replace("DOCUMENT_ID", documentId.toString())
                .replace("DOCUMENT_NAME", documentName)
                .replace("ERROR_MESSAGE", errorMessage)
                .replace("FAILED_TIME", failedTime.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            final JsonEnvelope commandEnvelope = createEnvelope("listing.command.mark-export-as-failed", jsonReader.readObject());
            listingCommandHandler.markAsExportFailed(commandEnvelope);
            verify(courtListAggregate).recordCourtListExportFailed(any(ZonedDateTime.class), eq(documentName), eq(errorMessage));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void listingCommandHandlerShouldTriggerExportSuccessfulForPublishEvent() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final String publishedTime = "2016-09-09T08:31:40Z";
        final String documentName = randomAlphanumeric(30).toString();
        when(eventSource.getStreamById(documentId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtListAggregate.class)).thenReturn(courtListAggregate);
        when(courtListAggregate.recordCourtListExportSuccessful(any(String.class), any(ZonedDateTime.class)))
                .thenReturn(Stream.of(publishCourtListExportSuccessful().build()));

        final String jsonString = givenPayload("/test-data/listing.command.mark-as-export-successful.json").toString()
                .replace("DOCUMENT_ID", documentId.toString())
                .replace("DOCUMENT_NAME", documentName)
                .replace("PUBLISHED_TIME", publishedTime.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            final JsonEnvelope commandEnvelope = createEnvelope("listing.command.mark-as-export-successful", jsonReader.readObject());
            listingCommandHandler.markAsExportSuccessful(commandEnvelope);
            verify(courtListAggregate).recordCourtListExportSuccessful(eq(documentName), any(ZonedDateTime.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private JsonEnvelope updateSequenceForHearingDayCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-sequence-for-hearing-day.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("SEQUENCE_1", SEQUENCE_1)
                .replace("SEQUENCE_2", SEQUENCE_2)
                .replace("HEARING_DATE_1", HEARING_DATE_1)
                .replace("HEARING_DATE_2", HEARING_DATE_2);

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.sequence-hearings", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: fix this
    private Case givenCaseSentForListing(UUID caseId) {

        return null;
    }


    private Hearing givenHearingListed(UUID hearingId) {


        return null;

    }


    private String getStartTime(JsonObject command) {
        return ZonedDateTimes.toString(parse(command.getJsonArray("startTimes").getString(0)));
    }

    private void givenHearingHasBeenListed(Hearing hearing) throws Exception {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(listHearingEventStream);
        when(aggregateService.get(listHearingEventStream, Hearing.class)).thenReturn(hearing);

        final JsonEnvelope listHearingEnvelope = listHearingCommandEnvelope();

    }

    private <T extends Aggregate> void givenEventStream(UUID id, EventStream eventStream, T aggregate, Class<T> clz) {
        when(this.eventSource.getStreamById(id)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, clz)).thenReturn(aggregate);
    }

    private JsonEnvelope listCourtHearingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.list-court-hearing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("OFFENCE_ID", OFFENCE_ID1.toString())
                .replace("REPORTING_RESTRICTIONS", REPORTING_RESTRICTIONS)
                .replace("PROSECUTOR_DATES_TO_AVOID", PROSECUTOR_DATES_TO_AVOID)
                .replace("JURISDICTION_TYPE", JURISDICTION_TYPE.toString())
                .replace("JUDICIAL_ID", JUDICIAL_ID_1.toString())
                .replace("AUTHORITY_ID", AUTHORITY_ID.toString())
                .replace("CUSTODY_TIME_LIMIT", CUSTODY_TIME_LIMIT)
                .replace("DEFAULT_DURATION", DEFAULT_DURATION)
                .replace("DEFAULT_START_TIME", DEFAULT_START_TIME)
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_ROOM_ID", COURT_ROOM_ID.toString())
                .replace("LISTING_DIRECTIONS", LISTING_DIRECTIONS)
                .replace("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("DEFENDANT_ID", DEFENDANT_ID1.toString())
                .replaceAll("CASE_ID", CASE_ID.toString())
                .replace("EARLIEST_START_TIME", EARLIEST_START_TIME);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.list-court-hearing", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope changeJudiciaryForHearingsCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.change-judiciary-for-hearings.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("HEARING_ID_2", HEARING_ID_2.toString())
                .replace("JUDICIAL_ID_1", JUDICIAL_ID_1.toString())
                .replace("JUDICIAL_ID_2", JUDICIAL_ID_2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.change-judiciary-for-hearing", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope restrictCourtListCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.restrict-court-list.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("CASE_ID_1", CASE_ID.toString())
                .replace("COURT_APPLICATION_ID_1", COURT_APPLICATION_ID.toString())
                .replace("COURT_APPLICATION_TYPE_1", COURT_APPLICATION_TYPE);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.restrict-court-list", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope ejectCaseCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.eject-case.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("CASE_ID_1", CASE_ID.toString())
                .replace("REMOVAL_REASON", "SomeReason");

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.eject-case-or-application", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope ejectApplicationCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.eject-application.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("COURT_APPLICATION_ID_1", COURT_APPLICATION_ID.toString())
                .replace("REMOVAL_REASON", "SomeReason");;

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.eject-case-or-application", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateCaseDefendantDetailsCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-case-defendant-details.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-case-defendant-details", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private JsonEnvelope updateCourtApplicationCommandEnvelope() {
            return createEnvelope("listing.command.update-court-application", createObjectBuilder().add("courtApplication",
                    createObjectBuilder().add("id",APPLICATION_ID.toString())
                            .add("type", createObjectBuilder().add("ApplicationType", "type"))
                            .add("applicant", createObjectBuilder()
                                    .add("id", randomUUID().toString()))
                            .build()).build());
    }


    private JsonEnvelope updateCaseDefendantOffencesCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-case-defendant-offences.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("DEFENDANT_ID2", DEFENDANT_ID2.toString())
                .replace("DEFENDANT_ID3", DEFENDANT_ID3.toString())
                .replace("UPDATED_OFFENCE_ID1", UPDATED_OFFENCE_ID1.toString())
                .replace("UPDATED_OFFENCE_ID2", UPDATED_OFFENCE_ID2.toString())
                .replace("UPDATED_OFFENCE_ID3", UPDATED_OFFENCE_ID3.toString())
                .replace("ADDED_OFFENCE_ID1", ADDED_OFFENCE_ID1.toString())
                .replace("ADDED_OFFENCE_ID2", ADDED_OFFENCE_ID2.toString())
                .replace("ADDED_OFFENCE_ID3", ADDED_OFFENCE_ID3.toString())
                .replace("DELETED_OFFENCE_ID1", DELETED_OFFENCE_ID1.toString())
                .replace("DELETED_OFFENCE_ID2", DELETED_OFFENCE_ID2.toString())
                .replace("DELETED_OFFENCE_ID3", DELETED_OFFENCE_ID3.toString())
                .replace("DELETED_OFFENCE_ID4", DELETED_OFFENCE_ID4.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-case-defendant-details", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateDefendantsForHearingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-defendants-for-hearing.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("DEFENDANT_ID", DEFENDANT_ID1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-case-defendant-details", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addDefendantsForHearingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.add-defendants-to-court-proceedings-for-hearing.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("DEFENDANT_ID", DEFENDANT_ID1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-defendants-to-court-proceedings", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addDefendantsForCourtProceedingsCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.add-defendants-to-court-proceedings.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("DEFENDANT_ID2", DEFENDANT_ID2.toString())
                .replace("OFFENCE_ID1", OFFENCE_ID1.toString());



        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-defendants-to-court-proceedings", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateHearingForListingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-hearing-for-listing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("HEARING_TYPE_ID", HEARING_TYPE.getId().toString())
                .replace("HEARING_TYPE_DESCRIPTION", HEARING_TYPE.getDescription())
                .replace("START_DATE", START_DATE.toString())
                .replace("END_DATE", END_DATE)
                .replace("HEARING_LANGUAGE", HEARING_LANGUAGE)
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_ROOM_ID", COURT_ROOM_ID.toString())
                .replace("NON_SITTING_DAY", NON_SITTING_DAY)
                .replace("NON_DEFAULT_DAY", NON_DEFAULT_DAY)
                .replace("DEFAULT_DURATION", DEFAULT_DURATION)
                .replace("DEFAULT_START_TIME", DEFAULT_START_TIME)
                .replace("JURISDICTION_TYPE", JURISDICTION_TYPE.toString())
                .replace("\"IS_DEPUTY\"", IS_DEPUTY.toString())
                .replace("\"IS_BENCH_CHAIRMAN\"", IS_BENCH_CHAIRMAN.toString())
                .replace("JUDICIAL_ID", JUDICIAL_ID_1.toString())
                .replace("JUDICIAL_ROLE_TYPE", JUDICIAL_ROLE_TYPE.toString())
                .replace("AUTHORITY_ID", AUTHORITY_ID.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-hearing-for-listing", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private JsonEnvelope updateHearingForListingwithWeekCommencingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-hearing-for-listing-week-commencing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("HEARING_TYPE_ID", HEARING_TYPE.getId().toString())
                .replace("HEARING_TYPE_DESCRIPTION", HEARING_TYPE.getDescription())
                .replace("WEEK_COMMENCING_START_DATE", WEEK_COMMENCING_START_DATE.toString())
                .replace("WEEK_COMMENCING_END_DATE", WEEK_COMMENCING_END_DATE.toString())
                .replace("WEEK_COMMENCING_DURATION", WEEK_COMMENCING_DURATION.toString())
                .replace("HEARING_LANGUAGE", HEARING_LANGUAGE)
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_ROOM_ID", COURT_ROOM_ID.toString())
                .replace("NON_SITTING_DAY", NON_SITTING_DAY)
                .replace("NON_DEFAULT_DAY", NON_DEFAULT_DAY)
                .replace("DEFAULT_DURATION", DEFAULT_DURATION)
                .replace("DEFAULT_START_TIME", DEFAULT_START_TIME)
                .replace("JURISDICTION_TYPE", JURISDICTION_TYPE.toString())
                .replace("\"IS_DEPUTY\"", IS_DEPUTY.toString())
                .replace("\"IS_BENCH_CHAIRMAN\"", IS_BENCH_CHAIRMAN.toString())
                .replace("JUDICIAL_ID", JUDICIAL_ID_1.toString())
                .replace("JUDICIAL_ROLE_TYPE", JUDICIAL_ROLE_TYPE)
                .replace("AUTHORITY_ID", AUTHORITY_ID.toString());
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-hearing-for-listing", jsonReader.readObject());
    }

    private JsonEnvelope addHearingToCaseCommandEnvelope(JsonObject commandJsonObject) {
        return createEnvelope("listing.command.add-hearing-to-case", commandJsonObject);
    }
    private JsonEnvelope addCourtApplicationToHearingCommandEnvelope(JsonObject commandJsonObject) {
        return createEnvelope("listing.command.add-court-application-to-hearing", commandJsonObject);
    }
    private JsonEnvelope updateCourtApplicationCommandEnvelope(JsonObject commandJsonObject) {
        return createEnvelope("listing.command.update-court-application", commandJsonObject);
    }
    private JsonEnvelope updateOffencesForHearingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-offences-for-hearing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("UPDATED_OFFENCE_ID1", UPDATED_OFFENCE_ID1.toString())
                .replace("UPDATED_OFFENCE_ID2", UPDATED_OFFENCE_ID2.toString())
                .replace("START_DATE", START_DATE.toString())
                .replace("END_DATE", END_DATE);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-offences-for-hearing", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope deleteOffencesForHearingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.delete-offences-for-hearing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("DELETED_OFFENCE_ID1", DELETED_OFFENCE_ID1.toString())
                .replace("DELETED_OFFENCE_ID2", DELETED_OFFENCE_ID2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.delete-offences-for-hearing", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addOffencesForHearingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.add-offences-for-hearing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("UPDATED_OFFENCE_ID1", UPDATED_OFFENCE_ID1.toString())
                .replace("UPDATED_OFFENCE_ID2", UPDATED_OFFENCE_ID2.toString())
                .replace("START_DATE", START_DATE.toString())
                .replace("END_DATE", END_DATE);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-offences-for-hearing", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateCourtApplicationForHearingsCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.update-court-application-for-hearings.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("HEARING_ID_2", HEARING_ID_2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-court-application-for-hearings", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addCourtApplicationForHearingCommandEnvelope() {
        String jsonString = givenPayload("/test-data/listing.command.add-court-application-for-hearing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-court-application-for-hearing", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope sendReListedCaseForListingCommandEnvelope() {
        JsonObject caseJson = createReListedListCourtHearingJson();
        return createEnvelope("listing.command.list-court-hearing", caseJson);
    }


    private JsonEnvelope listHearingCommandEnvelope() {
        JsonObject hearingJson = createCommandListHearingJson();
        return createEnvelope("listing.command.list-hearing", hearingJson);
    }

    private JsonEnvelope relistHearingCommandEnvelope() {
        JsonObject hearingJson = createCommandReListHearingJson();
        return createEnvelope("listing.command.list-hearing", hearingJson);
    }


    private JsonEnvelope updateHearingCommandEnvelopeWithOnlyMandatoryDataChanges() {
        JsonObject hearingJson = createUpdateHearingJsonWhereOnlyMandatoryDataHasChanged();
        return createEnvelope("listing.command.update-hearing-for-listing", hearingJson);
    }

    private JsonEnvelope updateHearingCommandEnvelopeWithCompleteChanges() {
        JsonObject hearingJson = createUpdateHearingJsonWhereAllDataHasChanged();
        return createEnvelope("listing.command.update-hearing-for-listing", hearingJson);
    }

    private JsonObject createListCourtHearingJson() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("urn", URN)
                .add("hearings", createHearingsJson())
                .build();
    }

    private JsonObject createUpdateDefendantsForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
                .add("defendants", createDefendantsJson())
                .build();
    }

    private JsonObject createUpdateOffencesForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
                .add("offences", createAddedOrUpdatedOffencesForHearing())
                .build();
    }

    private JsonObject createDAddHearingToCaseCommandJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
                .add("offences", createDeletedOffencesForHearing())
                .build();
    }

    private JsonObject createDeleteOffencesForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
                .add("offences", createDeletedOffencesForHearing())
                .build();
    }

    private JsonObject createAddOffencesForHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
                .add("offences", createAddedOrUpdatedOffencesForHearing())
                .build();
    }

    private JsonObject createUpdateCaseDefendantDetailsJson() {
        return createObjectBuilder()
                .add("defendant", createCourtsDefendantJson())
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
        JsonObject deletedCase = createObjectBuilder()
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

    private JsonObject createReListedListCourtHearingJson() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("urn", URN)
                .add("hearings", createReListedHearingsJson())
                .build();
    }

    private JsonObject createCommandListHearingJson() {
        return createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
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
                .add("hearingId", HEARING_ID_1.toString())
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
                .add("hearingId", HEARING_ID_1.toString())
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
                .add("hearingId", HEARING_ID_1.toString())
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
        JsonObject hearing = createObjectBuilder()
                .add("id", HEARING_ID_1.toString())
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
        JsonObject hearing = createObjectBuilder()
                .add("id", HEARING_ID_1.toString())
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
                .add("bailStatus", new BailStatus.Builder().withCode("P").withDescription("Conditional Bail with Pre-Release conditions").withId(UUID.fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).build().toString())
                .add("defenceOrganisation", DEFENCE_ORGANISATION_NAME)
                .add("offences", createAddedOrUpdatedOffences());

        if (hasCustodyTimeLimit) {
            defendantBuilder.add("custodyTimeLimit", CUSTODY_TIME_LIMIT);
        }
        JsonArrayBuilder defendants = createArrayBuilder().add(defendantBuilder.build());

        return defendants.build();
    }

    private JsonObject createCourtsDefendantJson() {
        JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("id", DEFENDANT_ID1.toString())
                .add("prosecutionCaseId", CASE_ID.toString())
                .add("defenceOrganisation", createDefenceOrganisationJson())
                .add("personDefendant", createCourtsPersonDefendantJson());

        return defendantBuilder.build();
    }

    private JsonObject createCourtsPersonDefendantJson() {
        JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("bailStatus", new BailStatus.Builder().withCode("P").withDescription("Conditional Bail with Pre-Release conditions").withId(UUID.fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).build().toString())
                .add("personDetails", createCourtsPersonDetailsJson());
        if (hasCustodyTimeLimit) {
            defendantBuilder.add("custodyTimeLimit", CUSTODY_TIME_LIMIT);
        }

        return defendantBuilder.build();
    }

    private JsonObject createCourtsPersonDetailsJson() {
        return createObjectBuilder()
                .add("title", Title.MISS.toString())
                .add("firstName", PERSON_FIRST_NAME1)
                .add("lastName", PERSON_LAST_NAME1)
                .add("dateOfBirth", PERSON_DOB)
                .add("nationalityCode", PERSON_NATIONALITY)
                .add("gender", Gender.MALE.toString())
                .add("address", createAddressJson())
                .build();
    }

    private JsonObject createDefenceOrganisationJson() {
        return createObjectBuilder()
                .add("id", DEFENCE_ORGANISATION_ID.toString())
                .add("name", DEFENCE_ORGANISATION_NAME)
                .build();
    }

    private JsonObject createAddressJson() {
        return createObjectBuilder()
                .add("address1", ADDRESS_LINE_1)
                .add("address2", ADDRESS_LINE_2)
                .add("address3", ADDRESS_LINE_3)
                .add("address4", ADDRESS_LINE_4)
                .add("address5", ADDRESS_LINE_4)
                .add("postcode", POSTCODE)
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

    private List<JudicialRole> createJudicalRoles() {
        return singletonList(JudicialRole.judicialRole().withJudicialId(JUDICIAL_ID_1)
                .withJudicialRoleType(
                        JudicialRoleType.judicialRoleType()
                                .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                                .build())
                .withIsBenchChairman(of(IS_BENCH_CHAIRMAN))
                .withIsDeputy(of(IS_DEPUTY)).build());
    }

    private ListedCase createdListedCase() {
        return ListedCase.listedCase()
                .withId(CASE_ID)
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode("TFL")
                        .withCaseReference("TFL12345")
                        .withAuthorityId(AUTHORITY_ID)
                        .build())
                .withDefendants(Arrays.asList(createDomainDefendant())
                )
                .build();
    }

    private Defendant createDomainDefendant() {
        return Defendant.defendant()
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDatesToAvoid(of("wednesdays"))
                .withDefenceOrganisation(Optional.empty())
                .withFirstName(of("Harry"))
                .withLastName(of("Kane Junior"))
                .withHearingLanguageNeeds(of(HearingLanguageNeeds.ENGLISH))
                .withId(DEFENDANT_ID1)
                .withOrganisationName(Optional.empty())
                .withSpecificRequirements(of("Screen"))
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(OFFENCE_ID1)
                        .withOffenceCode("AAA")
                        .withStartDate("2018-01-01")
                        .withEndDate(of("2018-01-01"))
                        .withOffenceWording("No Travel Card")
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withWelshTitle("a title in Welsh")
                                .withWelshLegislation(of("legislation in Welsh"))
                                .withLegislation(of("legislation"))
                                .withTitle("a title")
                                .build())

                        .build()))
                .build();
    }

    private Defendant createDomainDefendantForUpdateDefendant() {
        return Defendant.defendant()
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDefenceOrganisation(Optional.empty())
                .withHearingLanguageNeeds(empty())
                .withFirstName(of("Harry"))
                .withLastName(of("Kane Junior"))
                .withId(DEFENDANT_ID1)
                .withOrganisationName(of("withOrganisationName"))
                .withSpecificRequirements(of("Screen"))
                .withOffences(emptyList())
                .withDefenceOrganisation(of("withOrganisationName"))
                .build();
    }

    private Defendant createDomainDefendantForAddDefendantToCourtProceedings(){
        return Defendant.defendant()
                .withId(DEFENDANT_ID1)
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDefenceOrganisation(Optional.empty())
                .withHearingLanguageNeeds(empty())
                .withFirstName(of("Harry"))
                .withLastName(of("Kane Junior"))
                .withOrganisationName(Optional.empty())
                .withSpecificRequirements(of("Screen"))
                .withDatesToAvoid(Optional.empty())
                .withDefenceOrganisation(Optional.empty())
                .withHearingLanguageNeeds(Optional.empty())
                .withProsecutionCaseId(CASE_ID)
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(OFFENCE_ID1)
                        .withOffenceCode("TFL123")
                        .withOffenceWording("TFL ticket dodged")
                        .withStartDate("2019-05-01")
                        .withEndDate(Optional.empty())
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withWelshTitle("TFL Ticket Dodger")
                                .withWelshLegislation(Optional.empty())
                                .withLegislation(Optional.empty())
                                .withTitle("TFL Ticket Dodger")
                                .build()).build()))
                .build();

    }
    private Defendant createDomainDefendantForUpdateDefendantsForHearing() {
        return Defendant.defendant()
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDefenceOrganisation(Optional.empty())
                .withFirstName(of("Harry"))
                .withHearingLanguageNeeds(empty())
                .withLastName(of("Kane Junior"))
                .withId(DEFENDANT_ID1)
                .withDatesToAvoid(empty())
                .withOrganisationName(of("withOrganisationName"))
                .withSpecificRequirements(of("Screen"))
                .withOffences(emptyList())
                .withDefenceOrganisation(of("withOrganisationName"))
                .build();
    }

    private CourtApplication getNewCourtApplication() {
        return CourtApplication.courtApplication()
                .withLinkedCaseId(UUID.fromString("19e9d562-6abb-4871-bfb3-2d777aa90371"))
                .withParentApplicationId(UUID.fromString("9d9a431a-0f12-4386-878a-2bf6c4a0877e"))
                .withApplicationType("9vBchM49Go")
                .withId(UUID.fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withId(UUID.fromString("22b1078b-9430-4cef-ba46-eea40a129ca8"))
                        .withIsRespondent(false)
                        .withFirstName("David")
                        .withLastName("Dell")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .build())
                .withRespondents(Collections.singletonList(ApplicantRespondent.applicantRespondent()
                        .withIsRespondent(true)
                        .withFirstName("Luise")
                        .withLastName("Miller")
                        .withId(UUID.fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .build()))
                .withApplicationReference(Optional.of("REF-1"))
                .build();
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceAddedEventsWithCustodyTimeLimitData() throws Exception {
        givenEventStream(HEARING_ID_1, eventStream, hearing, Hearing.class);

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Case.class)).thenReturn(aCase);
        when(hearing.addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(Offence.class))).thenReturn(mock(Stream.class));
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        final JsonEnvelope commandEnvelope = addOffencesForHearingCommandEnvelopeWithCustodyTimeLimit();
        listingCommandHandler.addOffencesForHearing(commandEnvelope);

        verify(hearing, atMost(1)).addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainOffencesCaptor.capture());

        final List<Offence> capturedDomainOffences = domainOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-01\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                        "    \"offenceCode\": \"H8189\",\n" +
                        "    \"startDate\": \"2010-08-01\",\n" +
                        "    \"statementOfOffence\": {\n" +
                        "      \"legislation\": \"Welsh legislation\",\n" +
                        "      \"title\": \"Wounding with intent\",\n" +
                        "      \"welshLegislation\": \"legislation\",\n" +
                        "      \"welshTitle\": \"Wounding with intent in Welsh\"\n" +
                        "    },\n" +
                        "    \"custodyTimeLimit\": {\n" +
                        "      \"timeLimit\": \"2020-01-06\",\n" +
                        "      \"daysSpent\":1 \n" +
                        "    }\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-20\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID2 + "\",\n" +
                        "    \"offenceCode\": \"H8189X\",\n" +
                        "    \"startDate\": \"2010-08-10\",\n" +
                        "    \"statementOfOffence\": {\n" +
                        "      \"legislation\": \"Welsh legislation2\",\n" +
                        "      \"title\": \"Wounding with intent2\",\n" +
                        "      \"welshLegislation\": \"legislation2\",\n" +
                        "      \"welshTitle\": \"Wounding with intent in Welsh2\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "]\n";

        assertEquals(expectedDomainOffences, objectMapper.writeValueAsString(capturedDomainOffences), true);
    }

    private JsonEnvelope addOffencesForHearingCommandEnvelopeWithCustodyTimeLimit() {
        String jsonString = givenPayload("/test-data/listing.command.add-offences-for-hearing-including-ctl.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("UPDATED_OFFENCE_ID1", UPDATED_OFFENCE_ID1.toString())
                .replace("UPDATED_OFFENCE_ID2", UPDATED_OFFENCE_ID2.toString())
                .replace("START_DATE", START_DATE.toString())
                .replace("END_DATE", END_DATE);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-offences-for-hearing-including-ctl", jsonReader.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

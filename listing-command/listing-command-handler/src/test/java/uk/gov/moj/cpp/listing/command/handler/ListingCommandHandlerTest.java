package uk.gov.moj.cpp.listing.command.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.parse;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.justice.listing.event.PublishCourtListProduced.publishCourtListProduced;
import static uk.gov.justice.listing.event.PublishCourtListRequested.publishCourtListRequested;
import static uk.gov.justice.listing.event.PublishCourtListType.FIRM;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingHelper.getEnvelopeForExtendPartialHearing;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingHelper.getEnvelopeForExtendWholeHearing;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.ALLOCATED_HEARING_ID;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.CASE_ID1;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.CASE_ID2;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.DEF_ID1;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.DEF_ID2;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.DEF_ID3;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID1;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID2;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID3;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID4;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.UNALLOCATED_HEARING_ID;
import static uk.gov.moj.cpp.listing.command.utils.FileUtil.givenPayload;
import static uk.gov.moj.cpp.listing.domain.HearingLanguage.valueFor;
import static java.util.Optional.of;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.commands.RecordCourtListProduced;
import uk.gov.justice.listing.courts.AddCourtApplicationForHearing;
import uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand;
import uk.gov.justice.listing.courts.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.CancelHearingDays;
import uk.gov.justice.listing.courts.LinkedToCases;
import uk.gov.justice.listing.courts.SequenceHearings;
import uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.courts.UpdateLinkedCaseInHearing;
import uk.gov.justice.listing.courts.UpdateLinkedCases;
import uk.gov.justice.listing.event.CourtListExportRequested;
import uk.gov.justice.listing.event.HearingCounselModified;
import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishedCourtListStored;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.ApplicationEjected;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
import uk.gov.justice.listing.events.CasesAddedToHearing;
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
import uk.gov.justice.listing.events.HearingRescheduled;
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
import uk.gov.justice.listing.events.PublicListNoteChangedForHearing;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.command.factory.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.factory.HearingFactory;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.command.service.UUIDService;
import uk.gov.moj.cpp.listing.command.utils.CaseMarkersToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CasesToDomainConverter;
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
import uk.gov.moj.cpp.listing.command.utils.ExtendHearingHelper;
import uk.gov.moj.cpp.listing.command.utils.FileUtil;
import uk.gov.moj.cpp.listing.command.utils.HearingDaysToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.NonDefaultDayDurationBuilder;
import uk.gov.moj.cpp.listing.command.utils.ProsecutionCaseDefendantOffenceIdsBuilder;
import uk.gov.moj.cpp.listing.command.utils.ProsecutionCasesBuilder;
import uk.gov.moj.cpp.listing.command.utils.RotaSlotToNonDefaultDayConverter;
import uk.gov.moj.cpp.listing.command.utils.hearing.ExtendHearingUtils;
import uk.gov.moj.cpp.listing.common.azure.ProvisionalBookingService;
import uk.gov.moj.cpp.listing.common.azure.adapter.RotaSLServiceAdapter;
import uk.gov.moj.cpp.listing.domain.Address;
import uk.gov.moj.cpp.listing.domain.ApplicantRespondent;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
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
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.PublishCourtListRequestAggregate;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;
import uk.gov.moj.cpp.staginghmi.common.StagingHmiService;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
    private static final UUID DELETED_OFFENCE_CASE_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String FIRST_NAME = "Test Recipe";
    private static final String LAST_NAME = "Last Name";
    private static final String DATE_OF_BIRTH = "1980-07-15";
    private static final String PTP_TYPE = "PTP";
    private static final String SENTENCE_TYPE = "Sentence";
    private static final String INITIAL_START_DATE = "2018-05-30";
    private static final String END_DATE = "2018-06-03";
    private static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.now();
    private static final Integer WEEK_COMMENCING_DURATION = 1;
    private static final LocalDate WEEK_COMMENCING_END_DATE = LocalDate.now().plusWeeks(WEEK_COMMENCING_DURATION);
    private static final String UPDATED_START_DATE = "2018-06-01";
    private static final String UPDATED_END_DATE = "2018-06-03";
    private static final String UPDATED_START_TIME = "2018-06-01T11:30:00Z";
    private static final String OFFENCE_START_DATE = "2018-06-01";
    private static final String OFFENCE_END_DATE = "2018-06-07";
    private static final String NON_SITTING_DAY = "2018-06-02";
    private static final ZonedDateTime COURT_PROCEEDINGS_INITIATED = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
    private static final List<LocalDate> NON_SITTING_DAYS1 = singletonList(LocalDate.parse(NON_SITTING_DAY));
    private static final String NON_DEFAULT_DAY = "2018-06-01T11:00:00Z";
    private static final String NON_DEFAULT_DAY_PM = "2018-06-03T15:00:00Z";
    private static final int INITIAL_ESTIMATE_MINUTES = 640;
    private static final int UPDATED_ESTIMATE_MINUTES = 720;
    private static final String ESTIMATED_DURATION = "1 week";
    private static final String DEFENCE_ORGANISATION_NAME = "XYZ Organisation";
    private static final UUID DEFENCE_ORGANISATION_ID = randomUUID();
    private static final String URN = "urn";
    private static final UUID JUDGE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID COURT_ROOM_ID_1 = randomUUID();
    private static final String STATEMENT_OF_OFFENCE_TITLE = "title";
    private static final String STATEMENT_OF_OFFENCE_LEGISLATION = "Legislation";
    private static final String START_TIME = "10:30";
    private static final String CUSTODY_TIME_LIMIT = "2017-10-05";
    private static final String ADDRESS_LINE_2 = "Address line 1";
    private static final String ADDRESS_LINE_3 = "Address line 1";
    private static final String ADDRESS_LINE_4 = "Address line 1";
    private static final String POSTCODE = "Postcode";
    private static final String PERSON_TITLE = "Doctor";
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
            .withId(fromString("6e1bef55-7e13-4615-b3ba-8663f4438e16"))
            .withDescription("Trial")
            .build();
    private static final List NON_SITTING_DAYS = Collections.EMPTY_LIST;
    private static final List NON_DEFAULT_DAYS = Collections.EMPTY_LIST;
    private static final String EARLIEST_START_TIME = "2012-12-12T01:02:33Z";
    private static final String LISTED_START_TIME = "2020-07-01T10:00:00Z";
    private static final String BOOKING_TYPE = "Video";
    private static final String PRIORITY = "High";
    private static final List<String> SPECIAL_REQUIREMENTS = Arrays.asList("RVC", "GSN");

    private static final UUID JUDICIAL_ID_1 = randomUUID();
    private static final UUID JUDICIAL_ID_2 = randomUUID();
    private static final UUID USER_ID_1 = randomUUID();
    private static final UUID USER_ID_2 = randomUUID();
    private static final String JUDICIAL_ROLE_TYPE = "MAGISTRATE";
    private static final Boolean IS_DEPUTY = false;
    private static final Boolean IS_BENCH_CHAIRMAN = true;
    private static final UUID AUTHORITY_ID = randomUUID();
    private static final String HEARING_LANGUAGE = "ENGLISH";
    private static final String DEFAULT_DURATION = "6";
    private static final String DEFAULT_START_TIME = "10:30";
    private static final String SLOT_START_TIME = "2020-06-23T01:02:33Z";
    private static final Integer SLOT_DURATION = 25;
    private static final String SLOT_SCHEDULE_ID = randomUUID().toString();
    private static final String SLOT_SESSION = "AM";
    private static final String SLOT_SESSION_PM = "PM";
    private static final Integer SLOT_COURT_ROOM_ID = 123498;
    private static final String SLOT_OUCODE = "RASDFG";
    private static final String PROVISIONAL_SCHEDULE_ID = randomUUID().toString();
    private static final String PROVISIONAL_SESSION = "AM";
    private static final Integer PROVISIONAL_COURT_ROOM = 178498;
    private static final String PROVISIONAL_OUCODE = "WERDFG";
    private static final String PROVISIONAL_START_TIME = "2020-07-01";
    private static final String PROVISIONAL_SESSION_DATE = "2020-07-01";
    private static final String SEQUENCE_1 = "1";
    private static final String SEQUENCE_2 = "2";
    private static final String HEARING_DATE_1 = "2012-12-11";
    private static final String HEARING_DATE_2 = "2012-12-12";
    private static final Boolean HAS_VIDEO_LINK = true;
    private static final String PUBLIC_LIST_NOTE = "Public List Note";
    private static final UUID COURT_APPLICATION_ID = randomUUID();
    private static final String COURT_APPLICATION_TYPE = STRING.next();
    private static final UUID COURT_CENTRE_ID_ONE = UUID.fromString("89592405-c29b-3706-b1d3-b1dd3a08b227");
    private static final UUID COURT_CENTRE_ID_TWO = UUID.fromString("44497da7-ec8d-3137-94ad-ff7c0c57827a");
    private static final UUID REASON = randomUUID();

    private static final String HEARING_ID = "hearingId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String FIELD_APPLICATION_ID = "applicationId";
    private static final String REMOVAL_REASON = "removalReason";
    public static final String LINK_ACTION_TYPE = "LINK";
    private static final UUID COURT_SCHEDULE_ID_1 = randomUUID();
    private static final UUID COURT_SCHEDULE_ID_2 = randomUUID();
    private static final String PANEL = "ADULT";

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
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private Stream<Object> events;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private Clock clock = new StoppedClock(parse("2018-01-02T13:04:05+00:00[Europe/London]"));

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @InjectMocks
    ExtendHearingHelper extendHearingHelper;
    @InjectMocks
    @Spy
    private ListingCommandHandler listingCommandHandler;
    @Spy
    private final CommandToDomainConverter commandToDomainConverter = new CommandToDomainConverter();
    @Spy
    private final CourtsDefendantToDomainConverter defendantUpdatedToDomainConverter = new CourtsDefendantToDomainConverter();
    @Spy
    private CourtsOffenceToDomainOffence courtsOffenceToDomainOffence;
    @Spy
    private final CommandDefendantToDomainConverter commandDefendantToDomainConverter = new CommandDefendantToDomainConverter();
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
    @Spy
    private CaseMarkersToDomainConverter caseMarkersToDomainConverter;
    @Spy
    private CasesToDomainConverter casesToDomainConverter;
    @Spy
    private ProsecutionCasesBuilder prosecutionCasesBuilder;
    @Spy
    private ProsecutionCaseDefendantOffenceIdsBuilder prosecutionCaseDefendantOffenceIdsBuilder;
    @Spy
    private ExtendHearingUtils extendHearingUtils;
    @Spy
    private RotaSlotToNonDefaultDayConverter slotToNonDefaultDayConverter;
    @Spy
    private HearingDaysToDomainConverter hearingDaysToDomainConverter;

    private final boolean hasCustodyTimeLimit = true;
    @Mock
    private Hearing hearing;
    @Mock
    private Case aCase;
    @Mock
    private Application anApplication;
    @Mock
    private ProvisionalBookingService provisionalBookingService;

    @Mock
    private HearingTypeFactory hearingTypeFactory;
    @Mock
    private HearingFactory hearingFactory;
    @Mock
    private PublishCourtListRequestAggregate publishCourtListRequestAggregate;
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
    @Captor
    private ArgumentCaptor<List<uk.gov.justice.listing.events.HearingDay>> cancelHearingDaysCaptor;

    private UUID LAA_STATUS_ID;
    @Mock
    private JsonEnvelope jsonEnvelopeMock;
    @Mock
    private UUIDService uuidService;
    @Mock
    private NonDefaultDayDurationBuilder nonDefaultDayDurationBuilder;
    @Mock
    private CourtCentreFactory courtCentreFactory;
    @Mock
    private RotaSLServiceAdapter rotaSLServiceAdapter;
    @Mock
    private StagingHmiService stagingHmiService;
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtListExportRequested.class,
            HearingCounselModified.class);

    private final static LocalDate SUNDAY_25TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 25);
    private final static LocalDate MONDAY_26TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 26);
    private final static LocalDate TUESDAY_27TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 27);
    private final static LocalDate WEDNESDAY_28TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 28);
    private final static LocalDate THURSDAY_29TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 29);
    private final static LocalDate FRIDAY_30TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 30);
    private final static LocalDate SATURDAY_1ST_DECEMBER_2018 = LocalDate.of(2018, Month.DECEMBER, 1);
    private final static LocalDate MONDAY_3rd_DECEMBER_2018 = LocalDate.of(2018, Month.DECEMBER, 3);
    private final static Optional<String> APPLICATION_PARTICULARS = of("Application particulars");

    @Before
    public void setup() {
        fixClock(Instant.now());
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
                PublishCourtListExportFailed.class, PublishCourtListExportSuccessful.class, RecordCourtListProduced.class,
                PublishedCourtListStored.class, TrialVacated.class, HearingRescheduled.class, PublicListNoteChangedForHearing.class,
                CancelHearingDays.class, VideoLinkDetailsChangedForHearing.class, VideoLinkDetailsRemovedForHearing.class, VideoLinkDetailsAssignedForHearing.class, CancelHearingDays.class, CaseIdentifierUpdated.class, CasesAddedToHearing.class);

        setField(this.extendHearingUtils, "prosecutionCasesBuilder", prosecutionCasesBuilder);

        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        givenEventStream(eventStream, aCase, Case.class);
        givenEventStream(eventStream, hearing, Hearing.class);
        givenEventStream(eventStream, anApplication, Application.class);

    }

    @Test
    public void listingCommandHandlerShouldListHearing() throws Exception {
        final JsonEnvelope commandEnvelope = listCourtHearingCommandEnvelope();

        final LocalDate endDate = null;

        final List<ListedCase> listedCases = Arrays.asList(createdListedCase());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();

        final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(Integer.valueOf(DEFAULT_DURATION))
                .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                .withCourtCentreId(COURT_CENTRE_ID)
                .build();

        final List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplication());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());

        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));
        when(hearing.list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES),eq(ESTIMATED_DURATION), eq(listedCases), eq(COURT_CENTRE_ID), eq(judicialRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30), eq(empty()),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE)), eq(of(WEEK_COMMENCING_DURATION)), eq(NON_DEFAULT_DAYS), eq(false), eq(BOOKING_TYPE), eq(PRIORITY), eq(SPECIAL_REQUIREMENTS))).thenReturn(events);
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());

        listingCommandHandler.listCourtHearing(commandEnvelope);

        verify(hearing).list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES),eq(ESTIMATED_DURATION), eq(listedCases), eq(COURT_CENTRE_ID), eq(judicialRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30), eq(empty()),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE)), eq(of(WEEK_COMMENCING_DURATION)), eq(NON_DEFAULT_DAYS), eq(false), eq(BOOKING_TYPE), eq(PRIORITY), eq(SPECIAL_REQUIREMENTS));

    }

    @Test
    public void shouldPopulateNonDefaultDaysWhenBookedSlotsExist() throws Exception {
        final JsonEnvelope commandEnvelope = createListCourtHearingCommandEnvelopeWithBookSlot();

        final LocalDate endDate = null;

        final List<ListedCase> listedCases = Arrays.asList(createdListedCase());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();

        final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(Integer.valueOf(DEFAULT_DURATION))
                .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                .withCourtCentreId(COURT_CENTRE_ID)
                .build();

        final List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplication());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());

        final List<NonDefaultDay> nonDefaultDays = Arrays.asList(NonDefaultDay.nonDefaultDay()
                .withStartTime(parse(SLOT_START_TIME).withZoneSameInstant(ZoneId.of("UTC")))
                .withDuration(of(SLOT_DURATION))
                .withCourtRoomId(of(SLOT_COURT_ROOM_ID))
                .withOucode(of(SLOT_OUCODE))
                .withCourtScheduleId(of(SLOT_SCHEDULE_ID))
                .withSession(of(SLOT_SESSION))
                .withCourtCentreId(of(COURT_CENTRE_ID.toString()))
                .withRoomId(of(COURT_ROOM_ID.toString()))
                .build());


        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());
        when(hearing.list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES), eq(ESTIMATED_DURATION), eq(listedCases), eq(COURT_CENTRE_ID), eq(judicialRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30), eq(empty()),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE)), eq(of(WEEK_COMMENCING_DURATION)), eq(nonDefaultDays), eq(true), eq(BOOKING_TYPE), eq(PRIORITY), eq(SPECIAL_REQUIREMENTS))).thenReturn(events);

        listingCommandHandler.listCourtHearing(commandEnvelope);

        verify(hearing).list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES),eq(ESTIMATED_DURATION), eq(listedCases), eq(COURT_CENTRE_ID), eq(judicialRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30), eq(empty()),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE)), eq(of(WEEK_COMMENCING_DURATION)), eq(nonDefaultDays), eq(true), eq(BOOKING_TYPE), eq(PRIORITY), eq(SPECIAL_REQUIREMENTS));

    }

    @Test
    public void shouldPopulateNonDefaultDaysWhenAdjourningDateAndBookingReferenceAreExists() throws Exception {
        final JsonEnvelope commandEnvelope = createListCourtHearingCommandEnvelopeWithAdjournment();

        final LocalDate endDate = null;

        final List<ListedCase> listedCases = Arrays.asList(createdListedCase());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();

        final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(Integer.valueOf(DEFAULT_DURATION))
                .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                .withCourtCentreId(COURT_CENTRE_ID)
                .build();

        final List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplication());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());

        final List<NonDefaultDay> nonDefaultDays = Arrays.asList(NonDefaultDay.nonDefaultDay()
                .withStartTime(parse(LISTED_START_TIME).withZoneSameInstant(ZoneId.of("UTC")))
                .withDuration(of(INITIAL_ESTIMATE_MINUTES))
                .withCourtRoomId(of(PROVISIONAL_COURT_ROOM))
                .withOucode(of(PROVISIONAL_OUCODE))
                .withCourtScheduleId(of(PROVISIONAL_SCHEDULE_ID))
                .withSession(of(PROVISIONAL_SESSION))
                .withCourtCentreId(of(COURT_CENTRE_ID).map(UUID::toString))
                .withRoomId(of(COURT_ROOM_ID).map(UUID::toString))
                .build());

        final JsonEnvelope responseEnvelope = createEnvelope(".", getPayloadOfMultipleCrownCourtCentres());
        when(referenceDataService.getAllCourtRooms(commandEnvelope)).thenReturn(responseEnvelope);
        when(provisionalBookingService.getSlots(any())).thenReturn(Response.ok(createResponseForProvisonalBookSlot()).build());
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());
        when(hearing.list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES), eq(ESTIMATED_DURATION),eq(listedCases), eq(COURT_CENTRE_ID), eq(judicialRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30), eq(empty()),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE)), eq(of(WEEK_COMMENCING_DURATION)), eq(nonDefaultDays), eq(true), eq(BOOKING_TYPE), eq(PRIORITY), eq(SPECIAL_REQUIREMENTS))).thenReturn(events);

        listingCommandHandler.listCourtHearing(commandEnvelope);

        verify(hearing).list(eq(HEARING_ID_1), eq(HEARING_TYPE), eq(INITIAL_ESTIMATE_MINUTES),eq(ESTIMATED_DURATION), eq(listedCases), eq(COURT_CENTRE_ID), eq(judicialRoles),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE), eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(LISTED_START_TIME)), eq(endDate), eq(courtCentreDefaults), eq(courtApplications), eq(courtApplicationPartyListingNeeds), eq(30), eq(empty()),
                eq(empty()), eq(empty()), eq(empty()), eq(nonDefaultDays), eq(false), eq(BOOKING_TYPE), eq(PRIORITY), eq(SPECIAL_REQUIREMENTS));

    }

    private JsonObject createResponseForProvisonalBookSlot() {
        final String jsonProvisionalSlotString = FileUtil.givenPayload("/test-data/listing.command.provisional-slot-response.json").toString()
                .replace("PROVISIONAL_SCHEDULE_ID", PROVISIONAL_SCHEDULE_ID)
                .replace("PROVISIONAL_OUCODE", PROVISIONAL_OUCODE)
                .replace("PROVISIONAL_SESSION", PROVISIONAL_SESSION)
                .replace("PROVISIONAL_COURT_ROOM", String.valueOf(PROVISIONAL_COURT_ROOM))
                .replace("PROVISIONAL_START_TIME", PROVISIONAL_START_TIME)
                .replace("PROVISIONAL_SESS_DATE", PROVISIONAL_SESSION_DATE)
                .replace("PROVISIONAL_COURT_HOUSE_ID", COURT_CENTRE_ID.toString())
                .replace("PROVISIONAL_ROOM_ID", COURT_ROOM_ID.toString());
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonProvisionalSlotString));
        return jsonReader.readObject();
    }

    private CourtApplication getCourtApplication() {
        return CourtApplication.courtApplication()
                .withLinkedCaseIds(singletonList(fromString("19e9d562-6abb-4871-bfb3-2d777aa90371")))
                .withParentApplicationId(fromString("9d9a431a-0f12-4386-878a-2bf6c4a0877e"))
                .withApplicationType("App Type")
                .withId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withIsRespondent(false)
                        .withId(fromString("22b1078b-9430-4cef-ba46-eea40a129ca8"))
                        .withFirstName("Fred")
                        .withLastName("Perry")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(getAddress())
                        .build())
                .withRespondents(Collections.singletonList(ApplicantRespondent.applicantRespondent()
                        .withIsRespondent(true)
                        .withId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withFirstName("Dan")
                        .withLastName("Brown")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(getAddress())
                        .build()))
                .withApplicationReference(Optional.of("REF-1"))
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withOffences(emptyList())
                .build();
    }

    private Address getAddress() {
        return Address
                .address()
                .withAddress1("Address1")
                .withAddress2(of("Address2"))
                .withAddress3(of("Address3"))
                .withAddress4(of("Address4"))
                .withAddress5(of("Address5"))
                .withPostcode(of("SW13 0AA"))
                .build();
    }

    @Test
    public void listingCommandHandlerShouldUpdateJudiciaryForHearings() throws Exception {
        final JsonEnvelope commandEnvelope = changeJudiciaryForHearingsCommandEnvelope();
        when(hearing.assignJudiciary(any(), eq(HEARING_ID_1))).thenReturn(events);
        when(hearing.assignJudiciary(any(), eq(HEARING_ID_2))).thenReturn(events);
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(events);

        listingCommandHandler.changeJudiciaryForHearings(commandEnvelope);

        verify(hearing, atLeast(2)).assignJudiciary(judicialRoleCaptor.capture(), hearingIdCaptor.capture());
        verify(hearing, times(2)).applyAllocationRules(Collections.emptyList());
        verify(hearing, times(2)).raiseUpdateHearingInStagingHmi(any(Stream.class));

        final List<List<JudicialRole>> judicialRoleArguments = judicialRoleCaptor.getAllValues();

        assertThat(judicialRoleArguments.get(0).get(0).getJudicialId(), equalTo(JUDICIAL_ID_1));
        assertThat(judicialRoleArguments.get(0).get(1).getJudicialId(), equalTo(JUDICIAL_ID_2));
        assertThat(judicialRoleArguments.get(1).get(0).getJudicialId(), equalTo(JUDICIAL_ID_1));
        assertThat(judicialRoleArguments.get(1).get(1).getJudicialId(), equalTo(JUDICIAL_ID_2));
        assertThat(judicialRoleArguments.get(0).get(0).getUserId(), equalTo(USER_ID_1));
        assertThat(judicialRoleArguments.get(0).get(1).getUserId(), equalTo(USER_ID_2));
        assertThat(judicialRoleArguments.get(1).get(0).getUserId(), equalTo(USER_ID_1));
        assertThat(judicialRoleArguments.get(1).get(1).getUserId(), equalTo(USER_ID_2));

        final List<UUID> hearingIdArguments = hearingIdCaptor.getAllValues();

        assertThat(hearingIdArguments.get(0), equalTo(HEARING_ID_1));
        assertThat(hearingIdArguments.get(1), equalTo(HEARING_ID_2));
    }


    @Test
    public void listingCommandHandlerShouldUpdateHearingForListing() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingCommandEnvelope();
        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build();

        final List<NonDefaultDay> nonDefaultDays = Stream.of(NonDefaultDay.nonDefaultDay()
                        .withStartTime(parse(NON_DEFAULT_DAY).withZoneSameInstant(ZoneId.of("UTC")))
                        .withDuration(of(SLOT_DURATION))
                        .withCourtScheduleId(of(COURT_SCHEDULE_ID_1).map(UUID::toString))
                        .withCourtRoomId(of(SLOT_COURT_ROOM_ID))
                        .withOucode(of(SLOT_OUCODE))
                        .withSession(of(SLOT_SESSION))
                        .withCourtCentreId(of(COURT_CENTRE_ID).map(UUID::toString))
                        .withRoomId(of(COURT_ROOM_ID).map(UUID::toString))
                        .build(),
                NonDefaultDay.nonDefaultDay()
                        .withStartTime(parse(NON_DEFAULT_DAY_PM).withZoneSameInstant(ZoneId.of("UTC")))
                        .withDuration(of(SLOT_DURATION))
                        .withCourtScheduleId(of(COURT_SCHEDULE_ID_2).map(UUID::toString))
                        .withCourtRoomId(of(SLOT_COURT_ROOM_ID))
                        .withOucode(of(SLOT_OUCODE))
                        .withSession(of(SLOT_SESSION_PM))
                        .withCourtCentreId(of(COURT_CENTRE_ID).map(UUID::toString))
                        .withRoomId(of(COURT_ROOM_ID_1).map(UUID::toString))
                        .build()).collect(toList());

        final List<JudicialRole> judicialRoles = Arrays.asList(JudicialRole.judicialRole()
                .withIsBenchChairman(of(IS_BENCH_CHAIRMAN))
                .withIsDeputy(of(IS_DEPUTY))
                .withJudicialId(JUDICIAL_ID_1)
                .withJudicialRoleType(
                        JudicialRoleType.judicialRoleType()
                                .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                                .build())
                .build());


        when(hearing.changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, empty())).thenReturn(mock(Stream.class));
        when(hearing.changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonDefaultDays(nonDefaultDays, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeStartDate(START_DATE, HEARING_ID_1)).thenReturn(Stream.of());
        when(hearing.changeType(HEARING_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignJudiciary(judicialRoles, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre)).thenReturn(mock(Stream.class));
        when(hearing.applyRescheduledCheck(any())).thenReturn(mock(Stream.class));
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), Integer.valueOf(DEFAULT_DURATION)));
        when(hearing.removeWeekCommencingDates(HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearingFactory.getHearingById(any(), any())).thenReturn(getSampleStoredHearing());
        when(hearing.assignPublicListNote(PUBLIC_LIST_NOTE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignVideoLink(HAS_VIDEO_LINK, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());
        listingCommandHandler.updateHearingForListing(commandEnvelope);

        verify(hearing).changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1);
        verify(hearing).assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, empty());
        verify(hearing).changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1);
        verify(hearing).assignNonDefaultDays(nonDefaultDays, HEARING_ID_1);
        verify(hearing).assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1);
        verify(hearing).changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1);
        verify(hearing).changeStartDate(START_DATE, HEARING_ID_1);
        verify(hearing).changeType(HEARING_TYPE, HEARING_ID_1);
        verify(hearing).changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1);
        verify(hearing).assignJudiciary(judicialRoles, HEARING_ID_1);
        verify(hearing).applyRescheduledCheck(any());
        verify(hearing).assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS1, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre);
        verify(hearing).removeWeekCommencingDates(HEARING_ID_1);
        verify(hearing).assignPublicListNote(PUBLIC_LIST_NOTE, HEARING_ID_1);
        verify(hearing).assignVideoLink(HAS_VIDEO_LINK, HEARING_ID_1);
        verify(hearing).updateHmiFields(HEARING_ID_1, "Video", "High", Arrays.asList("RVC", "GSN"));
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Optional.class));
    }

    @Test
    public void listingCommandHandlerShouldUpdateHearingForListingWithoutJudiciariesOnMagistrates() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingWithoutJudiciariesCommandEnvelope();
        final UpdateHearingForListingEnriched updateHearingForListingEnriched = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), UpdateHearingForListingEnriched.class);
        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build();

        final List<NonDefaultDay> nonDefaultDays = Stream.of(NonDefaultDay.nonDefaultDay()
                        .withStartTime(parse(NON_DEFAULT_DAY).withZoneSameInstant(ZoneId.of("UTC")))
                        .withDuration(of(SLOT_DURATION))
                        .withCourtScheduleId(of(COURT_SCHEDULE_ID_1).map(UUID::toString))
                        .withCourtRoomId(of(SLOT_COURT_ROOM_ID))
                        .withOucode(of(SLOT_OUCODE))
                        .withSession(of(SLOT_SESSION))
                        .withCourtCentreId(of(COURT_CENTRE_ID).map(UUID::toString))
                        .withRoomId(of(COURT_ROOM_ID).map(UUID::toString))
                        .build(),
                NonDefaultDay.nonDefaultDay()
                        .withStartTime(parse(NON_DEFAULT_DAY_PM).withZoneSameInstant(ZoneId.of("UTC")))
                        .withDuration(of(SLOT_DURATION))
                        .withCourtScheduleId(of(COURT_SCHEDULE_ID_2).map(UUID::toString))
                        .withCourtRoomId(of(SLOT_COURT_ROOM_ID))
                        .withOucode(of(SLOT_OUCODE))
                        .withSession(of(SLOT_SESSION_PM))
                        .withCourtCentreId(of(COURT_CENTRE_ID).map(UUID::toString))
                        .withRoomId(of(COURT_ROOM_ID_1).map(UUID::toString))
                        .build()).collect(toList());

        final JsonObject hearingSlotsResponse = givenPayload("/stub-data/azure.rotasl.getHearingSlots.stub-data.json");

        final List<JudicialRole> judicialRoles = new ArrayList<>();
        ((JsonObject) hearingSlotsResponse
                .getJsonArray("hearingSlots").get(0))
                .getJsonArray("judiciaries")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(judiciaryJsonObject ->
                        judicialRoles.add(JudicialRole.judicialRole()
                                .withIsBenchChairman(of(judiciaryJsonObject.getBoolean("benchChairman")))
                                .withIsDeputy(of(judiciaryJsonObject.getBoolean("deputy")))
                                .withJudicialId(UUID.fromString(judiciaryJsonObject.getString("judiciaryId")))
                                .withJudicialRoleType(
                                        JudicialRoleType.judicialRoleType()
                                                .withJudiciaryType(judiciaryJsonObject.getString("judiciaryType"))
                                                .build())
                                .build())
                );


        when(rotaSLServiceAdapter.getJudicialRoles(anyString(), anyString(), any(), anyString())).thenReturn(judicialRoles);
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());
        when(nonDefaultDayDurationBuilder.buildNewUpdateHearingForListingWithNewNonDefaultDays(any(), any())).thenReturn(updateHearingForListingEnriched.getUpdateHearingForListing());
        when(hearing.changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, empty())).thenReturn(mock(Stream.class));
        when(hearing.changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonDefaultDays(nonDefaultDays, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeStartDate(START_DATE, HEARING_ID_1)).thenReturn(Stream.of());
        when(hearing.changeType(HEARING_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeJurisdictionType(JurisdictionType.MAGISTRATES, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignJudiciary(judicialRoles, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre)).thenReturn(mock(Stream.class));
        when(hearing.applyRescheduledCheck(any())).thenReturn(mock(Stream.class));
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), Integer.valueOf(DEFAULT_DURATION)));
        when(hearing.removeWeekCommencingDates(HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearingFactory.getHearingById(any(), any())).thenReturn(getSampleStoredHearing());
        when(hearing.assignPublicListNote(PUBLIC_LIST_NOTE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignVideoLink(HAS_VIDEO_LINK, HEARING_ID_1)).thenReturn(mock(Stream.class));

        when(rotaSLServiceAdapter.getPanelInfo(any(), any(LocalDate.class), any(LocalDate.class), any(UUID.class), anyString())).thenReturn(Optional.of("YOUTH"));

        listingCommandHandler.updateHearingForListing(commandEnvelope);

        verify(hearing).changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1);
        verify(hearing).assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, of("YOUTH"));
        verify(hearing).changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1);
        verify(hearing).assignNonDefaultDays(nonDefaultDays, HEARING_ID_1);
        verify(hearing).assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1);
        verify(hearing).changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1);
        verify(hearing).changeStartDate(START_DATE, HEARING_ID_1);
        verify(hearing).changeType(HEARING_TYPE, HEARING_ID_1);
        verify(hearing).changeJurisdictionType(JurisdictionType.MAGISTRATES, HEARING_ID_1);
        verify(hearing).assignJudiciary(judicialRoles, HEARING_ID_1);
        verify(hearing).applyRescheduledCheck(any());
        verify(hearing).assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS1, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre);
        verify(hearing).removeWeekCommencingDates(HEARING_ID_1);
        verify(hearing).assignPublicListNote(PUBLIC_LIST_NOTE, HEARING_ID_1);
        verify(hearing).assignVideoLink(HAS_VIDEO_LINK, HEARING_ID_1);
        verify(courtCentreFactory).getOrganisationUnit(COURT_CENTRE_ID, commandEnvelope);
        verify(rotaSLServiceAdapter).getJudicialRoles(anyString(), anyString(), any(), anyString());
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Optional.class));
    }

    @Test
    public void listingCommandHandlerShouldUpdateHearingForListingWithoutJudiciariesOnMagistratesAndShouldNotCallRotaForHmiEnabled() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingWithoutJudiciariesCommandEnvelope();
        final UpdateHearingForListingEnriched updateHearingForListingEnriched = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), UpdateHearingForListingEnriched.class);
        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build();

        final List<NonDefaultDay> nonDefaultDays = Stream.of(NonDefaultDay.nonDefaultDay()
                        .withStartTime(parse(NON_DEFAULT_DAY).withZoneSameInstant(ZoneId.of("UTC")))
                        .withDuration(of(SLOT_DURATION))
                        .withCourtScheduleId(of(COURT_SCHEDULE_ID_1).map(UUID::toString))
                        .withCourtRoomId(of(SLOT_COURT_ROOM_ID))
                        .withOucode(of(SLOT_OUCODE))
                        .withSession(of(SLOT_SESSION))
                        .withCourtCentreId(of(COURT_CENTRE_ID).map(UUID::toString))
                        .withRoomId(of(COURT_ROOM_ID).map(UUID::toString))
                        .build(),
                NonDefaultDay.nonDefaultDay()
                        .withStartTime(parse(NON_DEFAULT_DAY_PM).withZoneSameInstant(ZoneId.of("UTC")))
                        .withDuration(of(SLOT_DURATION))
                        .withCourtScheduleId(of(COURT_SCHEDULE_ID_2).map(UUID::toString))
                        .withCourtRoomId(of(SLOT_COURT_ROOM_ID))
                        .withOucode(of(SLOT_OUCODE))
                        .withSession(of(SLOT_SESSION_PM))
                        .withCourtCentreId(of(COURT_CENTRE_ID).map(UUID::toString))
                        .withRoomId(of(COURT_ROOM_ID_1).map(UUID::toString))
                        .build()).collect(toList());

        final JsonObject hearingSlotsResponse = givenPayload("/stub-data/azure.rotasl.getHearingSlots.stub-data.json");

        final List<JudicialRole> judicialRoles = new ArrayList<>();
        ((JsonObject) hearingSlotsResponse
                .getJsonArray("hearingSlots").get(0))
                .getJsonArray("judiciaries")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(judiciaryJsonObject ->
                        judicialRoles.add(JudicialRole.judicialRole()
                                .withIsBenchChairman(of(judiciaryJsonObject.getBoolean("benchChairman")))
                                .withIsDeputy(of(judiciaryJsonObject.getBoolean("deputy")))
                                .withJudicialId(UUID.fromString(judiciaryJsonObject.getString("judiciaryId")))
                                .withJudicialRoleType(
                                        JudicialRoleType.judicialRoleType()
                                                .withJudiciaryType(judiciaryJsonObject.getString("judiciaryType"))
                                                .build())
                                .build())
                );


        when(rotaSLServiceAdapter.getJudicialRoles(anyString(), anyString(), any(), anyString())).thenReturn(judicialRoles);
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());
        when(nonDefaultDayDurationBuilder.buildNewUpdateHearingForListingWithNewNonDefaultDays(any(), any())).thenReturn(updateHearingForListingEnriched.getUpdateHearingForListing());
        when(hearing.changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, empty())).thenReturn(mock(Stream.class));
        when(hearing.changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonDefaultDays(nonDefaultDays, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeStartDate(START_DATE, HEARING_ID_1)).thenReturn(Stream.of());
        when(hearing.changeType(HEARING_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeJurisdictionType(JurisdictionType.MAGISTRATES, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignJudiciary(judicialRoles, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre)).thenReturn(mock(Stream.class));
        when(hearing.applyRescheduledCheck(any())).thenReturn(mock(Stream.class));
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), Integer.valueOf(DEFAULT_DURATION)));
        when(hearing.removeWeekCommencingDates(HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearingFactory.getHearingById(any(), any())).thenReturn(getSampleStoredHearing());
        when(hearing.assignPublicListNote(PUBLIC_LIST_NOTE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignVideoLink(HAS_VIDEO_LINK, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(stagingHmiService.isHmiListingEnabled(any(Optional.class))).thenReturn(true);

        listingCommandHandler.updateHearingForListing(commandEnvelope);

        verify(hearing).changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1);
        verify(hearing).changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1);
        verify(hearing).assignNonDefaultDays(nonDefaultDays, HEARING_ID_1);
        verify(hearing).assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1);
        verify(hearing).changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1);
        verify(hearing).changeStartDate(START_DATE, HEARING_ID_1);
        verify(hearing).changeType(HEARING_TYPE, HEARING_ID_1);
        verify(hearing).changeJurisdictionType(JurisdictionType.MAGISTRATES, HEARING_ID_1);
        verify(hearing).applyRescheduledCheck(any());
        verify(hearing).assignHearingDays(START_DATE, LocalDate.parse(END_DATE), NON_SITTING_DAYS1, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre);
        verify(hearing).removeWeekCommencingDates(HEARING_ID_1);
        verify(hearing).assignPublicListNote(PUBLIC_LIST_NOTE, HEARING_ID_1);
        verify(hearing).assignVideoLink(HAS_VIDEO_LINK, HEARING_ID_1);
        verify(courtCentreFactory).getOrganisationUnit(COURT_CENTRE_ID, commandEnvelope);
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Optional.class));
        verify(rotaSLServiceAdapter, never()).getPanelInfo(any(Optional.class), any(LocalDate.class), any(LocalDate.class), any(UUID.class), anyString());
        verify(rotaSLServiceAdapter, never()).getJudicialRoles(anyString(), anyString(), any(), anyString());
    }

    @Test
    public void listingCommandHandlerShouldUpdateHearingForListingForWeekCommencingDate() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingwithWeekCommencingCommandEnvelope();
        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build();

        final List<NonDefaultDay> nonDefaultDays = singletonList(NonDefaultDay.nonDefaultDay()
                .withStartTime(parse(NON_DEFAULT_DAY).withZoneSameInstant(ZoneId.of("UTC")))
                .withDuration(of(1))
                .withCourtScheduleId(empty())
                .withCourtRoomId(empty())
                .withOucode(empty())
                .withSession(empty())
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


        when(hearing.changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, empty())).thenReturn(mock(Stream.class));
        when(hearing.changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonDefaultDays(nonDefaultDays, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeEndDate(LocalDate.parse(END_DATE), HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.removeStartDate(HEARING_ID_1, false)).thenReturn(Stream.of());
        when(hearing.removeEndDate(HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeType(HEARING_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, of(PANEL))).thenReturn(mock(Stream.class));
        when(hearing.assignJudiciary(judicialRoles, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearing.assignHearingDays(null, null, NON_SITTING_DAYS, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre)).thenReturn(mock(Stream.class));
        when(hearing.changeWeekCommencingDate(WEEK_COMMENCING_START_DATE, WEEK_COMMENCING_END_DATE, WEEK_COMMENCING_DURATION, HEARING_ID_1)).thenReturn(mock(Stream.class));
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), Integer.valueOf(DEFAULT_DURATION)));
        when(hearingFactory.getHearingById(any(), any())).thenReturn(getSampleStoredHearing());
        when(hearing.assignPublicListNote(any(), eq(HEARING_ID_1))).thenReturn(mock(Stream.class));
        when(hearing.assignVideoLink(anyBoolean(), eq(HEARING_ID_1))).thenReturn(mock(Stream.class));
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());

        listingCommandHandler.updateHearingForListing(commandEnvelope);

        verify(hearing).changeCourtCentre(COURT_CENTRE_ID, HEARING_ID_1);
        verify(hearing).assignCourtRoom(COURT_ROOM_ID, HEARING_ID_1, empty());
        verify(hearing).changeHearingLanguage(valueFor(HEARING_LANGUAGE).get(), HEARING_ID_1);
        verify(hearing).assignNonDefaultDays(nonDefaultDays, HEARING_ID_1);
        verify(hearing).assignNonSittingDays(NON_SITTING_DAYS1, HEARING_ID_1);
        verify(hearing).removeEndDate(HEARING_ID_1);
        verify(hearing).removeStartDate(HEARING_ID_1, false);
        verify(hearing).changeType(HEARING_TYPE, HEARING_ID_1);
        verify(hearing).changeJurisdictionType(JURISDICTION_TYPE, HEARING_ID_1);
        verify(hearing).assignJudiciary(judicialRoles, HEARING_ID_1);
        verify(hearing).assignHearingDays(null, null, NON_SITTING_DAYS1, nonDefaultDays,
                LocalTime.parse(DEFAULT_START_TIME), Integer.valueOf(DEFAULT_DURATION), HEARING_ID_1, defaultCourtCentre);
        verify(hearing).changeWeekCommencingDate(WEEK_COMMENCING_START_DATE, WEEK_COMMENCING_END_DATE, WEEK_COMMENCING_DURATION, HEARING_ID_1);
        verify(hearing).assignVideoLink(anyBoolean(), eq(HEARING_ID_1));
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Optional.class));
    }


    @Test
    public void shouldUpdateHearingForListingWithDeleteUnallocatedHearingWhenPartialAllocation() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingCommandEnvelope();

        when(hearingFactory.getHearingById(any(), any())).thenReturn(getSampleStoredHearing());
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearing.changeStartDate(START_DATE, HEARING_ID_1)).thenReturn(Stream.of());
        when(hearing.applyRescheduledCheck(any())).thenReturn(mock(Stream.class));
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());

        listingCommandHandler.updateHearingForListing(commandEnvelope);

        verify(hearing).updateUnallocatedHearingPartially(eq(HEARING_ID_1), any());
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Optional.class));

    }

    @Test
    public void shouldUpdateCaseIdentifier() throws Exception {
        final JsonObject payload = Json.createObjectBuilder()
                .add("prosecutionAuthorityId", randomUUID().toString())
                .add("prosecutionAuthorityCode", STRING.next())
                .add("hearingIds", Json.createArrayBuilder().add(randomUUID().toString()).add(randomUUID().toString()).build())
                .build();
        JsonEnvelope commandEnvelope = envelopeFrom(metadataBuilder().withName("listing.command.update-cps-prosecutor-with-associated-hearings").withId(randomUUID()).build(), payload);
        when(hearing.updateCaseIdentifier(any(), any(), any())).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        listingCommandHandler.updateProsecutorForAssociatedHearings(commandEnvelope);

        verify(hearing, times(2)).updateCaseIdentifier(any(), any(), any());
        verify(hearing, times(2)).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    private uk.gov.justice.listing.events.Hearing getSampleStoredHearing() {
        List<uk.gov.justice.listing.events.Offence> persistedOffenceList = new ArrayList<>();
        persistedOffenceList.add(uk.gov.justice.listing.events.Offence.offence().withId(fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")).build());
        persistedOffenceList.add(uk.gov.justice.listing.events.Offence.offence().withId(fromString("4789ab16-0bb7-4ef1-87ef-c936bf0364f2")).build());
        persistedOffenceList.add(uk.gov.justice.listing.events.Offence.offence().withId(fromString("36606d71-a062-4877-ab9a-b434c9e48876")).build());

        return uk.gov.justice.listing.events.Hearing.hearing()
                .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(fromString("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"))
                        .withDefendants(Arrays.asList(uk.gov.justice.listing.events.Defendant.defendant()
                                .withId(fromString("e1d32d9d-29ec-4934-a932-22a50f223966"))
                                .withOffences(persistedOffenceList)
                                .build()))
                        .build()))
                .build();
    }

    @Test
    public void shouldUpdateHearingForListingWithWhenPartialAllocation() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingCommandEnvelope();

        when(hearingFactory.getHearingById(any(), any())).thenReturn(uk.gov.justice.listing.events.Hearing.hearing()
                .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(fromString("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"))
                        .withDefendants(Arrays.asList(uk.gov.justice.listing.events.Defendant.defendant()
                                .withId(fromString("e1d32d9d-29ec-4934-a932-22a50f223966"))
                                .withOffences(Arrays.asList(uk.gov.justice.listing.events.Offence.offence()
                                                .withId(fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                                                .build(),
                                        uk.gov.justice.listing.events.Offence.offence()
                                                .withId(fromString("4789ab16-0bb7-4ef1-87ef-c936bf0364f2"))
                                                .build()))
                                .build()))
                        .build()))
                .build());
        when(hearing.changeStartDate(START_DATE, HEARING_ID_1)).thenReturn(Stream.of());
        when(hearing.applyRescheduledCheck(any())).thenReturn(mock(Stream.class));
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());

        listingCommandHandler.updateHearingForListing(commandEnvelope);
        verify(hearing, never()).updateUnallocatedHearingPartially(eq(HEARING_ID_1), any());
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Optional.class));
    }

    @Test
    public void shouldRaiseAllocatedEventWithWhenPartialAllocation() throws Exception {
        final JsonEnvelope commandEnvelope = updateHearingForListingCommandEnvelope();
        final List<uk.gov.justice.listing.events.Offence> storedOffenceList = new ArrayList<>();
        storedOffenceList.add(uk.gov.justice.listing.events.Offence.offence().withId(fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")).build());
        storedOffenceList.add(uk.gov.justice.listing.events.Offence.offence().withId(fromString("4789ab16-0bb7-4ef1-87ef-c936bf0364f2")).build());
        storedOffenceList.add(uk.gov.justice.listing.events.Offence.offence().withId(fromString("67c0dce9-0b85-4027-a252-a0d4a3825b77")).build());

        when(hearingFactory.getHearingById(any(), any())).thenReturn(uk.gov.justice.listing.events.Hearing.hearing()
                .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(fromString("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"))
                        .withDefendants(Arrays.asList(uk.gov.justice.listing.events.Defendant.defendant()
                                .withId(fromString("e1d32d9d-29ec-4934-a932-22a50f223966"))
                                .withOffences(storedOffenceList)
                                .build()))
                        .build()))
                .build());
        when(hearing.changeStartDate(START_DATE, HEARING_ID_1)).thenReturn(Stream.of());
        when(hearing.applyRescheduledCheck(any())).thenReturn(mock(Stream.class));
        when(courtCentreFactory.getOrganisationUnit(any(), any())).thenReturn(Json.createObjectBuilder().add("oucode", "B06AN00").build());
        when(hearing.updateUnallocatedHearingPartially(any(), any())).thenReturn(Stream.of(new Object()));

        listingCommandHandler.updateHearingForListing(commandEnvelope);
        verify(hearing).updateUnallocatedHearingPartially(eq(HEARING_ID_1), any());
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Optional.class));
        verify(hearing).applyAllocationRules(any(), any());
    }

    @Test
    public void listingCommandHandlerShouldTriggerDefendantsToBeUpdatedEvent() throws Exception {
        final Defendant defendant = createDomainDefendantForUpdateDefendant();
        when(aCase.updateDefendant(eq(CASE_ID), any(Defendant.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = updateCaseDefendantDetailsCommandEnvelope();

        listingCommandHandler.updateCaseDefendantDetails(commandEnvelope);

        verify(aCase).updateDefendant(CASE_ID, defendant);
    }


    @Test
    public void listingCommandHandlerShouldTriggerDefendantsToBeAddedToCourtProceedingsEvent() throws Exception {
        final Defendant defendant = createDomainDefendantForAddDefendantToCourtProceedings();
        when(aCase.addedDefendantForCourtProceedings(eq(CASE_ID), any(Defendant.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = addDefendantsForCourtProceedingsCommandEnvelope();

        listingCommandHandler.addDefendantsToCourtProceedings(commandEnvelope);

        verify(aCase, atLeast(1)).addedDefendantForCourtProceedings(CASE_ID, defendant);
    }

    @Test
    public void listingCommandHandlerShouldTriggerDefendantOffencesUpdatedEvent() throws Exception {
        when(aCase.updateDefendantOffences(any(CaseOffences.class))).thenReturn(mock(Stream.class));
        when(aCase.addedDefendantOffences(any(CaseOffences.class))).thenReturn(mock(Stream.class));
        when(aCase.deleteDefendantOffences(any(CaseSimpleOffences.class))).thenReturn(mock(Stream.class));

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
                "      \"count\": 2,\n" +
                "      \"orderIndex\": 0,\n" +
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
                "      \"count\": 2,\n" +
                "      \"orderIndex\": 1,\n" +
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
                "      \"count\": 2,\n" +
                "      \"orderIndex\": 0,\n" +
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
                "      \"count\": 2,\n" +
                "      \"orderIndex\": 1,\n" +
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
                        "      \"count\": 2,\n" +
                        "      \"orderIndex\": 0,\n" +
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
                        "      \"count\": 2,\n" +
                        "      \"orderIndex\": 0,\n" +
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
                        "      \"count\": 2,\n" +
                        "      \"orderIndex\": 1,\n" +
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
        final List<Defendant> defendants = singletonList(createDomainDefendantForUpdateDefendantsForHearing());
        when(hearing.updateDefendants(eq(CASE_ID), anyObject())).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = updateDefendantsForHearingCommandEnvelope();

        listingCommandHandler.updateDefendantsForHearing(commandEnvelope);

        verify(hearing).updateDefendants(CASE_ID, defendants);
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceUpdatedEvents() throws Exception {
        when(hearing.updateOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(Offence.class))).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = updateOffencesForHearingCommandEnvelope();
        listingCommandHandler.updateOffencesForHearing(commandEnvelope);

        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
        verify(hearing, atMost(1)).updateOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainOffencesCaptor.capture());

        final List<Offence> capturedDomainOffences = domainOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-01\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                        "    \"offenceCode\": \"H8189\",\n" +
                        "    \"startDate\": \"2010-08-01\",\n" +
                        "    \"count\": 1,\n" +
                        "    \"orderIndex\": 0,\n" +
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
                        "    \"count\": 1,\n" +
                        "    \"orderIndex\": 0,\n" +
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

        when(hearing.deleteOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(SimpleOffence.class))).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenAnswer(i -> i.getArguments()[0]);
        final JsonEnvelope commandEnvelope = deleteOffencesForHearingCommandEnvelope();
        listingCommandHandler.deleteOffencesForHearing(commandEnvelope);

        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
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

        final CourtApplication courtApplication = getNewCourtApplication();

        when(hearing.addCourtApplication(HEARING_ID_1, courtApplication)).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        listingCommandHandler.addCourtApplicationForHearing(commandEnvelope);

        verify(hearing).addCourtApplication(HEARING_ID_1, courtApplication);
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));

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
        when(hearing.addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(Offence.class))).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = addOffencesForHearingCommandEnvelope();
        listingCommandHandler.addOffencesForHearing(commandEnvelope);

        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
        verify(hearing, atMost(1)).addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainOffencesCaptor.capture());

        final List<Offence> capturedDomainOffences = domainOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-01\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                        "    \"offenceCode\": \"H8189\",\n" +
                        "    \"startDate\": \"2010-08-01\",\n" +
                        "    \"count\": 2,\n" +
                        "    \"orderIndex\": 0,\n" +
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
                        "    \"count\": 2,\n" +
                        "    \"orderIndex\": 1,\n" +
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
        givenEventStream(eventStream, new Case(), Case.class);

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

        final HearingDay hearingDay1 = HearingDay.hearingDay()
                .withSequence(Integer.valueOf(SEQUENCE_1))
                .withHearingDate(LocalDate.parse(HEARING_DATE_1))
                .build();
        final HearingDay hearingDay2 = HearingDay.hearingDay()
                .withSequence(Integer.valueOf(SEQUENCE_2))
                .withHearingDate(LocalDate.parse(HEARING_DATE_2))
                .build();
        final SequenceHearing sequenceHearing = SequenceHearing.sequenceHearing()
                .withId(HEARING_ID_1)
                .withHearingDays(Arrays.asList(hearingDay1, hearingDay2))
                .build();

        when(hearing.sequenceHearingDays(sequenceHearing)).thenReturn(Stream.of(HearingDaysSequenced.hearingDaysSequenced().build()));
        when(hearing.applyAllocationRules(anyList())).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        listingCommandHandler.sequenceHearings(commandEnvelope);
        verify(hearing).sequenceHearingDays(sequenceHearing);
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void listingCommandHandlerShouldTriggerUpdateCourtApplicationForHearingChange() throws Exception {

        final JsonEnvelope commandEnvelope = updateCourtApplicationForHearingsCommandEnvelope();
        final CourtApplication courtApplication = getCourtApplication();

        when(hearing.updateCourtApplication(any(), any())).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        listingCommandHandler.updateCourtApplicationForHearings(commandEnvelope);

        verify(hearing, times(2)).raiseUpdateHearingInStagingHmi(any(Stream.class));
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
    public void shouldAddApplicationToHearing() throws Exception {
        givenEventStream(eventStream, new Application(), Application.class);

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
    public void shouldUpdateCourtApplication() throws Exception {
        final uk.gov.justice.core.courts.CourtApplication courtApplication = uk.gov.justice.core.courts
                .CourtApplication.courtApplication().withId(APPLICATION_ID).build();
        when(anApplication.update(any(CourtApplication.class))).thenReturn(mock(Stream.class));
        final JsonEnvelope commandEnvelope = updateCourtApplicationCommandEnvelope();

        listingCommandHandler.updateCourtApplication(commandEnvelope);
        verify(anApplication).update(any(CourtApplication.class));

    }

    @Test
    public void shouldUpdateCaseMarkersForCase() throws Exception {
        when(aCase.addedCaseMarkers(eq(CASE_ID), anyListOf(CaseMarker.class))).thenReturn(mock(Stream.class));
        final JsonEnvelope commandEnvelope = addCaseMarkersForListedCaseCommandEnvelope();

        listingCommandHandler.updateCaseMarker(commandEnvelope);

        verify(aCase).addedCaseMarkers((CASE_ID), buildCaseMarkers());
    }

    @Test
    public void shouldHandleUpdateLinkedCases() throws Exception {
        when(aCase.linkCases(eq(LINK_ACTION_TYPE), any(), any(), any())).thenReturn(mock(Stream.class));
        final JsonEnvelope commandEnvelope = linkCaseEnvelope();
        final UpdateLinkedCases command = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), UpdateLinkedCases.class);
        listingCommandHandler.updateLinkedCases(commandEnvelope);
        verify(aCase).linkCases(LINK_ACTION_TYPE, CASE_ID, URN, casesToDomainConverter.convert(command.getCases().get(0)));
    }

    @Test
    public void shouldHandleUpdateLinkedCaseInHearing() throws Exception {
        when(hearing.linkCaseToHearing(eq(LINK_ACTION_TYPE), any(), any(), any())).thenReturn(mock(Stream.class));
        final JsonEnvelope commandEnvelope = linkCaseToHearingEnvelope();
        final UpdateLinkedCaseInHearing command = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), UpdateLinkedCaseInHearing.class);
        listingCommandHandler.updateLinkedCaseInHearing(commandEnvelope);
        verify(hearing).linkCaseToHearing(LINK_ACTION_TYPE, CASE_ID, URN, convertLinkedToCases(command.getLinkedToCases()));
    }

    private List<uk.gov.moj.cpp.listing.domain.LinkedToCases> convertLinkedToCases(final List<LinkedToCases> linkedToCases) {
        return linkedToCases.stream()
                .map(lc -> uk.gov.moj.cpp.listing.domain.LinkedToCases.linkedToCases()
                        .withCaseId(lc.getCaseId())
                        .withCaseUrn(lc.getCaseUrn())
                        .build())
                .collect(toList());
    }

    private List<CaseMarker> buildCaseMarkers() {
        return singletonList(CaseMarker.caseMarker()
                .withId(fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                .withMarkerTypeid(fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                .withMarkerTypeDescription("Prohibited Weapons")
                .withMarkerTypeCode("WP")
                .build());
    }

    @Test
    public void shouldRestrictCaseFromCourtListing() throws Exception {
        final JsonEnvelope commandEnvelope = restrictCourtListCommandEnvelope();
        when(hearing.restrictDetailsFromCourt(eq(HEARING_ID_1), anyObject())).thenReturn(mock(Stream.class));

        final RestrictCourtList restrictCourtList = RestrictCourtList.restrictCourtList()
                .withHearingId(HEARING_ID_1)
                .withCaseIds(Arrays.asList(CASE_ID))
                .withCourtApplicatonIds(Arrays.asList(COURT_APPLICATION_ID))
                .withCourtApplicationType(COURT_APPLICATION_TYPE)
                .build();
        listingCommandHandler.restrictFromCourtList(commandEnvelope);

        verify(hearing).restrictDetailsFromCourt((HEARING_ID_1), restrictCourtList);

    }


    @Test
    public void shouldEjectCaseFromCourtListing() throws Exception {
        final JsonEnvelope commandEnvelope = ejectCaseCommandEnvelope();
        when(aCase.ejectCaseForHearings(eq(Arrays.asList(HEARING_ID_1)), eq(CASE_ID), eq(Optional.of("SomeReason")))).thenReturn(mock(Stream.class));

        listingCommandHandler.ejectCaseOrApplication(commandEnvelope);
        verify(aCase).ejectCaseForHearings((Arrays.asList(HEARING_ID_1)), CASE_ID, Optional.of("SomeReason"));
    }

    @Test
    public void shouldEjectCaseForHearing() throws Exception {

        final JsonObject ejectCasePayload = Json.createObjectBuilder()
                .add(HEARING_ID, HEARING_ID_1.toString())
                .add(PROSECUTION_CASE_ID, CASE_ID.toString())
                .add(REMOVAL_REASON, "SomeReason")
                .build();


        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.eject-case", ejectCasePayload);

        when(hearing.ejectCase(eq(HEARING_ID_1), eq(CASE_ID), eq("SomeReason"))).thenReturn(mock(Stream.class));

        listingCommandHandler.ejectCase(commandEnvelope);

        verify(hearing).ejectCase(HEARING_ID_1, CASE_ID, "SomeReason");

    }

    @Test
    public void shouldEjectApplicationForHearing() throws Exception {

        final JsonObject ejectCasePayload = Json.createObjectBuilder()
                .add(HEARING_ID, HEARING_ID_1.toString())
                .add(FIELD_APPLICATION_ID, APPLICATION_ID.toString())
                .add(REMOVAL_REASON, "SomeReason")
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.eject-application", ejectCasePayload);

        when(hearing.ejectApplication(eq(HEARING_ID_1), eq(APPLICATION_ID), eq("SomeReason"))).thenReturn(mock(Stream.class));

        listingCommandHandler.ejectApplication(commandEnvelope);

        verify(hearing).ejectApplication(HEARING_ID_1, APPLICATION_ID, "SomeReason");

    }

    @Test
    public void shouldEjectApplicationFromCourtListing() throws Exception {
        final JsonEnvelope commandEnvelope = ejectApplicationCommandEnvelope();

        when(anApplication.ejectApplicationForHearings(eq(Arrays.asList(HEARING_ID_1)), eq(COURT_APPLICATION_ID), eq(Optional.of("SomeReason")))).thenReturn(mock(Stream.class));

        listingCommandHandler.ejectCaseOrApplication(commandEnvelope);

        verify(anApplication).ejectApplicationForHearings((Arrays.asList(HEARING_ID_1)), COURT_APPLICATION_ID, Optional.of("SomeReason"));

    }

    @Test
    public void listingCommandHandlerShouldTriggerDefendantsAddedForCourtProceedingsEvents() throws Exception {
        final List<Defendant> defendants = singletonList(createDomainDefendantForUpdateDefendantsForHearing());

        when(hearing.addDefendantsForCourtProceedings(eq(CASE_ID), anyObject())).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = addDefendantsForHearingCommandEnvelope();

        listingCommandHandler.addDefendantsToCourtProceedingsForHearing(commandEnvelope);

        final ArgumentCaptor<UUID> caseIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<List> defendantListCaptor = ArgumentCaptor.forClass(List.class);

        verify(hearing).addDefendantsForCourtProceedings(caseIdCaptor.capture(), defendantListCaptor.capture());
        Assert.assertThat(CASE_ID, Matchers.is(caseIdCaptor.getValue()));
        final List<Defendant> actualDefendantList = defendantListCaptor.getValue();
        Assert.assertThat(actualDefendantList, Matchers.is(defendants));

        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceUpdatedEventsForLaa() throws Exception {
        when(hearing.updateOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(Offence.class))).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));
        final JsonEnvelope commandEnvelope = updateOffencesWithLaaForHearingCommandEnvelope();
        listingCommandHandler.updateOffencesForHearing(commandEnvelope);

        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
        verify(hearing, atMost(1)).updateOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainOffencesCaptor.capture());

        final List<Offence> capturedDomainOffences = domainOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-01\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                        "    \"offenceCode\": \"H8189\",\n" +
                        "    \"startDate\": \"2010-08-01\",\n" +
                        "    \"count\": 2,\n" +
                        "    \"orderIndex\": 0,\n" +
                        "    \"statementOfOffence\": {\n" +
                        "      \"legislation\": \"Welsh legislation\",\n" +
                        "      \"title\": \"Wounding with intent\",\n" +
                        "      \"welshLegislation\": \"legislation\",\n" +
                        "      \"welshTitle\": \"Wounding with intent in Welsh\"\n},\n" +
                        "      \"laaApplnReference\": {" +
                        "        \"applicationReference\": \"APPLICATION_REFERENCE\",\n" +
                        "        \"effectiveEndDate\": \"2010-09-01\",\n" +
                        "        \"effectiveStartDate\": \"2011-09-01\",\n" +
                        "        \"statusCode\": \"STATUS_CODE\",\n" +
                        "        \"statusDate\": \"2010-12-01\",\n" +
                        "        \"statusDescription\": \"STATUS_DESCRIPTION\",\n" +
                        "        \"statusId\": \"" + LAA_STATUS_ID + "\"\n}\n" +
                        "       \n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-20\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID2 + "\",\n" +
                        "    \"offenceCode\": \"H8189X\",\n" +
                        "    \"startDate\": \"2010-08-10\",\n" +
                        "    \"count\": 2,\n" +
                        "    \"orderIndex\": 1,\n" +
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
    public void testUpdateDefendantLegalAidStatus() throws EventStreamException {

        final UUID defendantId = randomUUID();
        when(aCase.updateDefendantLegalAidStatus(CASE_ID, defendantId, "Granted")).thenReturn(mock(Stream.class));

        final JsonObject commandPayload = Json.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("caseId", CASE_ID.toString())
                .add("legalAidStatus", "Granted")
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.command.update-defendant-legalaid-status"), commandPayload);

        listingCommandHandler.updateDefendantLegalAidStatus(envelope);

        verify(aCase).updateDefendantLegalAidStatus(CASE_ID, defendantId, "Granted");
    }

    @Test
    public void testUpdateDefendantLegalAidStatusForHearing() throws EventStreamException {
        final UUID defendantId = randomUUID();
        when(hearing.updateDefendantLegalAidStatusForHearing(HEARING_ID_1, CASE_ID, defendantId, "Granted")).thenReturn(mock(Stream.class));

        final JsonObject commandPayload = Json.createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
                .add("defendantId", defendantId.toString())
                .add("caseId", CASE_ID.toString())
                .add("legalAidStatus", "Granted")
                .build();


        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.command.update-defendant-legalaid-status-for-hearing"), commandPayload);

        listingCommandHandler.updateDefendantLegalAidStatusForHearing(envelope);

        verify(hearing).updateDefendantLegalAidStatusForHearing(HEARING_ID_1, CASE_ID, defendantId, "Granted");
    }

    @Test
    public void shouldUpdateDefendantHearingResultedAndCaseResulted() throws EventStreamException {
        final UUID caseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("CASE_CLOSED")
                .withId(caseId)
                .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant()
                        .withProceedingsConcluded(true)
                        .build()))
                .build();
        final JsonObject commandPayload = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonValueConverter.convert(prosecutionCase))
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.command.update-case-resulted-defendant-proceedings-concluded"), commandPayload);

        when(aCase.updateDefendantCaseResultedAndUpdated(prosecutionCase)).thenReturn((mock(Stream.class)));
        listingCommandHandler.updateDefendantHearingResultedAndCaseResulted(envelope);

        verify(aCase).updateDefendantCaseResultedAndUpdated(prosecutionCase);
    }

    @Test
    public void shouldUpdateDefendantCourtProceedings() throws EventStreamException {
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("CASE_CLOSED")
                .withId(CASE_ID)
                .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant()
                        .withProceedingsConcluded(true)
                        .build()))
                .build();
        final JsonObject commandPayload = Json.createObjectBuilder()
                .add("hearingId", HEARING_ID_1.toString())
                .add("prosecutionCase", objectToJsonValueConverter.convert(prosecutionCase))
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("listing.command.update-defendant-court-proceedings"), commandPayload);

        when(hearing.updateDefendantCourtProceedingForHearing(HEARING_ID_1, prosecutionCase)).thenReturn((mock(Stream.class)));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        given(jsonObjectConverter.convert(envelope.payloadAsJsonObject().getJsonObject("prosecutionCase"), ProsecutionCase.class)).willReturn(prosecutionCase);

        listingCommandHandler.updateDefendantCourtProceedings(envelope);

        verify(hearing).updateDefendantCourtProceedingForHearing(HEARING_ID_1, prosecutionCase);
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void shouldCourtListRequestExport() throws EventStreamException {

        final UUID courtListId = randomUUID();
        final UUID courtCentreId = fromString("9689207b-a9d2-4c2e-bd38-269b78a132a8");
        final uk.gov.justice.listing.commands.PublishCourtListType publishCourtListType = uk.gov.justice.listing.commands.PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        final String courtListJson = "{}";

        final JsonObject payload = Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("publishCourtListType", publishCourtListType.name())
                .add("startDate", startDate.toString())
                .add("courtListJson", courtListJson)
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.court-list-request-export", payload);

        when(uuidService.getCourtListId(courtCentreId, publishCourtListType, startDate)).thenReturn(courtListId);
        listingCommandHandler.courtListRequestExport(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.event.court-list-export-requested")
                                .withCausationIds(commandEnvelope.metadata().id()),
                        payload().isJson(allOf(
                                withJsonPath("$.courtListId", equalTo(courtListId.toString())),
                                withJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())),
                                withJsonPath("$.publishCourtListType", equalTo(publishCourtListType.name())),
                                withJsonPath("$.startDate", equalTo(startDate.toString())),
                                withJsonPath("$.courtListJson", equalTo(courtListJson))
                        )))));
    }

    @Test
    public void shouldRaiseExportFailedEventForRecordExportFailedCommand() throws Exception {
        final UUID courtListId = randomUUID();
        final UUID courtCentreId = fromString("9689207b-a9d2-4c2e-bd38-269b78a132a8");
        final uk.gov.justice.listing.commands.PublishCourtListType publishCourtListType = uk.gov.justice.listing.commands.PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        final String failedTime = "2016-09-09T08:31:40Z";
        final String errorMessage = "Unable to download the file from file service";

        final JsonObject payload = Json.createObjectBuilder()
                .add("courtListId", courtListId.toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("publishCourtListType", publishCourtListType.name())
                .add("startDate", startDate.toString())
                .add("failedTime", failedTime)
                .add("errorMessage", errorMessage)
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.record-court-list-export-failed", payload);
        listingCommandHandler.recordCourtListExportFailed(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.event.publish-court-list-export-failed")
                                .withCausationIds(commandEnvelope.metadata().id()),
                        payload().isJson(allOf(
                                withJsonPath("$.courtListId", equalTo(courtListId.toString())),
                                withJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())),
                                withJsonPath("$.publishCourtListType", equalTo(publishCourtListType.name())),
                                withJsonPath("$.startDate", equalTo(startDate.toString())))))));
    }

    @Test
    public void shouldRaiseExportSuccessfulEventForExportSuccessfulCommand() throws Exception {

        final UUID courtListId = randomUUID();
        final UUID courtCentreId = fromString("9689207b-a9d2-4c2e-bd38-269b78a132a8");
        final uk.gov.justice.listing.commands.PublishCourtListType publishCourtListType = uk.gov.justice.listing.commands.PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        final String exportedTime = "2016-09-09T08:31:40Z";

        final JsonObject payload = Json.createObjectBuilder()
                .add("courtListId", courtListId.toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("publishCourtListType", publishCourtListType.name())
                .add("startDate", startDate.toString())
                .add("exportedTime", exportedTime)
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.record-court-list-export-successful", payload);

        listingCommandHandler.recordCourtListExportSuccessful(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.event.publish-court-list-export-successful")
                                .withCausationIds(commandEnvelope.metadata().id()),
                        payload().isJson(allOf(
                                withJsonPath("$.courtListId", equalTo(courtListId.toString())),
                                withJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())),
                                withJsonPath("$.publishCourtListType", equalTo(publishCourtListType.name())),
                                withJsonPath("$.startDate", equalTo(startDate.toString())))))));
    }

    @Test
    public void shouldCreatePublishCourtListRequestedEvent() throws Exception {
        final UUID courtCentreId = randomUUID();

        when(aggregateService.get(eventStream, PublishCourtListRequestAggregate.class)).thenReturn(publishCourtListRequestAggregate);
        when(publishCourtListRequestAggregate.recordCourtListRequested(any(UUID.class), any(UUID.class), any(LocalDate.class), any(LocalDate.class), any(PublishCourtListType.class), any(ZonedDateTime.class)))
                .thenReturn(Stream.of(publishCourtListRequested().build()));

        final String jsonString = givenPayload("/test-data/listing.command.publish-court-list.json").toString()
                .replace("COURT_CENTRE_ID", courtCentreId.toString());

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.publish-court-list", jsonReader.readObject());
        listingCommandHandler.publishCourtList(commandEnvelope);
        verify(publishCourtListRequestAggregate).recordCourtListRequested(any(UUID.class), any(UUID.class), any(LocalDate.class), any(LocalDate.class), any(PublishCourtListType.class), any(ZonedDateTime.class));
    }

    @Test
    public void shouldCreateRecordCourtListPublishedEvent() throws Exception {
        final UUID publishCourtListRequestId = randomUUID();
        final UUID courtCentreId = fromString("9689207b-a9d2-4c2e-bd38-269b78a132a8");
        final UUID courtListFileId = fromString("1deeec47-056b-431a-b131-0ea6f5d554ed");
        final String fileName = "FILENAME";
        final PublishCourtListType publishCourtListType = FIRM;
        final ZonedDateTime producedTime = parse("2019-11-15T11:13:19.314Z[UTC]");
        final LocalDate publishDate = LocalDate.now();

        when(aggregateService.get(eventStream, PublishCourtListRequestAggregate.class)).thenReturn(publishCourtListRequestAggregate);
        when(publishCourtListRequestAggregate.recordCourtListProduced(publishCourtListRequestId, courtCentreId, courtListFileId, fileName, publishCourtListType, producedTime, publishDate))
                .thenReturn(Stream.of(publishCourtListProduced().build()));

        final JsonObject payload = Json.createObjectBuilder()
                .add("publishCourtListRequestId", publishCourtListRequestId.toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("publishCourtListType", publishCourtListType.name())
                .add("courtListFileId", courtListFileId.toString())
                .add("courtListFileName", fileName)
                .add("producedTime", producedTime.toString())
                .add("publishDate", publishDate.toString())
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.record-court-list-produced", payload);
        listingCommandHandler.recordCourtListProduced(commandEnvelope);
        verify(publishCourtListRequestAggregate).recordCourtListProduced(publishCourtListRequestId, courtCentreId, courtListFileId, fileName, publishCourtListType, producedTime, publishDate);
    }

    @Test
    public void shouldStorePublishedCourtList() throws EventStreamException {
        final UUID courtListId = randomUUID();
        final UUID courtCentreId = fromString("9689207b-a9d2-4c2e-bd38-269b78a132a8");
        final PublishCourtListType publishCourtListType = FIRM;
        final LocalDate startDate = LocalDate.now();
        final String courtListJson = "{}";


        when(uuidService.getCourtListId(courtCentreId,
                uk.gov.justice.listing.commands.PublishCourtListType.valueOf(publishCourtListType.name()), startDate)).thenReturn(courtListId);

        final JsonObject payload = Json.createObjectBuilder()
                .add("courtListId", courtListId.toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("publishCourtListType", publishCourtListType.name())
                .add("startDate", startDate.toString())
                .add("courtListJson", courtListJson)
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.store-published-court-list", payload);
        listingCommandHandler.storePublishedCourtList(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.event.published-court-list-stored")
                                .withCausationIds(commandEnvelope.metadata().id()),
                        payload().isJson(allOf(
                                withJsonPath("$.courtListId", equalTo(courtListId.toString())),
                                withJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())),
                                withJsonPath("$.publishCourtListType", equalTo(publishCourtListType.name())),
                                withJsonPath("$.startDate", equalTo(startDate.toString())),
                                withJsonPath("$.courtListJson", equalTo(courtListJson))
                        )))));
    }

    @Test
    public void listingCommandHandlerShouldExtendWholeHearingForHearings() throws Exception {
        final UUID hearingID1 = randomUUID();
        final UUID hearingID2 = randomUUID();

        final JsonEnvelope commandEnvelope = getEnvelopeForExtendWholeHearing(hearingID1, hearingID2);

        final uk.gov.justice.listing.events.Hearing allocatedHearing = extendHearingHelper.getAllocatedHearingById(hearingID1, hearingID1, getCaseUrn());
        final uk.gov.justice.listing.events.Hearing unAllocatedHearing = extendHearingHelper.getUnAllocatedHearingById(hearingID2, hearingID2, getCaseUrn());

        doReturn(allocatedHearing).doReturn(unAllocatedHearing).when(hearingFactory)
                .getHearingById(any(UUID.class), any(JsonEnvelope.class));

        when(hearing.applyAllocationRulesForExtendedHearing(any(), anyBoolean())).thenReturn(Stream.of(new Object()));

        listingCommandHandler.extendHearingForHearing(commandEnvelope);

        verify(hearing, times(1)).updatedListedCasesInHearing(allocatedHearing, unAllocatedHearing, unAllocatedHearing.getListedCases());
        verify(hearing, times(1)).applyAllocationRulesForExtendedHearing(any(uk.gov.justice.listing.events.Hearing.class), any(Boolean.class));
    }

    @Test
    public void shouldAmendHearingToUpdateDefendantOrOffenceWhenAddingDefendantToTheExistingProsecutionCase() throws Exception {
        final UUID hearingID1 = randomUUID();
        final UUID hearingID2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final JsonEnvelope commandEnvelope = getEnvelopeForExtendWholeHearing(hearingID1, hearingID2);

        final uk.gov.justice.listing.events.Hearing allocatedHearing = extendHearingHelper.getAllocatedHearingById(hearingID1, prosecutionCaseId, getCaseUrn());
        final uk.gov.justice.listing.events.Hearing unAllocatedHearing = extendHearingHelper.getUnAllocatedHearingById(hearingID2, prosecutionCaseId, getCaseUrn());

        doReturn(allocatedHearing).doReturn(unAllocatedHearing).when(hearingFactory)
                .getHearingById(any(UUID.class), any(JsonEnvelope.class));

        when(hearing.applyAllocationRulesForExtendedHearing(any(), anyBoolean())).thenReturn(Stream.of(new Object()));

        listingCommandHandler.extendHearingForHearing(commandEnvelope);

        verify(hearing, times(1)).updatedListedCasesInHearing(allocatedHearing, unAllocatedHearing, unAllocatedHearing.getListedCases());
        verify(hearing, times(1)).applyAllocationRulesForExtendedHearing(any(uk.gov.justice.listing.events.Hearing.class), any(Boolean.class));
    }

    @Test
    public void shouldAmendUnAllocatedHearingToUpdateDefendantOrOffenceWhenAddingDefendantToTheExistingProsecutionCase() throws Exception {
        final UUID hearingID1 = randomUUID();
        final UUID hearingID2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final JsonEnvelope commandEnvelope = getEnvelopeForExtendPartialHearing(ALLOCATED_HEARING_ID.toString(),
                UNALLOCATED_HEARING_ID.toString(),
                CASE_ID1.toString(),
                CASE_ID2.toString(),
                DEF_ID1.toString(),
                DEF_ID2.toString(),
                DEF_ID3.toString(),
                OFF_ID1.toString(),
                OFF_ID2.toString(),
                OFF_ID3.toString(),
                OFF_ID4.toString());

        final uk.gov.justice.listing.events.Hearing allocatedHearing = extendHearingHelper.getAllocatedHearingById(hearingID1, prosecutionCaseId, getCaseUrn());
        final uk.gov.justice.listing.events.Hearing unAllocatedHearing = extendHearingHelper.getUnAllocatedHearingById(hearingID2, prosecutionCaseId, getCaseUrn());

        doReturn(allocatedHearing).doReturn(unAllocatedHearing).when(hearingFactory)
                .getHearingById(any(UUID.class), any(JsonEnvelope.class));

        when(hearing.applyAllocationRulesForExtendedHearing(any(), anyBoolean())).thenReturn(Stream.empty());
        when(hearing.addCasesToUnAllocatedHearing(any(), any())).thenReturn(Stream.empty());

        listingCommandHandler.extendHearingForHearing(commandEnvelope);

        verify(hearing, times(1)).applyAllocationRulesForExtendedHearing(any(uk.gov.justice.listing.events.Hearing.class), anyBoolean());
        verify(hearing, times(1)).addCasesToUnAllocatedHearing(any(), any());
        verify(hearing, times(1)).applyAllocationRulesForExtendedHearing(any(uk.gov.justice.listing.events.Hearing.class), any(Boolean.class));
    }

    @Test
    @Ignore("This test is failing after updating service parent pom. Need to fix this.")
    public void listingCommandHandlerShouldExtendPartialHearingForHearings() throws Exception {

        final JsonEnvelope commandEnvelope = getEnvelopeForExtendPartialHearing(ALLOCATED_HEARING_ID.toString(),
                UNALLOCATED_HEARING_ID.toString(),
                CASE_ID1.toString(),
                CASE_ID2.toString(),
                DEF_ID1.toString(),
                DEF_ID2.toString(),
                DEF_ID3.toString(),
                OFF_ID1.toString(),
                OFF_ID2.toString(),
                OFF_ID3.toString(),
                OFF_ID4.toString());

        final uk.gov.justice.listing.events.Hearing allocatedHearing = extendHearingHelper.getAllocatedHearingById(ALLOCATED_HEARING_ID, CASE_ID1, getCaseUrn());
        final uk.gov.justice.listing.events.Hearing unAllocatedHearing = extendHearingHelper.getUnAllocatedHearingById(UNALLOCATED_HEARING_ID,
                CASE_ID1,
                CASE_ID2,
                getCaseUrn(),
                getCaseUrn(),
                DEF_ID1,
                DEF_ID2,
                DEF_ID3,
                OFF_ID1,
                OFF_ID2,
                OFF_ID3,
                OFF_ID4);

        final uk.gov.justice.listing.events.Hearing unAllocatedHearingDeepCopy = extendHearingHelper.getUnAllocatedHearingById(UNALLOCATED_HEARING_ID,
                CASE_ID1,
                CASE_ID2,
                getCaseUrn(),
                getCaseUrn(),
                DEF_ID1,
                DEF_ID2,
                DEF_ID3,
                OFF_ID1,
                OFF_ID2,
                OFF_ID3,
                OFF_ID4);
        final JsonObject unallocatedHearingJson = objectToJsonObjectConverter.convert(unAllocatedHearing);

        given(objectToJsonObjectConverter.convert(unAllocatedHearing)).willReturn(unallocatedHearingJson);
        given(jsonObjectConverter.convert(unallocatedHearingJson, uk.gov.justice.listing.events.Hearing.class)).willReturn(unAllocatedHearingDeepCopy);
        doReturn(allocatedHearing).doReturn(unAllocatedHearing).when(hearingFactory)
                .getHearingById(any(UUID.class), any(JsonEnvelope.class));

        listingCommandHandler.extendHearingForHearing(commandEnvelope);

        verify(hearing, times(1)).updatedListedCasesInHearing(allocatedHearing, unAllocatedHearing, unAllocatedHearingDeepCopy.getListedCases());
        verify(hearing, times(1)).applyAllocationRulesForExtendedHearing(any(uk.gov.justice.listing.events.Hearing.class), true);
        verify(hearing, times(1)).updateUnallocatedHearingPartially(eq(UNALLOCATED_HEARING_ID), anyList());
    }

    @Test
    public void shouldRemovePartiallyMergedOffencesFromOriginalHearing() throws EventStreamException {
        final UUID hearingIdToBeUpdated = randomUUID();

        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingIdToBeUpdated", hearingIdToBeUpdated.toString())
                .add("prosecutionCasesToRemove", createArrayBuilder())
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.remove-partially-merged-offences-from-original-hearing", payload);
        listingCommandHandler.handleRemovePartiallyMergedOffencesFromOriginalHearing(commandEnvelope);

        verify(hearing, times(1)).updateUnallocatedHearingPartially(hearingIdToBeUpdated, Collections.emptyList());
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void shouldMarkHearingAsDeleted() throws EventStreamException {
        final UUID hearingId = randomUUID();

        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingIdToMarkAsDeleted", hearingId.toString())
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.mark-hearing-as-deleted", payload);
        listingCommandHandler.handleMarkHearingAsDeleted(commandEnvelope);

        verify(hearing, times(1)).deleteUnAllocatedHearing();
    }

    @Test
    public void listingCommandHandlerShouldVacateTrial() throws Exception {
        final JsonEnvelope commandEnvelope = getEnvelopeForVacateTrial(REASON);

        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearing.vacateTrial(HEARING_ID_1, REASON)).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        listingCommandHandler.vacateTrial(commandEnvelope);

        verify(hearing, times(1)).vacateTrial(HEARING_ID_1, REASON);
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void listingCommandHandlerShouldHearingVacateTrial() throws Exception {
        final JsonEnvelope commandEnvelope = getEnvelopeForHearingVacateTrial(REASON);

        when(hearing.hearingVacateTrial(of(REASON))).thenReturn(events);
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(events);

        listingCommandHandler.hearingVacateTrial(commandEnvelope);

        verify(hearing).hearingVacateTrial(of(REASON));
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void listingCommandHandlerShouldHearingVacateTrialWithoutReason() throws Exception {
        final JsonEnvelope commandEnvelope = getEnvelopeForHearingVacateTrialWithoutReason();

        when(hearing.hearingVacateTrial(empty())).thenReturn(events);
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(events);

        listingCommandHandler.hearingVacateTrial(commandEnvelope);

        verify(hearing).hearingVacateTrial(empty());
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    @Test
    public void shouldCancelHearingDaysWhenMultipleHearingDaysRequested() throws Exception {
        final Envelope<CancelHearingDays> envelope = getEnvelopeForCancelHearingDays();
        when(hearing.cancelHearingDays(any(UUID.class), anyListOf(uk.gov.justice.listing.events.HearingDay.class))).thenReturn(events);
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(events);

        listingCommandHandler.cancelHearingDays(envelope);

        // then
        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
        verify(hearing).cancelHearingDays(eq(envelope.payload().getHearingId()), cancelHearingDaysCaptor.capture());
        final List<uk.gov.justice.listing.events.HearingDay> hearingDays = cancelHearingDaysCaptor.getValue();
        assertThat(hearingDays, hasSize(3));
        assertThat(LocalDates.to(hearingDays.get(0).getHearingDate()), is("2020-08-18"));
        assertThat(hearingDays.get(0).getSequence(), is(0));
        assertThat(hearingDays.get(0).getDurationMinutes(), is(30));
        assertThat(ZonedDateTimes.toString(hearingDays.get(0).getStartTime()), is("2020-08-18T01:22:12.381Z"));
        assertThat(hearingDays.get(0).getIsCancelled(), nullValue());

        assertThat(LocalDates.to(hearingDays.get(1).getHearingDate()), is("2020-08-19"));
        assertThat(hearingDays.get(1).getSequence(), is(1));
        assertThat(hearingDays.get(1).getDurationMinutes(), is(10));
        assertThat(ZonedDateTimes.toString(hearingDays.get(1).getStartTime()), is("2020-08-19T01:22:12.381Z"));
        assertThat(hearingDays.get(1).getIsCancelled(), is(false));

        assertThat(LocalDates.to(hearingDays.get(2).getHearingDate()), is("2020-08-20"));
        assertThat(hearingDays.get(2).getSequence(), is(1));
        assertThat(hearingDays.get(2).getDurationMinutes(), is(20));
        assertThat(ZonedDateTimes.toString(hearingDays.get(2).getStartTime()), is("2020-08-20T02:22:12.381Z"));
        assertThat(hearingDays.get(2).getIsCancelled(), is(true));
    }

    @Test
    public void handleAddCasesForHearing() throws Exception {
        final JsonEnvelope commandEnvelope = getJsonEnvelopeForAddCaseForHearing();
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        listingCommandHandler.handleAddCasesToHearing(commandEnvelope);

        verify(hearing, times(1)).addCasesToHearing(any(List.class), anyList(), any());
        verify(hearing, times(1)).raiseUpdateHearingInStagingHmi(any(Stream.class));
    }

    private JsonEnvelope getEnvelopeForVacateTrial(final UUID reason) {
        final String requestBody = "{\"hearingId\":\"" + HEARING_ID_1 + "\",\"vacatedTrialReasonId\":\"" + reason + "\"}";
        final JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        return createEnvelope("listing.command.vacate-trial-enriched", jsonReader.readObject());
    }

    private JsonEnvelope getEnvelopeForHearingVacateTrial(final UUID reason) {
        final String requestBody = "{\"hearingId\":\"" + HEARING_ID_1 + "\",\"vacatedTrialReasonId\":\"" + reason + "\"}";
        final JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        return createEnvelope("listing.command.hearing-vacate-trial", jsonReader.readObject());
    }

    private JsonEnvelope getEnvelopeForHearingVacateTrialWithoutReason() {
        final String requestBody = "{\"hearingId\":\"" + HEARING_ID_1 + "\"}";
        final JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        return createEnvelope("listing.command.hearing-vacate-trial", jsonReader.readObject());
    }

    private Envelope<CancelHearingDays> getEnvelopeForCancelHearingDays() {
        final JsonObject cancelHearingDaysJsonObject = givenPayload("/test-data/listing.command.cancel-hearing-days.json");
        final CancelHearingDays cancelHearingDays = jsonObjectConverter.convert(cancelHearingDaysJsonObject, CancelHearingDays.class);
        final Metadata metadata = metadataBuilder().withName("listing.command.cancel-hearing-days").withId(randomUUID()).build();
        return envelopeFrom(metadata, cancelHearingDays);
    }

    private String getCaseUrn() {
        return STRING.next();
    }

    private JsonEnvelope getJsonEnvelopeForAddCaseForHearing() {
        final String jsonString = givenPayload("/test-data/listing.command.add-cases-to-hearing.json").toString();
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-cases-to-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateSequenceForHearingDayCommandEnvelope() {
        final String jsonString = givenPayload("/test-data/listing.command.update-sequence-for-hearing-day.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("SEQUENCE_1", SEQUENCE_1)
                .replace("SEQUENCE_2", SEQUENCE_2)
                .replace("HEARING_DATE_1", HEARING_DATE_1)
                .replace("HEARING_DATE_2", HEARING_DATE_2);

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.sequence-hearings", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Aggregate> void givenEventStream(final EventStream eventStream, final T aggregate, final Class<T> clz) {
        when(aggregateService.get(eventStream, clz)).thenReturn(aggregate);
    }

    private JsonEnvelope listCourtHearingCommandEnvelope() {
        final String jsonString = givenPayload("/test-data/listing.command.list-court-hearing.json").toString()
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
                .replace("EARLIEST_START_TIME", EARLIEST_START_TIME)
                .replace("WEEK_COMMENCING_START_DATE", WEEK_COMMENCING_START_DATE.toString())
                .replace("WEEK_COMMENCING_DURATION", WEEK_COMMENCING_DURATION.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.list-court-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope createListCourtHearingCommandEnvelopeWithBookSlot() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.list-court-hearing-with-booked-slots.json").toString()
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
                .replace("EARLIEST_START_TIME", EARLIEST_START_TIME)
                .replace("WEEK_COMMENCING_START_DATE", WEEK_COMMENCING_START_DATE.toString())
                .replace("WEEK_COMMENCING_DURATION", WEEK_COMMENCING_DURATION.toString())
                .replace("SLOT_START_TIME", SLOT_START_TIME)
                .replace("SLOT_DURATION", String.valueOf(SLOT_DURATION))
                .replace("SLOT_SCHEDULE_ID", SLOT_SCHEDULE_ID)
                .replace("SLOT_SESSION", SLOT_SESSION)
                .replace("SLOT_OUCODE", SLOT_OUCODE)
                .replace("SLOT_COURT_ROOM_ID", String.valueOf(SLOT_COURT_ROOM_ID));

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.list-court-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope createListCourtHearingCommandEnvelopeWithAdjournment() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.list-court-hearing-with-adjournment.json").toString()
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
                .replace("LISTED_START_TIME", LISTED_START_TIME);

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.list-court-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope changeJudiciaryForHearingsCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.change-judiciary-for-hearings.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("HEARING_ID_2", HEARING_ID_2.toString())
                .replace("JUDICIAL_ID_1", JUDICIAL_ID_1.toString())
                .replace("JUDICIAL_ID_2", JUDICIAL_ID_2.toString())
                .replace("USER_ID_1", USER_ID_1.toString())
                .replace("USER_ID_2", USER_ID_2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.change-judiciary-for-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope restrictCourtListCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.restrict-court-list.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("CASE_ID_1", CASE_ID.toString())
                .replace("COURT_APPLICATION_ID_1", COURT_APPLICATION_ID.toString())
                .replace("COURT_APPLICATION_TYPE_1", COURT_APPLICATION_TYPE);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.restrict-court-list", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope ejectCaseCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.eject-case.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("CASE_ID_1", CASE_ID.toString())
                .replace("REMOVAL_REASON", "SomeReason");

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.eject-case-or-application", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope ejectApplicationCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.eject-application.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("COURT_APPLICATION_ID_1", COURT_APPLICATION_ID.toString())
                .replace("REMOVAL_REASON", "SomeReason");

        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.eject-case-or-application", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateCaseDefendantDetailsCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-case-defendant-details.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-case-defendant-details", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateCourtApplicationCommandEnvelope() {
        return createEnvelope("listing.command.update-court-application", createObjectBuilder().add("courtApplication",
                createObjectBuilder().add("id", APPLICATION_ID.toString())
                        .add("type", createObjectBuilder().add("ApplicationType", "type"))
                        .add("applicant", createObjectBuilder()
                                .add("id", randomUUID().toString()))
                        .build()).build());
    }


    private JsonEnvelope updateCaseDefendantOffencesCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-case-defendant-offences.json").toString()
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
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateDefendantsForHearingCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-defendants-for-hearing.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("DEFENDANT_ID", DEFENDANT_ID1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-case-defendant-details", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addDefendantsForHearingCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.add-defendants-to-court-proceedings-for-hearing.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("DEFENDANT_ID", DEFENDANT_ID1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-defendants-to-court-proceedings", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addDefendantsForCourtProceedingsCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.add-defendants-to-court-proceedings.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("DEFENDANT_ID2", DEFENDANT_ID2.toString())
                .replace("OFFENCE_ID1", OFFENCE_ID1.toString());


        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-defendants-to-court-proceedings", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    private JsonEnvelope linkCaseEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-linked-cases.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("CASE_URN", URN);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-linked-cases", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope linkCaseToHearingEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-linked-case-in-hearing.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("CASE_URN", URN)
                .replace("HEARING_ID", HEARING_ID_1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-linked-case-in-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addCaseMarkersForListedCaseCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-case-markers.json").toString()
                .replace("CASE_ID", CASE_ID.toString())
                .replace("HEARING_ID_1", HEARING_ID_1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-case-markers", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateHearingForListingCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-hearing-for-listing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("HEARING_TYPE_ID", HEARING_TYPE.getId().toString())
                .replace("HEARING_TYPE_DESCRIPTION", HEARING_TYPE.getDescription())
                .replace("START_DATE", START_DATE.toString())
                .replace("END_DATE", END_DATE)
                .replace("HEARING_LANGUAGE", HEARING_LANGUAGE)
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_ROOM_ID", COURT_ROOM_ID.toString())
                .replace("ROOM_ID_1", COURT_ROOM_ID_1.toString())
                .replace("NON_SITTING_DAY", NON_SITTING_DAY)
                .replace("NON_DEFAULT_DAY", NON_DEFAULT_DAY)
                .replace("DEFAULT_DAY_1", NON_DEFAULT_DAY_PM)
                .replace("DEFAULT_DURATION", DEFAULT_DURATION)
                .replace("DEFAULT_START_TIME", DEFAULT_START_TIME)
                .replace("JURISDICTION_TYPE", JURISDICTION_TYPE.toString())
                .replace("\"IS_DEPUTY\"", IS_DEPUTY.toString())
                .replace("\"IS_BENCH_CHAIRMAN\"", IS_BENCH_CHAIRMAN.toString())
                .replace("JUDICIAL_ID", JUDICIAL_ID_1.toString())
                .replace("JUDICIAL_ROLE_TYPE", JUDICIAL_ROLE_TYPE)
                .replace("AUTHORITY_ID", AUTHORITY_ID.toString())
                .replace("\"HAS_VIDEO_LINK\"", HAS_VIDEO_LINK.toString())
                .replace("PUBLIC_LIST_NOTE", PUBLIC_LIST_NOTE)
                .replace("SLOT_SESSION", SLOT_SESSION)
                .replace("SESSION_1", SLOT_SESSION_PM)
                .replace("SLOT_DURATION", String.valueOf(SLOT_DURATION))
                .replace("OU_CODE", SLOT_OUCODE)
                .replace("COURT_ROOM_NUMBER", String.valueOf(SLOT_COURT_ROOM_ID))
                .replace("COURT_SCHEDULE_ID_1", COURT_SCHEDULE_ID_1.toString())
                .replace("COURT_SCHEDULE_ID_2", COURT_SCHEDULE_ID_2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-hearing-for-listing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateHearingForListingWithoutJudiciariesCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-hearing-for-listing-without-judiciaries.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("HEARING_TYPE_ID", HEARING_TYPE.getId().toString())
                .replace("HEARING_TYPE_DESCRIPTION", HEARING_TYPE.getDescription())
                .replace("START_DATE", START_DATE.toString())
                .replace("END_DATE", END_DATE)
                .replace("HEARING_LANGUAGE", HEARING_LANGUAGE)
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_ROOM_ID", COURT_ROOM_ID.toString())
                .replace("ROOM_ID_1", COURT_ROOM_ID_1.toString())
                .replace("NON_SITTING_DAY", NON_SITTING_DAY)
                .replace("NON_DEFAULT_DAY", NON_DEFAULT_DAY)
                .replace("DEFAULT_DAY_1", NON_DEFAULT_DAY_PM)
                .replace("DEFAULT_DURATION", DEFAULT_DURATION)
                .replace("DEFAULT_START_TIME", DEFAULT_START_TIME)
                .replace("JURISDICTION_TYPE", JurisdictionType.MAGISTRATES.toString())
                .replace("AUTHORITY_ID", AUTHORITY_ID.toString())
                .replace("\"HAS_VIDEO_LINK\"", HAS_VIDEO_LINK.toString())
                .replace("PUBLIC_LIST_NOTE", PUBLIC_LIST_NOTE)
                .replace("SLOT_SESSION", SLOT_SESSION)
                .replace("SESSION_1", SLOT_SESSION_PM)
                .replace("SLOT_DURATION", String.valueOf(SLOT_DURATION))
                .replace("OU_CODE", SLOT_OUCODE)
                .replace("COURT_ROOM_NUMBER", String.valueOf(SLOT_COURT_ROOM_ID))
                .replace("COURT_SCHEDULE_ID_1", COURT_SCHEDULE_ID_1.toString())
                .replace("COURT_SCHEDULE_ID_2", COURT_SCHEDULE_ID_2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-hearing-for-listing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    private JsonEnvelope updateHearingForListingwithWeekCommencingCommandEnvelope() {
        final String jsonString = givenPayload("/test-data/listing.command.update-hearing-for-listing-week-commencing.json").toString()
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

    private JsonEnvelope addHearingToCaseCommandEnvelope(final JsonObject commandJsonObject) {
        return createEnvelope("listing.command.add-hearing-to-case", commandJsonObject);
    }

    private JsonEnvelope addCourtApplicationToHearingCommandEnvelope(final JsonObject commandJsonObject) {
        return createEnvelope("listing.command.add-court-application-to-hearing", commandJsonObject);
    }

    private JsonEnvelope updateCourtApplicationCommandEnvelope(final JsonObject commandJsonObject) {
        return createEnvelope("listing.command.update-court-application", commandJsonObject);
    }

    private JsonEnvelope updateOffencesForHearingCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-offences-for-hearing.json").toString()
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
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateOffencesWithLaaForHearingCommandEnvelope() {
        LAA_STATUS_ID = randomUUID();
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-offences-for-hearing-to-add-laa.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("UPDATED_OFFENCE_ID1", UPDATED_OFFENCE_ID1.toString())
                .replace("UPDATED_OFFENCE_ID2", UPDATED_OFFENCE_ID2.toString())
                .replace("START_DATE", START_DATE.toString())
                .replace("END_DATE", END_DATE)
                .replace("LAA_STATUS_ID", LAA_STATUS_ID.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-offences-for-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope deleteOffencesForHearingCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.delete-offences-for-hearing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("CASE_ID", CASE_ID.toString())
                .replace("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replace("DELETED_OFFENCE_ID1", DELETED_OFFENCE_ID1.toString())
                .replace("DELETED_OFFENCE_ID2", DELETED_OFFENCE_ID2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.delete-offences-for-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addOffencesForHearingCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.add-offences-for-hearing.json").toString()
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
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope updateCourtApplicationForHearingsCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.update-court-application-for-hearings.json").toString()
                .replace("HEARING_ID_1", HEARING_ID_1.toString())
                .replace("HEARING_ID_2", HEARING_ID_2.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.update-court-application-for-hearings", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope addCourtApplicationForHearingCommandEnvelope() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.add-court-application-for-hearing.json").toString()
                .replace("HEARING_ID", HEARING_ID_1.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.add-court-application-for-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEnvelope sendReListedCaseForListingCommandEnvelope() {
        final JsonObject caseJson = createReListedListCourtHearingJson();
        return createEnvelope("listing.command.list-court-hearing", caseJson);
    }


    private JsonEnvelope listHearingCommandEnvelope() {
        final JsonObject hearingJson = createCommandListHearingJson();
        return createEnvelope("listing.command.list-hearing", hearingJson);
    }

    private JsonEnvelope relistHearingCommandEnvelope() {
        final JsonObject hearingJson = createCommandReListHearingJson();
        return createEnvelope("listing.command.list-hearing", hearingJson);
    }


    private JsonEnvelope updateHearingCommandEnvelopeWithOnlyMandatoryDataChanges() {
        final JsonObject hearingJson = createUpdateHearingJsonWhereOnlyMandatoryDataHasChanged();
        return createEnvelope("listing.command.update-hearing-for-listing", hearingJson);
    }

    private JsonEnvelope updateHearingCommandEnvelopeWithCompleteChanges() {
        final JsonObject hearingJson = createUpdateHearingJsonWhereAllDataHasChanged();
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
        final JsonObject updatedCase = createObjectBuilder()
                .add("caseId", UPDATED_OFFENCE_CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .add("offences", createAddedOrUpdatedOffences())
                .build();

        return createArrayBuilder().add(updatedCase).build();

    }

    private JsonArray createDeletedCaseDefendantOffences() {
        final JsonObject deletedCase = createObjectBuilder()
                .add("caseId", DELETED_OFFENCE_CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .add("offences", createDeletedOffences())
                .build();

        return createArrayBuilder().add(deletedCase).build();
    }

    private JsonArray createAddedCaseDefendantOffences() {
        final JsonObject addedCase = createObjectBuilder()
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
        final JsonObject hearing = createObjectBuilder()
                .add("id", HEARING_ID_1.toString())
                .add("courtCentreId", COURT_CENTRE_ID.toString())
                .add("type", PTP_TYPE)
                .add("startDate", INITIAL_START_DATE)
                .add("estimateMinutes", INITIAL_ESTIMATE_MINUTES)
                .add("defendants", createDefendantsJson())
                .build();

        final JsonArrayBuilder hearings = createArrayBuilder().add(hearing);
        return hearings.build();
    }

    private JsonArray createReListedHearingsJson() {
        final JsonObject hearing = createObjectBuilder()
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

        final JsonArrayBuilder hearings = createArrayBuilder().add(hearing);
        return hearings.build();
    }

    private JsonArray createDefendantsJson() {
        final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("id", DEFENDANT_ID1.toString())
                .add("personId", PERSON_ID.toString())
                .add("firstName", FIRST_NAME)
                .add("lastName", LAST_NAME)
                .add("dateOfBirth", DATE_OF_BIRTH)
                .add("bailStatus", new BailStatus.Builder().withCode("P").withDescription("Conditional Bail with Pre-Release conditions").withId(fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).build().toString())
                .add("defenceOrganisation", DEFENCE_ORGANISATION_NAME)
                .add("offences", createAddedOrUpdatedOffences());

        if (hasCustodyTimeLimit) {
            defendantBuilder.add("custodyTimeLimit", CUSTODY_TIME_LIMIT);
        }
        final JsonArrayBuilder defendants = createArrayBuilder().add(defendantBuilder.build());

        return defendants.build();
    }

    private JsonObject createCourtsDefendantJson() {
        final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("id", DEFENDANT_ID1.toString())
                .add("prosecutionCaseId", CASE_ID.toString())
                .add("defenceOrganisation", createDefenceOrganisationJson())
                .add("personDefendant", createCourtsPersonDefendantJson());

        return defendantBuilder.build();
    }

    private JsonObject createCourtsPersonDefendantJson() {
        final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("bailStatus", new BailStatus.Builder().withCode("P").withDescription("Conditional Bail with Pre-Release conditions").withId(fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).build().toString())
                .add("personDetails", createCourtsPersonDetailsJson());
        if (hasCustodyTimeLimit) {
            defendantBuilder.add("custodyTimeLimit", CUSTODY_TIME_LIMIT);
        }

        return defendantBuilder.build();
    }

    private JsonObject createCourtsPersonDetailsJson() {
        return createObjectBuilder()
                .add("title", PERSON_TITLE)
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

        final JsonObject offence = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .build();

        final JsonArrayBuilder offences = createArrayBuilder().add(offence);

        return offences.build();
    }

    private JsonArray createDeletedOffences() {

        final JsonArrayBuilder offences = createArrayBuilder().add(OFFENCE_ID1.toString());

        return offences.build();
    }

    private JsonArray createAddedOrUpdatedOffences() {
        final JsonObject statementOfOffence = createObjectBuilder()
                .add("title", STATEMENT_OF_OFFENCE_TITLE)
                .add("legislation", STATEMENT_OF_OFFENCE_LEGISLATION)
                .build();

        final JsonObject offence = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("wording", OFFENCE_WORDING)
                .add("offenceCode", PERSON_ID.toString())
                .add("startDate", OFFENCE_START_DATE)
                .add("endDate", OFFENCE_END_DATE)
                .add("count", OFFENCE_COUNT)
                .add("convictionDate", OFFENCE_CONVICTION_DATE)
                .add("statementOfOffence", statementOfOffence)
                .build();

        final JsonArrayBuilder offences = createArrayBuilder().add(offence);

        return offences.build();
    }

    private JsonArray createAddedOrUpdatedOffencesForHearing() {
        final JsonObject statementOfOffence = createObjectBuilder()
                .add("title", STATEMENT_OF_OFFENCE_TITLE)
                .add("legislation", STATEMENT_OF_OFFENCE_LEGISLATION)
                .build();

        final JsonObject offence = createObjectBuilder()
                .add("id", OFFENCE_ID1.toString())
                .add("defendantId", DEFENDANT_ID1.toString())
                .add("offenceCode", PERSON_ID.toString())
                .add("startDate", OFFENCE_START_DATE)
                .add("endDate", OFFENCE_END_DATE)
                .add("statementOfOffence", statementOfOffence)
                .build();

        final JsonArrayBuilder offences = createArrayBuilder().add(offence);

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
                .withDefendants(Arrays.asList(createDomainDefendant()))
                .withCaseMarkers(Arrays.asList(CaseMarker.caseMarker()
                        .withId(fromString("6e1bef55-7e13-4615-b3ba-8663f4438e17"))
                        .withMarkerTypeCode("01")
                        .withMarkerTypeDescription("Murder")
                        .withMarkerTypeid(fromString("7e1bef55-7e13-4615-b3ba-8663f4438e17")).build()))
                .withShadowListed(of(Boolean.FALSE))
                .build();
    }

    private Defendant createDomainDefendant() {
        return Defendant.defendant()
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDatesToAvoid(of("wednesdays"))
                .withDefenceOrganisation(empty())
                .withFirstName(of("Harry"))
                .withLastName(of("Kane Junior"))
                .withHearingLanguageNeeds(of(HearingLanguageNeeds.ENGLISH))
                .withId(DEFENDANT_ID1)
                .withMasterDefendantId(Optional.of(DEFENDANT_ID1))
                .withCourtProceedingsInitiated(Optional.of(ZonedDateTimes.fromString("2020-03-05T14:24:03.148Z").withZoneSameInstant(ZoneId.of("UTC"))))
                .withOrganisationName(empty())
                .withSpecificRequirements(of("Screen"))
                .withIsYouth(empty())
                .withNationalityDescription(empty())
                .withAddress(of(Address
                        .address()
                        .withAddress1("22")
                        .withAddress2(of("Acacia Avenue"))
                        .withAddress3(of("Acacia Town"))
                        .withAddress4(of("Acacia City"))
                        .withAddress5(of("Acacia Country"))
                        .withPostcode(of("GIR 0AA"))
                        .build()))
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(OFFENCE_ID1)
                        .withOffenceCode("AAA")
                        .withStartDate("2018-01-01")
                        .withEndDate(of("2018-01-01"))
                        .withCount(1)
                        .withOrderIndex(0)
                        .withLaidDate(Optional.of("2019-05-01"))
                        .withOffenceWording("No Travel Card")
                        .withLaaApplnReference(empty())
                        .withSeedingHearing(empty())
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withWelshTitle("a title in Welsh")
                                .withWelshLegislation(of("legislation in Welsh"))
                                .withLegislation(of("legislation"))
                                .withTitle("a title")
                                .build())
                        .withShadowListed(of(Boolean.FALSE))
                        .build()))
                .build();
    }

    private Defendant createDomainDefendantForUpdateDefendant() {
        return Defendant.defendant()
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDefenceOrganisation(empty())
                .withHearingLanguageNeeds(empty())
                .withFirstName(of("Harry"))
                .withLastName(of("Kane Junior"))
                .withId(DEFENDANT_ID1)
                .withMasterDefendantId(Optional.of(DEFENDANT_ID1))
                .withCourtProceedingsInitiated(empty())
                .withOrganisationName(of("withOrganisationName"))
                .withSpecificRequirements(of("Screen"))
                .withOffences(emptyList())
                .withDefenceOrganisation(of("withOrganisationName"))
                .withIsYouth(empty())
                .build();
    }

    private Defendant createDomainDefendantForAddDefendantToCourtProceedings() {
        return Defendant.defendant()
                .withId(DEFENDANT_ID1)
                .withMasterDefendantId(Optional.of(DEFENDANT_ID1))
                .withCourtProceedingsInitiated(Optional.of(ZonedDateTimes.fromString("2019-01-01T00:00:00.000Z").withZoneSameInstant(ZoneId.of("UTC"))))
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDefenceOrganisation(empty())
                .withHearingLanguageNeeds(empty())
                .withFirstName(of("Harry"))
                .withLastName(of("Kane Junior"))
                .withOrganisationName(empty())
                .withSpecificRequirements(of("Screen"))
                .withDatesToAvoid(empty())
                .withDefenceOrganisation(empty())
                .withHearingLanguageNeeds(empty())
                .withProsecutionCaseId(CASE_ID)
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(OFFENCE_ID1)
                        .withOffenceCode("TFL123")
                        .withOffenceWording("TFL ticket dodged")
                        .withStartDate("2019-05-01")
                        .withEndDate(empty())
                        .withLaaApplnReference(empty())
                        .withLaidDate(Optional.of("2019-05-01"))
                        .withCount(1)
                        .withOrderIndex(0)
                        .withSeedingHearing(empty())
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withWelshTitle("TFL Ticket Dodger")
                                .withWelshLegislation(empty())
                                .withLegislation(empty())
                                .withTitle("TFL Ticket Dodger")
                                .build())
                        .withShadowListed(of(Boolean.FALSE))
                        .build()))
                .build();

    }

    private Defendant createDomainDefendantForUpdateDefendantsForHearing() {
        return Defendant.defendant()
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDefenceOrganisation(empty())
                .withFirstName(of("Harry"))
                .withHearingLanguageNeeds(empty())
                .withLastName(of("Kane Junior"))
                .withId(DEFENDANT_ID1)
                .withMasterDefendantId(Optional.of(DEFENDANT_ID1))
                .withCourtProceedingsInitiated(Optional.of(COURT_PROCEEDINGS_INITIATED))
                .withDatesToAvoid(empty())
                .withOrganisationName(of("withOrganisationName"))
                .withSpecificRequirements(of("Screen"))
                .withOffences(emptyList())
                .withDefenceOrganisation(of("withOrganisationName"))
                .withIsYouth(empty())
                .build();
    }

    private CourtApplication getNewCourtApplication() {
        return CourtApplication.courtApplication()
                .withLinkedCaseIds(singletonList(fromString("19e9d562-6abb-4871-bfb3-2d777aa90371")))
                .withParentApplicationId(fromString("9d9a431a-0f12-4386-878a-2bf6c4a0877e"))
                .withApplicationType("9vBchM49Go")
                .withId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withId(fromString("22b1078b-9430-4cef-ba46-eea40a129ca8"))
                        .withIsRespondent(false)
                        .withFirstName("David")
                        .withLastName("Dell")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(getAddress())
                        .build())
                .withRespondents(Collections.singletonList(ApplicantRespondent.applicantRespondent()
                        .withIsRespondent(true)
                        .withFirstName("Luise")
                        .withLastName("Miller")
                        .withId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(getAddress())
                        .build()))
                .withApplicationReference(Optional.of("REF-1"))
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withOffences(emptyList())
                .build();
    }

    @Test
    public void listingCommandHandlerShouldTriggerOffenceAddedEventsWithCustodyTimeLimitData() throws Exception {
        when(hearing.addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), anyListOf(Offence.class))).thenReturn(mock(Stream.class));
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        final JsonEnvelope commandEnvelope = addOffencesForHearingCommandEnvelopeWithCustodyTimeLimit();
        listingCommandHandler.addOffencesForHearing(commandEnvelope);

        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));
        verify(hearing, atMost(1)).addOffences(eq(CASE_ID), eq(DEFENDANT_ID1), domainOffencesCaptor.capture());

        final List<Offence> capturedDomainOffences = domainOffencesCaptor.getValue();

        final String expectedDomainOffences =
                "[\n" +
                        "  {\n" +
                        "    \"endDate\": \"2011-08-01\",\n" +
                        "    \"id\": \"" + UPDATED_OFFENCE_ID1 + "\",\n" +
                        "    \"offenceCode\": \"H8189\",\n" +
                        "    \"count\": 2,\n" +
                        "    \"orderIndex\": 0,\n" +
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
                        "    \"count\": 2,\n" +
                        "    \"orderIndex\": 1,\n" +
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
    public void shouldNotMakeAnyRequestsToPublishACourtListForACrownCourtWhenThereAreNone() {

        final JsonEnvelope commandEnvelope = generateEmptyCommandEnvelope();
        final JsonObject payload = getPayloadOfZeroCrownCourtCentres();
        givenThatWeSuccessfullyGetAllOfTheCrownCourtCentres(commandEnvelope, payload);

        listingCommandHandler.publishFinalCourtListsForAllCrownCourts(commandEnvelope);

        verifyZeroInteractions(publishCourtListRequestAggregate);

    }


    @Test
    public void shouldRequestPublicationOfACourtListEvenAfterOneFails() {

        fixClock(WEDNESDAY_28TH_NOVEMBER_2018.atStartOfDay().toInstant(ZoneOffset.UTC));
        final JsonEnvelope commandEnvelope = generateEmptyCommandEnvelope();
        final JsonObject payload = getPayloadOfMultipleCrownCourtCentres();
        givenThatWeSuccessfullyGetAllOfTheCrownCourtCentres(commandEnvelope, payload);
        givenThatWeSuccessfullyGetTheStreamForAnyPublishCourtRequest();
        givenThatThePublishCourtListRequestAggregateExists();
        givenThatPublicationOfTheFinalCourtListFailsToBeRequested(COURT_CENTRE_ID_ONE, THURSDAY_29TH_NOVEMBER_2018);
        givenThatPublicationOfTheFinalCourtListIsSuccessfullyRequested(COURT_CENTRE_ID_TWO, THURSDAY_29TH_NOVEMBER_2018);

        listingCommandHandler.publishFinalCourtListsForAllCrownCourts(commandEnvelope);

        verifyThatPublicationOfTheFinalCourtListWasRequested(COURT_CENTRE_ID_TWO, THURSDAY_29TH_NOVEMBER_2018);
    }


    @Test
    public void shouldRequestPublicationOfACourtListForAllCrownCourtsWhenItsSunday() {

        shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(
                SUNDAY_25TH_NOVEMBER_2018,
                MONDAY_26TH_NOVEMBER_2018);

    }

    @Test
    public void shouldRequestPublicationOfACourtListForAllCrownCourtsWhenItsMonday() {

        shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(
                MONDAY_26TH_NOVEMBER_2018,
                TUESDAY_27TH_NOVEMBER_2018);
    }

    @Test
    public void shouldRequestPublicationOfACourtListForAllCrownCourtsWhenItsTuesday() {

        shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(
                TUESDAY_27TH_NOVEMBER_2018,
                WEDNESDAY_28TH_NOVEMBER_2018);

    }

    @Test
    public void shouldRequestPublicationOfACourtListForAllCrownCourtsWhenItsWednesday() {

        shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(
                WEDNESDAY_28TH_NOVEMBER_2018,
                THURSDAY_29TH_NOVEMBER_2018);

    }


    @Test
    public void shouldRequestPublicationOfACourtListForAllCrownCourtsWhenItsThursday() {

        shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(
                THURSDAY_29TH_NOVEMBER_2018,
                FRIDAY_30TH_NOVEMBER_2018);
    }


    @Test
    public void shouldRequestPublicationOfACourtListForAllCrownCourtsWhenItsFriday() {

        shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(
                FRIDAY_30TH_NOVEMBER_2018,
                MONDAY_3rd_DECEMBER_2018);
    }

    @Test
    public void shouldRequestPublicationOfACourtListForAllCrownCourtsWhenItsSaturday() {

        shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(
                SATURDAY_1ST_DECEMBER_2018,
                MONDAY_3rd_DECEMBER_2018);
    }

    @Test
    public void shouldHandleCorrectHearingDaysWithoutCourtCentreCommand() throws EventStreamException, IOException {
        final String hearingId = randomUUID().toString();
        final String hearingDaysUpdatedJson = "[{\"durationMinutes\":15,\"endTime\":\"2020-09-24T13:15:00.000Z\",\"hearingDate\":\"2020-09-24\",\"sequence\":0,\"startTime\":\"2020-09-24T13:00:00.000Z\",\"courtRoomId\":\"b4562684-9209-3ec4-a544-7f80dabd94d8\",\"courtCentreId\":\"f8254db1-1683-483e-afb3-b87fde5a0a26\"}]";

        final JsonObject payloadToBeCorrected = createObjectBuilder()
                .add("id", hearingId)
                .add("hearingDays", objectMapper.readValue(hearingDaysUpdatedJson, JsonArray.class)).build();

        final JsonEnvelope commandEnvelope = envelopeFrom(metadataWithRandomUUID("listing.command.correct-hearing-days-without-court-centre"), payloadToBeCorrected);
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenReturn(mock(Stream.class));

        listingCommandHandler.correctHearingDaysWithoutCourtCentre(commandEnvelope);

        verify(hearing).raiseUpdateHearingInStagingHmi(any(Stream.class));

        verify(hearing).raiseHearingDaysWithoutCourtCentreCorrected(any(), any());

    }

    private void shouldRequestPublicationOfACourtListForAllCrownCourtsAsExpected(final LocalDate dayOfRequest, final LocalDate expectedListingDate) {

        fixClock(dayOfRequest.atStartOfDay().toInstant(ZoneOffset.UTC));
        final JsonEnvelope commandEnvelope = generateEmptyCommandEnvelope();
        final JsonObject payload = getPayloadOfMultipleCrownCourtCentres();
        givenThatWeSuccessfullyGetAllOfTheCrownCourtCentres(commandEnvelope, payload);
        givenThatWeSuccessfullyGetTheStreamForAnyPublishCourtRequest();
        givenThatThePublishCourtListRequestAggregateExists();
        givenThatPublicationOfTheFinalCourtListIsSuccessfullyRequested(COURT_CENTRE_ID_ONE, expectedListingDate);
        givenThatPublicationOfTheFinalCourtListIsSuccessfullyRequested(COURT_CENTRE_ID_TWO, expectedListingDate);

        listingCommandHandler.publishFinalCourtListsForAllCrownCourts(commandEnvelope);

        verifyThatPublicationOfTheFinalCourtListWasRequested(COURT_CENTRE_ID_ONE, expectedListingDate);
        verifyThatPublicationOfTheFinalCourtListWasRequested(COURT_CENTRE_ID_TWO, expectedListingDate);
        verifyNoMoreInteractions(publishCourtListRequestAggregate);
    }

    private JsonEnvelope generateEmptyCommandEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }

    private JsonObject getPayloadOfMultipleCrownCourtCentres() {
        return FileUtil.givenPayload("/test-data/listing.command.publish-court-lists-for-crown-courts_several.json");
    }

    private JsonObject getPayloadOfZeroCrownCourtCentres() {
        return FileUtil.givenPayload("/test-data/listing.command.publish-court-lists-for-crown-courts_none.json");
    }

    private void givenThatWeSuccessfullyGetAllOfTheCrownCourtCentres(final JsonEnvelope expectedCommandEnvelope, final JsonObject returnedPayload) {
        final JsonEnvelope responseEnvelope = createEnvelope(".", returnedPayload);
        when(referenceDataService.getAllCrownCourtCentres(expectedCommandEnvelope)).thenReturn(responseEnvelope);
    }

    private void givenThatThePublishCourtListRequestAggregateExists() {
        when(aggregateService.get(eventStream, PublishCourtListRequestAggregate.class)).thenReturn(publishCourtListRequestAggregate);
    }

    private void givenThatWeSuccessfullyGetTheStreamForAnyPublishCourtRequest() {
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
    }


    private void givenThatPublicationOfTheFinalCourtListIsSuccessfullyRequested(final UUID expectedCourtCentreId, final LocalDate expectedListingDate) {
        when(publishCourtListRequestAggregate
                .recordCourtListRequested(
                        any(UUID.class),
                        eq(expectedCourtCentreId),
                        eq(expectedListingDate),
                        eq(expectedListingDate),
                        eq(PublishCourtListType.FINAL),
                        eq(clock.now())))
                .thenReturn(events);

    }

    private void givenThatPublicationOfTheFinalCourtListFailsToBeRequested(final UUID expectedCourtCentreId, final LocalDate expectedListingDate) {
        when(publishCourtListRequestAggregate
                .recordCourtListRequested(
                        any(UUID.class),
                        eq(expectedCourtCentreId),
                        eq(expectedListingDate),
                        eq(expectedListingDate),
                        eq(PublishCourtListType.FINAL),
                        eq(clock.now())))
                .thenThrow(new RuntimeException("!"));

    }


    private void verifyThatPublicationOfTheFinalCourtListWasRequested(final UUID expectedCourtCentreId, final LocalDate expectedListingDate) {
        verify(publishCourtListRequestAggregate).recordCourtListRequested(
                any(UUID.class),
                eq(expectedCourtCentreId),
                eq(expectedListingDate),
                eq(expectedListingDate),
                eq(PublishCourtListType.FINAL),
                eq(clock.now()));
    }


    private JsonEnvelope addOffencesForHearingCommandEnvelopeWithCustodyTimeLimit() {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.add-offences-for-hearing-including-ctl.json").toString()
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
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void fixClock(final Instant instant) {
        this.clock = new StoppedClock(ZonedDateTime.ofInstant(instant, DateAndTimeUtils.BST));
        listingCommandHandler.setClock(this.clock);
    }

    @Test
    public void shouldModifyHearingCounsels() throws Exception {
        final JsonObject modifyHearingCounselsCommand = FileUtil.givenPayload("/test-data/listing.command.handler.modify-hearing-counsel.json");
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.handler.modify-hearing-counsel",
                modifyHearingCounselsCommand);

        when(eventSource.getStreamById(UUID.fromString(modifyHearingCounselsCommand.getString("hearingId"))))
                .thenReturn(eventStream);
        when(hearing.raiseUpdateHearingInStagingHmi(any(Stream.class))).thenAnswer(i -> i.getArguments()[0]);

        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);

        listingCommandHandler.modifyHearingCounsels(commandEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.event.hearing-counsel-modified")
                                .withCausationIds(commandEnvelope.metadata().id()), payload()
                                .isJson(allOf(
                                        withJsonPath("$.hearingId",
                                                equalTo(modifyHearingCounselsCommand.getString("hearingId"))),
                                        withJsonPath("$.action",
                                                equalTo(modifyHearingCounselsCommand.getString("action"))),
                                        withJsonPath("$.counselType",
                                                equalTo(modifyHearingCounselsCommand.getString("counselType"))),
                                        withJsonPath("$.payload",
                                                equalTo(modifyHearingCounselsCommand.getString("payload")))
                                )))
                )
        );
    }

    @Test
    public void shouldDeleteHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();

        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.delete-hearing", payload);
        listingCommandHandler.deleteHearing(commandEnvelope);

        verify(hearing, times(1)).markHearingAsDeleted(hearingId);
    }


    @Test
    public void shouldParseStringToXML() {
        final String authContextInfoValue = "<Sw:AuthContextInfo xmlns:Sw=\"urn:swift:saml:Sw.01\"><Network>abcdef</Network><SubjectDN>cn=%1,cn=uk2345678,ou=operators,o=scblgb21,o=swift</SubjectDN><PolicyOID>SWIFT_OID</PolicyOID></Sw:AuthContextInfo>";

        System.out.print(authContextInfoValue.substring(authContextInfoValue.indexOf("<SubjectDN>") + "<SubjectDN>".length(), authContextInfoValue.indexOf("</SubjectDN>")));

        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            //Parse the content to Document object
            Document doc = builder.parse(new InputSource(new StringReader(authContextInfoValue)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

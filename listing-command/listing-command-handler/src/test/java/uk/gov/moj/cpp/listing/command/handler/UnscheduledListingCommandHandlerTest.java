package uk.gov.moj.cpp.listing.command.handler;

import static java.time.ZonedDateTime.parse;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.COURT_CENTRE_ID;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.COURT_ROOM_ID;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.DEFAULT_DURATION;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.EARLIEST_START_TIME;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.HEARING_ID_1;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.HEARING_TYPE;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.JURISDICTION_TYPE;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.LISTING_DIRECTIONS;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.PROSECUTOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.REPORTING_RESTRICTIONS;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.SEED_HEARING_ID_1;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.SITTING_DAY;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.TYPE_OF_LIST;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_DURATION;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_END_DATE;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_START_DATE;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.createJudicalRoles;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.createdListedCase;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.createdListedCaseWithProsecutor;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.getCourtApplication;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.getCourtApplicationForApplicationOnly;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.listUnscheduledCourtHearingCommandEnvelope;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.listUnscheduledCourtHearingForApplicationsCommandEnvelope;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.listUnscheduledNextHearingCommandEnvelope;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.listUnscheduledNextHearingWithProsecutorCommandEnvelope;

import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.UnscheduledNextHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"squid:S1607"})
@ExtendWith(MockitoExtension.class)
public class UnscheduledListingCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingListed.class);
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new JsonObjectConvertersFactory().objectToJsonValueConverter();
    @Spy
    private final CommandToDomainConverter commandToDomainConverter = new CommandToDomainConverter();
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private UnscheduledListingCommandHandler unscheduledListingCommandHandler;
    @Mock
    private HearingTypeFactory hearingTypeFactory;
    @Mock
    private Hearing hearing;
    @Spy
    private SeedHearingAggregate seedHearingAggregate;

    @Mock
    private Stream<Object> events;

    @Spy
    private Clock clock = new StoppedClock(parse("2018-01-02T13:04:05+00:00[Europe/London]"));

    @Captor
    private ArgumentCaptor<List<HearingUnscheduledListingNeeds>> unscheduledHearingsArgumentCaptor;

    @BeforeEach
    public void setup() {
        EnveloperFactory.createEnveloperWithEvents(HearingListed.class, UnscheduledNextHearingRequested.class);
    }

    @Test
    public void shouldListUnscheduledCourtHearingAsExpected() throws EventStreamException {

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final JsonEnvelope commandEnvelope = listUnscheduledCourtHearingCommandEnvelope();

        final List<ListedCase> listedCases = List.of(createdListedCase());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();

        final CourtCentreDefaults courtCentreDefaults = createCourtCentreDefaults();

        final List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplication());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());

        unscheduledListingCommandHandler.handleListUnscheduledCourtHearing(commandEnvelope);

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(listedCases),
                eq(COURT_CENTRE_ID),
                eq(judicialRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(null),
                eq(courtCentreDefaults),
                eq(courtApplications),
                eq(courtApplicationPartyListingNeeds),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST));
    }

    @Test
    public void shouldListUnscheduledCourtHearingForApplications() throws EventStreamException {

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final JsonEnvelope commandEnvelope = listUnscheduledCourtHearingForApplicationsCommandEnvelope();

        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();

        final CourtCentreDefaults courtCentreDefaults = createCourtCentreDefaults();

        final List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplicationForApplicationOnly());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());
        unscheduledListingCommandHandler.handleListUnscheduledCourtHearing(commandEnvelope);

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(Collections.emptyList()),
                eq(COURT_CENTRE_ID),
                eq(judicialRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(null),
                eq(courtCentreDefaults),
                eq(courtApplications),
                eq(courtApplicationPartyListingNeeds),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST));
    }

    @Test
    public void shouldListUnscheduledNextHearings() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(), eq(SeedHearingAggregate.class))).thenReturn(seedHearingAggregate);

        final JsonEnvelope commandEnvelope = buildListUnscheduledNextHearingsEnvelope();

        unscheduledListingCommandHandler.handleListUnscheduledNextHearings(commandEnvelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(events.size(), is(1));
        final JsonEnvelope nextHearingRequestedEventProduced = events.get(0);

        assertThat(nextHearingRequestedEventProduced.metadata().name(), is("listing.events.unscheduled-next-hearing-requested"));
        assertThat(nextHearingRequestedEventProduced.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(HEARING_ID_1.toString()));
        assertThat(nextHearingRequestedEventProduced.payloadAsJsonObject().getString("hearingDay"), is(SITTING_DAY));

    }

    @Test
    public void shouldListUnscheduledNextHearing() throws EventStreamException {

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final JsonEnvelope commandEnvelope = listUnscheduledNextHearingCommandEnvelope();

        final List<ListedCase> listedCases = List.of(createdListedCase());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();
        final CourtCentreDefaults courtCentreDefaults = createCourtCentreDefaults();

        unscheduledListingCommandHandler.handleListUnscheduledNextHearing(commandEnvelope);

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(listedCases),
                eq(COURT_CENTRE_ID),
                eq(judicialRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(null),
                eq(courtCentreDefaults),
                anyList(),
                anyList(),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST));
    }

    @Test
    public void shouldListUnscheduledNextHearingWithProsecutor() throws EventStreamException {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final JsonEnvelope commandEnvelope = listUnscheduledNextHearingWithProsecutorCommandEnvelope();

        final List<ListedCase> listedCases = List.of(createdListedCaseWithProsecutor());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();
        final CourtCentreDefaults courtCentreDefaults = createCourtCentreDefaults();

        unscheduledListingCommandHandler.handleListUnscheduledNextHearing(commandEnvelope);

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(listedCases),
                eq(COURT_CENTRE_ID),
                eq(judicialRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(null),
                eq(courtCentreDefaults),
                anyList(),
                anyList(),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST));
    }

    private JsonEnvelope buildListUnscheduledNextHearingsEnvelope() {
        final ListUnscheduledNextHearingsEnriched listUnscheduledNextHearingsEnriched = listUnscheduledNextHearingsCommandPayload();
        final JsonObject commandPayload = createObjectBuilder().build();
        when(jsonObjectConverter.convert(commandPayload, ListUnscheduledNextHearingsEnriched.class)).thenReturn(listUnscheduledNextHearingsEnriched);
        return createEnvelope("listing.command.list-unscheduled-next-hearings-enriched", commandPayload);
    }

    private ListUnscheduledNextHearingsEnriched listUnscheduledNextHearingsCommandPayload() {
        return ListUnscheduledNextHearingsEnriched.listUnscheduledNextHearingsEnriched()
                .withHearings(List.of(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withId(HEARING_ID_1).build()))
                .withCourtCentresDetails(List.of((CourtCentreDetails.courtCentreDetails()
                        .withId(COURT_CENTRE_ID)
                        .withDefaultDuration(6)
                        .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                        .build())))
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(SEED_HEARING_ID_1)
                        .withSittingDay(SITTING_DAY)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .build())
                .build();
    }

    private CourtCentreDefaults createCourtCentreDefaults() {
        return CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(Integer.valueOf(DEFAULT_DURATION))
                .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                .withCourtCentreId(COURT_CENTRE_ID)
                .build();
    }

}
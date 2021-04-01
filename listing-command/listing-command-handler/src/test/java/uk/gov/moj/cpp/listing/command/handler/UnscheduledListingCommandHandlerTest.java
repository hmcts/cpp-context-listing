package uk.gov.moj.cpp.listing.command.handler;

import static java.time.ZonedDateTime.parse;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.TYPE_OF_LIST;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_DURATION;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_END_DATE;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_START_DATE;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.createJudicalRoles;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.createdListedCase;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.getCourtApplication;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.getCourtApplicationForApplicationOnly;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.listUnscheduledCourtHearingCommandEnvelope;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.listUnscheduledCourtHearingForApplicationsCommandEnvelope;

import uk.gov.justice.listing.events.HearingListed;
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
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
@SuppressWarnings({"squid:S1607"})
@RunWith(MockitoJUnitRunner.class)
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

    @Mock
    private Stream<Object> events;

    @Spy
    private Clock clock = new StoppedClock(parse("2018-01-02T13:04:05+00:00[Europe/London]"));

    @Before
    public void setup() {

        this.clock = new StoppedClock(ZonedDateTime.ofInstant(Instant.now(), DateAndTimeUtils.BST));

        EnveloperFactory.createEnveloperWithEvents(HearingListed.class);

    }

    @Test
    public void shouldListUnscheduledCourtHearingAsExpected() throws EventStreamException {

        final JsonEnvelope commandEnvelope = listUnscheduledCourtHearingCommandEnvelope();

        final LocalDate endDate = null;

        final List<ListedCase> listedCases = Arrays.asList(createdListedCase());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicalRoles = createJudicalRoles();

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

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));
        when(hearing.listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(listedCases),
                eq(COURT_CENTRE_ID),
                eq(judicalRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(endDate),
                eq(courtCentreDefaults),
                eq(courtApplications),
                eq(courtApplicationPartyListingNeeds),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE)),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST)
        )).thenReturn(events);

        unscheduledListingCommandHandler.handleListUnscheduledCourtHearing(commandEnvelope);

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(listedCases),
                eq(COURT_CENTRE_ID),
                eq(judicalRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(endDate),
                eq(courtCentreDefaults),
                eq(courtApplications),
                eq(courtApplicationPartyListingNeeds),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE)),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST));


    }

    @Test
    public void shouldListUnscheduledCourtHearingForApplications() throws EventStreamException {

        final JsonEnvelope commandEnvelope = listUnscheduledCourtHearingForApplicationsCommandEnvelope();

        final LocalDate endDate = null;

        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicalRoles = createJudicalRoles();

        final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(Integer.valueOf(DEFAULT_DURATION))
                .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                .withCourtCentreId(COURT_CENTRE_ID)
                .build();

        final List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplicationForApplicationOnly());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));
        when(hearing.listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(Collections.emptyList()),
                eq(COURT_CENTRE_ID),
                eq(judicalRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(endDate),
                eq(courtCentreDefaults),
                eq(courtApplications),
                eq(courtApplicationPartyListingNeeds),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE)),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST)
        )).thenReturn(events);

        unscheduledListingCommandHandler.handleListUnscheduledCourtHearing(commandEnvelope);

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                eq(Collections.emptyList()),
                eq(COURT_CENTRE_ID),
                eq(judicalRoles),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(endDate),
                eq(courtCentreDefaults),
                eq(courtApplications),
                eq(courtApplicationPartyListingNeeds),
                eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE)),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST));
    }
}
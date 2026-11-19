package uk.gov.moj.cpp.listing.command.hearing.handler;

import static java.time.ZonedDateTime.parse;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createArrayBuilder;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.createEnvelope;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.verifyAppendAndGetArgumentFrom;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.listUnscheduledNextHearingCommandEnvelopeFor;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.COURT_CENTRE_ID;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.COURT_ROOM_ID;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.DEFAULT_DURATION;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.EARLIEST_START_TIME;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.HEARING_ID_1;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.HEARING_ID_2;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.HEARING_TYPE;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.JURISDICTION_TYPE;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.LISTING_DIRECTIONS;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.PROSECUTOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.REPORTING_RESTRICTIONS;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.SEED_HEARING_ID_1;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.SITTING_DAY;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.TYPE_OF_LIST;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_DURATION;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_END_DATE;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.WEEK_COMMENCING_START_DATE;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.createJudicalRoles;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.createdListedCase;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.createdListedCaseWithProsecutor;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.getCourtApplication;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.getCourtApplicationForApplicationOnly;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.listUnscheduledCourtHearingCommandEnvelope;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.listUnscheduledCourtHearingForApplicationsCommandEnvelope;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.listUnscheduledNextHearingCommandEnvelope;
import static uk.gov.moj.cpp.listing.command.hearing.handler.UnscheduledListingCommandBuilder.listUnscheduledNextHearingWithProsecutorCommandEnvelope;

import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.UnscheduledNextHearingRequested;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonValueConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.AggregateService;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventSource;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventStream;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.handler.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

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
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter();
    @Spy
    private final CommandToDomainConverter commandToDomainConverter = new CommandToDomainConverter();
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;

    @Mock
    private uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow hearingFlow;

    @BeforeEach
    void bridgeMigratedAggregates() {
        uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.HearingFlowTestBridge.bridge(hearingFlow, eventSource, aggregateService);
    }
    @InjectMocks
    private HearingCommandHandler unscheduledHearingCommandHandler;
    @InjectMocks
    private SeedHearingCommandHandler seedHearingCommandHandler;
    @Spy
    private uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules seedHearingAggregate;
    @Mock
    private HearingTypeFactory hearingTypeFactory;
    @Mock
    private HearingRules hearing;

    @Mock
    private Stream<Object> events;


    @Captor
    private ArgumentCaptor<List<HearingUnscheduledListingNeeds>> unscheduledHearingsArgumentCaptor;

    @BeforeEach
    public void setup() {
    }

    @Test
    public void shouldListUnscheduledCourtHearingAsExpected() {

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap()).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final Envelope commandEnvelope = listUnscheduledCourtHearingCommandEnvelope();

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

        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(commandEnvelope);

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
                eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    public void shouldListUnscheduledCourtHearingForApplications() {

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap()).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final Envelope commandEnvelope = listUnscheduledCourtHearingForApplicationsCommandEnvelope();

        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();

        final CourtCentreDefaults courtCentreDefaults = createCourtCentreDefaults();

        final List<CourtApplication> courtApplications = Collections.singletonList(getCourtApplicationForApplicationOnly());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = Collections.singletonList(
                CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withCourtApplicationPartyId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                        .withHearingLanguageNeeds(HearingLanguageNeeds.ENGLISH)
                        .build());
        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(commandEnvelope);

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
                eq(TYPE_OF_LIST), eq(null));
    }



    /**
     * LPT-2405 — the unscheduled command carries tier / list type as a sibling of the hearing,
     * because the hearing carrier itself is owned by coredomain. The command names one hearing,
     * so it carries at most one detail; the handler hands the aggregate the same value object
     * the scheduled path uses.
     */

    /**
     * The enriched command lists every next hearing being seeded, and carries a detail only for
     * the Crown Court trials among them. Each hearing gets its own event, so a detail must reach
     * the hearing it is keyed to and no other — the calendar would otherwise show a tier the
     * court never set for that hearing.
     */

    /**
     * The seed aggregate puts the inherited values on the requested event, so they survive the
     * hop to the per-hearing command.
     */


    private Envelope buildListUnscheduledNextHearingsEnvelope() {
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

    // SPRDT-807 — the handler used to call map.get(...) which returns null for an unmapped type.
    // After the fix it delegates to HearingDurationDefaults.resolveHearingTypeDuration which
    // falls back to DEFAULT_MIN=20. These tests lock in the guarantee for every path through
    // listUnscheduledHearing (both court-hearing and next-hearing enriched entry points converge here).
    @Test
    void shouldFallBackToDefaultMinWhenHearingTypeMissingFromDurationMap() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        // Intentionally empty map — real reference data could fail to resolve a new hearing type (e.g. Crown Plea)
        when(hearingTypeFactory.getHearingTypesIdDurationMap()).thenReturn(Collections.emptyMap());

        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(listUnscheduledCourtHearingCommandEnvelope());

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1),
                eq(HEARING_TYPE),
                anyList(),
                eq(COURT_CENTRE_ID),
                anyList(),
                eq(COURT_ROOM_ID),
                eq(LISTING_DIRECTIONS),
                eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID),
                eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)),
                eq(null),
                any(CourtCentreDefaults.class),
                anyList(),
                anyList(),
                eq(20),
                eq(of(WEEK_COMMENCING_START_DATE)),
                eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)),
                eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    void shouldFallBackToDefaultMinWhenMappedDurationIsZero() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap())
                .thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 0));

        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(listUnscheduledCourtHearingCommandEnvelope());

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(null), any(CourtCentreDefaults.class),
                anyList(), anyList(), eq(20),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    void shouldPassThroughMappedDurationWhenValid() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap())
                .thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 150));

        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(listUnscheduledCourtHearingCommandEnvelope());

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(null), any(CourtCentreDefaults.class),
                anyList(), anyList(), eq(150),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST), eq(null));
    }

    // Manage HearingRules on the unscheduled path posts a user-entered estimatedMinutes on
    // HearingUnscheduledListingNeeds. Previously the handler discarded it and always wrote
    // the hearing-type default into the HearingListed event. The fix prefers the user value
    // when meaningful (>1) and only falls back to the hearing-type / DEFAULT_MIN ladder when
    // the user did not supply a usable value, preserving the SPRDT-806/807 "never 0 / never null"
    // guarantee.

    @Test
    void shouldUseUserEnteredEstimatedMinutesOverHearingTypeDurationOnCourtHearing() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap())
                .thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(
                withEstimatedMinutesOnHearings(listUnscheduledCourtHearingCommandEnvelope(), 90));

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(null), any(CourtCentreDefaults.class),
                anyList(), anyList(), eq(90),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    void shouldUseUserEnteredEstimatedMinutesEvenWhenHearingTypeMissingFromDurationMap() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap())
                .thenReturn(Collections.emptyMap());

        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(
                withEstimatedMinutesOnHearings(listUnscheduledCourtHearingCommandEnvelope(), 640));

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(null), any(CourtCentreDefaults.class),
                anyList(), anyList(), eq(640),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    void shouldFallBackToHearingTypeDurationWhenUserEnteredEstimatedMinutesIsOne() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap())
                .thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        // 1 is treated as not-a-real-duration by HearingDurationDefaults.coerceToValidDuration,
        // so we fall back through to the hearing-type default. Mirrors SPRDT-807 invariant.
        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(
                withEstimatedMinutesOnHearings(listUnscheduledCourtHearingCommandEnvelope(), 1));

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(null), any(CourtCentreDefaults.class),
                anyList(), anyList(), eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    void shouldFallBackToHearingTypeDurationWhenUserEnteredEstimatedMinutesIsZero() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap())
                .thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        unscheduledHearingCommandHandler.handleListUnscheduledCourtHearing(
                withEstimatedMinutesOnHearings(listUnscheduledCourtHearingCommandEnvelope(), 0));

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(null), any(CourtCentreDefaults.class),
                anyList(), anyList(), eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST), eq(null));
    }


    /**
     * Inject {@code estimatedMinutes} into every hearing of an envelope whose payload has a
     * top-level {@code hearings} array (list-unscheduled-court-hearing path).
     */
    private static Envelope withEstimatedMinutesOnHearings(final Envelope envelope, final int estimatedMinutes) {
        final JsonObject payload = envelope.payload();
        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();
        payload.getJsonArray("hearings").forEach(hearingValue -> hearingsBuilder.add(
                copyWithEstimatedMinutes((JsonObject) hearingValue, estimatedMinutes)));
        final JsonObjectBuilder updatedPayload = createObjectBuilder();
        payload.forEach((key, value) -> {
            if (!"hearings".equals(key)) {
                updatedPayload.add(key, value);
            }
        });
        updatedPayload.add("hearings", hearingsBuilder.build());
        return createEnvelope(envelope.metadata().name(), updatedPayload.build());
    }

    /**
     * Inject {@code estimatedMinutes} into a single hearing whose payload has a top-level
     * {@code hearing} object (list-unscheduled-next-hearing path).
     */
    private static Envelope withEstimatedMinutesOnHearing(final Envelope envelope, final int estimatedMinutes) {
        final JsonObject payload = envelope.payload();
        final JsonObject hearing = payload.getJsonObject("hearing");
        final JsonObject updatedHearing = copyWithEstimatedMinutes(hearing, estimatedMinutes);
        final JsonObjectBuilder updatedPayload = createObjectBuilder();
        payload.forEach((key, value) -> {
            if (!"hearing".equals(key)) {
                updatedPayload.add(key, value);
            }
        });
        updatedPayload.add("hearing", updatedHearing);
        return createEnvelope(envelope.metadata().name(), updatedPayload.build());
    }

    private static JsonObject copyWithEstimatedMinutes(final JsonObject hearing, final int estimatedMinutes) {
        final JsonObjectBuilder builder = createObjectBuilder();
        hearing.forEach((key, value) -> {
            if (!"estimatedMinutes".equals(key)) {
                builder.add(key, value);
            }
        });
        builder.add("estimatedMinutes", estimatedMinutes);
        return builder.build();
    }

    // ---- P2.3.3: the JEE UnscheduledListingCommandHandlerTest cases of the next-hearing commands ----

    @Test
    public void shouldCarryPtphDetailOntoTheUnscheduledNextHearingRequestedEvent() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(), eq(SeedHearingRules.class))).thenReturn(seedHearingAggregate);

        final ListUnscheduledNextHearingsEnriched payload = ListUnscheduledNextHearingsEnriched.listUnscheduledNextHearingsEnriched()
                .withHearings(List.of(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withId(HEARING_ID_1).build()))
                .withCourtCentresDetails(List.of(CourtCentreDetails.courtCentreDetails()
                        .withId(COURT_CENTRE_ID)
                        .withDefaultDuration(6)
                        .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                        .build()))
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(SEED_HEARING_ID_1)
                        .withSittingDay(SITTING_DAY)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .build())
                .withPtphDetails(List.of(uk.gov.justice.listing.commands.HearingPtphDetail.hearingPtphDetail()
                        .withHearingId(HEARING_ID_1)
                        .withTier("TIER_3")
                        .withListType("TYPE_1_FIXED")
                        .withKeyReason("Vulnerable witness")
                        .build()))
                .build();

        final JsonObject commandPayload = createObjectBuilder().build();
        when(jsonObjectConverter.convert(commandPayload, ListUnscheduledNextHearingsEnriched.class)).thenReturn(payload);

        seedHearingCommandHandler.handleListUnscheduledNextHearings(
                createEnvelope("listing.command.list-unscheduled-next-hearings-enriched", commandPayload));

        final List<Envelope> events = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(events.size(), is(1));
        final JsonObject emitted = events.get(0).payload().getJsonObject("ptphDetail");
        assertThat(emitted.getString("tier"), is("TIER_3"));
        assertThat(emitted.getString("listType"), is("TYPE_1_FIXED"));
        assertThat(emitted.getString("keyReason"), is("Vulnerable witness"));
        assertThat(emitted.containsKey("hearingId"), is(false));
    }

    @Test
    public void shouldInheritPtphDetailOntoTheUnscheduledHearing() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap()).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final Envelope commandEnvelope = listUnscheduledNextHearingCommandEnvelopeFor(
                "/test-data/listing.command.list-unscheduled-next-hearing-with-ptph-detail.json");

        unscheduledHearingCommandHandler.handleListUnscheduledNextHearing(commandEnvelope);

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS), eq(parse(EARLIEST_START_TIME)),
                eq(null), any(CourtCentreDefaults.class), anyList(), anyList(), eq(30),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST),
                eq(new PtphDetail("TIER_3", "TYPE_1_FIXED", "Vulnerable witness")));
    }

    @Test
    public void shouldListUnscheduledNextHearing() {

        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap()).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final Envelope commandEnvelope = listUnscheduledNextHearingCommandEnvelope();

        final List<ListedCase> listedCases = List.of(createdListedCase());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();
        final CourtCentreDefaults courtCentreDefaults = createCourtCentreDefaults();

        unscheduledHearingCommandHandler.handleListUnscheduledNextHearing(commandEnvelope);

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
                eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    public void shouldListUnscheduledNextHearingWithProsecutor() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap()).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        final Envelope commandEnvelope = listUnscheduledNextHearingWithProsecutorCommandEnvelope();

        final List<ListedCase> listedCases = List.of(createdListedCaseWithProsecutor());
        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles = createJudicalRoles();
        final CourtCentreDefaults courtCentreDefaults = createCourtCentreDefaults();

        unscheduledHearingCommandHandler.handleListUnscheduledNextHearing(commandEnvelope);

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
                eq(TYPE_OF_LIST), eq(null));
    }

    @Test
    public void shouldListUnscheduledNextHearings() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(), eq(SeedHearingRules.class))).thenReturn(seedHearingAggregate);

        final Envelope commandEnvelope = buildListUnscheduledNextHearingsEnvelope();

        seedHearingCommandHandler.handleListUnscheduledNextHearings(commandEnvelope);

        final List<Envelope> events = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(events.size(), is(1));
        final Envelope nextHearingRequestedEventProduced = events.get(0);

        assertThat(nextHearingRequestedEventProduced.metadata().name(), is("listing.events.unscheduled-next-hearing-requested"));
        assertThat(nextHearingRequestedEventProduced.payload().getJsonObject("hearing").getString("id"), is(HEARING_ID_1.toString()));
        assertThat(nextHearingRequestedEventProduced.payload().getString("hearingDay"), is(SITTING_DAY));

    }

    @Test
    public void shouldOnlyCarryPtphDetailOntoTheHearingItIsKeyedTo() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(), eq(SeedHearingRules.class))).thenReturn(seedHearingAggregate);

        final ListUnscheduledNextHearingsEnriched payload = ListUnscheduledNextHearingsEnriched.listUnscheduledNextHearingsEnriched()
                .withHearings(List.of(
                        HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds().withId(HEARING_ID_1).build(),
                        HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds().withId(HEARING_ID_2).build()))
                .withCourtCentresDetails(List.of(CourtCentreDetails.courtCentreDetails()
                        .withId(COURT_CENTRE_ID)
                        .withDefaultDuration(6)
                        .withDefaultStartTime(LocalTime.parse(DEFAULT_START_TIME))
                        .build()))
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(SEED_HEARING_ID_1)
                        .withSittingDay(SITTING_DAY)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .build())
                .withPtphDetails(List.of(uk.gov.justice.listing.commands.HearingPtphDetail.hearingPtphDetail()
                        .withHearingId(HEARING_ID_2)
                        .withTier("TIER_3")
                        .withListType("TYPE_1_FIXED")
                        .build()))
                .build();

        final JsonObject commandPayload = createObjectBuilder().build();
        when(jsonObjectConverter.convert(commandPayload, ListUnscheduledNextHearingsEnriched.class)).thenReturn(payload);

        seedHearingCommandHandler.handleListUnscheduledNextHearings(
                createEnvelope("listing.command.list-unscheduled-next-hearings-enriched", commandPayload));

        final List<Envelope> events = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(events.size(), is(2));

        final JsonObject withoutDetail = events.get(0).payload();
        assertThat(withoutDetail.getJsonObject("hearing").getString("id"), is(HEARING_ID_1.toString()));
        assertThat(withoutDetail.containsKey("ptphDetail"), is(false));

        final JsonObject withDetail = events.get(1).payload();
        assertThat(withDetail.getJsonObject("hearing").getString("id"), is(HEARING_ID_2.toString()));
        assertThat(withDetail.getJsonObject("ptphDetail").getString("tier"), is("TIER_3"));
    }

    @Test
    void shouldUseUserEnteredEstimatedMinutesOnNextHearing() {
        when(eventSource.getStreamById(HEARING_ID_1)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearingTypeFactory.getHearingTypesIdDurationMap())
                .thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));

        unscheduledHearingCommandHandler.handleListUnscheduledNextHearing(
                withEstimatedMinutesOnHearing(listUnscheduledNextHearingCommandEnvelope(), 240));

        verify(hearing).listUnscheduled(
                eq(HEARING_ID_1), eq(HEARING_TYPE), anyList(), eq(COURT_CENTRE_ID), anyList(),
                eq(COURT_ROOM_ID), eq(LISTING_DIRECTIONS), eq(JURISDICTION_TYPE),
                eq(PROSECUTOR_DATES_TO_AVOID), eq(REPORTING_RESTRICTIONS),
                eq(parse(EARLIEST_START_TIME)), eq(null), any(CourtCentreDefaults.class),
                anyList(), anyList(), eq(240),
                eq(of(WEEK_COMMENCING_START_DATE)), eq(of(WEEK_COMMENCING_END_DATE.minusDays(1))),
                eq(of(WEEK_COMMENCING_DURATION)), eq(TYPE_OF_LIST), eq(null));
    }
}

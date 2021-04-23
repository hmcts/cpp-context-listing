package uk.gov.moj.cpp.listing.command.handler;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearing;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class UnscheduledListingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnscheduledListingCommandHandler.class);

    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Inject
    private CommandToDomainConverter commandToDomainConverter;
    @Inject
    private HearingTypeFactory hearingTypeFactory;

    @SuppressWarnings({"squid:S3655", "squid:S1188"})
    @Handles("listing.command.list-unscheduled-court-hearing-enriched")
    public void handleListUnscheduledCourtHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final ListUnscheduledCourtHearingEnriched listCourtHearingEnriched = jsonObjectConverter.convert(payload, ListUnscheduledCourtHearingEnriched.class);

        LOGGER.info("'listing.command.list-unscheduled-court-hearing-enriched' listCourtHearing: {}", listCourtHearingEnriched);


        final List<HearingUnscheduledListingNeeds> listCourtHearing = listCourtHearingEnriched.getHearings();
        final Map<UUID, CourtCentreDetails> courtCentres = listCourtHearingEnriched.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));

        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(command);

        for (final HearingUnscheduledListingNeeds commandHearing : listCourtHearing) {
            listUnscheduledHearing(command, hearingTypesIdDurationMap, commandHearing, courtCentres);
        }
    }

    @Handles("listing.command.list-unscheduled-next-hearings-enriched")
    public void handleListUnscheduledNextHearings(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-unscheduled-next-hearings-enriched' listCourtHearing: {}", command.toObfuscatedDebugString());
        }

        final ListUnscheduledNextHearingsEnriched listUnscheduledNextHearingsEnriched = jsonObjectConverter.convert(command.payloadAsJsonObject(), ListUnscheduledNextHearingsEnriched.class);

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds = listUnscheduledNextHearingsEnriched.getHearings();
        final List<CourtCentreDefaults> courtCentresDetails = listUnscheduledNextHearingsEnriched.getCourtCentresDetails().stream()
                .map(courtCentreDetails -> CourtCentreDefaults.courtCentreDefaults()
                        .withCourtCentreId(courtCentreDetails.getId())
                        .withDefaultDuration(courtCentreDetails.getDefaultDuration())
                        .withDefaultStartTime(courtCentreDetails.getDefaultStartTime())
                        .build())
                .collect(toList());

        final SeedingHearing seedingHearing = listUnscheduledNextHearingsEnriched.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();

        updateSeedHearingEventStream(command, seedingHearingId, (SeedHearingAggregate seedHearingAggregate) -> seedHearingAggregate.requestNextUnscheduledHearings(unscheduledListingNeeds, hearingDay, courtCentresDetails));
    }

    @Handles("listing.command.list-unscheduled-next-hearing")
    public void handleListUnscheduledNextHearing(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-unscheduled-next-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(command);
        final ListUnscheduledNextHearing listUnscheduledNextHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), ListUnscheduledNextHearing.class);
        final HearingUnscheduledListingNeeds commandHearing = listUnscheduledNextHearing.getHearing();
        final Map<UUID, CourtCentreDetails> courtCentres = listUnscheduledNextHearing.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));

        listUnscheduledHearing(command, hearingTypesIdDurationMap, commandHearing, courtCentres);
    }

    @SuppressWarnings({"squid:S3655", "squid:S1188"})
    private void listUnscheduledHearing(final JsonEnvelope command, final Map<String, Integer> hearingTypesIdDurationMap, final HearingUnscheduledListingNeeds commandHearing, final Map<UUID, CourtCentreDetails> courtCentres) throws EventStreamException {
        final Optional<LocalDate> weekCommencingStartDate = commandToDomainConverter.getWeekCommencingStartDate(commandHearing);
        final Optional<Integer> weekCommencingDurationInWeeks = commandToDomainConverter.getWeekCommencingDurationInWeeks(commandHearing);
        final Optional<LocalDate> weekCommencingEndDate = commandToDomainConverter.getWeekCommencingEndDate(weekCommencingStartDate, weekCommencingDurationInWeeks);

        updateHearingEventStream(command, commandHearing.getId(), (Hearing hearing) -> hearing.listUnscheduled(
                commandHearing.getId(),
                commandToDomainConverter.buildHearingType(commandHearing.getType()),
                commandToDomainConverter.mapToListedCases(commandHearing, commandHearing.getProsecutionCases()),
                commandHearing.getCourtCentre().getId(),
                commandToDomainConverter.getJudicialRoles(commandHearing),
                commandHearing.getCourtCentre().getRoomId().orElse(null),
                commandHearing.getListingDirections().orElse(null),
                commandToDomainConverter.getJurisdictionType(commandHearing),
                commandHearing.getProsecutorDatesToAvoid().orElse(null),
                commandHearing.getReportingRestrictionReason().orElse(null),
                CommandToDomainConverter.extractStartDate(commandHearing),
                commandHearing.getEndDate().isPresent() ? LocalDate.parse(commandHearing.getEndDate().get()) : null,
                commandToDomainConverter.getCourtCentreDefaults(courtCentres, commandHearing),
                commandToDomainConverter.getCourtApplications(commandHearing),
                commandToDomainConverter.getCourtApplicationPartyListingNeeds(commandHearing),
                hearingTypesIdDurationMap.get(commandHearing.getType().getId().toString()),
                weekCommencingStartDate,
                weekCommencingEndDate,
                weekCommencingDurationInWeeks,
                commandToDomainConverter.convertTypeOfList(commandHearing.getTypeOfList())
        ));
    }

    private void updateHearingEventStream(final JsonEnvelope command, final UUID hearingId,
                                          final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final Hearing hearing = aggregateService.get(eventStream, Hearing.class);

        final Stream<Object> events = aggregatorFunction.apply(hearing);

        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(command)));
    }

    private void updateSeedHearingEventStream(final JsonEnvelope command, final UUID seedHearingId,
                                              final Function<SeedHearingAggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(seedHearingId);
        final SeedHearingAggregate seedHearingAggregate = aggregateService.get(eventStream, SeedHearingAggregate.class);

        final Stream<Object> events = aggregatorFunction.apply(seedHearingAggregate);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

}

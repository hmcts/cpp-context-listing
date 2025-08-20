package uk.gov.moj.cpp.listing.command.handler;


import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.courts.ChangeNextHearingDay;
import uk.gov.justice.listing.courts.DeleteNextHearings;
import uk.gov.justice.listing.courts.DeleteSeededHearing;
import uk.gov.justice.listing.courts.ListNextHearing;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.RemoveOffencesFromExistingHearing;
import uk.gov.justice.listing.courts.RemoveSelectedOffencesFromExistingHearing;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.HearingListingNeedsConverterCommandToCore;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188"})
public class ListNextHearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListNextHearingCommandHandler.class);

    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Inject
    private CommandToDomainConverter commandToDomainConverter;
    @Inject
    private HearingListingNeedsConverterCommandToCore hearingListingNeedsConverterCommandToCore; //LPT-1090 check if we still need this
    @Inject
    private HearingSlotsService hearingSlotsService;
    @SuppressWarnings("squid:S3655")
    @Handles("listing.command.list-next-hearings-enriched-v2")
    public void listNextHearings(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-next-hearings-enriched' received with payload {}", command.toObfuscatedDebugString());
        }

        final JsonObject payload = command.payloadAsJsonObject();
        final ListNextHearingsEnrichedV2 listNextHearingsEnriched = jsonObjectConverter.convert(payload, ListNextHearingsEnrichedV2.class);
        final SeedingHearing seedingHearing = listNextHearingsEnriched.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();
        final List<uk.gov.justice.listing.commands.HearingListingNeeds> hearings = listNextHearingsEnriched.getListNextHearings().getHearings().stream().map(hearingListingNeedsConverterCommandToCore::convert).collect(toList());
        final String adjournedFromDate = listNextHearingsEnriched.getAdjournedFromDate();
        final List<CourtCentreDefaults> courtCentresDetails = listNextHearingsEnriched.getCourtCentresDetails().stream()
                .map(courtCentreDetails -> CourtCentreDefaults.courtCentreDefaults()
                        .withCourtCentreId(courtCentreDetails.getId())
                        .withDefaultDuration(courtCentreDetails.getDefaultDuration())
                        .withDefaultStartTime(courtCentreDetails.getDefaultStartTime())
                        .build())
                .toList();
        final List<UUID> shadowListedOffences = listNextHearingsEnriched.getListNextHearings().getShadowListedOffences();
        updateSeedAggregateEventStream(command, StringUtils.EMPTY, seedingHearingId, (SeedHearingAggregate seedHearingAggregate) ->
                seedHearingAggregate.requestNextHearings(hearings, hearingDay, courtCentresDetails, ofNullable(adjournedFromDate), shadowListedOffences));

    }


    @SuppressWarnings("squid:S3655")
    @Handles("listing.command.list-next-hearing")
    public void listNextCourtHearing(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-next-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final ListNextHearing listNextHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), ListNextHearing.class);
        final HearingListingNeeds commandHearing = listNextHearing.getHearing();
        final Map<UUID, CourtCentreDetails> courtCentres = listNextHearing.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));

        List<NonDefaultDay> nonDefaultDaysList = new ArrayList<>();

        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = commandToDomainConverter.convert(commandHearing, convertNonDefaultDaysToDomain(nonDefaultDaysList), listNextHearing.getShadowListedOffences());

        final CourtCentreDetails courtCentre = courtCentres.get(domainHearing.getCourtCentreId());

        final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(courtCentre.getDefaultDuration())
                .withDefaultStartTime(courtCentre.getDefaultStartTime())
                .withCourtCentreId(courtCentre.getId())
                .build();
        final String adjournedFromDate = listNextHearing.getAdjournedFromDate();

        final UUID finalBookingReference = commandHearing.getBookingReference();
        final boolean isSlotsBooked = isNotEmpty(commandHearing.getBookedSlots());
        updateHearingEventStream(command, commandHearing.getId(), (Hearing hearing) -> {
            final Stream<Object> listingEvents = hearing.list(domainHearing.getId(),
                    domainHearing.getType(),
                    domainHearing.getEstimatedMinutes(),
                    domainHearing.getEstimatedDuration(),
                    domainHearing.getListedCases(), domainHearing.getCourtCentreId(),
                    domainHearing.getJudiciary(),
                    domainHearing.getCourtRoomId().orElse(null),
                    domainHearing.getListingDirections().orElse(null),
                    domainHearing.getJurisdictionType(),
                    domainHearing.getProsecutorDatesToAvoid().orElse(null),
                    domainHearing.getReportingRestrictionReason().orElse(null),
                    domainHearing.getStartDateTime(),
                    domainHearing.getEndDate().orElse(null),
                    courtCentreDefaults,
                    domainHearing.getCourtApplications(),
                    domainHearing.getCourtApplicationPartyListingNeeds(),
                    ofNullable(adjournedFromDate),
                    domainHearing.getWeekCommencingStartDate(),
                    domainHearing.getWeekCommencingEndDate(),
                    domainHearing.getWeekCommencingDurationInWeeks(),
                    domainHearing.getHearingDays(),
                    domainHearing.getNonDefaultDays(),
                    domainHearing.getNonSittingDays(),
                    isSlotsBooked,
                    listNextHearing.getHearing().getBookingType(),
                    listNextHearing.getHearing().getPriority(),
                    listNextHearing.getHearing().getSpecialRequirements(),
                    domainHearing.getIsPossibleDisqualification(),
                    domainHearing.getGroupProceedings(),
                    domainHearing.getNumberOfGroupCases()
            );

            final Stream<Object> allocationEvents = hearing.applyAllocationRules(ofNullable(finalBookingReference), false, false, Collections.emptyList(), empty(), commandHearing.getIsGroupProceedings());

            return Stream.of(listingEvents, allocationEvents).flatMap(i -> i);
        });


    }

    @Handles("listing.command.delete-seeded-hearing")
    public void deleteSeededHearing(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.delete-seeded-hearing' received with payload {}", command.toObfuscatedDebugString());
        }
        final DeleteSeededHearing deleteSeededHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), DeleteSeededHearing.class);
        final UUID hearingId = deleteSeededHearing.getHearingId();
        final UUID seedingHearingId = deleteSeededHearing.getSeedingHearingId();

        updateHearingEventStream(command, deleteSeededHearing.getHearingId(), (Hearing hearing) -> hearing.deleteHearing(seedingHearingId, hearingId));
    }

    @Handles("listing.command.delete-next-hearings")
    public void deleteNextHearings(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();
        final DeleteNextHearings deleteNextHearings = jsonObjectConverter.convert(payload, DeleteNextHearings.class);
        final SeedingHearing seedingHearing = deleteNextHearings.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'deleting next hearings for hearingId: {} hearingDay: {}", seedingHearingId, hearingDay);
        }

        updateSeedAggregateEventStream(command, seedingHearing.getSittingDay(), seedingHearingId, (SeedHearingAggregate seedHearingAggregate) ->
                seedHearingAggregate.deleteNextHearings(seedingHearingId, hearingDay));

    }

    @Handles("listing.command.change-next-hearing-day")
    public void changeNextHearingDay(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final ChangeNextHearingDay changeNextHearingDay = jsonObjectConverter.convert(payload, ChangeNextHearingDay.class);

        final UUID hearingId = changeNextHearingDay.getHearingId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.change-next-hearing-day hearingId: {} ", hearingId);
        }

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.changeNextHearingDate(hearingId));
    }

    @Handles("listing.command.remove-offences-from-existing-hearing")
    public void removeOffencesFromExistingHearing(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.remove-offences-from-existing-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final RemoveOffencesFromExistingHearing removeOffencesFromExistingHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), RemoveOffencesFromExistingHearing.class);
        final UUID hearingId = removeOffencesFromExistingHearing.getHearingId();
        final UUID seedingHearingId = removeOffencesFromExistingHearing.getSeedingHearingId();

        updateHearingEventStream(command, removeOffencesFromExistingHearing.getHearingId(), (Hearing hearing) -> hearing.removeOffencesFromExistingHearing(seedingHearingId, hearingId));
    }

    @Handles("listing.command.remove-selected-offences-from-existing-hearing")
    public void removeSelectedOffencesFromExistingHearing(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.remove-selected-offences-from-existing-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final RemoveSelectedOffencesFromExistingHearing removeSelectedOffencesFromExistingHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), RemoveSelectedOffencesFromExistingHearing.class);
        final UUID hearingId = removeSelectedOffencesFromExistingHearing.getHearingId();

        updateHearingEventStream(command, hearingId, (Hearing hearing) -> hearing.removeSelectedOffencesFromExistingHearing(hearingId, removeSelectedOffencesFromExistingHearing.getOffenceIds(), Hearing.SOURCE_HEARING, false));
    }


    private List<uk.gov.moj.cpp.listing.domain.NonDefaultDay> convertNonDefaultDaysToDomain(final List<uk.gov.justice.listing.commands.NonDefaultDay> commandDefaultDays) {
        List<uk.gov.moj.cpp.listing.domain.NonDefaultDay> domainDefaultDays = Collections.emptyList();
        if (commandDefaultDays != null && !commandDefaultDays.isEmpty()) {
            domainDefaultDays = commandDefaultDays.stream().map(ndd -> uk.gov.moj.cpp.listing.domain.NonDefaultDay.nonDefaultDay()
                    .withStartTime(ndd.getStartTime())
                    .withDuration(ofNullable(ndd.getDuration()))
                    .withCourtScheduleId(ofNullable(ndd.getCourtScheduleId()))
                    .withCourtRoomId(ofNullable(ndd.getCourtRoomId()))
                    .withOucode(ofNullable(ndd.getOucode()))
                    .withSession(ofNullable(ndd.getSession()))
                    .withRoomId(ofNullable(ndd.getRoomId()))
                    .withCourtCentreId(ofNullable(ndd.getCourtCentreId())).build())
                    .collect(toList());
        }
        return domainDefaultDays;
    }

    private void updateHearingEventStream(final JsonEnvelope command, final UUID hearingId,
                                          final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final Hearing hearing = aggregateService.get(eventStream, Hearing.class);

        final Stream<Object> events = aggregatorFunction.apply(hearing);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    private void updateSeedAggregateEventStream(final JsonEnvelope command, final String sittingDay, final UUID seedHearingId,
                                                final Function<SeedHearingAggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(seedHearingId);
        final SeedHearingAggregate seedHearingAggregate = aggregateService.get(eventStream, SeedHearingAggregate.class);
        final List<DeleteNextHearingRequested> deleteNextHearingRequestedList =
                seedHearingAggregate.retrieveDeleteHearingsList(seedHearingId, sittingDay);

        for (final DeleteNextHearingRequested deleteNextHearingRequested : deleteNextHearingRequestedList) {
            hearingSlotsService.delete(deleteNextHearingRequested.getHearingId());
        }
        final Stream<Object> events = aggregatorFunction.apply(seedHearingAggregate);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

}

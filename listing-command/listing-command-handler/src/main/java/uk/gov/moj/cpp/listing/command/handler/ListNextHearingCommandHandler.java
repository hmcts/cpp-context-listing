package uk.gov.moj.cpp.listing.command.handler;


import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.courts.ChangeNextHearingDay;
import uk.gov.justice.listing.courts.DeleteNextHearings;
import uk.gov.justice.listing.courts.DeleteSeededHearing;
import uk.gov.justice.listing.courts.ListNextHearing;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.RemoveOffencesFromExistingHearing;
import uk.gov.justice.listing.courts.RemoveSelectedOffencesFromExistingHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.service.HmiService;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.NonDefaultDayDurationBuilder;
import uk.gov.moj.cpp.listing.command.utils.RotaSlotToNonDefaultDayConverter;
import uk.gov.moj.cpp.listing.common.azure.ProvisionalBookingService;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188"})
public class ListNextHearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListNextHearingCommandHandler.class);
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private RotaSlotToNonDefaultDayConverter rotaSlotToNonDefaultDayConverter;

    @Inject
    private ProvisionalBookingService provisionalBookingService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NonDefaultDayDurationBuilder nonDefaultDayDurationBuilder;

    @Inject
    private CommandToDomainConverter commandToDomainConverter;

    @Inject
    private HearingTypeFactory hearingTypeFactory;

    @Inject
    private HmiService hmiService;

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
        final List<HearingListingNeeds> hearings = listNextHearingsEnriched.getListNextHearings().getHearings();
        final String adjournedFromDate = listNextHearingsEnriched.getAdjournedFromDate();
        final List<CourtCentreDefaults> courtCentresDetails = listNextHearingsEnriched.getCourtCentresDetails().stream()
                .map(courtCentreDetails -> CourtCentreDefaults.courtCentreDefaults()
                        .withCourtCentreId(courtCentreDetails.getId())
                        .withDefaultDuration(courtCentreDetails.getDefaultDuration())
                        .withDefaultStartTime(courtCentreDetails.getDefaultStartTime())
                        .build())
                .collect(toList());
        final List<UUID> shadowListedOffences = listNextHearingsEnriched.getListNextHearings().getShadowListedOffences();
        updateSeedAggregateEventStream(command, seedingHearingId, (SeedHearingAggregate seedHearingAggregate) ->
                seedHearingAggregate.requestNextHearings(hearings, hearingDay, courtCentresDetails, ofNullable(adjournedFromDate), shadowListedOffences));

    }


    @SuppressWarnings("squid:S3655")
    @Handles("listing.command.list-next-hearing")
    public void listNextCourtHearing(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-next-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(command);
        final ListNextHearing listNextHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), ListNextHearing.class);
        final HearingListingNeeds commandHearing = listNextHearing.getHearing();
        final Map<UUID, CourtCentreDetails> courtCentres = listNextHearing.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));

        List<NonDefaultDay> nonDefaultDaysList = new ArrayList<>();
        UUID bookingReference = null;
        final AtomicBoolean countBasedSlots = new AtomicBoolean(false);
        final boolean isSlotsBooked = isNotEmpty(commandHearing.getBookedSlots());

        final CourtCentre defaultCourtCentre = commandHearing.getCourtCentre();

        if (isSlotsBooked) {
            final List<uk.gov.justice.listing.commands.NonDefaultDay> finalNonDefaultDaysList = nonDefaultDaysList;
            commandHearing.getBookedSlots()
                    .forEach(b -> finalNonDefaultDaysList.add(rotaSlotToNonDefaultDayConverter.convert(b, defaultCourtCentre)));
        } else {
            if (nonNull(listNextHearing.getAdjournedFromDate()) && nonNull(commandHearing.getBookingReference())) {
                bookingReference = commandHearing.getBookingReference();
                final List<CourtSchedule> courtScheduleList = getCourtSchedules(bookingReference.toString());
                if(!courtScheduleList.isEmpty()) {
                    Collections.sort(courtScheduleList);
                    countBasedSlots.set(!courtScheduleList.get(0).getMaxSlots().equals(0));
                } else {
                    countBasedSlots.set(false);
                }
                generateNonDefaultDays(nonDefaultDaysList, courtScheduleList, countBasedSlots.get(), commandHearing);
            }

            if (isSchedulingAndListingUpdateRequired(commandHearing.getJurisdictionType(), nonDefaultDaysList)) {
                nonDefaultDaysList = nonDefaultDayDurationBuilder.updateNonDefaultDayWithNewDuration(nonDefaultDaysList, commandHearing.getEstimatedMinutes());
            }
        }
        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = commandToDomainConverter.convert(commandHearing, convertNonDefaultDaysToDomain(nonDefaultDaysList), listNextHearing.getShadowListedOffences());

        final CourtCentreDetails courtCentre = courtCentres.get(domainHearing.getCourtCentreId());

        final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(courtCentre.getDefaultDuration())
                .withDefaultStartTime(courtCentre.getDefaultStartTime())
                .withCourtCentreId(courtCentre.getId())
                .build();
        final String adjournedFromDate = listNextHearing.getAdjournedFromDate();

        final UUID finalBookingReference = bookingReference;
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
                    hearingTypesIdDurationMap.get(domainHearing.getType().getId().toString()),
                    ofNullable(adjournedFromDate),
                    domainHearing.getWeekCommencingStartDate(),
                    domainHearing.getWeekCommencingEndDate(),
                    domainHearing.getWeekCommencingDurationInWeeks(),
                    domainHearing.getNonDefaultDays(),
                    countBasedSlots.get(),
                    listNextHearing.getHearing().getBookingType(),
                    listNextHearing.getHearing().getPriority(),
                    listNextHearing.getHearing().getSpecialRequirements(),
                    domainHearing.getIsPossibleDisqualification()
            );

            final Stream<Object> allocationEvents = hearing.applyAllocationRules(ofNullable(finalBookingReference), false, false);

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

        updateHearingEventStream(command, deleteSeededHearing.getHearingId(), (Hearing hearing) -> {
            final boolean isHmiEnabled = hmiService.isHmiEnabled(hearing.getCurrentHearingEventState(), command);
            final Stream<Object> deleteHearingForHmiStream = isHmiEnabled ? hearing.deleteHearingForHmi() : Stream.empty();
            return Stream.of(hearing.deleteHearing(seedingHearingId, hearingId), deleteHearingForHmiStream).flatMap(i -> i);
        });
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

        updateSeedAggregateEventStream(command, seedingHearingId, (SeedHearingAggregate seedHearingAggregate) ->
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

        updateHearingEventStream(command, removeOffencesFromExistingHearing.getHearingId(), (Hearing hearing) -> {
            final boolean isHmiEnabled = hmiService.isHmiEnabled(hearing.getCurrentHearingEventState(), command);
            final Stream<Object> events = hearing.removeOffencesFromExistingHearing(seedingHearingId, hearingId);
            return isHmiEnabled ? hearing.raiseUpdateHearingInStagingHmi(events) : events;
        });
    }

    @Handles("listing.command.remove-selected-offences-from-existing-hearing")
    public void removeSelectedOffencesFromExistingHearing(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.remove-selected-offences-from-existing-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final RemoveSelectedOffencesFromExistingHearing removeSelectedOffencesFromExistingHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), RemoveSelectedOffencesFromExistingHearing.class);
        final UUID hearingId = removeSelectedOffencesFromExistingHearing.getHearingId();

        updateHearingEventStream(command, hearingId, (Hearing hearing) -> {
            final Stream<Object> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, removeSelectedOffencesFromExistingHearing.getOffenceIds(), Hearing.SOURCE_HEARING);
            final boolean isHmiEnabled = hmiService.isHmiEnabled(hearing.getCurrentHearingEventState(), command);
            return isHmiEnabled ? hearing.raiseUpdateHearingInStagingHmi(events) : events;
        });
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

    private boolean isSchedulingAndListingUpdateRequired(final uk.gov.justice.core.courts.JurisdictionType jurisdictionType, final List<uk.gov.justice.listing.commands.NonDefaultDay> nonDefaultDays) {
        return MAGISTRATES.equals(jurisdictionType)
                && !nonDefaultDays.isEmpty()
                && nonDefaultDays.stream().anyMatch(ndd -> StringUtils.isNotEmpty(ndd.getCourtScheduleId()));
    }

    private void updateHearingEventStream(final JsonEnvelope command, final UUID hearingId,
                                          final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final Hearing hearing = aggregateService.get(eventStream, Hearing.class);

        final Stream<Object> events = aggregatorFunction.apply(hearing);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    private void updateSeedAggregateEventStream(final JsonEnvelope command, final UUID seedHearingId,
                                                final Function<SeedHearingAggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(seedHearingId);
        final SeedHearingAggregate seedHearingAggregate = aggregateService.get(eventStream, SeedHearingAggregate.class);

        final Stream<Object> events = aggregatorFunction.apply(seedHearingAggregate);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    private List<CourtSchedule> getCourtSchedules(final String bookingId) {
        final Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("bookingIds", bookingId);
        final Response slotsResponse = provisionalBookingService.getSlots(paramsMap);
        final JsonObject resultJson = objectToJsonObjectConverter.convert(slotsResponse.getEntity());

        final List<CourtSchedule> courtScheduleList = new ArrayList<>();

        final JsonArray provisionalSlots = resultJson.getJsonArray("provisionalSlots");
        for (int i = 0; i < provisionalSlots.size(); i++) {

            courtScheduleList.add(jsonObjectConverter.convert(provisionalSlots.getJsonObject(i), CourtSchedule.class));
        }
        return courtScheduleList;
    }

    private void generateNonDefaultDays(final List<uk.gov.justice.listing.commands.NonDefaultDay> nonDefaultDaysList,
                                        final List<CourtSchedule> courtScheduleList,
                                        final Boolean isCountBasedSlot, final HearingListingNeeds commandHearing) {
        final OffsetDateTime startDate = CommandToDomainConverter.getStartDateTime(commandHearing)
                .toInstant().atOffset(ZoneOffset.UTC);
        final int hour = startDate.getHour();
        final long minute = startDate.getMinute();
        courtScheduleList.forEach(cs ->
                nonDefaultDaysList.add(
                        uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(cs.getCourtScheduleId())
                                .withOucode(cs.getOuCode())
                                .withSession(cs.getCourtSession())
                                .withDuration(isCountBasedSlot ? 1 : commandHearing.getEstimatedMinutes())
                                .withCourtRoomId(cs.getCourtRoomNumber()) // for prospect developers, names mismatch but fields point to the same context
                                .withStartTime(isBlank(cs.getHearingStartTime()) ? cs.getSessionDate().atStartOfDay(UTC).withHour(hour).minusMinutes(minute) : ZonedDateTime.parse(cs.getHearingStartTime()))
                                .withRoomId(cs.getCourtRoomId())
                                .withCourtCentreId(cs.getCourtHouseId())
                                .build()
                )

        );
    }


}
